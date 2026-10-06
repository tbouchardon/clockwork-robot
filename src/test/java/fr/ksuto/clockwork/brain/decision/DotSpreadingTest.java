package fr.ksuto.clockwork.brain.decision;

import fr.ksuto.clockwork.brain.data.SpellDatabase;
import fr.ksuto.clockwork.brain.data.SpellDatabaseFixture;
import fr.ksuto.clockwork.brain.perception.GameState;
import fr.ksuto.clockwork.brain.perception.Group;
import fr.ksuto.clockwork.brain.perception.KeyState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Répartition des DoT entre plusieurs ennemis : chaque cible est reconnue par son identifiant (grille v4), le cerveau
 * retient ses lancers sur chacune, et passe à la suivante (Tab) tant qu'il en reste à affliger.
 */
class DotSpreadingTest {

    private static final double NEVER = Double.POSITIVE_INFINITY;
    private static final int    UNSTABLE_AFFLICTION = 1259790, AGONY = 980, CORRUPTION = 172, SHADOW_BOLT = 686;

    private final AtomicLong    now       = new AtomicLong(1_000_000);
    private final Brain         brain     = new Brain(now::get);
    private final SpellDatabase spellbook = SpellDatabaseFixture.create();
    private final Rotation      rotation  = brain.parse(Files.readString(Path.of("rotations", "demoniste-affliction.yaml"), StandardCharsets.UTF_8));

    DotSpreadingTest() throws IOException {}

    private static KeyState key(String key, int spellId, double sinceCastOnTarget) {

        return new KeyState(key, spellId, 0, true, KeyState.Range.IN, sinceCastOnTarget, sinceCastOnTarget, false);
    }

    /**
     * Cible {@code targetId}, {@code enemies} ennemis en combat ; Agonie et Corruption lancées sur cette cible il y a tant
     * de secondes (d'après l'addon, qui les retrouve par cible). Affliction instable en recharge.
     */
    private Optional<Brain.Decision> decide(int targetId, int enemies, double agony, double corruption) {

        Map<String, KeyState> keys = new LinkedHashMap<>();
        keys.put("1", new KeyState("1", UNSTABLE_AFFLICTION, 20, true, KeyState.Range.IN, NEVER, NEVER, false));
        keys.put("2", key("2", AGONY, agony));
        keys.put("3", key("3", CORRUPTION, corruption));
        keys.put("4", key("4", SHADOW_BOLT, NEVER));
        GameState state = new GameState(100, 100, true, true, 80, 0, true, true, false, enemies, 0, 0, true, false, false, false, false, 0, 0, 9,
                                        265, 1, GameState.Cast.NONE, GameState.TargetCast.NONE, keys, Group.NONE, 0, targetId);
        return brain.decide(state, rotation, spellbook);
    }

    private String action(int targetId, int enemies, double agony, double corruption) {

        return decide(targetId, enemies, agony, corruption).map(decision -> decision.key().equals(Brain.NEXT_TARGET) ? "Tab"
                                                                                                                     : String.valueOf(decision.spellId()))
                                                           .orElse("");
    }

    @Test
    void spreadsAgonyAndCorruptionOverEveryEnemyThenFills() {

        // Ennemi A : Agonie, Corruption, puis Tab (B sans Agonie)
        assertEquals(String.valueOf(AGONY), action(0xA, 2, NEVER, NEVER));
        assertEquals(String.valueOf(CORRUPTION), action(0xA, 2, 1, NEVER));
        assertEquals("Tab", action(0xA, 2, 2, 1));

        // Ennemi B : idem ; les deux portent Agonie : remplissage, pas de Tab
        now.addAndGet(2_000);
        assertEquals(String.valueOf(AGONY), action(0xB, 2, NEVER, NEVER));
        assertEquals(String.valueOf(CORRUPTION), action(0xB, 2, 1, NEVER));
        assertEquals(String.valueOf(SHADOW_BOLT), action(0xB, 2, 2, 1), "tous affligés");

        // 13 s plus tard, l'Agonie de A expire (14 s) : on y retourne
        now.addAndGet(13_000);
        assertEquals("Tab", action(0xB, 2, 4, 5));
    }

    @Test
    void aloneTheRotationNeverTabs() {

        assertEquals(String.valueOf(SHADOW_BOLT), action(0xA, 1, 2, 1));
        assertEquals(String.valueOf(SHADOW_BOLT), action(0, 1, 2, 1), "grille sans identifiant de cible");
    }

    @Test
    void waitsForTheGridBeforeTabbingAgain() {

        assertEquals("Tab", action(0xA, 3, 2, 1));
        assertEquals(String.valueOf(SHADOW_BOLT), action(0xA, 3, 2, 1), "Tab à l'instant : la grille ne décrit pas encore la nouvelle cible");
        now.addAndGet(Brain.NEXT_TARGET_DELAY);
        assertEquals("Tab", action(0xA, 3, 2, 1));
    }

    @Test
    void actionMustBeKnown() {

        assertThrows(IllegalArgumentException.class, () -> brain.parse("rules:\n  - action: danser\n"));
        assertThrows(IllegalArgumentException.class, () -> brain.parse("rules:\n  - action: next-target\n    cast: Éclair\n"));
    }
}
