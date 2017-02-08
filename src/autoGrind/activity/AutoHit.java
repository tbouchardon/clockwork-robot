package autoGrind.activity;

import autoGrind.AutoHit_UI;
import autoGrind.AutoHit_UI.Status;
import autoGrind.tellMeWhen.Key;
import autoGrind.tellMeWhen.TMW;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class AutoHit {
    
    private static final int iGreyColor = 100;
    private final AutoHit_UI               autoHit_UI;
    private final TBoPeripheralRobotHelper peripherals;
    public Status status = Status.RUN;
    private Robot robot;
    
    private Healer priest = null;
    
    public AutoHit(TBoPeripheralRobotHelper peripherals, AutoHit_UI autoHitControl) {
        
        this.peripherals = peripherals;
        this.autoHit_UI = autoHitControl;
    }
    
    public void play() throws AWTException {
        
        robot = new Robot();
        
        BufferedImage biCapturedScreen;
        
        //noinspection InfiniteLoopStatement
        while (true) {
            
            TMW tmw = autoHit_UI.getTMW();
            
            robot.delay(100);
            
            biCapturedScreen = robot.createScreenCapture(new Rectangle(0, 0, peripherals.getScreen().i_SCREEN_WIDTH, peripherals.getScreen().i_SCREEN_HEIGHT));
            
            //File outputfile = new File("saved.png");
            //ImageIO.write(biCapturedScreen, "png", outputfile);
            
            if (status == Status.PAUSE || autoHit_UI.isWindowFocused) { pause(); }
            
            if (status == Status.FISHING) { fish(); }
            
            if (status == Status.TOMTOM) { autoPilot(); }
            
            if (autoHit_UI.isHealerModOn()) {
                if (priest == null) { priest = new Healer(peripherals); }
                priest.searchForWoundedPlayer(biCapturedScreen);
            }
            else {
                priest = null;
            }
            
            findSomethingToDo(biCapturedScreen, tmw);
        }
    }
    
    private void findSomethingToDo(BufferedImage biCapturedScreen, TMW tmw) {
        
        boolean foundSomethingToDo;
        
        for (Key key : tmw.getKeys()) {
            
            foundSomethingToDo = checkColor(key.xPosition, key.yPosition, biCapturedScreen, key.color);
            
            if (foundSomethingToDo) {
                
                autoHit_UI.setTextField(key.key);
                autoHit_UI.setiGrey(iGreyColor);
                
                if (key.ShiftModifier) { robot.keyPress(KeyEvent.VK_SHIFT); }
                pressKey(key.hitKey);
                if (key.ShiftModifier) { robot.keyRelease(KeyEvent.VK_SHIFT); }
                
                break;
            }
        }
        
        if (autoHit_UI.isAutoCycleOn()) {
            pressKey(KeyEvent.VK_TAB); // Without the 5s timer limit
        }
        //if (autoHit_UI.isAutoCycleOn() && (System.currentTimeMillis() - lClickTiming) < 5000 && !foundSomethingToDo) pressKey(KeyEvent.VK_TAB);
    }
    
    private void pause() {
    
        while (status == Status.PAUSE) { robot.delay(250); }
    }
    
    private void autoPilot() throws AWTException {
        
        TomTom navigation = new TomTom(peripherals, this);
        int[]  aiCenter   = null;
        while ((aiCenter == null) && (status == Status.TOMTOM)) { aiCenter = navigation.searchMiniArrow(); }
        while (status == Status.TOMTOM) { navigation.follow(aiCenter, autoHit_UI.getDirection()); }
    }
    
    private void fish() throws AWTException {
        
        Fisher natPagle = new Fisher(peripherals);
        natPagle.setup();
        boolean bKeepFishing = true;
        while (bKeepFishing) { bKeepFishing = natPagle.fish(); }
        natPagle.leave();
        if (status == Status.FISHING) { autoHit_UI.dojButtonFishClick(); }
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
