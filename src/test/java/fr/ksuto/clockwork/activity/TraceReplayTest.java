package fr.ksuto.clockwork.activity;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rejoue des lancers réels (traces-peche, mesures image par image) avec la détection de touche actuelle : le clic doit
 * partir au moment de la vraie touche, observée en jeu. Protège les réglages contre les régressions ; un lancer mal
 * détecté en jeu rejoint ce jeu de données avec la bonne réponse (src/test/resources/fishing/traces/attendu.csv).
 */
class TraceReplayTest {

    /**
     * Tolérance : le clic peut partir un peu avant le clic d'origine (règles affinées depuis), pas après.
     */
    private static final long EARLIER = 200;
    private static final long LATER   = 40;

    private record Frame(long ms, Optional<BobberDetector.Blob> blob) {}

    private static List<String> lines(String resource) throws IOException {

        try (InputStream stream = TraceReplayTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().toList();
        }
    }

    /**
     * Trace : ms;x;y;pixels;hauteur;verdict (décimales à virgule ou à point selon la version qui l'a écrite).
     */
    private static List<Frame> trace(String name) throws IOException {

        List<Frame> frames = new ArrayList<>();
        for (String line : lines("/fishing/traces/" + name + ".csv").subList(1, lines("/fishing/traces/" + name + ".csv").size())) {
            String[] cells = line.split(";", -1);
            long     ms    = Long.parseLong(cells[0]);
            if (cells[1].isEmpty()) {
                frames.add(new Frame(ms, Optional.empty()));
            }
            else {
                frames.add(new Frame(ms, Optional.of(new BobberDetector.Blob(Double.parseDouble(cells[1].replace(',', '.')),
                                                                             Double.parseDouble(cells[2].replace(',', '.')),
                                                                             Integer.parseInt(cells[3]), Integer.parseInt(cells[4])))));
            }
        }
        return frames;
    }

    /**
     * @return l'instant du clic avec la détection actuelle, ou -1 si elle ne clique pas
     */
    private static long replay(List<Frame> frames) {

        BobberDetector.Blob first = frames.stream().flatMap(frame -> frame.blob().stream()).findFirst().orElseThrow();
        BobberDetector.BiteWatcher watcher = new BobberDetector.BiteWatcher(new Point((int) Math.round(first.x()), (int) Math.round(first.y())));
        for (Frame frame : frames) {
            if (watcher.feed(frame.blob()) != BobberDetector.BiteWatcher.Verdict.WAITING) {return frame.ms();}
        }
        return -1;
    }

    @Test
    void realCastsClickAtTheRealBite() throws IOException {

        List<String> failures = new ArrayList<>();
        int          checked  = 0;
        for (String line : lines("/fishing/traces/attendu.csv")) {
            if (line.isBlank() || line.startsWith("#")) {continue;}
            String[] cells    = line.split(";", 3);
            long     expected = Long.parseLong(cells[1]);
            long     clicked  = replay(trace(cells[0]));
            if (clicked < expected - EARLIER || clicked > expected + LATER) {
                failures.add("%s (%s) : clic attendu vers %d ms, obtenu %s".formatted(cells[0], cells[2], expected, clicked < 0 ? "aucun" : clicked + " ms"));
            }
            checked++;
        }
        assertTrue(checked >= 27, "jeu de données incomplet : " + checked);
        assertTrue(failures.isEmpty(), "Lancers réels mal détectés :\n" + String.join("\n", failures));
    }
}
