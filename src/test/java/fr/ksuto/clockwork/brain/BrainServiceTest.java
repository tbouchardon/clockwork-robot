package fr.ksuto.clockwork.brain;

import fr.ksuto.clockwork.brain.data.SpellDatabase;
import fr.ksuto.clockwork.brain.data.SpellDatabaseFixture;
import fr.ksuto.clockwork.brain.perception.GameState;
import fr.ksuto.clockwork.brain.perception.KeyState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Choix de la rotation selon la classe et la spécialisation lues dans la grille.
 */
class BrainServiceTest {

    private static final int SHAMAN = 7, DRUID = 11, MAGE = 8, ELEMENTAL = 262, ENHANCEMENT = 263;

    @TempDir
    Path folder;

    private final SpellDatabase spells = SpellDatabaseFixture.create();

    BrainServiceTest() throws IOException {}

    /**
     * Rotations : Élémentaire lance Éclair, toute la classe chaman lance Afflux de soins, le druide Éclat lunaire.
     */
    private BrainService service() throws IOException {

        Path rotations = Files.createDirectories(folder.resolve("rotations"));
        Files.writeString(rotations.resolve("chaman-elementaire.yaml"), "class: SHAMAN\nspec: Élémentaire\nrules:\n  - cast: Éclair\n", StandardCharsets.UTF_8);
        Files.writeString(rotations.resolve("chaman.yaml"), "class: SHAMAN\nrules:\n  - cast: Afflux de soins\n", StandardCharsets.UTF_8);
        Files.writeString(rotations.resolve("druide.yml"), "class: DRUID\nrules:\n  - cast: Éclat lunaire\n", StandardCharsets.UTF_8);
        Files.writeString(rotations.resolve("notes.txt"), "pas une rotation", StandardCharsets.UTF_8);
        return new BrainService(rotations, folder.resolve("rotation.yaml"), spells);
    }

    /**
     * Touches 1 à 3 prêtes : Éclair, Afflux de soins, Éclat lunaire.
     */
    private static GameState character(int classId, int specId) {

        KeyState lightning = new KeyState("1", 188196, 0, true, KeyState.Range.IN, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, false);
        KeyState surge     = new KeyState("2", 8004, 0, true, KeyState.Range.IN, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, false);
        KeyState moonfire  = new KeyState("3", 8921, 0, true, KeyState.Range.IN, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, false);
        return new GameState(100, 100, true, true, 80, 0, true, true, false, 1, 0, 0, true, 0, 0, classId, specId, 1,
                             Map.of("1", lightning, "2", surge, "3", moonfire));
    }

    private static String keyFor(BrainService service, GameState state) {

        return service.decide(state).map(decision -> decision.key()).orElse("");
    }

    @Test
    void specRotationWinsOverClassRotation() throws IOException {

        BrainService service = service();

        assertEquals("1", keyFor(service, character(SHAMAN, ELEMENTAL)));
        assertEquals("2", keyFor(service, character(SHAMAN, ENHANCEMENT)), "pas de rotation Amélioration : celle de la classe");
        assertEquals("3", keyFor(service, character(DRUID, 102)));
    }

    @Test
    void addonDecidesWhenNoRotationFitsTheCharacter() throws IOException {

        BrainService service = service();

        assertTrue(service.hasRotations());
        assertTrue(service.handles(character(SHAMAN, ELEMENTAL)));
        assertFalse(service.handles(character(MAGE, 63)));
        assertFalse(service.handles(character(0, 0)), "classe inconnue");
    }

    @Test
    void rotationYamlIsImposedOnEveryCharacter() throws IOException {

        service();
        Files.writeString(folder.resolve("rotation.yaml"), "rules:\n  - cast: Éclat lunaire\n", StandardCharsets.UTF_8);
        BrainService forced = new BrainService(folder.resolve("rotations"), folder.resolve("rotation.yaml"), spells);

        assertTrue(forced.handles(character(MAGE, 63)));
        assertEquals("3", keyFor(forced, character(SHAMAN, ELEMENTAL)));
    }

    @Test
    void noRotationAtAll() {

        BrainService empty = new BrainService(folder.resolve("absent"), folder.resolve("rotation.yaml"), spells);

        assertFalse(empty.hasRotations());
        assertFalse(empty.handles(character(SHAMAN, ELEMENTAL)));
    }
}
