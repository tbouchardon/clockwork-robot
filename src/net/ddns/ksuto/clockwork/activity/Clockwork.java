package net.ddns.ksuto.clockwork.activity;

import net.ddns.ksuto.clockwork.ClockWork_UI;
import net.ddns.ksuto.clockwork.ClockWork_UI.Status;
import net.ddns.ksuto.clockwork.entities.Key;
import net.ddns.ksuto.clockwork.ksuto.Ksuto;
import net.ddns.ksuto.clockwork.tools.RGBConverter;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class Clockwork {
    
    private static final int iGreyColor = 100;
    private final ClockWork_UI             clockWork_UI;
    private final TBoPeripheralRobotHelper peripherals;
    public Status status = Status.RUN;
    private Robot  robot;
    private TomTom tomtom;
    
    public Clockwork(TBoPeripheralRobotHelper peripherals, ClockWork_UI autoHitControl) {
        
        this.peripherals = peripherals;
        this.clockWork_UI = autoHitControl;
    }
    
    public void play() throws AWTException {
        
        robot = new Robot();
        
        BufferedImage biCapturedScreen;
        
        //noinspection InfiniteLoopStatement
        while (true) {
            
            Ksuto ksuto = clockWork_UI.getKsuto();
            
            robot.delay(100);
            
            biCapturedScreen = robot.createScreenCapture(new Rectangle(0, 0, peripherals.getScreen().i_SCREEN_WIDTH, peripherals.getScreen().i_SCREEN_HEIGHT));
            
            //File outputfile = new File("saved.png");
            //ImageIO.write(biCapturedScreen, "png", outputfile);
            
            if (status == Status.FISHING) { fish(); }
            
            findSomethingToDo(biCapturedScreen, ksuto);
        }
    }
    
    private void findSomethingToDo(BufferedImage biCapturedScreen, Ksuto ksuto) throws AWTException {
        
        ksuto.TOGGLE_ON_OFF.active = biCapturedScreen.getRGB(ksuto.TOGGLE_ON_OFF.xPosition, ksuto.TOGGLE_ON_OFF.yPosition) == RGBConverter.WHITE;
        ksuto.TARGET_NEAREST_ENEMY.active = biCapturedScreen.getRGB(ksuto.TARGET_NEAREST_ENEMY.xPosition, ksuto.TARGET_NEAREST_ENEMY.yPosition) == RGBConverter.WHITE;
        ksuto.ADD_WAYPOINT.active = biCapturedScreen.getRGB(ksuto.ADD_WAYPOINT.xPosition, ksuto.ADD_WAYPOINT.yPosition) == RGBConverter.WHITE;
        ksuto.CLEAR_WAYPOINTS.active = biCapturedScreen.getRGB(ksuto.CLEAR_WAYPOINTS.xPosition, ksuto.CLEAR_WAYPOINTS.yPosition) == RGBConverter.WHITE;
        ksuto.DRIVE_MOD.active = biCapturedScreen.getRGB(ksuto.DRIVE_MOD.xPosition, ksuto.DRIVE_MOD.yPosition) == RGBConverter.WHITE;
        ksuto.DRIVE_LOOP.active = biCapturedScreen.getRGB(ksuto.DRIVE_LOOP.xPosition, ksuto.DRIVE_LOOP.yPosition) == RGBConverter.WHITE;
        ksuto.DEBUG_MOD.active = biCapturedScreen.getRGB(ksuto.DEBUG_MOD.xPosition, ksuto.DEBUG_MOD.yPosition) == RGBConverter.RED;
        
        
        if (!ksuto.TOGGLE_ON_OFF.active) { return; }
        
        if (ksuto.ADD_WAYPOINT.active) {
            tomtom.getCoordinates(ksuto, peripherals);
            ksuto.path.add(ksuto.currenPosition);
            Ksuto.typeInChat(peripherals, "/kto wpadded");
            peripherals.robot.delay(500);
        }
        
        if (ksuto.CLEAR_WAYPOINTS.active) {
            ksuto.path.clear();
            Ksuto.typeInChat(peripherals, "/kto wpcleared");
            peripherals.robot.delay(500);
        }
        
        Key     key2hit       = null;
        boolean shiftModifier = false;
        
        for (Key key : ksuto.getKeys()) {
            
            int iCapturedRGB = biCapturedScreen.getRGB(key.xPosition, key.yPosition); //-1 == white && -16777216 == black
            
            if (iCapturedRGB == RGBConverter.WHITE && key2hit == null) { key2hit = key; }
            if (iCapturedRGB == RGBConverter.BLUE && !shiftModifier) {
                shiftModifier = true;
                key2hit = key;
            }
            
            //peripherals.robot.mouseMove(key.xPosition, key.yPosition);
        }
        
        if (key2hit != null) {
            
            if (shiftModifier) { robot.keyPress(Event.SHIFT_MASK); }
            
            clockWork_UI.setTextField(key2hit.key);
            clockWork_UI.setiGrey(iGreyColor);
            
            pressKey(key2hit.hitKey);
            
            if (shiftModifier) { robot.keyRelease(Event.SHIFT_MASK); }
        }
        
        if (ksuto.TARGET_NEAREST_ENEMY.active) { pressKey(KeyEvent.VK_TAB); } // Without the 5s timer limit
        //if (ksuto.TARGET_NEAREST_ENEMY.active && (System.currentTimeMillis() - lClickTiming) < 5000 && !foundSomethingToDo) pressKey(KeyEvent.VK_TAB);
        
        if (ksuto.DRIVE_MOD.active) {
            if (tomtom == null) { tomtom = new TomTom(peripherals); }
            tomtom.drive(ksuto, peripherals, key2hit != null);
        }
    }
    
    private void fish() throws AWTException {
        
        Fisher natPagle = new Fisher(peripherals);
        natPagle.setup();
        boolean bKeepFishing = true;
        while (bKeepFishing) { bKeepFishing = natPagle.fish(); }
        natPagle.leave();
        if (status == Status.FISHING) { clockWork_UI.dojButtonFishClick(); }
    }
    
    private void pressKey(int iKey) {
        
        robot.keyPress(iKey);
        robot.keyRelease(iKey);
        robot.delay(500);
    }
    
    private boolean checkColor(int iX, int iY, BufferedImage biCapturedScreen, int iColor) {
        
        int iCapturedRGB = biCapturedScreen.getRGB(iX, iY);
        // System.out.println("(" + iX + ", " + iY + ") Searching : " + iColor + ", found : " + iCapturedRGB + ".");
        return (iColor == iCapturedRGB);
    }
}
