package fr.ksuto.clockwork.activity;

import fr.ksuto.clockwork.entities.Player;
import fr.ksuto.clockwork.tools.ShowZone;
import fr.ksuto.prh.PeripheralRobotHelper;
import fr.ksuto.prh.capture.Capture;
import fr.ksuto.prh.capture.Frame;
import fr.ksuto.prh.capture.Rgb;
import fr.ksuto.prh.peripherals.Screen;
import fr.ksuto.tools.Debug;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;

/**
 * Created by Admin on 05/04/2015!
 */
class Healer {
    
    private final PeripheralRobotHelper peripherals;
    private       int                   hbMaxBarWidth  = 0;
    private       int                   hbMaxBarHeight = 0;
    private       int                   hbMinBarWidth  = 80;
    private       int                   hbMinBarHeight = 25;
    private       long                  lastSearch     = 0;
    private       ArrayList<Player>     raid           = new ArrayList<>();
    private       Player                woundedPlayer  = null;
    private       ShowZone              healbotZone;
    
    private Healer(PeripheralRobotHelper peripherals) {
    
        this.peripherals = peripherals;
        healbotZone = new ShowZone(peripherals);
    }
    
    public static void main(String[] args) throws AWTException {
        
        PeripheralRobotHelper peripherals = new PeripheralRobotHelper();
        
        Healer bot = new Healer(peripherals);
        
        Robot robot = new Robot();
        for (int i = 3; i != 0; i--) {
            robot.delay(1000);
            Debug.sout("" + i);
        }
        
        Frame         capturedScreen = Capture.screen();
        double        time           = System.currentTimeMillis();
        bot.lookForHealbotMembers(capturedScreen);
        Debug.sout("lookForHealbotMembers time : " + (System.currentTimeMillis() - time));
        
        for (Player p : bot.raid) {
            robot.mouseMove(p.xPosition, p.yPosition);
            robot.delay(1000);
        }
    }
    
    void searchForWoundedPlayer(Frame biCapturedScreen) {
        
        Player currentWoundedPlayer;
        
        if (woundedPlayer == null) {
            woundedPlayer = watchOverPlayers(biCapturedScreen);
        }
        else {
            currentWoundedPlayer = watchOverPlayers(biCapturedScreen);
            if (currentWoundedPlayer != null && !currentWoundedPlayer.equals(woundedPlayer)) {
                woundedPlayer = currentWoundedPlayer;
    
                peripherals.robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                peripherals.robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                int x = MouseInfo.getPointerInfo().getLocation().x;
                int y = MouseInfo.getPointerInfo().getLocation().y;
                peripherals.robot.mouseMove(woundedPlayer.xPosition + 5, woundedPlayer.yPosition + 5);
                peripherals.robot.delay(100);
                peripherals.getMouse().clickLeft();
                peripherals.robot.mouseMove(x, y);
            }
        }
    }
    
    private ArrayList<Player> lookForHealbotMembers(Frame capturedScreen) {
    
        int maxRed   = 50;
        int minGreen = 150;
        int maxBlue  = 40;
        int red;
        int green;
        int blue;
    
        for (int x = Screen.SCREEN_WIDTH / 2; x < Screen.SCREEN_WIDTH; x++) {
            for (int y = 0; y < Screen.SCREEN_HEIGHT; y++) {
            
                int tempWitdh  = 0;
                int tempHeight = 0;
            
                red = capturedScreen.red(x, y);
                green = capturedScreen.green(x, y);
                blue = capturedScreen.blue(x, y);
            
                if ((red < maxRed) && (green > minGreen) && (blue < maxBlue)) {
                    
                    while ((red < maxRed) && (green > minGreen) && (blue < maxBlue)) {
                        int rgb = capturedScreen.rgb(x + tempWitdh++, y);
                        red = Rgb.red(rgb);
                        green = Rgb.green(rgb);
                        blue = Rgb.blue(rgb);
                    }
                    
                    do {
                        int rgb = capturedScreen.rgb(x, y + tempHeight++);
                        red = Rgb.red(rgb);
                        green = Rgb.green(rgb);
                        blue = Rgb.blue(rgb);
                    } while ((red < maxRed) && (green > minGreen) && (blue < maxBlue));
                    
                    if (tempWitdh >= hbMinBarWidth - 10 && tempHeight >= hbMinBarHeight - 5) {
                        
                        if (tempWitdh >= hbMinBarWidth) {hbMinBarWidth = tempWitdh;}
                        if (tempHeight >= hbMinBarHeight) {hbMinBarHeight = tempHeight;}
                        
                        boolean exists = false;
                        for (Player player : raid) {
                            if (x >= player.xPosition - 3 && x <= player.xPosition + 3 + hbMinBarWidth &&
                                y >= player.yPosition - 3 && y <= player.yPosition + 3 + hbMinBarHeight) {exists = true;}
                        }
                        if (!exists) {
                            if (hbMaxBarWidth < tempWitdh + 4) {hbMaxBarWidth = tempWitdh + 4;}
                            if (hbMaxBarHeight < tempHeight + 4) {hbMaxBarHeight = tempHeight + 4;}
    
                            Player player = new Player(x, y, System.currentTimeMillis());
                            player.zone = new ShowZone.Zone(hbMaxBarWidth, hbMaxBarHeight, x - 2, y - 2);
                            healbotZone.addZone(player.zone);
                            raid.add(player);
                            Debug.sout(hbMinBarWidth + " " + hbMinBarHeight);
                        }
                    }
                }
            }
        }
        
        int iFound = raid.size();
        for (int i = raid.size() - 1; i >= 0; i--) {
            Player player = raid.get(i);
            
            Debug.sout("delta lastseen : " + (System.currentTimeMillis() - player.lastSeen));
            if (System.currentTimeMillis() - player.lastSeen > 1000) {
                iFound--;
            }
            if (System.currentTimeMillis() - player.lastSeen > 10000) {
                if (!player.zone.isWarning()) {healbotZone.setWarning(player.zone);}
            }
            else {
                if (!player.zone.isOK()) {healbotZone.setOK(player.zone);}
            }
            if (System.currentTimeMillis() - player.lastSeen > 180000) {
                healbotZone.removeZone(player.zone);
                raid.remove(i);
            }
        }
        
        Debug.sout("raid size = " + raid.size() + ", found = " + iFound);
        lastSearch = System.currentTimeMillis();
        return raid;
    }
    
    private Player watchOverPlayers(Frame capturedScreen) {
    
        int    capturedRGB;
        int    r;
        int    g;
        int    b;
        Player mostWoundedPlayer = null;
    
        while ((raid.isEmpty() && System.currentTimeMillis() - lastSearch > 1000) ||
               (System.currentTimeMillis() - lastSearch > 10000)) {raid = lookForHealbotMembers(capturedScreen);}
    
        if (!raid.isEmpty()) {
            for (Player p : raid) {
            
                capturedRGB = capturedScreen.rgb(p.xPosition, p.yPosition);
                r = Rgb.red(capturedRGB);
                g = Rgb.green(capturedRGB);
                b = Rgb.blue(capturedRGB);
                
                if (b < 15) {
                    p.damage = r + (255 - g);
                    
                    if (mostWoundedPlayer == null) {mostWoundedPlayer = p;}
                    
                    if (r > 100 || g > 100) {
                        p.lastSeen = System.currentTimeMillis();
                        if (mostWoundedPlayer.damage < p.damage && p.damage > 65) {mostWoundedPlayer = p;}
                    }
                }
            }
            if (mostWoundedPlayer == null) {mostWoundedPlayer = raid.get(0);}
        }
        return mostWoundedPlayer;
    }
}

