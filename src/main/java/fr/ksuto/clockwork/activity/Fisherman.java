package fr.ksuto.clockwork.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fr.ksuto.bot.generated.enums.InterfaceEnum;
import fr.ksuto.clockwork.ClockWorkUI;
import fr.ksuto.clockwork.tools.ShowZone;
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
    private ShowZone show;
    
    Fisherman(ClockWorkUI ui, PeripheralRobotHelper peripherals, Supplier<Optional<CastKey>> fishingKey) {
        
        this.ui = ui;
        
        this.peripherals = peripherals;
        this.fishingKey = fishingKey;
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
            peripherals.robot.delay(3000);
        }

        if (new Date().getTime() - lCurrentBaitTime > BAIT_TIME) {
            lCurrentBaitTime = new Date().getTime();
            ui.appendLog("W");
            peripherals.getKeyboard().pressKey(KeyEvent.VK_W, false, false, true);
            peripherals.robot.delay(1000);
        }

        // Image de l'eau avant le lancer : le bouchon sera ce qui est apparu depuis
        Rectangle                 searchArea = maxSearchArea();
        Frame                     before     = Capture.zone(searchArea);
        BobberDetector.Background water      = BobberDetector.Background.of(before, searchArea);

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
        peripherals.robot.delay(2500);

        Optional<Point> found = findBobber(searchArea, before, water);
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
        peripherals.robot.delay(2000);

        // Suivi du bouchon : capture de sa seule zone (DXGI : ~0,1 ms), une image toutes les ~15 ms
        BobberDetector.BiteWatcher watcher = new BobberDetector.BiteWatcher(found.get());
        int                        margin  = BobberDetector.TRACK_RADIUS + 6;
        long                       start   = System.currentTimeMillis();
        int                        frames  = 0;

        while (System.currentTimeMillis() - start < 21000) {
            Point pointer = MouseInfo.getPointerInfo().getLocation();
            if (pointer.distance(mouse) > 20) {
                logger.debug("Mouse moved, exiting. ({} != {})", pointer, mouse);
                return false;
            }

            Point average = watcher.average();
            Frame frame   = Capture.zone(new Rectangle(average.x - margin, average.y - margin, 2 * margin, 2 * margin));
            BobberDetector.BiteWatcher.Verdict verdict = watcher.feed(BobberDetector.track(frame, average, water));
            frames++;

            if (verdict != BobberDetector.BiteWatcher.Verdict.WAITING) {
                logger.debug("Touche : bouchon {} après {} ms ({} images)", verdict == BobberDetector.BiteWatcher.Verdict.LOST ? "disparu" : "écarté",
                             System.currentTimeMillis() - start, frames);
                ui.appendLog(verdict == BobberDetector.BiteWatcher.Verdict.LOST ? "ϡ?" : "ϡ");
                peripherals.getMouse().clickLeft();
                peripherals.robot.delay(2000);
                return true;
            }
            peripherals.robot.delay(15);
        }

        return true;
    }

    /**
     * Zone de pêche en vue à la première personne : le tiers central de la largeur, de 45 à 85 % de la hauteur (le
     * bouchon tombe devant, bas à l'écran ; vérifié en jeu vers 75 %).
     */
    static Rectangle maxSearchArea() {

        return new Rectangle(Screen.SCREEN_WIDTH / 3, Screen.SCREEN_HEIGHT * 45 / 100, Screen.SCREEN_WIDTH / 3, Screen.SCREEN_HEIGHT * 40 / 100);
    }
    
    void leave() {
        
        show.dispose();
        
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
    
        show = new ShowZone(peripherals);
        Rectangle     area = maxSearchArea();
        ShowZone.Zone zone = new ShowZone.Zone("Fishing Zone", area.width, area.height, area.x, area.y, 150, 150, 200, 0);
        show.addZone(zone);
        
        peripherals.robot.delay(100);
        peripherals.robot.mouseMove(Screen.SCREEN_WIDTH / 2, Screen.SCREEN_HEIGHT / 2 + 10);
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
     * Cherche le bouchon dans toute la zone de pêche : grâce au filtre avant/après le lancer, le décor rouge et bleu est
     * écarté, inutile de commencer petit. Trois essais (le bouchon peut encore tomber), puis un dernier sans filtre
     * (eau rougeâtre, caméra qui a bougé...).
     */
    private Optional<Point> findBobber(Rectangle searchArea, Frame before, BobberDetector.Background water) {

        for (int count = 0; count < 4; count++) {
            Frame           after  = Capture.zone(searchArea);
            Optional<Point> bobber = BobberDetector.locate(after, count < 3 ? before : null, searchArea, water);
            if (bobber.isPresent()) {
                if (count > 0) {logger.debug("Bouchon trouvé au {}e essai", count + 1);}
                return bobber;
            }
            peripherals.robot.delay(250);
        }
        return Optional.empty();
    }
}
