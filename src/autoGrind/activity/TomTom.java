package autoGrind.activity;

import autoGrind.AutoHit_UI.Status;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class TomTom {
    
    private static final int KEY_J = 74, KEY_Y = 89, KEY_SPACE = 32;
    private final int i_DELAY = 100;
    private TBoPeripheralRobotHelper peripherals;
    private boolean isFlying  = false;
    private boolean isRunning = false;
    private Robot robot;
    private int iTime = 0;
    private AutoHit autoHit;
    
    TomTom(TBoPeripheralRobotHelper peripherals, AutoHit autoHit) throws AWTException {
        
        robot = new Robot();
        this.peripherals = peripherals;
        this.autoHit = autoHit;
    }
    
    int[] searchMiniArrow() {
        
        boolean found = false;
        int     iCapturedRGB;
        int     r, g, b;
        
        robot.delay(3000);
        BufferedImage biCapturedScreen = robot.createScreenCapture(new Rectangle(0, 0, peripherals.getScreen().i_SCREEN_WIDTH, peripherals.getScreen().i_SCREEN_HEIGHT));
        
        for (int y = 50; y < 300; y++) {
            for (int x = 1300; x < peripherals.getScreen().i_SCREEN_WIDTH; x++) {
                for (int m = 0; m <= 2; ) {
                    for (int n = 0; n <= 2; ) {
                        iCapturedRGB = biCapturedScreen.getRGB(x + n, y + m);
                        b = (iCapturedRGB) & 0xFF;
                        g = (iCapturedRGB >> 8) & 0xFF;
                        r = (iCapturedRGB >> 16) & 0xFF;
                        if ((n == 1) && (m == 1) && (r >= 95) && (r <= 139) && (g >= 114) && (g <= 150) && (b >= 121) && (b <= 165)) {
                            System.out.println("Center : " + "n = " + n + ", m = " + m + ", rgb = " + r + "," + g + "," + b);
                            n++;
                            continue;
                        }
                        else if ((n == 1) && (m == 1)) {
                            System.out.println("Out ! : rgb = " + r + "," + g + "," + b + " (" + x + "," + y + ")");
                            found = false;
                            break;
                        }
                        if ((r <= 100) && (r <= b - 23) && (g <= 125) && (g <= b - 13) && (b >= 99)) {
                            found = true;
                            if (n == 2 && m == 2) { return new int[]{x + 1, y + 1}; }
                            if (n == 2) {
                                m++;
                                n = 0;
                            }
                            else { n++; }
                        }
                        else {
                            found = false;
                            break;
                        }
                    }
                    if (!found) {
                        break;
                    }
                }
            }
        }
        return null;
    }
    
    //	public int[] searchArrow() throws AWTException {
    //
    //		robot.delay(3000);
    //		int iSquareSize = 7;
    //
    //		Scanner scan = new Scanner();
    //		ArrayList<int[]> alGreenCoords = scan.searchColorBlocks(-40, 210, -40, iSquareSize, iSquareSize);
    //		ArrayList<int[]> alRedCoords = scan.searchColorBlocks(210, -40, -40, iSquareSize, iSquareSize);
    //
    //		for (int[] coordGreen : alGreenCoords)
    //			for (int[] coordRed : alRedCoords)
    //				if (coordGreen[0] == coordRed[0] && coordGreen[1] == coordRed[1]) return new int[] { coordRed[0], coordRed[1] };
    //
    //		return null;
    //	}
    
    void follow(int[] aiCenter, Arrows arrow) throws AWTException {
        
        int[] aiArrowPosition;
        // int[] aiArrowDirection = searchMiniArrowPoint(aiCenter);
        if (arrow == Arrows.ARCHEO) { aiArrowPosition = searchMiniArcheoArrow(aiCenter); }
        else {
            aiArrowPosition = searchMiniQuestArrow(aiCenter); // (arrow == Arrows.QUEST)
        }
        if (aiArrowPosition != null) {
            // Si Position y > 0 => Tourner � Droite;
            if (aiArrowPosition[0] - aiCenter[0] > 5) {
                turnRight(15);
                return;
            }
            // Si Position y < 0 => Tourner � Gauche;
            if (aiArrowPosition[0] - aiCenter[0] < -5) {
                turnLeft(15);
                return;
            }
            if (!isFlying) { fly(); }
            robot.delay(250);
            return;
        }
        if (isRunning && iTime == 2) { runStop(); }
        robot.delay(1000);
        iTime++;
        if (iTime <= 2) {
            if (iTime / 2 == ((double) iTime / 2d)) { turnLeft(50); }
            else { turnRight(50); }
        }
        if (iTime == 10) {
            isFlying = false;
            while (autoHit.status == Status.TOMTOM && aiArrowPosition == null) {
                if (arrow == Arrows.ARCHEO) { aiArrowPosition = searchMiniArcheoArrow(aiCenter); }
                else { aiArrowPosition = searchMiniQuestArrow(aiCenter); }
                robot.delay(1000);
            }
            if (autoHit.status == Status.TOMTOM) { robot.delay(5000); }
            iTime = 0;
        }
    }
    
    //	public int[] searchMiniArrowPoint(int[] aiCenter) throws AWTException {
    //		int iCapturedRGB;
    //		int r, g, b;
    //
    //		BufferedImage biCapturedScreen = robot.createScreenCapture(new Rectangle(aiCenter[0] - 10, aiCenter[1] - 10, aiCenter[0] + 10, aiCenter[1] + 10));
    //
    //		for (int y = 0; y < 20; y++) {
    //			for (int x = 0; x < 20; x++) {
    //				if (x == 6 && y >= 5 && y <= 15) x = 15;
    //				iCapturedRGB = biCapturedScreen.getRGB(x, y);
    //				b = (iCapturedRGB) & 0xFF;
    //				g = (iCapturedRGB >> 8) & 0xFF;
    //				r = (iCapturedRGB >> 16) & 0xFF;
    //				if ((r >= 250) && (r <= 255) && (g >= 230) && (g <= 255) && (b >= 205) && (b <= 225)) {
    //					// robot.mouseMove(x + aiCenter[0] - 10, y + aiCenter[1] - 10);
    //					// System.out.println("(" + (x) + "," + (y) + ") " + ", rgb = " + r + "," + g + "," + b);
    //					return new int[] { x + aiCenter[0] - 10, y + aiCenter[1] - 10 };
    //				}
    //			}
    //		}
    //		return null;
    //	}
    
    private int[] searchMiniArcheoArrow(int[] aiCenter) {
        
        boolean found = false;
        int     iCapturedRGB;
        int     r, g, b;
        
        BufferedImage biCapturedScreen = robot.createScreenCapture(new Rectangle(aiCenter[0] - 63, aiCenter[1] - 63, aiCenter[0] + 63, aiCenter[1] + 63));
        
        for (int y = 0; y < 125; y++) {
            for (int x = 0; x < 125; x++) {
                for (int m = 0; m <= 1; ) {
                    for (int n = 0; n <= 1; ) {
                        iCapturedRGB = biCapturedScreen.getRGB(x + n, y + m);
                        b = (iCapturedRGB) & 0xFF;
                        g = (iCapturedRGB >> 8) & 0xFF;
                        r = (iCapturedRGB >> 16) & 0xFF;
                        if ((r <= g - 70) && (g >= 135) && (g <= 165) && (b <= g - 80)) {
                            found = true;
                            // robot.mouseMove(x + aiCenter[0] - 63, y + aiCenter[1] - 63);
                            // System.out.println("(" + (x + n) + "," + (y + m) + ") " + "n = " + n + ", m = " + m + ", rgb = " + r + "," + g + "," + b);
                            if (n == 1 && m == 1) { return new int[]{x + aiCenter[0] - 63, y + aiCenter[1] - 63}; }
                            if (n == 1) {
                                m++;
                                n = 0;
                            }
                            else { n++; }
                        }
                        else {
                            found = false;
                            break;
                        }
                    }
                    if (!found) {
                        break;
                    }
                }
            }
        }
        return null;
    }
    
    private int[] searchMiniQuestArrow(int[] aiCenter) {
        
        boolean found = false;
        int     iCapturedRGB;
        int     r, g, b;
        
        BufferedImage biCapturedScreen = robot.createScreenCapture(new Rectangle(aiCenter[0] - 63, aiCenter[1] - 63, aiCenter[0] + 63, aiCenter[1] + 63));
        
        for (int y = 0; y < 125; y++) {
            for (int x = 0; x < 125; x++) {
                for (int m = 0; m <= 3; ) {
                    for (int n = 0; n <= 3; ) {
                        iCapturedRGB = biCapturedScreen.getRGB(x + n, y + m);
                        b = (iCapturedRGB) & 0xFF;
                        g = (iCapturedRGB >> 8) & 0xFF;
                        r = (iCapturedRGB >> 16) & 0xFF;
                        if ((r >= 240) && (r <= 255) && (g >= 160) && (g <= 235) && (b >= 55) && (b <= 110)) {
                            found = true;
                            // robot.mouseMove(x + aiCenter[0] - 63, y + aiCenter[1] - 63);
                            // System.out.println("(" + (x + n) + "," + (y + m) + ") " + "n = " + n + ", m = " + m + ", rgb = " + r + "," + g + "," + b);
                            if (n == 3 && m == 3) { return new int[]{x + aiCenter[0] - 63 + 2, y + aiCenter[1] - 63 + 2}; }
                            if (n == 3) {
                                m++;
                                n = 0;
                            }
                            else { n++; }
                        }
                        else {
                            found = false;
                            break;
                        }
                    }
                    if (!found) {
                        break;
                    }
                }
            }
        }
        return null;
    }
    
    @SuppressWarnings("Duplicates")
    private void turnLeft(int iTime) {
        
        if (isFlying && !isRunning) {
            peripherals.getKeyboard().typeString("j");
            isRunning = true;
        }
        robot.keyPress(37);
        robot.delay(iTime);
        robot.keyRelease(37);
    }
    
    @SuppressWarnings("Duplicates")
    private void turnRight(int iTime) {
        
        if (isFlying && !isRunning) {
            peripherals.getKeyboard().typeString("j");
            isRunning = true;
        }
        robot.keyPress(39);
        robot.delay(iTime);
        robot.keyRelease(39);
    }
    
    private void fly() throws AWTException {
        
        peripherals.getKeyboard().typeString("y");
        
        for (int n = 1; n <= 4; n++) {
            robot.keyPress(KeyEvent.VK_HOME);
            robot.keyRelease(KeyEvent.VK_HOME);
            robot.delay(i_DELAY);
        }
        
        robot.delay(2000);
        
        peripherals.getMouse().DragLeft2Right(10, InputEvent.BUTTON3_DOWN_MASK);
        
        for (int n = 1; n <= 3; n++) {
            robot.keyPress(KeyEvent.VK_END);
            robot.keyRelease(KeyEvent.VK_END);
            robot.delay(i_DELAY);
        }
        
        peripherals.getKeyboard().typeString("j");
        isRunning = true;
        robot.delay(i_DELAY);
        robot.keyPress(KEY_SPACE);
        robot.delay(10000);
        robot.keyRelease(KEY_SPACE);
        isFlying = true;
    }
    
    private void runStop() {
        
        robot.keyPress(KEY_J);
        robot.delay(i_DELAY);
        robot.keyRelease(KEY_J);
        isRunning = false;
    }
    
    //	public void land() throws AWTException {}
    
    //	public void runStart() {
    //		robot.keyPress(KEY_J);
    //		robot.delay(i_DELAY);
    //		robot.keyRelease(KEY_J);
    //		isRunning = true;
    //	}
    
    public enum Arrows {
        QUEST, ARCHEO
    }
}
