package net.ddns.ksuto.clockwork.tellMeWhen;

import net.ddns.ksuto.clockwork.tools.Scanner;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

public class ConfigureTMW {
    
    @SuppressWarnings("FieldCanBeLocal")
    private final int      i_DELAY        = 100;
    @SuppressWarnings("FieldCanBeLocal")
    private final int      i_DEBUG_DELAY  = 0;
    private final TMW      tmw            = new TMW();
    //private int[] aiTMW = new int[22];
    private final int[]    aiKeys         = new int[]{
            KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9, KeyEvent.VK_0, KeyEvent.VK_RIGHT_PARENTHESIS,
            KeyEvent.VK_EQUALS,
            KeyEvent.VK_T, KeyEvent.VK_G, KeyEvent.VK_Q, KeyEvent.VK_D, KeyEvent.VK_H,
            KeyEvent.VK_T, KeyEvent.VK_G, KeyEvent.VK_Q, KeyEvent.VK_D, KeyEvent.VK_H};
    private final String[] asKeys         = new String[]{
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", ")", "=",
            "t", "g", "q", "d", "h",
            "T", "G", "Q", "D", "H"};
    private       double   dTMWButtonSize = 0;
    private       int      iXTMW          = 0, iYTMW = 0;
    private TBoPeripheralRobotHelper peripherals;
    
    public ConfigureTMW() throws AWTException {
        
        peripherals = new TBoPeripheralRobotHelper();
    }
    
    public void run() throws AWTException, IOException {
        
        Robot robot = new Robot();
        
        System.out.println("Starting AutoConfig");
        
        Scanner                  scan        = new Scanner(peripherals);
        ArrayList<int[]>         resultsWoW  = null;
        ArrayList<Scanner.Block> positionTMW = null;
        boolean                  bFound      = false;
        
        for (int j = 10; j >= 0; j--) {
            resultsWoW = scan.searchPicture("/Pictures/scan.WoW.png");
            if (resultsWoW.isEmpty()) { resultsWoW = scan.searchPicture("/Pictures/scan.WoW.Legion.png"); }
            positionTMW = scan.searchColorBlocks(0, 255, 0, 10, 0);
            if (!resultsWoW.isEmpty()) { break; }
            robot.delay(1000);
        }
        
        if (!resultsWoW.isEmpty() && positionTMW.isEmpty()) {
            System.out.println("Found WoW : X = " + resultsWoW.get(0)[0] + ", Y = " + resultsWoW.get(0)[0]);
            
            robot.mouseMove(resultsWoW.get(0)[0] + 50, resultsWoW.get(0)[1] + 50);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(i_DELAY);
            
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);
            robot.delay(i_DELAY);
            peripherals.getKeyboard().typeString("/tmw");
            robot.delay(i_DELAY);
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);
            
            for (int i = 10; i >= 0; i--) {
                positionTMW = scan.searchColorBlocks(0, 255, 0, 10, 0);
                if (!positionTMW.isEmpty()) { break; }
                robot.delay(1000);
            }
        }
        
        if (!resultsWoW.isEmpty() && !positionTMW.isEmpty()) {
            
            dTMWButtonSize = (double) (positionTMW.get(1).xPosition - positionTMW.get(0).xPosition) / 13;
            
            iXTMW = (int) (positionTMW.get(0).xPosition + (dTMWButtonSize) + (dTMWButtonSize / 4));
            iYTMW = (int) (positionTMW.get(0).yPosition + (dTMWButtonSize / 4));
            
            System.out.println("Found TMW : X = " + iXTMW + ", Y = " + iYTMW + ", carrying on.");
            System.out.println("Button size is : (int) " + (int) dTMWButtonSize + ",(double) " + dTMWButtonSize);
            
            robot.delay(i_DELAY + i_DEBUG_DELAY);
            bFound = true;
        }
        
        if (bFound) {
            
            robot.mouseMove(10, 10);
            robot.delay(i_DELAY);
            
            BufferedImage biCapturedScreen = robot.createScreenCapture(new Rectangle(0, 0, peripherals.getScreen().i_SCREEN_WIDTH, peripherals.getScreen().i_SCREEN_HEIGHT));
            
            robot.mouseMove(iXTMW + (int) (dTMWButtonSize * aiKeys.length), iYTMW);
            robot.delay(i_DELAY + i_DEBUG_DELAY);
            
            for (int i = aiKeys.length - 1; i >= 0; i--) {
                
                Key key = new Key(aiKeys[i]);
                key.key = asKeys[i];
                if (i >= 17) { key.ShiftModifier = true; }
                
                if (i >= 12) {
                    //aiTMW[i] = biCapturedScreen.getRGB(iXTMW + (int) (dTMWButtonSize * i - dTMWButtonSize * 12), iYTMW - (int) dTMWButtonSize);
                    robot.mouseMove(iXTMW + (int) (dTMWButtonSize * i - dTMWButtonSize * 12), iYTMW - (int) dTMWButtonSize);
                    
                    key.color = biCapturedScreen.getRGB(iXTMW + (int) (dTMWButtonSize * i - dTMWButtonSize * 12), iYTMW - (int) dTMWButtonSize);
                    key.xPosition = iXTMW + (int) (dTMWButtonSize * i - dTMWButtonSize * 12);
                    key.yPosition = iYTMW - (int) dTMWButtonSize;
                }
                else {
                    //aiTMW[i] = biCapturedScreen.getRGB(iXTMW + (int) (dTMWButtonSize * i), iYTMW);
                    robot.mouseMove(iXTMW + (int) (dTMWButtonSize * i), iYTMW);
                    
                    key.color = biCapturedScreen.getRGB(iXTMW + (int) (dTMWButtonSize * i), iYTMW);
                    key.xPosition = iXTMW + (int) (dTMWButtonSize * i);
                    key.yPosition = iYTMW;
                }
                
                tmw.add(key);
                robot.delay(i_DELAY / 4);
            }
            
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.delay(i_DELAY);
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);
            robot.delay(i_DELAY);
            peripherals.getKeyboard().typeString("/tmw");
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);
            robot.delay(i_DELAY);
        }
        
        if (dTMWButtonSize == 0) { System.out.println("AutoConfig Failed"); }
        else { System.out.println("AutoConfig Done"); }
    }
    
    public TMW getTMW() {
        
        return tmw;
    }
    
    public double getdTMWButtonSize() {
        
        return dTMWButtonSize;
    }
}
