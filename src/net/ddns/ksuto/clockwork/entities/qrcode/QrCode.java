package net.ddns.ksuto.clockwork.entities.qrcode;

import static net.ddns.ksuto.prh.properties.Constants.i_DELAY;

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
    public Dot inCombat;
    public Dot casting;
    public Dot stepBack;
    public Dot playerHealth;
    public Dot playerMana;
    public Dot targetReaction;
    public Dot targetHealth;
    public Dot targetMana;
    public List<RaidMember> raid                 = new ArrayList<>();
    //    public Position         currenPlayerPosition = new Position();
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
            casting = new Dot(3, 2);
            stepBack = new Dot(4, 2);
            
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
    
            int index = 1;
            raid.add(new RaidMember(KeyEvent.VK_A, 3, -1, "a", true, false, true, index++)); //raid1
            raid.add(new RaidMember(KeyEvent.VK_B, 4, -1, "b", true, false, true, index++)); //raid2
            raid.add(new RaidMember(KeyEvent.VK_C, 5, -1, "c", true, false, true, index++)); //raid3
            raid.add(new RaidMember(KeyEvent.VK_D, 6, -1, "d", true, false, true, index++)); //raid4
            raid.add(new RaidMember(KeyEvent.VK_E, 7, -1, "e", true, false, true, index++)); //raid5
            raid.add(new RaidMember(KeyEvent.VK_F, 8, -1, "f", true, false, true, index++)); //raid6
            raid.add(new RaidMember(KeyEvent.VK_G, 9, -1, "g", true, false, true, index++)); //raid7
            raid.add(new RaidMember(KeyEvent.VK_H, 10, -1, "h", true, false, true, index++)); //raid8
            raid.add(new RaidMember(KeyEvent.VK_I, 11, -1, "i", true, false, true, index++)); //raid9
            raid.add(new RaidMember(KeyEvent.VK_J, 12, -1, "j", true, false, true, index++)); //raid10
    
            raid.add(new RaidMember(KeyEvent.VK_K, 14, -3, "k", true, false, true, index++)); //raid11
            raid.add(new RaidMember(KeyEvent.VK_L, 14, -4, "l", true, false, true, index++)); //raid12
            raid.add(new RaidMember(KeyEvent.VK_M, 14, -5, "m", true, false, true, index++)); //raid13
            raid.add(new RaidMember(KeyEvent.VK_N, 14, -6, "n", true, false, true, index++)); //raid14
            raid.add(new RaidMember(KeyEvent.VK_O, 14, -7, "o", true, false, true, index++)); //raid15
            raid.add(new RaidMember(KeyEvent.VK_P, 14, -8, "p", true, false, true, index++)); //raid16
            raid.add(new RaidMember(KeyEvent.VK_Q, 14, -9, "q", true, false, true, index++)); //raid17
            raid.add(new RaidMember(KeyEvent.VK_R, 14, -10, "r", true, false, true, index++)); //raid18
            raid.add(new RaidMember(KeyEvent.VK_S, 14, -11, "s", true, false, true, index++)); //raid19
            raid.add(new RaidMember(KeyEvent.VK_T, 14, -12, "t", true, false, true, index++)); //raid20
    
            raid.add(new RaidMember(KeyEvent.VK_A, 12, -1, "a", true, true, false, index++)); //raid21
            raid.add(new RaidMember(KeyEvent.VK_B, 11, -1, "b", true, true, false, index++)); //raid22
            raid.add(new RaidMember(KeyEvent.VK_C, 10, -1, "c", true, true, false, index++)); //raid23
            raid.add(new RaidMember(KeyEvent.VK_D, 9, -1, "d", true, true, false, index++)); //raid24
            raid.add(new RaidMember(KeyEvent.VK_E, 8, -1, "e", true, true, false, index++)); //raid25
            raid.add(new RaidMember(KeyEvent.VK_F, 7, -1, "f", true, true, false, index++)); //raid26
            raid.add(new RaidMember(KeyEvent.VK_G, 6, -1, "g", true, true, false, index++)); //raid27
            raid.add(new RaidMember(KeyEvent.VK_H, 5, -1, "h", true, true, false, index++)); //raid28
            raid.add(new RaidMember(KeyEvent.VK_I, 4, -1, "i", true, true, false, index++)); //raid29
            raid.add(new RaidMember(KeyEvent.VK_J, 3, -1, "j", true, true, false, index++)); //raid30
    
            raid.add(new RaidMember(KeyEvent.VK_K, 1, -12, "k", true, true, false, index++)); //raid31
            raid.add(new RaidMember(KeyEvent.VK_L, 1, -11, "l", true, true, false, index++)); //raid32
            raid.add(new RaidMember(KeyEvent.VK_M, 1, -10, "m", true, true, false, index++)); //raid33
            raid.add(new RaidMember(KeyEvent.VK_N, 1, -9, "n", true, true, false, index++)); //raid34
            raid.add(new RaidMember(KeyEvent.VK_O, 1, -8, "o", true, true, false, index++)); //raid35
            raid.add(new RaidMember(KeyEvent.VK_P, 1, -7, "p", true, true, false, index++)); //raid36
            raid.add(new RaidMember(KeyEvent.VK_Q, 1, -6, "q", true, true, false, index++)); //raid37
            raid.add(new RaidMember(KeyEvent.VK_R, 1, -5, "r", true, true, false, index++)); //raid38
            raid.add(new RaidMember(KeyEvent.VK_S, 1, -4, "s", true, true, false, index++)); //raid39
            raid.add(new RaidMember(KeyEvent.VK_T, 1, -3, "t", true, true, false, index)); //raid40
    
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

