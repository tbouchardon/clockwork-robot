package fr.ksuto.clockwork.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fr.ksuto.clockwork.ClockWorkUI;
import fr.ksuto.clockwork.brain.perception.FishingResult;
import fr.ksuto.prh.PeripheralRobotHelper;
import fr.ksuto.prh.capture.Capture;
import fr.ksuto.prh.capture.Frame;
import fr.ksuto.prh.peripherals.Screen;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

public class Fisherman {
    
    private static final Logger logger = LoggerFactory.getLogger(Fisherman.class);
    
    /**
     * Préparations au plus avant un lancer (leurre puis appât...), contre une règle qui se répéterait sans fin.
     */
    private static final int                  MAX_PREPARATIONS = 3;
    public static final int                   STARTING_HEIGHT = 90;
    public static final int                   STARTING_WIDTH  = 180;
    public static final int                   Y_OFFSET        = -190;
    private final       PeripheralRobotHelper peripherals;
    private final       ClockWorkUI           ui;

    /**
     * Touche à appuyer pour lancer un sort, avec ses modificateurs.
     *
     * @param label nom de la combinaison, pour le journal (SHIFT-R...)
     */
    record CastKey(int keyCode, boolean alt, boolean ctrl, boolean shift, String label) {}

    /**
     * Touche du sort Pêche d'après la grille, vide si elle n'est pas sur une touche décrite.
     */
    private final Supplier<Optional<CastKey>> fishingKey;

    /**
     * Objet à utiliser avant de lancer (leurre, appât...) d'après rotations/peche.yaml, vide si rien à faire.
     */
    private final Supplier<Optional<CastKey>> preparation;

    /**
     * Le personnage incante (pose d'un leurre...), d'après la grille.
     */
    private final BooleanSupplier casting;

    /**
     * Résultat du dernier lancer terminé, publié par l'addon (vide avant la grille v4).
     */
    private final Supplier<Optional<FishingResult>> results;

    /**
     * Résultats des lancers de cette session.
     */
    private final Map<FishingResult.Outcome, Integer> stats = new EnumMap<>(FishingResult.Outcome.class);

    Fisherman(ClockWorkUI ui, PeripheralRobotHelper peripherals, Supplier<Optional<CastKey>> fishingKey, Supplier<Optional<CastKey>> preparation,
              BooleanSupplier casting, Supplier<Optional<FishingResult>> results) {

        this.ui = ui;

        this.peripherals = peripherals;
        this.fishingKey = fishingKey;
        this.preparation = preparation;
        this.casting = casting;
        this.results = results;
    }
    
    /**
     * Position où le bot a laissé la souris : si elle s'en écarte, c'est le joueur qui la bouge, et la pêche s'arrête.
     */
    private Point expected;

    /**
     * @return la souris s'est écartée de plus de 20 pixels de la position où le bot l'a laissée
     */
    private boolean userMoved() {

        Point pointer = MouseInfo.getPointerInfo().getLocation();
        if (expected == null) {expected = pointer;}
        if (pointer.distance(expected) <= 20) {return false;}
        logger.info("Souris bougée par le joueur : fin de la pêche");
        return true;
    }

    /**
     * Attend, en surveillant la souris : le joueur peut arrêter la pêche à tout moment, pas seulement pendant le suivi
     * du bouchon.
     *
     * @return faux si le joueur a bougé la souris
     */
    private boolean pause(int milliseconds) {

        long end = System.currentTimeMillis() + milliseconds;
        while (System.currentTimeMillis() < end) {
            if (userMoved()) {return false;}
            peripherals.robot.delay((int) Math.min(30, Math.max(1, end - System.currentTimeMillis())));
        }
        return !userMoved();
    }

    /**
     * Attend la fin d'une incantation (pose d'un leurre : quelques secondes), 10 s au plus.
     *
     * @return faux si le joueur a bougé la souris
     */
    private boolean waitEndOfCast() {

        long end = System.currentTimeMillis() + 10000;
        while (casting.getAsBoolean() && System.currentTimeMillis() < end) {
            if (!pause(200)) {return false;}
        }
        return pause(300);
    }

