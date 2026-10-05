package fr.ksuto.clockwork.brain.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class SpellDatabaseLoaderTest {

    private static final GameInstall RETAIL = new GameInstall("wow", "12.1.0.69933", "frFR");

    @TempDir
    Path cache;

    /**
     * Source factice : seules les versions disponibles se chargent ; sans réseau, seules celles déjà en cache.
     */
    private SpellDatabaseLoader loader(Set<String> available, boolean online, List<String> known) {

        SpellDatabaseLoader.TableSource source = tables -> {
            boolean cached = Files.exists(tables.folder().resolve(SpellDatabaseLoader.COMPLETE));
            if (cached) {return SpellDatabase.EMPTY;}
            if (!online) {throw new IOException("pas de réseau");}
            if (!available.contains(tables.install().build())) {throw new WagoTables.MissingTableException("400");}
            return SpellDatabase.EMPTY;
        };
        return new SpellDatabaseLoader(cache, product -> known, source);
    }

    private void cached(GameInstall install) throws IOException {

        Path folder = Files.createDirectories(WagoTables.folder(cache, install));
        Files.createFile(folder.resolve(SpellDatabaseLoader.COMPLETE));
    }

    @Test
    void loadsTheExactBuildAndPrunesOlderBuildsOfTheSameProductOnly() throws Exception {

        GameInstall older   = RETAIL.withBuild("12.1.0.69875");
        GameInstall vanilla = new GameInstall("wow_classic_era", "1.15.9.70003", "frFR");
        cached(older);
        cached(vanilla);

        SpellDatabaseLoader.Loaded loaded = loader(Set.of("12.1.0.69933"), true, List.of()).load(RETAIL);

        assertEquals(RETAIL, loaded.install());
        assertTrue(Files.exists(WagoTables.folder(cache, RETAIL).resolve(SpellDatabaseLoader.COMPLETE)));
        assertFalse(Files.exists(cache.resolve("wow").resolve("12.1.0.69875")), "ancienne version retail supprimée");
        assertTrue(Files.exists(WagoTables.folder(cache, vanilla)), "le cache vanilla est conservé");
    }

    @Test
    void unknownBuildFallsBackToTheNearestKnownBuild() throws Exception {

        List<String> known = List.of("12.1.0.69933", "12.1.0.69875", "12.0.7.68000");

        SpellDatabaseLoader.Loaded loaded = loader(Set.copyOf(known), true, known).load(RETAIL.withBuild("12.1.0.69900"));

        assertEquals("12.1.0.69875", loaded.install().build());
    }

    @Test
    void withoutNetworkFallsBackToTheLatestCachedBuild() throws Exception {

        cached(RETAIL.withBuild("12.0.7.68000"));
        cached(RETAIL.withBuild("12.1.0.69875"));
        cached(new GameInstall("wow", "12.1.0.69933", "enUS")); // autre langue : ignorée

        SpellDatabaseLoader.Loaded loaded = loader(Set.of(), false, List.of()).load(RETAIL);

        assertEquals("12.1.0.69875", loaded.install().build());
    }

    @Test
    void failsWhenNothingIsAvailable() {

        assertThrows(IOException.class, () -> loader(Set.of(), false, List.of()).load(RETAIL));
    }

    @Test
    void nearestPrefersTheLatestBuildNotAboveOurs() {

        List<String> known = List.of("12.1.0.69933", "12.1.0.69875", "1.15.9.70003");

        assertEquals(Optional.of("12.1.0.69875"), SpellDatabaseLoader.nearest("12.1.0.69900", known));
        assertEquals(Optional.of("12.1.0.69933"), SpellDatabaseLoader.nearest("12.2.0.1", known));
        assertEquals(Optional.of("1.15.9.70003"), SpellDatabaseLoader.nearest("1.0.0.1", known));
        assertEquals(Optional.empty(), SpellDatabaseLoader.nearest("12.1.0.1", List.of()));
        assertTrue(SpellDatabaseLoader.compareVersions("12.1.0.9", "12.1.0.10") < 0, "comparaison numérique, pas alphabétique");
    }

    @Test
    void readsKnownBuildsOfAProductFromTheWagoApi() {

        String json = """
                {"wow":[{"product":"wow","version":"12.1.0.69933","created_at":"x"},{"product":"wow","version":"12.1.0.69875"}],
                 "wow_classic_era":[{"product":"wow_classic_era","version":"1.15.9.70003"}]}
                """;

        assertEquals(List.of("12.1.0.69933", "12.1.0.69875"), WagoTables.buildsOf(json, "wow"));
        assertEquals(List.of("1.15.9.70003"), WagoTables.buildsOf(json, "wow_classic_era"));
    }
}
