package fr.ksuto.clockwork.entities.qrcode;

import fr.ksuto.clockwork.tools.RGBConverter;
import fr.ksuto.prh.PeripheralRobotHelper;
import fr.ksuto.prh.entities.ColorBlock;
import fr.ksuto.prh.helpers.ColorSearch;
import fr.ksuto.tools.Debug;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Created by Administrateur on 19/05/15!
 */
public class QrCode {
    
    private final ArrayList<Key> keys      = new ArrayList<>();
    public        int            xPosition = 0, yPosition = 0;
    public  Dot              inCombat;
    public  Dot              casting;
    @Deprecated
    public  Dot              stepBack;
    public  Dot              turnAround;
    public  Dot              playerHealth;
    public  Dot              playerMana;
    public  Dot              numberOfTargets;
    public  Dot              targetReaction;
    public  Dot              targetHealth;
    public  Dot              targetMana;
    public  Dot              stance;
    public  List<ComplexKey> raid = new ArrayList<>();
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
    private BufferedImage    capturedQrCode;
    
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
    
    public BufferedImage captureQrCode(PeripheralRobotHelper peripherals) {
    
        BufferedImage capture = peripherals.robot.createScreenCapture(new Rectangle(xPosition, yPosition, 16, 16));
    
        try {
            BufferedWriter writer     = null;
            File           outputfile = new File("qrCode.jpg");
            ImageIO.write(capture, "png", outputfile);
        }
        catch (IOException e) {
        }
    
        this.capturedQrCode = capture;
    
        //        File outputfile = new File("qrCode.png");
        //        try {
        //            ImageIO.write(capture, "png", outputfile);
        //        }
        //        catch (IOException e) {
        //            e.printStackTrace();
        //        }
    
        return capture;
    }
    
    public boolean hasTarget() {return (targetReaction.getRgb(capturedQrCode) != RGBConverter.ARGB_BLACK);}
    