    /**
     * Une pêche : leurre et appât si besoin, lancer, repérer le bouchon, cliquer dès la touche.
     *
     * @return faux si le joueur a bougé la souris (fin de la pêche)
     */
    boolean fish() {

        // Leurre, appât... : seulement si la règle le demande (enchantement de la canne ou aura absents), plus de touche fixe
        for (int i = 0; i < MAX_PREPARATIONS; i++) {
            Optional<CastKey> item = preparation.get();
            if (item.isEmpty()) {break;}
            ui.appendLog(item.get().label());
            peripherals.getKeyboard().pressKey(item.get().keyCode(), item.get().alt(), item.get().ctrl(), item.get().shift());
            if (!pause(1000) || !waitEndOfCast()) {return false;}
        }

        // Image de l'eau avant le lancer : le bouchon sera ce qui est apparu depuis
        Rectangle searchArea = maxSearchArea();
        Frame     before     = Capture.zone(searchArea);

        // Sort Pêche : sa touche d'après la grille (barres modifiables à tout moment), à défaut H
        Optional<CastKey> key = fishingKey.get();
        if (key.isPresent()) {
            ui.appendLog(key.get().label());
            peripherals.getKeyboard().pressKey(key.get().keyCode(), key.get().alt(), key.get().ctrl(), key.get().shift());
        }
        else {
            ui.appendLog("h");
            peripherals.getKeyboard().pressKey(KeyEvent.VK_H);
        }
        if (!pause(2500)) {return false;}

        // Compteur de lancers terminés, lu une fois ce lancer parti : un lancer précédent interrompu par celui-ci est
        // clôturé par l'addon à son démarrage. Le prochain changement annoncera le résultat de ce lancer
        Optional<FishingResult> previous = results.get();

        Optional<Point> found = findBobber(searchArea, before);
        if (userMoved()) {return false;}
        if (found.isEmpty()) {
            logger.debug("Bobber not found =(");
            try {
                ImageIO.write(Capture.zone(searchArea).image(), "png", new File(System.currentTimeMillis() + ".png"));
            }
            catch (IOException ignored) {
                logger.debug("ERROR  : Écriture impossible !");
            }
            return true;
        }

        Point mouse = new Point(found.get().x + 5, found.get().y + 5);
        peripherals.getMouse().move(mouse.x, mouse.y);
        expected = mouse;
        if (!pause(2000)) {return false;}

        // Suivi de la plume rouge : capture de sa seule zone (DXGI : ~0,1 ms), une image toutes les ~15 ms
        BobberDetector.BiteWatcher watcher = new BobberDetector.BiteWatcher(found.get());
        StringBuilder              trace   = new StringBuilder("ms;x;y;pixels;hauteur;verdict\n");
        long                       start   = System.currentTimeMillis();
        long                       traceId = start;
        int                        frames  = 0;

        try {
            while (System.currentTimeMillis() - start < 21000) {
                if (userMoved()) {return false;}

                Frame                              frame    = Capture.zone(watcher.window());
                Optional<BobberDetector.Blob>      measured = BobberDetector.measure(frame, before, watcher.window(), watcher.seed());
                BobberDetector.BiteWatcher.Verdict verdict  = watcher.feed(measured);
                frames++;
                trace.append(System.currentTimeMillis() - start).append(';')
                     .append(measured.map(blob -> String.format(Locale.ROOT, "%.1f;%.1f;%d;%d", blob.x(), blob.y(), blob.count(), blob.height())).orElse(";;0;0"))
                     .append(';').append(verdict).append('\n');

                if (verdict != BobberDetector.BiteWatcher.Verdict.WAITING) {
                    logger.debug("Touche : bouchon {} après {} ms ({} images)", verdict == BobberDetector.BiteWatcher.Verdict.LOST ? "plongé" : "écarté",
                                 System.currentTimeMillis() - start, frames);
                    ui.appendLog(verdict == BobberDetector.BiteWatcher.Verdict.LOST ? "ϡ?" : "ϡ");
                    peripherals.getMouse().clickLeft();
                    return awaitOutcome(previous, traceId, System.currentTimeMillis() - start);
                }
                peripherals.robot.delay(15);
            }
            return awaitOutcome(previous, traceId, -1);
        }
        finally {
            writeTrace(traceId, trace);
        }
    }

    /**
     * Attend le résultat du lancer publié par l'addon (le compteur change 1,5 s après la fin de la canalisation),
     * le journalise avec les statistiques de la session, et l'ajoute à {@value #RESULTS_FILE}.
     *
     * @param previous résultat lu avant le lancer (vide : grille sans résultat de pêche, on attend simplement)
     * @param clickMs  moment du clic depuis le début du suivi, -1 sans clic
     * @return faux si le joueur a bougé la souris
     */
    private boolean awaitOutcome(Optional<FishingResult> previous, long traceId, long clickMs) {

        if (previous.isEmpty()) {return pause(2000);}
        long end = System.currentTimeMillis() + 4000;
        while (System.currentTimeMillis() < end) {
            if (!pause(100)) {return false;}
            Optional<FishingResult> now = results.get();
            if (now.isPresent() && now.get().counter() != previous.get().counter()) {
                record(now.get().outcome(), traceId, clickMs);
                return pause(500);
            }
        }
        logger.debug("Pêche : résultat du lancer non publié par l'addon");
        return true;
    }

    private void record(FishingResult.Outcome outcome, long traceId, long clickMs) {

        stats.merge(outcome, 1, Integer::sum);
        int casts  = stats.values().stream().mapToInt(Integer::intValue).sum();
        int caught = stats.getOrDefault(FishingResult.Outcome.CAUGHT, 0);
        logger.info("Pêche : {}{} — {} prise(s) sur {} lancer(s), {} échappé(s), {} faux clic(s)", label(outcome),
                    clickMs < 0 ? " (sans clic)" : "", caught, casts, stats.getOrDefault(FishingResult.Outcome.ESCAPED, 0),
                    stats.getOrDefault(FishingResult.Outcome.NOT_HOOKED, 0));
        appendResult(Path.of(TRACES_FOLDER), traceId, clickMs, outcome);
    }

