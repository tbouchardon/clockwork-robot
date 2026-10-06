package fr.ksuto.clockwork.brain.decision;

import fr.ksuto.clockwork.brain.data.SpellDatabase;
import fr.ksuto.clockwork.brain.data.SpellDatabaseFixture;
import fr.ksuto.clockwork.brain.perception.GameState;
import fr.ksuto.clockwork.brain.perception.Group;
import fr.ksuto.clockwork.brain.perception.KeyState;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Règles {@code on} : soins sur les membres du groupe (grille v4), mode soigneur.
 */
class GroupHealingTest {

    private static final double NEVER = Double.POSITIVE_INFINITY;
    private static final int    HEAL  = 8004;   // Afflux de soins
    private static final int    BOLT  = 188196; // Éclair

    private final AtomicLong    now       = new AtomicLong(1_000_000);
    private final Brain         brain     = new Brain(now::get);
    private final SpellDatabase spellbook = SpellDatabaseFixture.create();

    GroupHealingTest() throws IOException {}

    private static KeyState ready(String key, int spellId) {

        return new KeyState(key, spellId, 0, true, KeyState.Range.IN, NEVER, NEVER, false);
    }

    private static Group.Member member(int slot, double health, Group.Role role) {

        return new Group.Member(slot, health, true, false, false, slot == 1, role);
    }

    private static GameState state(boolean healerMode, boolean hasTarget, List<Group.Member> members, KeyState... keys) {

        Map<String, KeyState> map = new LinkedHashMap<>();
        for (KeyState key : keys) {map.put(key.key(), key);}
        return new GameState(100, 100, hasTarget, hasTarget, 80, 0, true, true, false, 1, 0, 0, true, false, false, false, false, 0, 0, 7, 264, 1,
                             GameState.Cast.NONE, GameState.TargetCast.NONE, map, new Group(healerMode, false, members));
    }

    private static final List<Group.Member> PARTY = List.of(member(1, 90, Group.Role.HEALER), member(2, 40, Group.Role.TANK),
                                                            member(3, 25, Group.Role.DAMAGER), member(4, 70, Group.Role.DAMAGER));

    private Optional<Brain.Decision> decide(String yaml, GameState state) {

        return brain.decide(state, brain.parse(yaml), spellbook);
    }

    @Test
    void healsTheMostWoundedMemberMatchingTheCondition() {

        String yaml = "rules:\n  - cast: Afflux de soins\n    on: lowest\n    when: \"member.health < 50\"\n";

        Brain.Decision decision = decide(yaml, state(true, true, PARTY, ready("Q", HEAL))).orElseThrow();
        assertEquals("Q", decision.key());
        assertEquals(3, decision.member(), "le plus blessé : 25 %");
        assertTrue(decision.returnToTarget(), "retour à la cible précédente par défaut");

        String tankOnly = "rules:\n  - cast: Afflux de soins\n    on: lowest\n    when: \"member.health < 50 && member.tank\"\n";
        assertEquals(2, decide(tankOnly, state(true, true, PARTY, ready("Q", HEAL))).orElseThrow().member(),
                     "la condition est évaluée membre par membre, du plus blessé au moins blessé");
    }

    @Test
    void healingOthersNeedsHealerModeUnlessAlways() {

        String yaml = "rules:\n  - cast: Afflux de soins\n    on: lowest\n    when: \"member.health < 50\"\n";
        assertTrue(decide(yaml, state(false, true, PARTY, ready("Q", HEAL))).isEmpty(), "hors mode soigneur");

        String emergency = "rules:\n  - cast: Afflux de soins\n    on: lowest\n    always: true\n    when: \"member.health < 30\"\n";
        assertEquals(3, decide(emergency, state(false, true, PARTY, ready("Q", HEAL))).orElseThrow().member(), "urgence");

        String self = "rules:\n  - cast: Afflux de soins\n    on: self\n    when: \"member.health < 95\"\n";
        assertEquals(1, decide(self, state(false, true, PARTY, ready("Q", HEAL))).orElseThrow().member(), "soi-même : toujours");
    }

    @Test
    void picksTheRoleAskedFor() {

        String tank = "rules:\n  - cast: Afflux de soins\n    on: tank\n";
        assertEquals(2, decide(tank, state(true, true, PARTY, ready("Q", HEAL))).orElseThrow().member());

        String healer = "rules:\n  - cast: Afflux de soins\n    on: healer\n";
        assertEquals(1, decide(healer, state(true, true, PARTY, ready("Q", HEAL))).orElseThrow().member());
    }

