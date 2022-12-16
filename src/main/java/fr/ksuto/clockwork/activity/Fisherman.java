package fr.ksuto.clockwork.activity;

import static fr.ksuto.prh.properties.Constants.i_DELAY;

import fr.ksuto.bot.generated.enums.InterfaceEnum;
import fr.ksuto.clockwork.ClockWorkUI;
import fr.ksuto.clockwork.tools.ShowZone;
import fr.ksuto.prh.PeripheralRobotHelper;
import fr.ksuto.prh.entities.Position;
import fr.ksuto.prh.helpers.PictureSearch;
import fr.ksuto.prh.peripherals.Screen;
import fr.ksuto.prh.research.paralelism.CaptureScheduler;
import fr.ksuto.tools.Debug;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.imageio.ImageIO;

public class Fisherman {
    
    public static final int                   BAIT_TIME       = 5 * 60000; // Shift + W
    public static final int                   LURE_TIME       = 10 * 60000; // W
    public static final int                   STARTING_HEIGHT = 90;
    public static final int                   STARTING_WIDTH  = 180;
    public static final int                   Y_OFFSET        = -190;
    private final       PeripheralRobotHelper peripherals;
    private final       ClockWorkUI           ui;
    int heightOffset = 0;
    int widthOffset  = 0;
    private long     lCurrentBaitTime = 0;
    private long     lCurrentLureTime = 0;
    private ShowZone show;
    
    Fisherman(ClockWorkUI ui, PeripheralRobotHelper peripherals) {
        
        this.ui = ui;
        
        this.peripherals = peripherals;
    }
    
    boolean fish(CaptureScheduler captureScheduler) {
    
        int[] currentCoordinates;
        int[] initialCoordinates;
        int[] averagePosition;
        int   loop = 1;
    
        if (new Date().getTime() - lCurrentLureTime > LURE_TIME) {
            lCurrentLureTime = new Date().getTime();
            ui.appendLog("w");
            peripherals.getKeyboard().pressKey(KeyEvent.VK_W);
            peripherals.robot.delay(3000);
        }
    
        if (new Date().getTime() - lCurrentBaitTime > BAIT_TIME) {
            lCurrentBaitTime = new Date().getTime();
            ui.appendLog("W");
            peripherals.getKeyboard().pressKey(KeyEvent.VK_W, false, false, true);
            peripherals.robot.delay(1000);
        }
    
        ui.appendLog("h");
        peripherals.getKeyboard().pressKey(KeyEvent.VK_H);
    
        long fishingTime = new Date().getTime();
        long currentTime = new Date().getTime();
    
        peripherals.robot.delay(2500);
    
        currentCoordinates = searchBobber(captureScheduler.getLastImage(), true, null);
        initialCoordinates = currentCoordinates;
        averagePosition = currentCoordinates;
    
        if (initialCoordinates == null) {
            Debug.sout("Bobber not found =(");
            
            ShowZone.Zone zone             = show.zoneList.get(0);
            BufferedImage biCapturedScreen = peripherals.robot.createScreenCapture(new Rectangle(zone.getX1(), zone.getY1(), zone.getWidth(), zone.getHeight()));
        
            try {
                File outputfile = new File(System.currentTimeMillis() + ".jpg");
                ImageIO.write(biCapturedScreen, "png", outputfile);
            }
            catch (IOException ignored) {
                Debug.sout("ERROR  : Écriture impossible !");
            }
            
            return true;
        }
        
        peripherals.getMouse().move(initialCoordinates[0] + 5, initialCoordinates[1] + 5);
        peripherals.robot.delay(2000);
        
        int  maxDistance      = 0;
        Long blobberFoundTime = null;
        
        while (currentTime - fishingTime < 21000) {
            currentCoordinates = searchBobber(captureScheduler.getLastImage(), false, averagePosition);
            
            if (currentCoordinates == null) {
                
                Debug.sout("Blobber lost, trying to catch anyway.");
                ui.appendLog("ϡ?");
                peripherals.getMouse().clickLeft();
                peripherals.robot.delay(2000);
                return true;
            }
            
            averagePosition[0] = (averagePosition[0] * (loop - 1) + currentCoordinates[0]) / loop;
            averagePosition[1] = (averagePosition[1] * (loop - 1) + currentCoordinates[1]) / loop;
            
            int distance = (int) Point2D.distance(averagePosition[0], averagePosition[1], currentCoordinates[0], currentCoordinates[1]);
            
            peripherals.robot.delay(100);
            // Check if mouse moved (and shall exit fishing modh)
            if (Point2D.distance(MouseInfo.getPointerInfo().getLocation().x, MouseInfo.getPointerInfo().getLocation().y, initialCoordinates[0], initialCoordinates[1]) > 20) {
                
                Debug.sout("Mouse moved, exiting. (" + MouseInfo.getPointerInfo().getLocation().x + " != " + initialCoordinates[0] + ")");
                return false;
            }
            
            if (maxDistance < distance) {
                maxDistance = distance;
            }
            if (blobberFoundTime == null && distance >= 6) {blobberFoundTime = new Date().getTime();}
            if (blobberFoundTime != null && new Date().getTime() - blobberFoundTime > 1000) {
    
                Debug.sout("Got a catch ? Bobber Moving (distance = " + maxDistance + ")");
                Debug.sout("loops/s : " + (loop / ((currentTime - fishingTime) / 1000)));
                
                ui.appendLog("ϡ" + maxDistance);
                peripherals.getMouse().clickLeft();
                peripherals.robot.delay(2000);
                return true;
            }
            
            currentTime = new Date().getTime();
            if ((currentCoordinates[0] == 0) && (currentTime - fishingTime > 4000)) {
                
                Debug.sout("Bobber not found after 4 seconds");
                return true;
            }
            
            loop++;
        }
        
        return true;
    }
    
