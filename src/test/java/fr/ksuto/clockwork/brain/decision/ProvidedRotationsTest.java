package fr.ksuto.clockwork.brain.decision;

import fr.ksuto.clockwork.brain.data.SpellDatabase;
import fr.ksuto.clockwork.brain.data.SpellDatabaseFixture;
import fr.ksuto.clockwork.brain.perception.GameState;
import fr.ksuto.clockwork.brain.perception.KeyState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rotations fournies dans rotations/ : toutes valides, et le guerrier crie, frappe et interrompt.
 */
class ProvidedRotationsTest {

    private static final double NEVER = Double.POSITIVE_INFINITY;

    private final Brain         brain = new Brain();
    private final SpellDatabase spells = SpellDatabaseFixture.create();

    ProvidedRotationsTest() throws IOException {}

    private Rotation load(String file) throws IOException {

        return brain.parse(Files.readString(Path.of("rotations", file), StandardCharsets.UTF_8));
    }

    @Test
    void everyProvidedRotationIsValid() throws IOException {

        int count = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(Path.of("rotations"), "*.yaml")) {
            for (Path file : files) {
                Rotation rotation = brain.parse(Files.readString(file, StandardCharsets.UTF_8));
                assertTrue(!rotation.playerClass().isBlank() || !rotation.activity().isEmpty(), file + " : ni classe ni activité");
                assertFalse(rotation.rules().isEmpty(), file + " : aucune règle");
                count++;
            }
        }
        assertTrue(count >= 5);
    }

    private static KeyState key(String key, int spellId, double sinceCastOnTarget) {

        return new KeyState(key, spellId, 0, true, KeyState.Range.IN, sinceCastOnTarget, sinceCastOnTarget, false);
    }

    /**
     * Guerrier : touches 1 Cri de guerre (buff actif ou non), 2 Heurtoir, 3 Volée de coups ; la cible incante ou non.
     */
    private int warriorCasts(Rotation rotation, boolean shoutActive, boolean targetCasting) {

        Map<String, KeyState> keys = new LinkedHashMap<>();
        keys.put("1", new KeyState("1", 6673, 0, true, KeyState.Range.NONE, NEVER, NEVER, false, shoutActive));
        keys.put("2", key("2", 1464, NEVER));
        keys.put("3", key("3", 6552, NEVER));
        GameState state = new GameState(100, 100, true, true, 80, 0, true, true, false, 1, 0, 0, true, false, false, false, false, 0, 0, 1, 72, 1,
                                        GameState.Cast.NONE, targetCasting ? new GameState.TargetCast(true, 0, true) : GameState.TargetCast.NONE,
                                        keys);
        return brain.decide(state, rotation, spells).map(Brain.Decision::spellId).orElse(0);
    }

    @Test
    void warriorShoutsThenFightsAndInterrupts() throws IOException {

        Rotation rotation = load("guerrier.yaml");

        assertEquals(6673, warriorCasts(rotation, false, false), "Cri de guerre absent : priorité 200");
        assertEquals(1464, warriorCasts(rotation, true, false), "buff actif : Heurtoir");
        assertEquals(6552, warriorCasts(rotation, true, true), "cible qui incante : Volée de coups (180) avant Heurtoir");
    }
}
