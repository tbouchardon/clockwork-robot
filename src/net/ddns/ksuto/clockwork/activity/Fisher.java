package net.ddns.ksuto.clockwork.activity;

import static net.ddns.ksuto.prh.properties.Constants.i_DELAY;

import net.ddns.ksuto.clockwork.tools.Scanner;
import net.ddns.ksuto.clockwork.tools.ShowZone;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

//import clockwork.AutoGrind_UI.Status;

class Fisher {
    
    @SuppressWarnings("FieldCanBeLocal")
    private final        int iLureTime = 10 * 60000; // W
    @SuppressWarnings("FieldCanBeLocal")
    private final        int iBaitTime = 5 * 60000; // Shift + W
    
    private final TBoPeripheralRobotHelper peripherals;
    private final int    iMargin          = 30;
    private final int    iDeltaY          = 50;
    private       double iRatio           = 1;
    private       int    iWidth           = 100;
    private       int    iHeight          = 50;
    private       long   lCurrentBaitTime = 0;
    private       long   lCurrentLureTime = 0;
    private ShowZone show;
    
    Fisher(TBoPeripheralRobotHelper peripherals) {
        
        this.peripherals = peripherals;
    }
    
    boolean fish() throws AWTException {
        
        Robot robot = new Robot();
        int[] coords, tempcoords;
        
        if (new Date().getTime() - lCurrentLureTime > iLureTime) {
            lCurrentLureTime = new Date().getTime();
            peripherals.getKeyboard().typeString("w");
            robot.delay(3000);
        }
        
        if (new Date().getTime() - lCurrentBaitTime > iBaitTime) {
            lCurrentBaitTime = new Date().getTime();
            peripherals.getKeyboard().typeString("W");
            robot.delay(1000);
        }
        
        peripherals.getKeyboard().typeString("h");
        
        long lFishingTime = new Date().getTime(), lCurTime = new Date().getTime();
        
        robot.delay(2500);
        
        coords = searchFloat();
        tempcoords = coords;
        
        while (lCurTime - lFishingTime < 21000) {
            // System.out.println(lCurTime + " " + lInitTime + " " + (lCurTime - lInitTime));
            // if (autoGrindUI.getStatus() != Status.FISHING) break;
            // autoGrindUI.setjLabText(String.valueOf(22 - ((lCurTime - lFishingTime) / 1000)));
            coords = searchFloat();
            
            if (tempcoords == null || coords == null) { return true; }
            
            if (coords[0] != 0) {
                if ((MouseInfo.getPointerInfo().getLocation().x - coords[0] > 5) || (MouseInfo.getPointerInfo().getLocation().x - coords[0] < -5)
                    || (MouseInfo.getPointerInfo().getLocation().y - coords[1] > 5) || (MouseInfo.getPointerInfo().getLocation().y - coords[1] < -5)) { robot.mouseMove(coords[0], coords[1]); }
                robot.delay(100);
                if ((MouseInfo.getPointerInfo().getLocation().x - coords[0] > 10) || (MouseInfo.getPointerInfo().getLocation().x - coords[0] < -10)
                    || (MouseInfo.getPointerInfo().getLocation().y - coords[1] > 10) || (MouseInfo.getPointerInfo().getLocation().y - coords[1] < -10)) {
                    return false;
                }
            }
            
            if (((coords[1] - tempcoords[1] > 2) || (coords[1] - tempcoords[1] < -2) || (coords[0] - tempcoords[0] > 5) || (coords[0] - tempcoords[0] < -5))
                && (coords[1] - tempcoords[1] < 15) && (coords[1] - tempcoords[1] > -15) && (coords[0] - tempcoords[0] < 15) && (coords[0] - tempcoords[0] >
                                                                                                                                 -15)) {
                System.out.println("Got a catch ? Float Moving : dY = " + (coords[1] - tempcoords[1]) + ", dX = " + (coords[0] - tempcoords[0]));
                peripherals.getMouse().clickLeft();
                robot.delay(2000);
                return true;
            }
            
            tempcoords = coords;
            lCurTime = new Date().getTime();
            if ((coords[0] == 0) && (lCurTime - lFishingTime > 4000)) { return true; }
        }
        
        return true;
    }
    