    void leave() {
        
        show.dispose();
        
        for (int n = 1; n <= 3; n++) {
            peripherals.getKeyboard().pressKey(KeyEvent.VK_END);
            peripherals.robot.delay(300);
        }
        peripherals.getKeyboard().pressKey(KeyEvent.VK_X);
    }
    
    void setup() throws AWTException {
    
        Position resultsWoW = PictureSearch.getDefault(InterfaceEnum.WOW).search().getFirstResult().getFirstPosition();
        if (resultsWoW != null) {
            int iWoWSize = Screen.SCREEN_WIDTH - (resultsWoW.getX() * 2);
            Debug.sout("WoW Width = " + iWoWSize);
        }
    
        show = new ShowZone(peripherals);
        ShowZone.Zone zone = new ShowZone.Zone("Fishing Zone",
                                               STARTING_WIDTH,
                                               STARTING_HEIGHT,
                                               Screen.SCREEN_WIDTH / 2 - STARTING_WIDTH / 2,
                                               Screen.SCREEN_HEIGHT / 2 - STARTING_HEIGHT / 2 + Y_OFFSET,
                                               150, 150, 200, 0);
        show.addZone(zone);
    
        peripherals.robot.delay(i_DELAY);
        peripherals.robot.mouseMove(Screen.SCREEN_WIDTH / 2, Screen.SCREEN_HEIGHT / 2 + 10);
        peripherals.robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        peripherals.robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        peripherals.robot.delay(i_DELAY);
    
        peripherals.getKeyboard().pressKey(KeyEvent.VK_X);
    
        for (int n = 1; n <= 4; n++) {
            peripherals.getKeyboard().pressKey(KeyEvent.VK_END);
            peripherals.robot.delay(300);
        }
    
        peripherals.getKeyboard().pressKey(KeyEvent.VK_HOME);
        peripherals.robot.delay(300);
    }
    
    private int[] searchBobber(BufferedImage biCapturedScreen, boolean autoIncreaseSearchArea, int[] knownCoordinates) {
    
        int iCapturedRGB;
        int r;
        int g;
        int b;
    
        if (show.zoneList.isEmpty()) {return new int[0];}
        ShowZone.Zone searchZone = show.zoneList.get(0);
    
        int x1 = searchZone.getX1();
        int x2 = searchZone.getX2();
        int y1 = searchZone.getY1();
        int y2 = searchZone.getY2();
    
        if (knownCoordinates != null) {
            x1 = knownCoordinates[0] - 10;
            x2 = knownCoordinates[0] + 10;
            y1 = knownCoordinates[1] - 10;
            y2 = knownCoordinates[1] + 10;
        }
    
        for (int count = 0; count < 4; count++) {
            for (int y = y1; y < y2; y++) {
                for (int x = x1; x < x2; x++) {
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
                                if ((b + 10 > r) && (b + 10 > g)) {return new int[]{x + 1, y};}
                            }
                            n++;
                        }
                        else {break;}
                    }
                }
            }
            
            if (autoIncreaseSearchArea) {
                widthOffset += 9;
                heightOffset += 3;
            }
            
            peripherals.robot.delay(250);
            
            if (STARTING_WIDTH + widthOffset > Screen.SCREEN_WIDTH / 3) {
                widthOffset = Screen.SCREEN_WIDTH / 3 - STARTING_WIDTH;
                heightOffset = widthOffset / 3;
            }
            
            show.changeZoneSize(STARTING_WIDTH + widthOffset,
                                STARTING_HEIGHT + heightOffset,
                                Screen.SCREEN_WIDTH / 2 - (STARTING_WIDTH + widthOffset) / 2,
                                Screen.SCREEN_HEIGHT / 2 - (STARTING_HEIGHT + heightOffset) / 2 + Y_OFFSET);
        }
        return new int[0];
    }
}
