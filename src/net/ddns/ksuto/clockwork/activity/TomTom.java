package net.ddns.ksuto.clockwork.activity;

import net.ddns.ksuto.clockwork.ksuto.Ksuto;
import net.ddns.ksuto.clockwork.tools.RGBConverter;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class TomTom {
    
    private static final int KEY_J = 74, KEY_Y = 89, KEY_SPACE = 32;
    private final int i_DELAY = 100;
    private TBoPeripheralRobotHelper peripherals;
    private boolean  isFlying         = false;
    private boolean  isRunning        = false;
    private double[] lastPlayerCoords = null;
    private Double   lastDistance     = null;
    private int      pathIndex        = 0;
    
    TomTom(TBoPeripheralRobotHelper peripherals) throws AWTException {
        
        this.peripherals = peripherals;
    }
    
    public void addCurrentPositionToPathList(Ksuto ksuto, TBoPeripheralRobotHelper peripherals) {
        
        getCoordinates(ksuto, peripherals);
    }
    
    public void getCoordinates(Ksuto ksuto, TBoPeripheralRobotHelper peripherals) {
        
        BufferedImage biCapturedScreen = peripherals.robot.createScreenCapture(new Rectangle(0, 0, peripherals.getScreen().i_SCREEN_WIDTH, peripherals.getScreen().i_SCREEN_HEIGHT));
        
        for (int i = 2; i < 10; i++) {
            if (biCapturedScreen.getRGB(ksuto.position.xPosition + i, ksuto.position.yPosition + 5) == RGBConverter.WHITE) {
                ksuto.currenPosition.xPos_Xxxx = i;
                break;
            }
        }
        
        for (int i = 2; i < 10; i++) {
            if (biCapturedScreen.getRGB(ksuto.position.xPosition + i, ksuto.position.yPosition + 6) == RGBConverter.WHITE) {
                ksuto.currenPosition.xPos_xXxx = i;
                break;
            }
        }
        
        for (int i = 2; i < 10; i++) {
            if (biCapturedScreen.getRGB(ksuto.position.xPosition + i, ksuto.position.yPosition + 7) == RGBConverter.WHITE) {
                ksuto.currenPosition.xPos_xxXx = i;
                break;
            }
        }
        
        for (int i = 2; i < 10; i++) {
            if (biCapturedScreen.getRGB(ksuto.position.xPosition + i, ksuto.position.yPosition + 8) == RGBConverter.WHITE) {
                ksuto.currenPosition.xPos_xxxX = i;
                break;
            }
        }
        
        for (int i = 2; i < 10; i++) {
            if (biCapturedScreen.getRGB(ksuto.position.xPosition + i, ksuto.position.yPosition + 9) == RGBConverter.WHITE) {
                ksuto.currenPosition.yPos_Xxxx = i;
                break;
            }
        }
        
        for (int i = 2; i < 10; i++) {
            if (biCapturedScreen.getRGB(ksuto.position.xPosition + i, ksuto.position.yPosition + 10) == RGBConverter.WHITE) {
                ksuto.currenPosition.yPos_xXxx = i;
                break;
            }
        }
        
        for (int i = 2; i < 10; i++) {
            if (biCapturedScreen.getRGB(ksuto.position.xPosition + i, ksuto.position.yPosition + 11) == RGBConverter.WHITE) {
                ksuto.currenPosition.yPos_xxXx = i;
                break;
            }
        }
        
        for (int i = 2; i < 10; i++) {
            if (biCapturedScreen.getRGB(ksuto.position.xPosition + i, ksuto.position.yPosition + 12) == RGBConverter.WHITE) {
                ksuto.currenPosition.yPos_xxxX = i;
                break;
            }
        }
    }
    
    void drive(Ksuto ksuto, TBoPeripheralRobotHelper peripherals, boolean activeTarget) {
        
        if (isRunning && activeTarget) {
            runStop();
            return;
        }
        
        if (!isRunning && activeTarget) { return; }
        
        if (ksuto.path.isEmpty()) { return; }
        
        if (ksuto.DRIVE_LOOP.active && pathIndex > ksuto.path.size() - 1) { pathIndex = 0;}
        
        if (pathIndex > ksuto.path.size() - 1) {
            ksuto.path.clear();
            return;
        }
        
        Ksuto.Position path = ksuto.path.get(pathIndex);
        
        getCoordinates(ksuto, peripherals);
        
        double[] playerCoords = ksuto.currenPosition.getCoordinates();
        if (lastPlayerCoords == null) {
            lastPlayerCoords = playerCoords;
            runStart();
            return;
        }
        
        double a = (path.getCoordinates()[0] - lastPlayerCoords[0]) / (path.getCoordinates()[1] - lastPlayerCoords[1]); // (yB - yA) / (xB - xA)
        double b = path.getCoordinates()[1] - (a * path.getCoordinates()[0]);
        
        double y = a * playerCoords[0] + b;
        
        if (y < playerCoords[1]) { turnRight(250); }
        else { turnLeft(250); }
        
        double distance = Math.sqrt(Math.pow(path.getCoordinates()[0] - playerCoords[0], 2) + Math.pow(path.getCoordinates()[1] - playerCoords[1], 2));
        if (lastDistance != null && distance > lastDistance) {
            runStop();
            turnRight(2000);
            runStart();
        }
        
        lastDistance = distance;
        lastPlayerCoords = playerCoords;
    }
    
    private void runStop() {
        
        peripherals.robot.keyPress(Event.DOWN);
        peripherals.robot.delay(i_DELAY);
        peripherals.robot.keyRelease(Event.DOWN);
        isRunning = false;
    }
    
    private void runStart() {
        
        peripherals.robot.keyPress(KEY_J);
        peripherals.robot.delay(i_DELAY);
        peripherals.robot.keyRelease(KEY_J);
        isRunning = true;
    }
    
    @SuppressWarnings("Duplicates")
    private void turnLeft(int iTime) {
        
        if (isFlying && !isRunning) {
            peripherals.getKeyboard().typeString("j");
            isRunning = true;
        }
        peripherals.robot.keyPress(37);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(37);
    }
    
    @SuppressWarnings("Duplicates")
    private void turnRight(int iTime) {
        
        if (isFlying && !isRunning) {
            peripherals.getKeyboard().typeString("j");
            isRunning = true;
        }
        peripherals.robot.keyPress(39);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(39);
    }
    
    //	public void land() throws AWTException {}
    
    private void fly() throws AWTException {
        
        peripherals.getKeyboard().typeString("y");
        
        for (int n = 1; n <= 4; n++) {
            peripherals.robot.keyPress(KeyEvent.VK_HOME);
            peripherals.robot.keyRelease(KeyEvent.VK_HOME);
            peripherals.robot.delay(i_DELAY);
        }
    
        peripherals.robot.delay(2000);
        
        peripherals.getMouse().DragLeft2Right(10, InputEvent.BUTTON3_DOWN_MASK);
        
        for (int n = 1; n <= 3; n++) {
            peripherals.robot.keyPress(KeyEvent.VK_END);
            peripherals.robot.keyRelease(KeyEvent.VK_END);
            peripherals.robot.delay(i_DELAY);
        }
        
        peripherals.getKeyboard().typeString("j");
        isRunning = true;
        peripherals.robot.delay(i_DELAY);
        peripherals.robot.keyPress(KEY_SPACE);
        peripherals.robot.delay(10000);
        peripherals.robot.keyRelease(KEY_SPACE);
        isFlying = true;
    }
    
    private void pressKey(int iKey) {
        
        peripherals.robot.keyPress(iKey);
        peripherals.robot.keyRelease(iKey);
        peripherals.robot.delay(500);
    }
}
