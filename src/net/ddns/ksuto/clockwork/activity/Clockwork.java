package net.ddns.ksuto.clockwork.activity;

import net.ddns.ksuto.clockwork.ClockWork_UI;
import net.ddns.ksuto.clockwork.ClockWork_UI.Status;
import net.ddns.ksuto.clockwork.entities.Key;
import net.ddns.ksuto.clockwork.entities.Position;
import net.ddns.ksuto.clockwork.entities.middleman.MiddleMan;
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
        try {
            tomtom = new TomTom(peripherals);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }
    
    public void play() throws AWTException {
        
        robot = new Robot();
        
        BufferedImage biCapturedScreen;
        
        //noinspection InfiniteLoopStatement
        while (true) {
    
            MiddleMan middleMan = clockWork_UI.getMiddleMan();
            
            robot.delay(100);
            
            biCapturedScreen = robot.createScreenCapture(new Rectangle(0, 0, peripherals.getScreen().i_SCREEN_WIDTH, peripherals.getScreen().i_SCREEN_HEIGHT));
            
            //File outputfile = new File("saved.png");
            //ImageIO.write(biCapturedScreen, "png", outputfile);
            
            if (status == Status.FISHING) { fish(); }
    
            findSomethingToDo(biCapturedScreen, middleMan);
        }
    }
    
    private void findSomethingToDo(BufferedImage biCapturedScreen, MiddleMan middleMan) throws AWTException {
        
        middleMan.TOGGLE_ON_OFF.active = biCapturedScreen.getRGB(middleMan.TOGGLE_ON_OFF.xPosition, middleMan.TOGGLE_ON_OFF.yPosition) == RGBConverter.WHITE;
        middleMan.TARGET_NEAREST_ENEMY.active = biCapturedScreen.getRGB(middleMan.TARGET_NEAREST_ENEMY.xPosition, middleMan.TARGET_NEAREST_ENEMY.yPosition) == RGBConverter.WHITE;
        middleMan.ADD_WAYPOINT.active = biCapturedScreen.getRGB(middleMan.ADD_WAYPOINT.xPosition, middleMan.ADD_WAYPOINT.yPosition) == RGBConverter.WHITE;
        middleMan.CLEAR_WAYPOINTS.active = biCapturedScreen.getRGB(middleMan.CLEAR_WAYPOINTS.xPosition, middleMan.CLEAR_WAYPOINTS.yPosition) == RGBConverter.WHITE;
        middleMan.DRIVE_MOD.active = biCapturedScreen.getRGB(middleMan.DRIVE_MOD.xPosition, middleMan.DRIVE_MOD.yPosition) == RGBConverter.WHITE;
        middleMan.DRIVE_LOOP.active = biCapturedScreen.getRGB(middleMan.DRIVE_LOOP.xPosition, middleMan.DRIVE_LOOP.yPosition) == RGBConverter.WHITE;
        middleMan.DEBUG_MOD.active = biCapturedScreen.getRGB(middleMan.DEBUG_MOD.xPosition, middleMan.DEBUG_MOD.yPosition) == RGBConverter.RED;
        
        
        if (!middleMan.TOGGLE_ON_OFF.active) { return; }
        
        if (middleMan.ADD_WAYPOINT.active) {
            tomtom.getCoordinates(middleMan, peripherals);
            middleMan.path.add(new Position(middleMan.currenPosition));
            MiddleMan.typeInChat(peripherals, "/kto wpadded");
            peripherals.robot.delay(500);
        }
        
        if (middleMan.CLEAR_WAYPOINTS.active) {
            middleMan.path.clear();
            MiddleMan.typeInChat(peripherals, "/kto wpcleared");
            peripherals.robot.delay(500);
        }
        
        Key     key2hit       = null;
        boolean shiftModifier = false;
        
        for (Key key : middleMan.getKeys()) {
            
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
        
        if (middleMan.TARGET_NEAREST_ENEMY.active) { pressKey(KeyEvent.VK_TAB); } // Without the 5s timer limit
        //if (middleMan.TARGET_NEAREST_ENEMY.active && (System.currentTimeMillis() - lClickTiming) < 5000 && !foundSomethingToDo) pressKey(KeyEvent.VK_TAB);
        
        if (middleMan.DRIVE_MOD.active) {
            if (tomtom == null) { tomtom = new TomTom(peripherals); }
            tomtom.drive(middleMan, peripherals, key2hit != null);
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
