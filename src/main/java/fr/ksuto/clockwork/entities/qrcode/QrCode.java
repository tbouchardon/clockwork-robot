package fr.ksuto.clockwork.entities.qrcode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fr.ksuto.prh.PeripheralRobotHelper;
import fr.ksuto.prh.capture.Capture;
import fr.ksuto.prh.capture.Frame;
import fr.ksuto.prh.capture.Rgb;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrateur on 19/05/15!
 */
public class QrCode {
    
    private static final Logger logger = LoggerFactory.getLogger(QrCode.class);
    
    private final ArrayList<Key> keys      = new ArrayList<>();
    public        int            xPosition = 0, yPosition = 0;
    public  Dot              inCombat;
    public  Dot              casting;
    @Deprecated
    public  Dot              playerHealth;
    public  Dot              playerMana;
    public  Dot              numberOfTargets;
    public  Dot              targetReaction;
    public  Dot              targetHealth;
    public  Dot              targetMana;
    public  Dot              FISH_MOD;
    //    public Position         currenPlayerPosition = new Position();
    public  Dot              TOGGLE_ON_OFF;
    public  Dot              TARGET_NEAREST_ENEMY;
    public  Dot              ADD_WAYPOINT;
    public  Dot              CLEAR_WAYPOINTS;
    public  Dot              DRIVE_MOD;
    public  Dot              DRIVE_LOOP;
    public  Dot              DEBUG_MOD;
    private Camera           cameraPosition;
    private Dot              qrCodePosition;
    private Frame            capturedQrCode;
    
    public static void typeInChat(PeripheralRobotHelper peripherals, String s) {
        
        peripherals.robot.keyPress(KeyEvent.VK_ENTER);
        peripherals.robot.keyRelease(KeyEvent.VK_ENTER);
        peripherals.robot.delay(100);
        peripherals.getKeyboard().typeString(s);
        peripherals.robot.delay(100);
        peripherals.robot.keyPress(KeyEvent.VK_ENTER);
        peripherals.robot.keyRelease(KeyEvent.VK_ENTER);
    }
    
    static void openCloseKsuto(PeripheralRobotHelper peripherals) {
    
        typeInChat(peripherals, "/clk toggle");
    }
    
    static void startKsuto(PeripheralRobotHelper peripherals, Dot dot) {
        
        peripherals.robot.mouseMove(dot.xPosition, dot.yPosition);
        peripherals.robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        peripherals.robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        peripherals.robot.delay(100);
        
        openCloseKsuto(peripherals);
    }
    
    /**
     * Active l'addon (/clk toggle) s'il est éteint.
     *
     * @return vrai si la commande a été envoyée
     */
    public boolean ensureAddonActive(PeripheralRobotHelper peripherals) {
        
        captureQrCode(peripherals);
        update();
        if (TOGGLE_ON_OFF.active) {return false;}
        
        logger.info("Addon désactivé : clic sur le QR code pour donner le focus à WoW, puis /clk toggle");
        startKsuto(peripherals, qrCodePosition);
        return true;
    }
    
    /**
     * Coin haut gauche du QR code : un carré de 16×16 dont les quatre coins sont vert pur. Vrai que l'addon soit
     * allumé (seuls les coins sont verts) ou éteint (le carré entier est vert).
     *
     * @return la position à l'écran, ou null si absent
     */
    static Point findQrCode(Frame screen) {
        
        for (int y = 0; y + 15 < screen.height(); y++) {
            for (int x = 0; x + 15 < screen.width(); x++) {
                if (screen.rgb(x, y) == Rgb.ARGB_GREEN
                    && screen.rgb(x + 15, y) == Rgb.ARGB_GREEN
                    && screen.rgb(x, y + 15) == Rgb.ARGB_GREEN
                    && screen.rgb(x + 15, y + 15) == Rgb.ARGB_GREEN
                    // vrai coin : ni à droite ni en dessous d'un autre pixel vert
                    && (x == 0 || screen.rgb(x - 1, y) != Rgb.ARGB_GREEN)
                    && (y == 0 || screen.rgb(x, y - 1) != Rgb.ARGB_GREEN)) {
                    return new Point(screen.x() + x, screen.y() + y);
                }
            }
        }
        return null;
    }
    
    public void cameraCombat(PeripheralRobotHelper peripherals) {
        
        if (cameraPosition == Camera.COMBAT) {return;}
        
        cameraDrive(peripherals);
        
        cameraPosition = Camera.COMBAT;
        
        for (int n = 1; n <= 2; n++) {
            peripherals.robot.keyPress(KeyEvent.VK_HOME);
            peripherals.robot.keyRelease(KeyEvent.VK_HOME);
            peripherals.robot.delay(100);
        }
    }
    
    public void cameraDrive(PeripheralRobotHelper peripherals) {
        
        if (cameraPosition == Camera.DRIVE) {return;}
        
        cameraPosition = Camera.DRIVE;
        
        for (int n = 1; n <= 5; n++) {
            peripherals.robot.keyPress(KeyEvent.VK_END);
            peripherals.robot.keyRelease(KeyEvent.VK_END);
            peripherals.robot.delay(100);
        }
    }
    
    public Frame captureQrCode(PeripheralRobotHelper peripherals) {
    
        // 32x32 : carré v3 (quatre blocs) ; une grille v2 n'occupe que le bloc haut gauche
        this.capturedQrCode = Capture.zone(xPosition, yPosition, 32, 32);
    
        return capturedQrCode;
    }
    
    public boolean hasTarget() {return (targetReaction.getRgb(capturedQrCode) != Rgb.ARGB_BLACK);}
    
