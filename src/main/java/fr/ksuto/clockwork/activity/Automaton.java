package fr.ksuto.clockwork.activity;

import fr.ksuto.clockwork.ClockWork_UI;
import fr.ksuto.clockwork.entities.qrcode.ComplexKey;
import fr.ksuto.clockwork.entities.qrcode.Key;
import fr.ksuto.clockwork.entities.qrcode.QrCode;
import fr.ksuto.clockwork.tools.RGBConverter;
import fr.ksuto.prh.PeripheralRobotHelper;
import fr.ksuto.tools.Debug;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class Automaton {
    
    private static final int                   iGreyColor     = 100;
    private final        ClockWork_UI          clockWork_UI;
    private final        PeripheralRobotHelper peripherals;
    public               ClockWork_UI.Status   status         = ClockWork_UI.Status.RUN;
    public               boolean               stepingBack    = false;
    private              Robot                 robot;
    private              TomTom                tomtom;
    private              boolean               wasInCombat    = false;
    private              long                  lastActionTime = 0;
    
    public Automaton(PeripheralRobotHelper peripherals, ClockWork_UI autoHitControl) {
        
        this.peripherals = peripherals;
        this.clockWork_UI = autoHitControl;
        try {
            tomtom = new TomTom(peripherals);
        }
        catch (AWTException e) {
            e.printStackTrace();
        }
    }
    
    public void play() throws AWTException {
        
        Debug.sout("");
        
        robot = new Robot();
        
        BufferedImage biCapturedScreen;
        
        //noinspection InfiniteLoopStatement
        while (true) {
            
            QrCode qrCode = clockWork_UI.getQrCode();
            
            robot.delay(100);
            
            biCapturedScreen = qrCode.captureQrCode(peripherals);
            
            //            File outputfile = new File("saved.png");
            //            try {
            //                ImageIO.write(biCapturedScreen, "png", outputfile);
            //            }
            //            catch (IOException e) {
            //                e.printStackTrace();
            //            }
            
            if (status == ClockWork_UI.Status.FISHING) {
                fish();
            }
            
            searchForSomethingToDo(biCapturedScreen, qrCode);
        }
    }
    
    private void checkParty(BufferedImage capturedScreen, QrCode qrCode) {
    
        for (ComplexKey raidMember : qrCode.raid) {
            
            raidMember.updateActive(capturedScreen);
            if (raidMember.getBlue(capturedScreen) != 0) {
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
    
    private void fish() throws AWTException {
        
        Fisher natPagle = new Fisher(peripherals);
        natPagle.setup();
        boolean bKeepFishing = true;
        while (bKeepFishing) {
            bKeepFishing = natPagle.fish();
        }
        natPagle.leave();
        if (status == ClockWork_UI.Status.FISHING) {
            clockWork_UI.dojButtonFishClick();
        }
    }
    
    private void hitKey(Key key2hit, boolean altModifier, boolean ctrlModifier, boolean shiftModifier) {
        
        clockWork_UI.setTextField(key2hit.key);
        clockWork_UI.setiGrey(iGreyColor);
        
        peripherals.getKeyboard().pressKey(key2hit.hitKey, altModifier, ctrlModifier, shiftModifier);
    }
    
    private void searchForSomethingToDo(BufferedImage capturedScreen, QrCode qrCode) throws AWTException {
        
        // On ne fait rien si l’addon n’est pas visible
        if (capturedScreen.getRGB(0, 0) != RGBConverter.GREEN) {
            return;
        }
    
        qrCode.inCombat.updateActive(capturedScreen);
        qrCode.casting.updateActive(capturedScreen);
        qrCode.stepBack.updateActive(capturedScreen);
    
        double playerHealth = 100D / 255D * (double) qrCode.playerHealth.getRed(capturedScreen);
        double playerMana   = 100D / 255D * (double) qrCode.playerMana.getBlue(capturedScreen);
    
        boolean hostileTarget   = (qrCode.targetReaction.getRgb(capturedScreen) == RGBConverter.RED);
        int     numberOfTargets = (int) Math.floor(100D / 255D * (double) qrCode.playerHealth.getRed(capturedScreen) + 0.5);
        boolean target          = (qrCode.targetReaction.getRgb(capturedScreen) != RGBConverter.BLACK);
    
        double targetHealth = 100D / 255D * (double) qrCode.targetHealth.getRed(capturedScreen);
        double targetMana   = 100D / 255D * (double) qrCode.targetMana.getBlue(capturedScreen);
    
        qrCode.TOGGLE_ON_OFF.updateActive(capturedScreen);
        qrCode.TARGET_NEAREST_ENEMY.updateActive(capturedScreen);
        qrCode.ADD_WAYPOINT.updateActive(capturedScreen);
        qrCode.CLEAR_WAYPOINTS.updateActive(capturedScreen);
        qrCode.DRIVE_MOD.updateActive(capturedScreen);
        qrCode.DRIVE_LOOP.updateActive(capturedScreen);
        qrCode.DEBUG_MOD.updateActive(capturedScreen);
        
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
    
        checkParty(capturedScreen, qrCode);
    
        // Check Stance
        RGBConverter rgbConverter = new RGBConverter(capturedScreen, qrCode.stance.xPosition, qrCode.stance.yPosition);
        rgbConverter.invoke();
        //        System.out.println((int) Math.floor(rgbConverter.getRed() + 0.5));
        //        System.out.println((int) Math.floor(rgbConverter.getGreen() + 0.5));
        //        System.out.println((int) Math.floor(rgbConverter.getBlue() + 0.5));
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
            rgbConverter = new RGBConverter(capturedScreen, key.xPosition, key.yPosition);
            rgbConverter.invoke();
            int     keyMod   = (int) Math.floor(rgbConverter.getRed() + 0.5);
            int     priority = (int) Math.floor(rgbConverter.getGreen() + 0.5);
            boolean active   = rgbConverter.getBlue() != 0;
        
            //            if (active) {Debug.sout(key.key + " => " + priority + ", " + keyMod + ", ");}
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
    
        //        for (Key key : qrCode.getKeys()) {
        //
        //            int iCapturedRGB = capturedScreen.getRGB(key.xPosition, key.yPosition); //-1 == white && -16777216 == black
        //
        //            if (!ctrlModifier) {
        //                if (iCapturedRGB == RGBConverter.GREEN) {
        //                    ctrlModifier = true;
        //                    key2hit = key;
        //                }
        //
        //                if (!shiftModifier) {
        //                    if (iCapturedRGB == RGBConverter.RED) {
        //                        shiftModifier = true;
        //                        key2hit = key;
        //                    }
        //
        //                    if (!altModifier) {
        //                        if (iCapturedRGB == RGBConverter.BLUE) {
        //                            altModifier = true;
        //                            key2hit = key;
        //                        }
        //
        //                        if (iCapturedRGB == RGBConverter.WHITE && key2hit == null) {
        //                            key2hit = key;
        //                        }
        //                    }
        //                }
        //            }
        //        }
    
        if (key2hit != null) {System.out.println("key2hit = " + key2hit.key + ", bestPriority = " + bestPriority);}
    
        if (key2hit != null) {
            lastActionTime = System.currentTimeMillis();
            hitKey(key2hit, altModifier, ctrlModifier, shiftModifier);
        }
    
        if (qrCode.TARGET_NEAREST_ENEMY.active && key2hit == null && !qrCode.casting.active) {
            peripherals.getKeyboard().pressKey(KeyEvent.VK_TAB);
        }
        
        tomtom.drive(qrCode, peripherals, key2hit != null, qrCode.casting.active, qrCode.inCombat.active, lastActionTime, playerHealth);
    
        if (qrCode.stepBack.active && qrCode.DRIVE_MOD.active) {faceEnemy();}
        
        wasInCombat = qrCode.inCombat.active;
        
        if (key2hit == null) {peripherals.robot.delay(200);}
        else {peripherals.robot.delay(750);}
    }
    
    private void targetPartyMember(ComplexKey raidMember) {
        
        switch (raidMember.index) {
            
            case 1:
                peripherals.getKeyboard().f2();
                break;
            case 2:
                peripherals.getKeyboard().f3();
                break;
            case 3:
                peripherals.getKeyboard().f4();
                break;
            case 4:
                peripherals.getKeyboard().f5();
                break;
        }
    }
    
    private void targetRaidMember(ComplexKey raidMember) {
        
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
}
