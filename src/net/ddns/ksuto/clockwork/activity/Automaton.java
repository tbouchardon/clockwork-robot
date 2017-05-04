package net.ddns.ksuto.clockwork.activity;

import net.ddns.ksuto.clockwork.ClockWork_UI;
import net.ddns.ksuto.clockwork.ClockWork_UI.Status;
import net.ddns.ksuto.clockwork.entities.qrcode.Key;
import net.ddns.ksuto.clockwork.entities.qrcode.QrCode;
import net.ddns.ksuto.clockwork.entities.qrcode.RaidMember;
import net.ddns.ksuto.clockwork.tools.RGBConverter;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;
import net.ddns.ksuto.tools.TboTools_Debug;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class Automaton {
    
    private static final int iGreyColor = 100;
    private final ClockWork_UI             clockWork_UI;
    private final TBoPeripheralRobotHelper peripherals;
    public Status  status      = Status.RUN;
    public boolean stepingBack = false;
    private Robot  robot;
    private TomTom tomtom;
    private boolean wasInCombat    = false;
    private long    lastActionTime = 0;
    
    public Automaton(TBoPeripheralRobotHelper peripherals, ClockWork_UI autoHitControl) {
        
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
    
        TboTools_Debug.sout("");
        
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
    
            if (status == Status.FISHING) {
                fish();
            }
    
            searchForSomethingToDo(biCapturedScreen, qrCode);
        }
    }
    
    private void searchForSomethingToDo(BufferedImage capturedScreen, QrCode qrCode) throws AWTException {
        
        // On ne fait rien si L'addon n'est pas visible
        if (capturedScreen.getRGB(0, 0) != RGBConverter.GREEN) {
            return;
        }
        
        qrCode.inCombat.updateActive(capturedScreen);
        qrCode.casting.updateActive(capturedScreen);
        qrCode.stepBack.updateActive(capturedScreen);
        
        double playerHealth = 100D / 255D * (double) qrCode.playerHealth.getRed(capturedScreen);
        double playerMana   = 100D / 255D * (double) qrCode.playerMana.getBlue(capturedScreen);
        
        boolean hostileTarget = (qrCode.targetReaction.getRgb(capturedScreen) == RGBConverter.RED);
        boolean target        = (qrCode.targetReaction.getRgb(capturedScreen) != RGBConverter.BLACK);
        
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
    
        if (wasInCombat && !qrCode.inCombat.active) { tryToLoot();}
        
        Key     key2hit       = null;
        boolean shiftModifier = false;
        boolean ctrlModifier  = false;
        boolean altModifier   = false;
        
        for (Key key : qrCode.getKeys()) {
    
            int iCapturedRGB = capturedScreen.getRGB(key.xPosition, key.yPosition); //-1 == white && -16777216 == black
            
            if (!ctrlModifier) {
                if (iCapturedRGB == RGBConverter.GREEN) {
                    ctrlModifier = true;
                    key2hit = key;
                }
    
                if (!shiftModifier) {
                    if (iCapturedRGB == RGBConverter.RED) {
                        shiftModifier = true;
                        key2hit = key;
                    }
        
                    if (!altModifier) {
                        if (iCapturedRGB == RGBConverter.BLUE) {
                            altModifier = true;
                            key2hit = key;
                        }
            
                        if (iCapturedRGB == RGBConverter.WHITE && key2hit == null) {
                            key2hit = key;
                        }
                    }
                }
            }
        }
    
        if (key2hit != null) {
            lastActionTime = System.currentTimeMillis();
            hitKey(key2hit, shiftModifier, ctrlModifier, altModifier);
        }
    
        if (qrCode.TARGET_NEAREST_ENEMY.active && key2hit == null && !qrCode.casting.active) {
            peripherals.getKeyboard().pressKey(KeyEvent.VK_TAB);
        }
    
        tomtom.drive(qrCode, peripherals, key2hit != null, qrCode.casting.active, qrCode.inCombat.active, lastActionTime, playerHealth);
    
        if (qrCode.stepBack.active) { faceEnemy(); }
    
        wasInCombat = qrCode.inCombat.active;
    
        if (key2hit == null) { peripherals.robot.delay(200); }
        else { peripherals.robot.delay(750); }
    }
    
    private void hitKey(Key key2hit, boolean shiftModifier, boolean ctrlModifier, boolean altModifier) {
        
        
        
        clockWork_UI.setTextField(key2hit.key);
        clockWork_UI.setiGrey(iGreyColor);
    
        peripherals.getKeyboard().pressKey(key2hit.hitKey);
        
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
    
    private void tryToLoot() {
        
        peripherals.robot.delay(500);
        
        int hitZoneX = peripherals.getScreen().i_SCREEN_WIDTH / 2 + (int) ((double) peripherals.getScreen().i_SCREEN_WIDTH / 100d * 4.6875);
        int hitZoneY = peripherals.getScreen().i_SCREEN_HEIGHT / 2 + (int) ((double) peripherals.getScreen().i_SCREEN_HEIGHT / 100d * 14.8148);
        
        peripherals.robot.keyPress(KeyEvent.VK_SHIFT);
        peripherals.getMouse().clickRight(hitZoneX, hitZoneY);
        peripherals.getMouse().clickRight(hitZoneX, hitZoneY - 100);
        peripherals.getMouse().clickRight(hitZoneX, hitZoneY + 100);
        peripherals.getMouse().clickRight(hitZoneX - 100, hitZoneY);
        peripherals.getMouse().clickRight(hitZoneX + 100, hitZoneY);
        peripherals.robot.keyRelease(KeyEvent.VK_SHIFT);
    }
    
    private void fish() throws AWTException {
        
        Fisher natPagle = new Fisher(peripherals);
        natPagle.setup();
        boolean bKeepFishing = true;
        while (bKeepFishing) {
            bKeepFishing = natPagle.fish();
        }
        natPagle.leave();
        if (status == Status.FISHING) {
            clockWork_UI.dojButtonFishClick();
        }
    }
    
    private void checkParty(BufferedImage capturedScreen, QrCode qrCode) {
    
        for (RaidMember raidMember : qrCode.raid) {
        
            raidMember.updateActive(capturedScreen);
            if (raidMember.getBlue(capturedScreen) != 0) {
                if (raidMember.index < 5) { targetPartyMember(raidMember); }
                else { targetRaidMember(raidMember); }
                break;
            }
        }
    }
    
    private void targetRaidMember(RaidMember raidMember) {
        
        peripherals.getKeyboard().pressKey(raidMember.hitKey, raidMember.alt, raidMember.ctrl, raidMember.shift);
        
    }
    
    private void targetPartyMember(RaidMember raidMember) {
        
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
}
