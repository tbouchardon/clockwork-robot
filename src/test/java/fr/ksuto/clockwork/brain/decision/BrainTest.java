package fr.ksuto.clockwork.brain.decision;

import fr.ksuto.clockwork.brain.data.SpellDatabase;
import fr.ksuto.clockwork.brain.data.SpellDatabaseFixture;
import fr.ksuto.clockwork.brain.perception.GameState;
import fr.ksuto.clockwork.brain.perception.KeyState;

import java.io.IOException;
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

    private final Brain         brain     = new Brain();
    private final SpellDatabase spellbook = SpellDatabaseFixture.create();

    BrainTest() throws IOException {}

    private static KeyState ready(String key, int spellId) {

        return new KeyState(key, spellId, 0, true, KeyState.Range.IN, NEVER, NEVER, false);
    }

    private static GameState state(int recommended, double targetHealth, KeyState... keys) {

        return state(true, true, recommended, targetHealth, keys);
    }

    private static GameState state(boolean aggro, boolean targetInCombat, int recommended, double targetHealth, KeyState... keys) {

        Map<String, KeyState> map = new LinkedHashMap<>();
        for (KeyState key : keys) {map.put(key.key(), key);}
        return new GameState(100, 100, true, true, targetHealth, 0, targetInCombat, true, false, 1, 0, recommended, aggro, false, false, false, false, 0, 0, 7, 262, 1, GameState.Cast.NONE, GameState.TargetCast.NONE, map);
    }

    private Optional<Brain.Decision> decide(String yaml, GameState state) {

        return brain.decide(state, brain.parse(yaml), spellbook);
    }

    @Test
    void rotationNamesItsClass() {

        Rotation elemental = brain.parse("name: Élém\nclass: SHAMAN\nspec: Élémentaire\nrules: []\n");
        Rotation shaman    = brain.parse("class: SHAMAN\nrules: []\n");
        SpellDatabase.Spec spec = new SpellDatabase.Spec(262, "SHAMAN", "Élémentaire", 0);
        SpellDatabase.Spec enhancement = new SpellDatabase.Spec(263, "SHAMAN", "Amélioration", 1);

        assertEquals("Élém", elemental.label());
        assertEquals("SHAMAN", shaman.label());
        assertTrue(elemental.appliesTo("SHAMAN", spec));
        assertFalse(elemental.appliesTo("SHAMAN", enhancement));
        assertFalse(elemental.appliesTo("MAGE", spec));
        assertTrue(shaman.appliesTo("SHAMAN", enhancement), "sans spec : toute la classe");
        assertTrue(brain.parse("class: SHAMAN\nspec: 262\nrules: []\n").appliesTo("SHAMAN", spec), "spec par identifiant");
        assertTrue(brain.parse("class: SHAMAN\nspec: elementaire\nrules: []\n").appliesTo("SHAMAN", spec), "accents indifférents");
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
        
        Rotation rotation = brain.parse(Files.readString(Path.of("rotations", "chaman-elementaire.yaml"), StandardCharsets.UTF_8));
        
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
    void ignoresBlizzardRecommendationWithoutHostileTarget() {

        String yaml = "assisted:\n  follow: true\nrules: []\n";
        Map<String, KeyState> keys = Map.of("1", ready("1", 188196));
        GameState noTarget = new GameState(100, 100, false, false, 0, 0, false, false, false, 0, 0, 188196, true, false, false, false, false, 0, 0, 7, 262, 1, GameState.Cast.NONE, GameState.TargetCast.NONE, keys);

        assertTrue(decide(yaml, noTarget).isEmpty());
    }

    /**
     * État avec les garde-fous : incantation, monture, mort, cible marquée par un autre joueur.
     */
    private static GameState guarded(boolean casting, boolean mounted, boolean dead, boolean tapDenied) {

        Map<String, KeyState> keys = Map.of("1", ready("1", 188196));
        return new GameState(100, 100, true, true, 80, 0, true, true, casting, 1, 0, 188196, true, dead, mounted, tapDenied, false, 0, 0, 7, 262, 1, GameState.Cast.NONE, GameState.TargetCast.NONE, keys);
    }

    @Test
    void safetyGuardsApplyToEveryRotation() {

        String rule     = "rules:\n  - cast: Éclair\n";
        String assisted = "assisted:\n  follow: true\nrules: []\n";

        assertTrue(decide(rule, guarded(false, false, false, false)).isPresent(), "témoin");
        assertTrue(decide(rule, guarded(true, false, false, false)).isEmpty(), "incantation en cours");
        assertTrue(decide(rule, guarded(false, true, false, false)).isEmpty(), "sur une monture");
        assertTrue(decide(rule, guarded(false, false, true, false)).isEmpty(), "mort");
        assertTrue(decide(assisted, guarded(false, false, false, true)).isEmpty(), "cible marquée par un autre joueur");
        assertTrue(decide("rules:\n  - cast: Éclair\n    when: target.hostile\n", guarded(false, false, false, true)).isEmpty(),
                   "target.hostile est faux sur une cible marquée");
    }

    /**
     * Touches : 1 Éclair, 3 Horion de flammes ; le joueur incante ou canalise le sort donné.
     */
    private static GameState casting(GameState.Cast cast, KeyState... keys) {

        Map<String, KeyState> map = new LinkedHashMap<>();
        for (KeyState key : keys) {map.put(key.key(), key);}
        return new GameState(100, 100, true, true, 80, 0, true, true, true, 1, 0, 0, true, false, false, false, false, 0, 0, 7, 262, 1, cast, GameState.TargetCast.NONE, map);
    }

    private static KeyState onCooldown(String key, int spellId) {

        return new KeyState(key, spellId, 5, true, KeyState.Range.IN, NEVER, NEVER, false);
    }

    @Test
    void higherPriorityRuleInterruptsAFillerChannel() {

        Rotation rotation = brain.parse("rules:\n  - cast: Horion de flammes\n    priority: 120\n  - cast: Éclair\n    priority: 10\n");

        // Éclair (canalisé pour l'exemple) est lancé en remplissage, priorité 10
        assertEquals("1", brain.decide(state(0, 80, ready("1", 188196), onCooldown("3", 188389)), rotation, spellbook).orElseThrow().key());

        GameState.Cast channel = new GameState.Cast(188196, true, 3);
        assertTrue(brain.decide(casting(channel, ready("1", 188196), onCooldown("3", 188389)), rotation, spellbook).isEmpty(),
                   "sa propre règle ne relance pas la canalisation");
        assertEquals("3", brain.decide(casting(channel, ready("1", 188196), ready("3", 188389)), rotation, spellbook).orElseThrow().key(),
                     "règle plus prioritaire : on coupe le remplissage");
    }

    @Test
    void highPriorityChannelGoesToTheEnd() {

        Rotation rotation = brain.parse("rules:\n  - cast: Éclair\n    priority: 150\n  - cast: Horion de flammes\n    priority: 120\n");

        assertEquals("1", brain.decide(state(0, 80, ready("1", 188196), ready("3", 188389)), rotation, spellbook).orElseThrow().key());
        assertTrue(brain.decide(casting(new GameState.Cast(188196, true, 3), ready("1", 188196), ready("3", 188389)), rotation, spellbook).isEmpty(),
                   "Horion (120) ne coupe pas le sort prioritaire (150)");
    }

    @Test
    void hardCastWaitsForTheSpellQueueWindow() {

        Rotation rotation = brain.parse("rules:\n  - cast: Éclair\n");

        assertTrue(brain.decide(casting(new GameState.Cast(188196, false, 1.5), ready("1", 188196)), rotation, spellbook).isEmpty(),
                   "WoW refuserait le sort pendant l'incantation");
        assertEquals("1", brain.decide(casting(new GameState.Cast(188196, false, 0.3), ready("1", 188196)), rotation, spellbook).orElseThrow().key(),
                     "dernières 400 ms : le sort suivant part en file d'attente");
    }

    @Test
    void higherPriorityRuleStopsAHardCastFirst() {

        String yaml = "rules:\n  - cast: Horion de flammes\n    priority: 120\n  - cast: Éclair\n    priority: 10\n";
        GameState.Cast boltCasting = new GameState.Cast(188196, false, 1.5);

        // Éclair lancé par sa règle (10) ; pendant l'incantation, Horion (120) redevient disponible
        Rotation rotation = brain.parse(yaml);
        brain.decide(state(0, 80, ready("1", 188196), onCooldown("3", 188389)), rotation, spellbook).orElseThrow();
        Brain.Decision jump = brain.decide(casting(boltCasting, ready("1", 188196), ready("3", 188389)), rotation, spellbook).orElseThrow();
        assertEquals("3", jump.key());
        assertEquals(Rotation.StopCasting.JUMP, jump.stopFirst(), "saut par défaut pour couper l'incantation");

        Rotation back = brain.parse("stopCasting: back\n" + yaml);
        brain.decide(state(0, 80, ready("1", 188196), onCooldown("3", 188389)), back, spellbook).orElseThrow();
        assertEquals(Rotation.StopCasting.BACK, brain.decide(casting(boltCasting, ready("1", 188196), ready("3", 188389)), back, spellbook).orElseThrow().stopFirst());

        brain.decide(state(0, 80, ready("1", 188196), onCooldown("3", 188389)), rotation, spellbook).orElseThrow();
        assertTrue(brain.decide(casting(boltCasting, ready("1", 188196), onCooldown("3", 188389)), rotation, spellbook).isEmpty(),
                   "sa propre règle ne coupe pas l'incantation");
        assertEquals(Rotation.StopCasting.NONE,
                     brain.decide(casting(new GameState.Cast(188196, false, 0.3), ready("1", 188196), onCooldown("3", 188389)), rotation, spellbook)
                          .orElseThrow().stopFirst(), "fin d'incantation : file d'attente, rien à couper");
        assertThrows(IllegalArgumentException.class, () -> brain.parse("stopCasting: danser\nrules: []\n"));
    }

    @Test
    void neverInterruptsAChannelItDidNotStart() {

        Rotation rotation = brain.parse("rules:\n  - cast: Horion de flammes\n    priority: 120\n");

        assertTrue(brain.decide(casting(new GameState.Cast(188196, true, 3), ready("3", 188389)), rotation, spellbook).isEmpty(),
                   "canalisation lancée à la main");
        assertTrue(brain.decide(casting(GameState.Cast.NONE, ready("3", 188389)), rotation, spellbook).isEmpty(), "sort en cours inconnu");
    }

    @Test
    void conditionsSeeTheSpellInProgress() {

        Rotation rotation = brain.parse("rules:\n  - cast: Éclair\n    when: \"player.castSpell == 'Éclair' && !player.channeling && player.castRemaining < 0.4\"\n");

        assertEquals("1", brain.decide(casting(new GameState.Cast(188196, false, 0.2), ready("1", 188196)), rotation, spellbook).orElseThrow().key());
    }

    @Test
    void conditionsSeeTheTargetCast() {

        Map<String, KeyState> keys = Map.of("1", ready("1", 188196));
        String yaml = "rules:\n  - cast: Éclair\n    when: \"target.casting && target.interruptible && target.castSpell == 'Afflux de soins'\"\n";
        GameState healing = new GameState(100, 100, true, true, 80, 0, true, true, false, 1, 0, 0, true, false, false, false, false, 0, 0, 7, 262, 1,
                                          GameState.Cast.NONE, new GameState.TargetCast(true, 8004, true), keys);
        GameState idle    = new GameState(100, 100, true, true, 80, 0, true, true, false, 1, 0, 0, true, false, false, false, false, 0, 0, 7, 262, 1,
                                          GameState.Cast.NONE, GameState.TargetCast.NONE, keys);

        assertEquals("1", decide(yaml, healing).orElseThrow().key(), "la cible se soigne : on l'interrompt");
        assertTrue(decide(yaml, idle).isEmpty());
    }

    @Test
    void conditionsSeePlayerBuffs() {

        String   yaml   = "rules:\n  - cast: Éclair\n    when: \"!spell.buffActive('Éclair')\"\n";
        KeyState buffed = new KeyState("1", 188196, 0, true, KeyState.Range.IN, NEVER, NEVER, false, true);

        assertTrue(decide(yaml, state(0, 80, buffed)).isEmpty(), "buff déjà actif");
        assertEquals("1", decide(yaml, state(0, 80, ready("1", 188196))).orElseThrow().key());
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

    private static KeyState item(String key, int itemId, int count, double cooldown) {

        return new KeyState(key, 0, cooldown, count > 0, KeyState.Range.NONE, NEVER, NEVER, false, false, itemId, count);
    }

    @Test
    void usesItemsByName() {

        String yaml = """
                rules:
                  - use: Pierre de soins
                    when: "player.health < 40 && item.count('Pierre de soins') > 0"
                    priority: 200
                  - cast: Éclair
                """;
        GameState hurt = new GameState(30, 100, true, true, 80, 0, true, true, false, 1, 0, 0, true, false, false, false, false, 0, 0, 7, 262, 1,
                                       GameState.Cast.NONE, GameState.TargetCast.NONE, Map.of("1", ready("1", 188196), "Q", item("Q", 19004, 1, 0)));

        Brain.Decision decision = decide(yaml, hurt).orElseThrow();
        assertEquals("Q", decision.key(), "une des « Pierre de soins » du jeu, quel que soit son identifiant");
        assertEquals("règle objet « Pierre de soins »", decision.reason());

        GameState used = new GameState(30, 100, true, true, 80, 0, true, true, false, 1, 0, 0, true, false, false, false, false, 0, 0, 7, 262, 1,
                                       GameState.Cast.NONE, GameState.TargetCast.NONE, Map.of("1", ready("1", 188196), "Q", item("Q", 19004, 1, 50)));
        assertEquals("1", decide(yaml, used).orElseThrow().key(), "objet en recharge");
        assertEquals("1", decide(yaml, state(0, 80, ready("1", 188196))).orElseThrow().key(), "objet absent des barres");
    }

    @Test
    void ruleNeedsCastOrUseButNotBoth() {

        assertThrows(IllegalArgumentException.class, () -> brain.parse("rules:\n  - cast: Éclair\n    use: Pierre de soins\n"));
        assertThrows(IllegalArgumentException.class, () -> brain.parse("activity: danse\nrules: []\n"));
    }
}
