package fr.ksuto.clockwork.activity;

import fr.ksuto.clockwork.brain.perception.FishingResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class FishingResultsTest {

    @TempDir
    Path folder;

    @Test
    void eachCastAddsALineNextToItsTrace() throws IOException {

        Fisherman.appendResult(folder, 1759740000000L, 5230, FishingResult.Outcome.CAUGHT);
        Fisherman.appendResult(folder, 1759740030000L, -1, FishingResult.Outcome.NOTHING);

        List<String> lines = Files.readAllLines(folder.resolve(Fisherman.RESULTS_FILE), StandardCharsets.UTF_8);
        assertEquals(List.of("trace;clic_ms;resultat", "1759740000000;5230;CAUGHT", "1759740030000;-1;NOTHING"), lines);
    }
}