    @Test
    void skipsDeadOfflineAndOutOfRangeMembers() {

        List<Group.Member> members = List.of(member(1, 90, Group.Role.NONE),
                                             new Group.Member(2, 0, true, true, false, false, Group.Role.NONE),
                                             new Group.Member(3, 10, true, false, true, false, Group.Role.NONE),
                                             new Group.Member(4, 20, false, false, false, false, Group.Role.NONE),
                                             member(5, 60, Group.Role.NONE));
        String yaml = "rules:\n  - cast: Afflux de soins\n    on: lowest\n";

        assertEquals(5, decide(yaml, state(true, true, members, ready("Q", HEAL))).orElseThrow().member());
    }

    @Test
    void keyRangeConcernsTheTargetNotTheMember() {

        KeyState outOfRangeOfTheEnemy = new KeyState("Q", HEAL, 0, true, KeyState.Range.OUT, NEVER, NEVER, false);
        String   yaml                 = "rules:\n  - cast: Afflux de soins\n    on: lowest\n";

        assertEquals(3, decide(yaml, state(true, true, PARTY, outOfRangeOfTheEnemy)).orElseThrow().member());

        KeyState onCooldown = new KeyState("Q", HEAL, 5, true, KeyState.Range.IN, NEVER, NEVER, false);
        assertTrue(decide(yaml, state(true, true, PARTY, onCooldown)).isEmpty());
    }

    @Test
    void groupConditionsCountWoundedMembers() {

        String yaml = """
                rules:
                  - cast: Afflux de soins
                    on: lowest
                    when: "healer && group.below(75) >= 3 && group.size == 4 && group.avgHealth < 60"
                """;

        assertTrue(decide(yaml, state(true, true, PARTY, ready("Q", HEAL))).isPresent());
        assertTrue(decide(yaml.replace(">= 3", ">= 4"), state(true, true, PARTY, ready("Q", HEAL))).isEmpty());
    }

    @Test
    void remembersSpellsCastOnEachMember() {

        // Récupération façon HoT : pas deux fois sur le même membre en 12 s, on passe au suivant
        String rotation = "rules:\n  - cast: Afflux de soins\n    on: lowest\n    when: \"member.health < 50 && member.sinceCast('Afflux de soins') > 12\"\n";
        Rotation parsed = brain.parse(rotation);
        GameState state = state(true, true, PARTY, ready("Q", HEAL));

        assertEquals(3, brain.decide(state, parsed, spellbook).orElseThrow().member());
        now.addAndGet(2_000);
        assertEquals(2, brain.decide(state, parsed, spellbook).orElseThrow().member(), "membre 3 servi il y a 2 s");
        now.addAndGet(2_000);
        assertTrue(brain.decide(state, parsed, spellbook).isEmpty(), "tous servis");
        now.addAndGet(9_000);
        assertEquals(3, brain.decide(state, parsed, spellbook).orElseThrow().member(), "13 s plus tard");
    }

    @Test
    void healsEvenWhenTheTargetDoesNotAllowAttacking() {

        // Sans aggro, en combat, cible hors combat : on n'attaque pas, mais on soigne
        Map<String, KeyState> keys = new LinkedHashMap<>();
        keys.put("1", ready("1", BOLT));
        keys.put("Q", ready("Q", HEAL));
        GameState state = new GameState(100, 100, true, true, 80, 0, false, true, false, 1, 0, 0, false, false, false, false, false, 0, 0, 7, 264,
                                        1, GameState.Cast.NONE, GameState.TargetCast.NONE, keys, new Group(true, false, PARTY));
        String yaml = "rules:\n  - cast: Éclair\n    priority: 100\n  - cast: Afflux de soins\n    on: lowest\n    priority: 10\n";

        assertEquals("Q", decide(yaml, state).orElseThrow().key());
    }

    @Test
    void returnToTargetIsOptional() {

        String yaml = "returnToTarget: false\nrules:\n  - cast: Afflux de soins\n    on: lowest\n";
        assertFalse(decide(yaml, state(true, true, PARTY, ready("Q", HEAL))).orElseThrow().returnToTarget());

        String byDefault = "rules:\n  - cast: Afflux de soins\n    on: lowest\n";
        assertFalse(decide(byDefault, state(true, false, PARTY, ready("Q", HEAL))).orElseThrow().returnToTarget(),
                    "pas de cible avant : rien à retrouver");
    }

    @Test
    void ruleOnMembersNeedsAVersion4Grid() {

        String yaml = "rules:\n  - cast: Afflux de soins\n    on: self\n";

        assertTrue(decide(yaml, state(true, true, List.of(), ready("Q", HEAL))).isEmpty());
    }

    @Test
    void unknownOnIsRejectedAtLoading() {

        assertThrows(IllegalArgumentException.class, () -> brain.parse("rules:\n  - cast: Soins\n    on: focus\n"));
    }
}