    private static String label(FishingResult.Outcome outcome) {

        return switch (outcome) {
            case CAUGHT -> "prise";
            case ESCAPED -> "poisson échappé (clic trop tard)";
            case NOT_HOOKED -> "rien à ferrer (clic trop tôt)";
            case NOTHING -> "rien";
            case NONE -> "?";
        };
    }

    /**
     * Fichier des résultats, à côté des traces : une ligne par lancer (trace, moment du clic, résultat). C'est la
     * vérité terrain des tests de rejeu : une touche manquée ou un faux clic s'y retrouvent sans avoir à les noter.
     */
    static final String RESULTS_FILE = "resultats.csv";

    static void appendResult(Path folder, long traceId, long clickMs, FishingResult.Outcome outcome) {

        try {
            Files.createDirectories(folder);
            Path file = folder.resolve(RESULTS_FILE);
            if (!Files.exists(file)) {Files.writeString(file, "trace;clic_ms;resultat\n", StandardCharsets.UTF_8);}
            Files.writeString(file, traceId + ";" + clickMs + ";" + outcome + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        }
        catch (IOException e) {
            logger.debug("Résultat de pêche non écrit : {}", e.getMessage());
        }
    }

    /**
     * Traces conservées : les plus récentes (quelques Mo au plus).
     */
    static final int KEPT_TRACES = 300;

    static final String TRACES_FOLDER = "traces-peche";

    /**
     * Trace du suivi de chaque lancer (centre, surface et hauteur des plumes à chaque image), pour régler les seuils et
     * enrichir les tests de rejeu (TraceReplayTest) : dossier traces-peche du dossier de lancement, limité aux
     * {@value #KEPT_TRACES} plus récentes.
     */
    private static void writeTrace(long traceId, StringBuilder trace) {

        try {
            Path folder = Files.createDirectories(Path.of(TRACES_FOLDER));
            Files.writeString(folder.resolve(traceId + ".csv"), trace, StandardCharsets.UTF_8);
            try (var files = Files.list(folder)) {
                // Noms horodatés : l'ordre alphabétique est l'ordre chronologique
                List<Path> traces = files.filter(file -> file.toString().endsWith(".csv") && !file.endsWith(RESULTS_FILE)).sorted().toList();
                for (Path old : traces.subList(0, Math.max(0, traces.size() - KEPT_TRACES))) {Files.deleteIfExists(old);}
            }
        }
        catch (IOException e) {
            logger.debug("Trace de pêche non écrite : {}", e.getMessage());
        }
    }

    /**
     * Zone de pêche en vue à la première personne : le tiers central de la largeur, de 35 à 78 % de la hauteur. Le
     * bouchon tombe devant, vers 50 à 75 % selon l'inclinaison de la caméra (vérifié en jeu) ; la zone s'arrête au-dessus
     * de la barre d'incantation et des barres d'action.
     */
    static Rectangle maxSearchArea() {

        return new Rectangle(Screen.SCREEN_WIDTH / 3, Screen.SCREEN_HEIGHT * 35 / 100, Screen.SCREEN_WIDTH / 3, Screen.SCREEN_HEIGHT * 43 / 100);
    }
    
    void leave() {
        
        
        for (int n = 1; n <= 3; n++) {
            peripherals.getKeyboard().pressKey(KeyEvent.VK_END);
            peripherals.robot.delay(300);
        }
        peripherals.getKeyboard().pressKey(KeyEvent.VK_X);
    }
    
    void setup() throws AWTException {
    
        peripherals.robot.delay(100);
        peripherals.robot.mouseMove(Screen.SCREEN_WIDTH / 2, Screen.SCREEN_HEIGHT / 2 + 10);
        expected = new Point(Screen.SCREEN_WIDTH / 2, Screen.SCREEN_HEIGHT / 2 + 10);
        peripherals.robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        peripherals.robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        peripherals.robot.delay(100);
    
        peripherals.getKeyboard().pressKey(KeyEvent.VK_X);
    
        // Vue à la première personne : zoom avant (Origine) jusqu'au bout ; leave() dézoome avec Fin
        for (int n = 1; n <= 20; n++) {
            peripherals.getKeyboard().pressKey(KeyEvent.VK_HOME);
            peripherals.robot.delay(80);
        }
        peripherals.robot.delay(300);
    }
    
    /**
     * Cherche le bouchon dans toute la zone de pêche, par comparaison avec l'image d'avant le lancer : le décor, déjà
     * là, est écarté. Quatre essais : le bouchon peut encore tomber.
     */
    private Optional<Point> findBobber(Rectangle searchArea, Frame before) {

        for (int count = 0; count < 4; count++) {
            Frame           after  = Capture.zone(searchArea);
            Optional<Point> bobber = BobberDetector.locate(after, before, searchArea);
            if (bobber.isPresent()) {
                if (count > 0) {logger.debug("Bouchon trouvé au {}e essai", count + 1);}
                return bobber;
            }
            if (!pause(250)) {break;}
        }
        return Optional.empty();
    }
}
