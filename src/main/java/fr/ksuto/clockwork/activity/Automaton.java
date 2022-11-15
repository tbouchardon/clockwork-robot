package fr.ksuto.clockwork.activity;

import fr.ksuto.clockwork.ClockWork_UI;
import fr.ksuto.clockwork.entities.qrcode.ComplexKey;
import fr.ksuto.clockwork.entities.qrcode.Key;
import fr.ksuto.clockwork.entities.qrcode.QrCode;
import fr.ksuto.clockwork.tools.RGBConverter;
import fr.ksuto.prh.PeripheralRobotHelper;
import fr.ksuto.prh.peripherals.Screen;
import fr.ksuto.tools.Debug;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class Automaton {
    
    private static final int                   iGreyColor          = 100;
    private final        PeripheralRobotHelper peripherals;
    private final        long                  turnAroundCoolDown  = 4000;
    private final        ClockWork_UI          ui;
    public               ClockWork_UI.Status   status              = ClockWork_UI.Status.RUN;
    public               boolean               stepingBack         = false;
    private              Robot                 robot;
    private              TomTom                tomtom;
    private              boolean               wasInCombat         = false;
    private              long                  lastActionTime      = 0;
    private              long                  lockTurnAroundUntil = System.currentTimeMillis();
    
    public Automaton(PeripheralRobotHelper peripherals, ClockWork_UI autoHitControl) {
        
        this.peripherals = peripherals;
        this.ui = autoHitControl;
        try {
            tomtom = new TomTom(peripherals);
        }
        catch (AWTException e) {
            e.printStackTrace();
        }
    }
    
    public void play() throws Exception {
        
        Debug.sout("");
        
        robot = new Robot();
        
        //noinspection InfiniteLoopStatement
        while (true) {
            
            QrCode qrCode = ui.getQrCode();
            
            robot.delay(100);
            
            if (status == ClockWork_UI.Status.FISHING) {
                fish();
            }
            
            searchForSomethingToDo(qrCode);
        }
    }
    
    private void checkParty(BufferedImage capturedQrCode, QrCode qrCode) {
        
        for (ComplexKey raidMember : qrCode.raid) {
            
            raidMember.updateActive(capturedQrCode);
            if (raidMember.getBlue(capturedQrCode) != 0) {
                if (raidMember.index < 5) {targetPartyMember(raidMember);}
                else {targetRaidMember(raidMember);}
                break;
            }
        }
    }
    
    private void faceEnemy() {
        
        peripherals.robot.keyPress(40); // Recule
        peripherals.robot.keyPress(32); // Saute
        peripherals.robot.keyPress(39); // Tourne
        peripherals.robot.delay(100);
        peripherals.robot.keyRelease(32); // Stop Saute
        peripherals.robot.delay(400);
        peripherals.robot.keyRelease(39); // Stop Tourne
        peripherals.robot.delay(500);
        peripherals.robot.keyPress(32); // Saute
        peripherals.robot.delay(100);
        peripherals.robot.keyRelease(32); // Stop Saute
        peripherals.robot.delay(900);
        peripherals.robot.keyRelease(40); // Stop Recule
    }
    
    private void fish() throws Exception {
        
        Fisherman natPagle = new Fisherman(ui, peripherals);
        natPagle.setup();
        boolean keepFishing = true;
        peripherals.getScreen().startCapture();
        while (keepFishing) {
            keepFishing = natPagle.fish(peripherals.getScreen().getCaptureScheduler());
        }
        peripherals.getScreen().stopCapture();
        natPagle.leave();
        if (status == ClockWork_UI.Status.FISHING) {
            ui.dojButtonFishClick();
        }
    }
    
    private void hitKey(Key key2hit, boolean altModifier, boolean ctrlModifier, boolean shiftModifier) {
        
        String key = shiftModifier ? key2hit.key.toUpperCase() : key2hit.key.toLowerCase();
        
        ui.appendLog(key);
        ui.setiGrey(iGreyColor);
        
        peripherals.getKeyboard().pressKey(key2hit.hitKey, altModifier, ctrlModifier, shiftModifier);
    }
    
    private void searchForSomethingToDo(QrCode qrCode) {
        
        qrCode.captureQrCode(peripherals);
        
        // On ne fait rien si l’addon n’est pas visible
        if (qrCode.getCapturedQrCode().getRGB(0, 0) != RGBConverter.GREEN) {
            return;
        }
        
        qrCode.update();
        
        if (!qrCode.TOGGLE_ON_OFF.active) {
            return;
        }
        
        if (qrCode.ADD_WAYPOINT.active) {
            tomtom.addWayPoint(qrCode);
        }
        
        if (qrCode.CLEAR_WAYPOINTS.active) {
            tomtom.clearWayPoints();
        }
        
        if (wasInCombat && !qrCode.inCombat.active) {
            //            tryToLoot();
        }
        
        Key     key2hit       = null;
        int     bestPriority  = -1;
        boolean shiftModifier = false;
        boolean ctrlModifier  = false;
        boolean altModifier   = false;
        
        checkParty(qrCode.getCapturedQrCode(), qrCode);
        
        // Check Stance
        RGBConverter rgbConverter = new RGBConverter(qrCode.getCapturedQrCode(), qrCode.stance.xPosition, qrCode.stance.yPosition);
        rgbConverter.invoke();
        if (rgbConverter.getBlue() != 0) {
            bestPriority = (int) Math.floor(rgbConverter.getGreen() + 0.5);
            int stance = (int) Math.floor(rgbConverter.getBlue() + 0.5);
            
            switch (stance) {
                case 112:
                    key2hit = new Key(KeyEvent.VK_F1, "F1");
                    ;
                    break;
                case 113:
                    key2hit = new Key(KeyEvent.VK_F2, "F2");
                    ;
                    break;
                case 114:
                    key2hit = new Key(KeyEvent.VK_F3, "F3");
                    ;
                    break;
                case 115:
                    key2hit = new Key(KeyEvent.VK_F4, "F4");
                    ;
                    break;
                case 116:
                    key2hit = new Key(KeyEvent.VK_F5, "F5");
                    ;
                    break;
                default:
                    key2hit = new Key(0x0, "");
            }
        }
        
        if (key2hit != null) {System.out.println("key2hit = " + key2hit.key + ", bestPriority = " + bestPriority);}
        
        for (Key key : qrCode.getKeys()) {
            rgbConverter = new RGBConverter(qrCode.getCapturedQrCode(), key.xPosition, key.yPosition);
            rgbConverter.invoke();
            int     keyMod   = (int) Math.floor(rgbConverter.getRed() + 0.5);
            int     priority = (int) Math.floor(rgbConverter.getGreen() + 0.5);
            boolean active   = rgbConverter.getBlue() != 0;
            
            if (active &&
                priority > bestPriority) {
                
                if (key2hit != null) {System.out.println("key2hit = " + key2hit.key + ", bestPriority = " + bestPriority);}
                key2hit = key;
                ctrlModifier = keyMod == 1;
                altModifier = keyMod == 2;
                shiftModifier = keyMod == 4;
                bestPriority = priority;
            }
        }
        
        if (key2hit != null) {System.out.println("key2hit = " + key2hit.key + ", bestPriority = " + bestPriority);}
        
        if (key2hit != null) {
            lastActionTime = System.currentTimeMillis();
            hitKey(key2hit, altModifier, ctrlModifier, shiftModifier);
        }
        
        if (qrCode.TARGET_NEAREST_ENEMY.active && key2hit == null && !qrCode.casting.active) {
            peripherals.getKeyboard().pressKey(KeyEvent.VK_TAB);
        }
        
        tomtom.drive(qrCode, peripherals, key2hit != null, qrCode.casting.active, qrCode.inCombat.active, lastActionTime, qrCode.getPlayerHealth());
        
        if (qrCode.stepBack.active && qrCode.DRIVE_MOD.active) {faceEnemy();}
        
        if (qrCode.turnAround.active && System.currentTimeMillis() > lockTurnAroundUntil && qrCode.DRIVE_MOD.active) {
            lockTurnAroundUntil = System.currentTimeMillis() + turnAroundCoolDown;
            turnAround();
        }
        
        wasInCombat = qrCode.inCombat.active;
        
        if (key2hit == null) {peripherals.robot.delay(200);}
        else {peripherals.robot.delay(750);}
    }
    
    private void targetPartyMember(ComplexKey raidMember) {
    
        System.out.println("Target party member " + raidMember.index);
    
        switch (raidMember.index) {
        
            case 1:
                peripherals.getKeyboard().pressKey(KeyEvent.VK_F2, false, false, true);
                break;
            case 2:
                peripherals.getKeyboard().pressKey(KeyEvent.VK_F3, false, false, true);
                break;
            case 3:
                peripherals.getKeyboard().pressKey(KeyEvent.VK_F4, false, false, true);
                break;
            case 4:
                peripherals.getKeyboard().pressKey(KeyEvent.VK_F5, false, false, true);
                break;
        }
    }
    
    private void targetRaidMember(ComplexKey raidMember) {
    
        System.out.println("Target raid member " + raidMember.index);
    
        peripherals.getKeyboard().pressKey(raidMember.hitKey, raidMember.alt, raidMember.ctrl, raidMember.shift);
    }
    
    private void tryToLoot() {
        
        peripherals.robot.delay(500);
        
        int hitZoneX = peripherals.getScreen().SCREEN_WIDTH / 2 + (int) ((double) peripherals.getScreen().SCREEN_WIDTH / 100d * 4.6875);
        int hitZoneY = peripherals.getScreen().SCREEN_HEIGHT / 2 + (int) ((double) peripherals.getScreen().SCREEN_HEIGHT / 100d * 14.8148);
    
        peripherals.robot.keyPress(KeyEvent.VK_SHIFT);
        peripherals.getMouse().clickRight(hitZoneX, hitZoneY);
        peripherals.getMouse().clickRight(hitZoneX, hitZoneY - 100);
        peripherals.getMouse().clickRight(hitZoneX, hitZoneY + 100);
        peripherals.getMouse().clickRight(hitZoneX - 100, hitZoneY);
        peripherals.getMouse().clickRight(hitZoneX + 100, hitZoneY);
        peripherals.robot.keyRelease(KeyEvent.VK_SHIFT);
    }
    
    private void turnAround() {
        
        robot.mouseMove(Screen.X_START, Screen.Y_START);
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        
        for (int iLR = Screen.X_START; iLR < Screen.X_START + 800; iLR += 10) {
            robot.mouseMove(iLR, Screen.Y_START);
            robot.delay(2);
        }
        robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
    }
}
