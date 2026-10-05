package fr.ksuto.clockwork.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fr.ksuto.bot.generated.enums.InterfaceEnum;
import fr.ksuto.clockwork.ClockWorkUI;
import fr.ksuto.prh.PeripheralRobotHelper;
import fr.ksuto.prh.capture.Capture;
import fr.ksuto.prh.capture.Frame;
import fr.ksuto.prh.entities.Position;
import fr.ksuto.prh.helpers.PictureSearch;
import fr.ksuto.prh.peripherals.Screen;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

public class Fisherman {
    
    private static final Logger logger = LoggerFactory.getLogger(Fisherman.class);
    
    public static final int                   BAIT_TIME       = 5 * 60000; // Shift + W
    public static final int                   LURE_TIME       = 10 * 60000; // W
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
    private long     lCurrentBaitTime = 0;
    private long     lCurrentLureTime = 0;
    
    Fisherman(ClockWorkUI ui, PeripheralRobotHelper peripherals, Supplier<Optional<CastKey>> fishingKey) {
        
        this.ui = ui;
        
        this.peripherals = peripherals;
        this.fishingKey = fishingKey;
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
     * Une pêche : leurre et appât si besoin, lancer, repérer le bouchon, cliquer dès la touche.
     *
     * @return faux si le joueur a bougé la souris (fin de la pêche)
     */
    boolean fish() {

        if (new Date().getTime() - lCurrentLureTime > LURE_TIME) {
            lCurrentLureTime = new Date().getTime();
            ui.appendLog("w");
            peripherals.getKeyboard().pressKey(KeyEvent.VK_W);
            if (!pause(3000)) {return false;}
        }

        if (new Date().getTime() - lCurrentBaitTime > BAIT_TIME) {
            lCurrentBaitTime = new Date().getTime();
            ui.appendLog("W");
            peripherals.getKeyboard().pressKey(KeyEvent.VK_W, false, false, true);
            if (!pause(1000)) {return false;}
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
        int                        frames  = 0;

        try {
            while (System.currentTimeMillis() - start < 21000) {
                if (userMoved()) {return false;}

                Frame                              frame    = Capture.zone(watcher.window());
                Optional<BobberDetector.Blob>      measured = BobberDetector.measure(frame, before, watcher.window(), watcher.seed());
                BobberDetector.BiteWatcher.Verdict verdict  = watcher.feed(measured);
                frames++;
                trace.append(System.currentTimeMillis() - start).append(';')
                     .append(measured.map(blob -> "%.1f;%.1f;%d;%d".formatted(blob.x(), blob.y(), blob.count(), blob.height())).orElse(";;0;0"))
                     .append(';').append(verdict).append('\n');

                if (verdict != BobberDetector.BiteWatcher.Verdict.WAITING) {
                    logger.debug("Touche : bouchon {} après {} ms ({} images)", verdict == BobberDetector.BiteWatcher.Verdict.LOST ? "plongé" : "écarté",
                                 System.currentTimeMillis() - start, frames);
                    ui.appendLog(verdict == BobberDetector.BiteWatcher.Verdict.LOST ? "ϡ?" : "ϡ");
                    peripherals.getMouse().clickLeft();
                    return pause(2000);
                }
                peripherals.robot.delay(15);
            }
            return true;
        }
        finally {
            writeTrace(trace);
        }
    }

    /**
     * Trace du suivi de chaque lancer (centre, surface et hauteur de la plume à chaque image), pour régler les seuils :
     * dossier traces-peche du dossier de lancement.
     */
    private static void writeTrace(StringBuilder trace) {

        try {
            Path folder = Files.createDirectories(Path.of("traces-peche"));
            Files.writeString(folder.resolve(System.currentTimeMillis() + ".csv"), trace, StandardCharsets.UTF_8);
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
    
        Position resultsWoW = PictureSearch.getDefault(InterfaceEnum.WOW).search().getFirstResult().getFirstPosition();
        if (resultsWoW != null) {
            int iWoWSize = Screen.SCREEN_WIDTH - (resultsWoW.getX() * 2);
            logger.debug("WoW Width = " + iWoWSize);
        }
    
        
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
