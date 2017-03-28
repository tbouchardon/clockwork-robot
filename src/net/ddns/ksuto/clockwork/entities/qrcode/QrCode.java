package net.ddns.ksuto.clockwork.entities.qrcode;

import static net.ddns.ksuto.prh.properties.Constants.i_DELAY;

import net.ddns.ksuto.clockwork.entities.Position;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;
import net.ddns.ksuto.prh.entities.ColorBlock;
import net.ddns.ksuto.tools.TboTools_Debug;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrateur on 19/05/15!
 */
public class QrCode {
    
    private final ArrayList<Key> keys      = new ArrayList<>();
    public        int            xPosition = 0, yPosition = 0;
    public List<Position> path = new ArrayList<>();
    public Dot inCombat;
    public Dot stepBack;
    public Dot playerHealth;
    public Dot playerMana;
    public Dot targetReaction;
    public Dot targetHealth;
    public Dot targetMana;
    public Position currenPlayerPosition = new Position();
    public  Dot    TOGGLE_ON_OFF;
    public  Dot    TARGET_NEAREST_ENEMY;
    public  Dot    ADD_WAYPOINT;
    public  Dot    CLEAR_WAYPOINTS;
    public  Dot    DRIVE_MOD;
    public  Dot    DRIVE_LOOP;
    public  Dot    DEBUG_MOD;
    private Camera cameraPosition;
    private Dot    qrCodePosition;
    
    public static void typeInChat(TBoPeripheralRobotHelper peripherals, String s) {
        
        
        peripherals.robot.keyPress(KeyEvent.VK_ENTER);
        peripherals.robot.keyRelease(KeyEvent.VK_ENTER);
        peripherals.robot.delay(i_DELAY);
        peripherals.getKeyboard().typeString(s);
        peripherals.robot.delay(i_DELAY);
        peripherals.robot.keyPress(KeyEvent.VK_ENTER);
        peripherals.robot.keyRelease(KeyEvent.VK_ENTER);
    }
    
    public static void pressKey(TBoPeripheralRobotHelper peripherals, int iKey) {
        
        peripherals.robot.keyPress(iKey);
        peripherals.robot.delay(i_DELAY);
        peripherals.robot.keyRelease(iKey);
        peripherals.robot.delay(i_DELAY);
    }
    
    static void startKsuto(TBoPeripheralRobotHelper peripherals, Dot dot) {
        
        peripherals.robot.mouseMove(dot.xPosition, dot.yPosition);
        peripherals.robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        peripherals.robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        peripherals.robot.delay(i_DELAY);
        
        openCloseKsuto(peripherals);
    }
    
    static void openCloseKsuto(TBoPeripheralRobotHelper peripherals) {
    
        typeInChat(peripherals, "/kto toggle");
    }
    
    public void cameraDrive(TBoPeripheralRobotHelper peripherals) {
    
        if (cameraPosition == Camera.DRIVE) { return; }
    
        cameraPosition = Camera.DRIVE;
        
        for (int n = 1; n <= 5; n++) {
            peripherals.robot.keyPress(KeyEvent.VK_END);
            peripherals.robot.keyRelease(KeyEvent.VK_END);
            peripherals.robot.delay(i_DELAY);
        }
    }
    
    public void cameraCombat(TBoPeripheralRobotHelper peripherals) {
    
        if (cameraPosition == Camera.COMBAT) { return; }
        
        cameraDrive(peripherals);
    
        cameraPosition = Camera.COMBAT;
        
        for (int n = 1; n <= 2; n++) {
            peripherals.robot.keyPress(KeyEvent.VK_HOME);
            peripherals.robot.keyRelease(KeyEvent.VK_HOME);
            peripherals.robot.delay(i_DELAY);
        }
    }
    
    public boolean init(TBoPeripheralRobotHelper peripherals) throws AWTException, IOException {
        
        Robot robot = peripherals.robot;
        
        TboTools_Debug.sout("Starting AutoConfig");
        
        ArrayList<ColorBlock> ksutoPosition = null;
        boolean               bFound        = false;
        
        for (int i = 7; i >= 0; i--) {
            ksutoPosition = peripherals.getScreen().searchColorBlocks(0, 255, 0, 14, 0);
            if (!ksutoPosition.isEmpty()) {
                break;
            }
            
            if (i == 4) {
                QrCode.openCloseKsuto(peripherals);
            }
            
            robot.delay(750);
        }
        
        if (!ksutoPosition.isEmpty()) {
            
            xPosition = ksutoPosition.get(0).xPosition;
            yPosition = ksutoPosition.get(0).yPosition;
    
            qrCodePosition = new Dot(xPosition, yPosition);
            
            TboTools_Debug.sout("Found QrCode : X = " + xPosition + ", Y = " + yPosition + ", carrying on.");
            peripherals.robot.mouseMove(xPosition, yPosition);
            
            robot.delay(i_DELAY);
            bFound = true;
        }
        
        if (bFound) {
            
            robot.delay(i_DELAY);
            
            // L'ordre d'ajout correspond à l'ordre de priorité. L'interface WoW et celui-ci doivent correspondre.
            keys.add(new Key(KeyEvent.VK_T, 6, 4, "T"));
            keys.add(new Key(KeyEvent.VK_G, 5, 4, "G"));
            keys.add(new Key(KeyEvent.VK_Q, 4, 4, "Q"));
            keys.add(new Key(KeyEvent.VK_D, 3, 4, "D"));
            keys.add(new Key(KeyEvent.VK_H, 2, 4, "H"));
            
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
            stepBack = new Dot(3, 2);
            
            playerHealth = new Dot(12, 2);
            playerMana = new Dot(13, 2);
            targetReaction = new Dot(11, 3);
            targetHealth = new Dot(12, 3);
            targetMana = new Dot(13, 3);
    
            TOGGLE_ON_OFF = new Dot(2, 13);
            TARGET_NEAREST_ENEMY = new Dot(3, 13);
            ADD_WAYPOINT = new Dot(4, 13);
            CLEAR_WAYPOINTS = new Dot(5, 13);
            DRIVE_MOD = new Dot(6, 13);
            DRIVE_LOOP = new Dot(7, 13);
            DEBUG_MOD = new Dot(13, 13);
    
            QrCode.startKsuto(peripherals, qrCodePosition);
            
            //            QrCode.pressKey(peripherals, KeyEvent.VK_ESCAPE);
            
            TboTools_Debug.sout("AutoConfig Done");
            
            return true;
        }
        else {
            TboTools_Debug.sout("AutoConfig Failed");
            
            return false;
        }
    }
    
    public BufferedImage captureQrCode(TBoPeripheralRobotHelper peripherals) {
        
        return peripherals.robot.createScreenCapture(new Rectangle(xPosition, yPosition, 16, 16));
    }
    
    public ArrayList<Key> getKeys() {
    
        return keys;
    }
    
    public enum Camera {
        DRIVE,
        COMBAT;
    }
}