    public boolean init(PeripheralRobotHelper peripherals) throws AWTException, IOException {
        
        Robot robot = peripherals.robot;
        
        Debug.sout("Starting AutoConfig");
        
        ColorBlock qrCodePosition = null;
        boolean    bFound         = false;
        
        for (int i = 7; i >= 0; i--) {
    
            ColorSearch colorSearch = ColorSearch.getDefault(0, 255, 0, 256, 256);
            colorSearch.search();
            qrCodePosition = colorSearch.getFirstResult();
    
            if (qrCodePosition != null) {
                break;
            }
    
            if (i == 4) {
                QrCode.openCloseKsuto(peripherals);
            }
    
            robot.delay(750);
        }
        
        if (qrCodePosition != null) {
            
            xPosition = qrCodePosition.getFirstPosition().getX();
            yPosition = qrCodePosition.getFirstPosition().getY();
            
            this.qrCodePosition = new Dot(xPosition, yPosition);
            
            Debug.sout("Found QrCode : X = " + xPosition + ", Y = " + yPosition + ", carrying on.");
            //            peripherals.robot.mouseMove(xPosition, yPosition);
            
            robot.delay(100);
            bFound = true;
        }
        
        if (bFound) {
            
            robot.delay(100);
    
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
            stepBack = new Dot(4, 2);
            turnAround = new Dot(5, 2);
    
            playerHealth = new Dot(12, 2);
            playerMana = new Dot(13, 2);
            numberOfTargets = new Dot(2, 3);
            targetReaction = new Dot(11, 3);
            targetHealth = new Dot(12, 3);
            targetMana = new Dot(13, 3);
    
            stance = new Dot(12, 4);
    
            TOGGLE_ON_OFF = new Dot(2, 13);
            TARGET_NEAREST_ENEMY = new Dot(3, 13);
            ADD_WAYPOINT = new Dot(4, 13);
            CLEAR_WAYPOINTS = new Dot(5, 13);
            DRIVE_MOD = new Dot(6, 13);
            DRIVE_LOOP = new Dot(7, 13);
            DEBUG_MOD = new Dot(13, 13);
    
            int index = 1;
            raid.add(new ComplexKey(KeyEvent.VK_A, 3, 1, "a", true, false, true, index++)); //raid1
            raid.add(new ComplexKey(KeyEvent.VK_B, 4, 1, "b", true, false, true, index++)); //raid2
            raid.add(new ComplexKey(KeyEvent.VK_C, 5, 1, "c", true, false, true, index++)); //raid3
            raid.add(new ComplexKey(KeyEvent.VK_D, 6, 1, "d", true, false, true, index++)); //raid4
            raid.add(new ComplexKey(KeyEvent.VK_E, 7, 1, "e", true, false, true, index++)); //raid5
            raid.add(new ComplexKey(KeyEvent.VK_F, 8, 1, "f", true, false, true, index++)); //raid6
            raid.add(new ComplexKey(KeyEvent.VK_G, 9, 1, "g", true, false, true, index++)); //raid7
            raid.add(new ComplexKey(KeyEvent.VK_H, 10, 1, "h", true, false, true, index++)); //raid8
            raid.add(new ComplexKey(KeyEvent.VK_I, 11, 1, "i", true, false, true, index++)); //raid9
            raid.add(new ComplexKey(KeyEvent.VK_J, 12, 1, "j", true, false, true, index++)); //raid10
    
            raid.add(new ComplexKey(KeyEvent.VK_K, 14, 3, "k", true, false, true, index++)); //raid11
            raid.add(new ComplexKey(KeyEvent.VK_L, 14, 4, "l", true, false, true, index++)); //raid12
            raid.add(new ComplexKey(KeyEvent.VK_M, 14, 5, "m", true, false, true, index++)); //raid13
            raid.add(new ComplexKey(KeyEvent.VK_N, 14, 6, "n", true, false, true, index++)); //raid14
            raid.add(new ComplexKey(KeyEvent.VK_O, 14, 7, "o", true, false, true, index++)); //raid15
            raid.add(new ComplexKey(KeyEvent.VK_P, 14, 8, "p", true, false, true, index++)); //raid16
            raid.add(new ComplexKey(KeyEvent.VK_Q, 14, 9, "q", true, false, true, index++)); //raid17
            raid.add(new ComplexKey(KeyEvent.VK_R, 14, 10, "r", true, false, true, index++)); //raid18
            raid.add(new ComplexKey(KeyEvent.VK_S, 14, 11, "s", true, false, true, index++)); //raid19
            raid.add(new ComplexKey(KeyEvent.VK_T, 14, 12, "t", true, false, true, index++)); //raid20
    
            raid.add(new ComplexKey(KeyEvent.VK_A, 12, 14, "a", true, true, false, index++)); //raid21
            raid.add(new ComplexKey(KeyEvent.VK_B, 11, 14, "b", true, true, false, index++)); //raid22
            raid.add(new ComplexKey(KeyEvent.VK_C, 10, 14, "c", true, true, false, index++)); //raid23
            raid.add(new ComplexKey(KeyEvent.VK_D, 9, 14, "d", true, true, false, index++)); //raid24
            raid.add(new ComplexKey(KeyEvent.VK_E, 8, 14, "e", true, true, false, index++)); //raid25
            raid.add(new ComplexKey(KeyEvent.VK_F, 7, 14, "f", true, true, false, index++)); //raid26
            raid.add(new ComplexKey(KeyEvent.VK_G, 6, 14, "g", true, true, false, index++)); //raid27
            raid.add(new ComplexKey(KeyEvent.VK_H, 5, 14, "h", true, true, false, index++)); //raid28
            raid.add(new ComplexKey(KeyEvent.VK_I, 4, 14, "i", true, true, false, index++)); //raid29
            raid.add(new ComplexKey(KeyEvent.VK_J, 3, 14, "j", true, true, false, index++)); //raid30
    
            raid.add(new ComplexKey(KeyEvent.VK_K, 1, 12, "k", true, true, false, index++)); //raid31
            raid.add(new ComplexKey(KeyEvent.VK_L, 1, 11, "l", true, true, false, index++)); //raid32
            raid.add(new ComplexKey(KeyEvent.VK_M, 1, 10, "m", true, true, false, index++)); //raid33
            raid.add(new ComplexKey(KeyEvent.VK_N, 1, 9, "n", true, true, false, index++)); //raid34
            raid.add(new ComplexKey(KeyEvent.VK_O, 1, 8, "o", true, true, false, index++)); //raid35
            raid.add(new ComplexKey(KeyEvent.VK_P, 1, 7, "p", true, true, false, index++)); //raid36
            raid.add(new ComplexKey(KeyEvent.VK_Q, 1, 6, "q", true, true, false, index++)); //raid37
            raid.add(new ComplexKey(KeyEvent.VK_R, 1, 5, "r", true, true, false, index++)); //raid38
            raid.add(new ComplexKey(KeyEvent.VK_S, 1, 4, "s", true, true, false, index++)); //raid39
            raid.add(new ComplexKey(KeyEvent.VK_T, 1, 3, "t", true, true, false, index)); //raid40
    
            //            QrCode.startKsuto(peripherals, this.qrCodePosition);
    
            Debug.sout("AutoConfig Done");
    
            return true;
        }
        else {
            Debug.sout("AutoConfig Failed");
    
            return false;
        }
    }
    
    public void update() {
        
        inCombat.updateActive(capturedQrCode);
        casting.updateActive(capturedQrCode);
        stepBack.updateActive(capturedQrCode);
        turnAround.updateActive(capturedQrCode);
        TOGGLE_ON_OFF.updateActive(capturedQrCode);
        TARGET_NEAREST_ENEMY.updateActive(capturedQrCode);
        ADD_WAYPOINT.updateActive(capturedQrCode);
        CLEAR_WAYPOINTS.updateActive(capturedQrCode);
        DRIVE_MOD.updateActive(capturedQrCode);
        DRIVE_LOOP.updateActive(capturedQrCode);
        DEBUG_MOD.updateActive(capturedQrCode);
    }
    
    public BufferedImage getCapturedQrCode() {
        
        return capturedQrCode;
    }
    
    public ArrayList<Key> getKeys() {
        
        return keys;
    }
    
    public int getNumberOfTargets()  {return (int) Math.floor(100D / 255D * (double) playerHealth.getRed(capturedQrCode) + 0.5);}
    
    public double getPlayerHealth()  {return 100D / 255D * (double) playerHealth.getRed(capturedQrCode);}
    
    public double getPlayerMana()    {return 100D / 255D * (double) playerMana.getBlue(capturedQrCode);}
    
    public double getTargetHealth()  {return 100D / 255D * (double) targetHealth.getRed(capturedQrCode);}
    
    public double getTargetMana()    {return 100D / 255D * (double) targetMana.getBlue(capturedQrCode);}
    
    public boolean isTargetHostile() {return (targetReaction.getRgb(capturedQrCode) == RGBConverter.ARGB_RED);}
    
    public enum Camera {
        DRIVE,
        COMBAT
    }
}