    void setup() throws AWTException {
        
        Scanner scan = new Scanner(peripherals);
        try {
            ArrayList<int[]> resultsWoW = scan.searchPicture("Pictures/scan.WoW.png");
            if (!resultsWoW.isEmpty()) {
                int iWoWSize = peripherals.getScreen().i_SCREEN_WIDTH - (resultsWoW.get(0)[0] * 2);
                System.out.println("WoW Width = " + iWoWSize);
                iRatio = (double) iWoWSize / (double) peripherals.getScreen().i_SCREEN_WIDTH;
                System.out.println("Ratio = " + iRatio);
            }
        }
        catch (IOException ignored) {
        }
        
        Robot robot = new Robot();
        
        show = new ShowZone(peripherals);
        show.zone("Fishing Zone", iWidth + iMargin, iHeight + iMargin, peripherals.getScreen().i_SCREEN_WIDTH / 2 - (iWidth / 2 + iMargin), peripherals.getScreen().i_SCREEN_HEIGHT / 2 - (iHeight +
                                                                                                                                                                                           iMargin)
                                                                                                                                            - iDeltaY, 150,
                  150, 200, 0);
        show.setVisible(true);
        robot.delay(i_DELAY);
        robot.mouseMove(peripherals.getScreen().i_SCREEN_WIDTH / 2, peripherals.getScreen().i_SCREEN_HEIGHT / 2 + 10);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(i_DELAY);
        
        robot.keyPress(KeyEvent.VK_X);
        robot.keyRelease(KeyEvent.VK_X);
        robot.delay(i_DELAY);
        
        for (int n = 1; n <= 4; n++) {
            robot.keyPress(KeyEvent.VK_END);
            robot.keyRelease(KeyEvent.VK_END);
            robot.delay(i_DELAY);
        }
        
        for (int n = 1; n <= 2; n++) {
            robot.keyPress(KeyEvent.VK_HOME);
            robot.keyRelease(KeyEvent.VK_HOME);
            robot.delay(i_DELAY);
        }
    }
    
    void leave() throws AWTException {
        
        show.dispose();
        Robot robot = new Robot();
        
        for (int n = 1; n <= 1; n++) {
            robot.keyPress(KeyEvent.VK_END);
            robot.keyRelease(KeyEvent.VK_END);
            robot.delay(500);
        }
        robot.keyPress(KeyEvent.VK_X);
        robot.keyRelease(KeyEvent.VK_X);
    }
    
    private int[] searchFloat() throws AWTException {
        
        Robot         robot            = new Robot();
        BufferedImage biCapturedScreen = robot.createScreenCapture(new Rectangle(0, 0, peripherals.getScreen().i_SCREEN_WIDTH, peripherals.getScreen().i_SCREEN_HEIGHT));
        int           iCapturedRGB;
        int           r, g, b;
        
        for (int count = 0; count < 4; count++) {
            for (int y = peripherals.getScreen().i_SCREEN_HEIGHT / 2 - iHeight - iDeltaY; y < peripherals.getScreen().i_SCREEN_HEIGHT / 2 - iDeltaY; y++) {
                for (int x = peripherals.getScreen().i_SCREEN_WIDTH / 2 - iWidth / 2; x < peripherals.getScreen().i_SCREEN_WIDTH / 2 + iWidth / 2; x++) {
                    for (int n = 0; n <= 2; ) {
                        iCapturedRGB = biCapturedScreen.getRGB(x + n, y);
                        b = (iCapturedRGB) & 0xFF;
                        g = (iCapturedRGB >> 8) & 0xFF;
                        r = (iCapturedRGB >> 16) & 0xFF;
                        if ((r > 100) && (g < r - 50) && (b < r - 50) && (g < 100) && (b < 100)) {
                            if (n == 2) {
                                iCapturedRGB = biCapturedScreen.getRGB(x, y - 5);
                                b = (iCapturedRGB) & 0xFF;
                                g = (iCapturedRGB >> 8) & 0xFF;
                                r = (iCapturedRGB >> 16) & 0xFF;
                                if ((b > r) && (b > g)) { return new int[]{x + 1, y}; }
                            }
                            n++;
                        }
                        else { break; }
                    }
                }
            }
            
            iWidth += 9;
            iHeight += 3;
            
            robot.delay(250);
            
            if (iWidth > peripherals.getScreen().i_SCREEN_WIDTH / (3 / iRatio)) {
                iWidth = (int) (peripherals.getScreen().i_SCREEN_WIDTH / (3 / iRatio));
                iHeight = iWidth / 3;
            }
            
            show.changeSize(iWidth + iMargin, iHeight + iMargin + 10, peripherals.getScreen().i_SCREEN_WIDTH / 2 - ((iWidth + iMargin) / 2), peripherals.getScreen().i_SCREEN_HEIGHT / 2 - (iHeight +
                                                                                                                                                                                            ((iMargin
                                                                                                                                                                                              + 10) /
                                                                                                                                                                                             2))
                                                                                                                                             - iDeltaY);
        }
        return null;
    }
}
