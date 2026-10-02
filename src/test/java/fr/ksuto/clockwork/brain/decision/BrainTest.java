package fr.ksuto.clockwork.brain.decision;

import fr.ksuto.clockwork.brain.perception.GameState;
import fr.ksuto.clockwork.brain.perception.KeyState;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BrainTest {

    private static final double NEVER = Double.POSITIVE_INFINITY;

    private final Brain     brain     = new Brain();
    private final Spellbook spellbook = Spellbook.parse(SpellbookTest.SAVED_VARIABLES);

    private static KeyState ready(String key, int spellId) {

        return new KeyState(key, spellId, 0, true, KeyState.Range.IN, NEVER, NEVER, false);
    }

    private static GameState state(int recommended, double targetHealth, KeyState... keys) {

        return state(true, true, recommended, targetHealth, keys);
    }

    private static GameState state(boolean aggro, boolean targetInCombat, int recommended, double targetHealth, KeyState... keys) {

        Map<String, KeyState> map = new LinkedHashMap<>();
        for (KeyState key : keys) {map.put(key.key(), key);}
        return new GameState(100, 100, true, true, targetHealth, 0, targetInCombat, true, false, 1, 0, recommended, aggro, 1, map);
    }

    private Optional<Brain.Decision> decide(String yaml, GameState state) {

        return brain.decide(state, brain.parse(yaml), spellbook);
    }

    @Test
    void highestPriorityApplicableRuleWins() {

        String yaml = """
                rules:
                  - cast: Éclair
                    priority: 10
                  - cast: Horion de flammes
                    priority: 120
                """;

        Brain.Decision decision = decide(yaml, state(0, 80, ready("1", 188196), ready("3", 188389))).orElseThrow();

        assertEquals("3", decision.key());
        assertEquals(120, decision.priority());
    }

    @Test
    void conditionsUseGameStateAndSpellHistory() {

        String yaml = """
                rules:
                  - cast: Horion de flammes
                    when: "spell.sinceCastOnTarget('Horion de flammes') > 15"
                    priority: 120
                  - cast: Éclair
                    when: "target.hostile && player.combat"
                    priority: 10
                """;
        KeyState recentlyCast = new KeyState("3", 188389, 0, true, KeyState.Range.IN, 4, 4, false);

        assertEquals("1", decide(yaml, state(0, 80, ready("1", 188196), recentlyCast)).orElseThrow().key(),
                     "DoT posé il y a 4 s : on ne le réapplique pas");
        assertEquals("3", decide(yaml, state(0, 80, ready("1", 188196), ready("3", 188389))).orElseThrow().key(),
                     "jamais posé sur la cible : on l'applique");
    }

    @Test
    void skipsSpellsThatAreNotReady() {

        String yaml = """
                rules:
                  - cast: Horion de flammes
                    priority: 120
                  - cast: Éclair
                    priority: 10
                """;
        KeyState onCooldown = new KeyState("3", 188389, 4.5, true, KeyState.Range.IN, NEVER, NEVER, false);
        KeyState outOfRange = new KeyState("3", 188389, 0, true, KeyState.Range.OUT, NEVER, NEVER, false);

        assertEquals("1", decide(yaml, state(0, 80, ready("1", 188196), onCooldown)).orElseThrow().key());
        assertEquals("1", decide(yaml, state(0, 80, ready("1", 188196), outOfRange)).orElseThrow().key());
    }

    @Test
    void followsBlizzardRecommendationBetweenRules() {

        String yaml = """
                assisted:
                  follow: true
                  priority: 50
                rules:
                  - cast: Horion de flammes
                    when: "target.health < 30"
                    priority: 120
                  - cast: Éclair
                    priority: 10
                """;
        GameState healthyTarget = state(51505, 80, ready("1", 188196), ready("2", 51505), ready("3", 188389));
        GameState dyingTarget   = state(51505, 20, ready("1", 188196), ready("2", 51505), ready("3", 188389));

        Brain.Decision assisted = decide(yaml, healthyTarget).orElseThrow();
        assertEquals("2", assisted.key(), "recommandation (50) avant la règle de base (10)");
        assertTrue(assisted.reason().contains("Explosion de lave"));
        assertEquals("3", decide(yaml, dyingTarget).orElseThrow().key(), "règle prioritaire (120) avant la recommandation");
    }

    @Test
    void nothingToDoWithoutApplicableRule() {

        String yaml = """
                rules:
                  - cast: Éclair
                    when: "enemies > 3"
                """;

        assertTrue(decide(yaml, state(0, 80, ready("1", 188196))).isEmpty());
    }

    @Test
    void invalidConditionIsIgnoredNotFatal() {

        String yaml = """
                rules:
                  - cast: Horion de flammes
                    when: "spell.unknownMethod('x')"
                    priority: 120
                  - cast: Éclair
                    priority: 10
                """;

        assertEquals("1", decide(yaml, state(0, 80, ready("1", 188196), ready("3", 188389))).orElseThrow().key());
    }

    @Test
    void exampleRotationIsValid() throws Exception {
        
        Rotation rotation = brain.parse(Files.readString(Path.of("rotation.example.yaml"), StandardCharsets.UTF_8));
        
        assertEquals(4, rotation.rules().size());
        assertTrue(rotation.followAssisted());
        assertEquals("Afflux de soins", rotation.rules().getFirst().cast(), "règles triées par priorité");
        
        // Données réelles du chaman : Horion de flamme (variante 470411) sur la touche 4
        KeyState flameShockAlreadyUp = new KeyState("4", 470411, 0, true, KeyState.Range.IN, 5, 5, false);
        assertEquals("1", brain.decide(state(0, 80, ready("1", 188196), flameShockAlreadyUp), rotation, spellbook).orElseThrow().key());
        assertEquals("4", brain.decide(state(0, 80, ready("1", 188196), ready("4", 470411)), rotation, spellbook).orElseThrow().key());
    }
    
    @Test
    void findsTheButtonOfTheRecommendedBaseSpell() {

        // Blizzard recommande la forme de base 73899, le bouton contient Explosion de lave 51505 (cas réel)
        String yaml = "assisted:\n  follow: true\nrules: []\n";

        assertEquals("2", decide(yaml, state(73899, 80, ready("1", 188196), ready("2", 51505))).orElseThrow().key());
    }

    @Test
    void usesSpellsBoundWithModifiers() {

        String yaml = "rules:\n  - cast: Explosion de lave\n    priority: 100\n  - cast: Éclair\n";

        assertEquals("SHIFT-2", decide(yaml, state(0, 80, ready("1", 188196), ready("SHIFT-2", 51505))).orElseThrow().key());
    }

    @Test
    void respectsAggroModeLikeTheAddon() {

        String yaml = "rules:\n  - cast: Éclair\n";

        assertTrue(decide(yaml, state(false, false, 0, 80, ready("1", 188196))).isEmpty(),
                   "sans aggro, en combat : on n'attaque pas une cible hors combat");
        assertTrue(decide(yaml, state(false, true, 0, 80, ready("1", 188196))).isPresent(), "cible en combat : on riposte");
        assertTrue(decide(yaml, state(true, false, 0, 80, ready("1", 188196))).isPresent(), "mode aggro : on attaque");
    }

    @Test
    void waitsUntilTheAddonHasFilledTheKeys() {

        String yaml = "assisted:\n  follow: true\nrules:\n  - cast: Éclair\n";

        assertTrue(decide(yaml, state(188196, 80, new KeyState("1", 0, 0, false, KeyState.Range.NONE, NEVER, NEVER, false))).isEmpty());
    }

    @Test
    void ruleWithoutCastIsRejectedAtLoading() {

        assertThrows(IllegalArgumentException.class, () -> brain.parse("rules:\n  - when: \"true\"\n"));
    }
}