    public boolean init(PeripheralRobotHelper peripherals) throws AWTException, IOException {
        
        Robot robot = peripherals.robot;
        
        logger.debug("Starting AutoConfig");
        
        Point qrCodePosition = null;
        
        for (int i = 7; i >= 0; i--) {
    
            qrCodePosition = findQrCode(Capture.screen());
    
            if (qrCodePosition != null) {
                break;
            }
    
            if (i == 4) {
                QrCode.openCloseKsuto(peripherals);
            }
    
            robot.delay(750);
        }

        return configure(qrCodePosition);
    }

    /**
     * Cherche le QR code une seule fois, sans clic ni attente : recherche automatique au démarrage de ClockWork.
     *
     * @return vrai si le QR code est à l'écran (positions et touches alors configurées)
     */
    public boolean initIfVisible() {

        return configure(findQrCode(Capture.screen()));
    }

    private boolean configure(Point qrCodePosition) {

        boolean bFound = false;

        if (qrCodePosition != null) {
            
            xPosition = qrCodePosition.x;
            yPosition = qrCodePosition.y;
            
            this.qrCodePosition = new Dot(xPosition, yPosition);
            
            logger.debug("Found QrCode : X = " + xPosition + ", Y = " + yPosition + ", carrying on.");
            
            bFound = true;
        }
        
        if (bFound) {
            
    
            keys.add(new Key(KeyEvent.VK_Q, 2, 4, "Q"));
            keys.add(new Key(KeyEvent.VK_D, 3, 4, "D"));
            keys.add(new Key(KeyEvent.VK_R, 4, 4, "R"));
            keys.add(new Key(KeyEvent.VK_T, 5, 4, "T"));
            keys.add(new Key(KeyEvent.VK_F, 6, 4, "F"));
            keys.add(new Key(KeyEvent.VK_G, 7, 4, "G"));
    
            keys.add(new Key(KeyEvent.VK_EQUALS, 13, 5, "="));
            keys.add(new Key(KeyEvent.VK_RIGHT_PARENTHESIS, 12, 5, ")"));
            keys.add(new Key(KeyEvent.VK_0, 11, 5, "0"));
            keys.add(new Key(KeyEvent.VK_9, 10, 5, "9"));
            keys.add(new Key(KeyEvent.VK_8, 9, 5, "8"));
            keys.add(new Key(KeyEvent.VK_7, 8, 5, "7"));
            keys.add(new Key(KeyEvent.VK_6, 7, 5, "6"));
            keys.add(new Key(KeyEvent.VK_5, 6, 5, "5"));
            keys.add(new Key(KeyEvent.VK_4, 5, 5, "4"));
            keys.add(new Key(KeyEvent.VK_3, 4, 5, "3"));
            keys.add(new Key(KeyEvent.VK_2, 3, 5, "2"));
            keys.add(new Key(KeyEvent.VK_1, 2, 5, "1"));
    
            inCombat = new Dot(2, 2);
            casting = new Dot(3, 2);
    
            playerHealth = new Dot(12, 2);
            playerMana = new Dot(13, 2);
            numberOfTargets = new Dot(2, 3);
            targetReaction = new Dot(11, 3);
            targetHealth = new Dot(12, 3);
            targetMana = new Dot(13, 3);
    
            FISH_MOD = new Dot(12, 4); // pêche demandée par l'addon (/clk fish)
    
            TOGGLE_ON_OFF = new Dot(2, 13);
            TARGET_NEAREST_ENEMY = new Dot(3, 13);
            ADD_WAYPOINT = new Dot(4, 13);
            CLEAR_WAYPOINTS = new Dot(5, 13);
            DRIVE_MOD = new Dot(6, 13);
            DRIVE_LOOP = new Dot(7, 13);
            DEBUG_MOD = new Dot(13, 13);
    
    
            //            QrCode.startKsuto(peripherals, this.qrCodePosition);
    
            logger.debug("AutoConfig Done");
    
            return true;
        }
        else {
            logger.debug("AutoConfig Failed");
    
            return false;
        }
    }
    
    public void update() {
        
        inCombat.updateActive(capturedQrCode);
        casting.updateActive(capturedQrCode);
        TOGGLE_ON_OFF.updateActive(capturedQrCode);
        TARGET_NEAREST_ENEMY.updateActive(capturedQrCode);
        ADD_WAYPOINT.updateActive(capturedQrCode);
        CLEAR_WAYPOINTS.updateActive(capturedQrCode);
        DRIVE_MOD.updateActive(capturedQrCode);
        DRIVE_LOOP.updateActive(capturedQrCode);
        DEBUG_MOD.updateActive(capturedQrCode);
        FISH_MOD.updateActive(capturedQrCode);
    }
    
    public Frame getCapturedQrCode() {
        
        return capturedQrCode;
    }
    
    public ArrayList<Key> getKeys() {
        
        return keys;
    }
    
    public int getNumberOfTargets()  {return (int) Math.floor(100D / 255D * (double) numberOfTargets.getRed(capturedQrCode) + 0.5);}
    
    public double getPlayerHealth()  {return 100D / 255D * (double) playerHealth.getRed(capturedQrCode);}
    
    public double getPlayerMana()    {return 100D / 255D * (double) playerMana.getBlue(capturedQrCode);}
    
    public double getTargetHealth()  {return 100D / 255D * (double) targetHealth.getRed(capturedQrCode);}
    
    public double getTargetMana()    {return 100D / 255D * (double) targetMana.getBlue(capturedQrCode);}
    
    public boolean isTargetHostile() {return (targetReaction.getRgb(capturedQrCode) == Rgb.ARGB_RED);}
    
    public enum Camera {
        DRIVE,
        COMBAT
    }
}

