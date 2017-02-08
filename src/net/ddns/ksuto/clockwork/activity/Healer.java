package net.ddns.ksuto.clockwork.activity;

import net.ddns.ksuto.clockwork.entities.Player;
import net.ddns.ksuto.clockwork.tools.RGBConverter;
import net.ddns.ksuto.clockwork.tools.ShowZone;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Created by Admin on 05/04/2015!
 */
class Healer {
    
    
    private final TBoPeripheralRobotHelper peripherals;
    private int               hbMaxBarWidth  = 0;
    private int               hbMaxBarHeight = 0;
    private int               hbMinBarWidth  = 80;
    private int               hbMinBarHeight = 25;
    private long              lastSearch     = 0;
    private ArrayList<Player> raid           = new ArrayList<>();
    private Player            woundedPlayer  = null;
    private ShowZone showZone;
    
    private Healer(TBoPeripheralRobotHelper peripherals) {
        
        this.peripherals = peripherals;
        showZone = new ShowZone(peripherals);
    }
    
    public static void main(String[] args) throws AWTException, InterruptedException {
        
        TBoPeripheralRobotHelper peripherals = new TBoPeripheralRobotHelper();
        
        Healer bot = new Healer(peripherals);
        
        Robot robot = new Robot();
        for (int i = 3; i != 0; i--) {
            robot.delay(1000);
            System.out.println(i);
        }
        
        BufferedImage capturedScreen = robot.createScreenCapture(new Rectangle(0, 0, peripherals.getScreen().i_SCREEN_WIDTH, peripherals.getScreen().i_SCREEN_HEIGHT));
        double        time           = System.currentTimeMillis();
        bot.lookForHealbotMembers(capturedScreen);
        System.out.println("lookForHealbotMembers time : " + (System.currentTimeMillis() - time));
        
        for (Player p : bot.raid) {
            robot.mouseMove(p.xPosition, p.yPosition);
            robot.delay(1000);
        }
    }
    
    void searchForWoundedPlayer(BufferedImage biCapturedScreen) {
        
        Player currentWoundedPlayer;
        
        if (woundedPlayer == null) {
            woundedPlayer = watchOverPlayers(biCapturedScreen);
        }
        else {
            currentWoundedPlayer = watchOverPlayers(biCapturedScreen);
            if (currentWoundedPlayer != null && !currentWoundedPlayer.equals(woundedPlayer)) {
                woundedPlayer = currentWoundedPlayer;
                
                peripherals.robot.mouseRelease(InputEvent.BUTTON1_MASK);
                peripherals.robot.mouseRelease(InputEvent.BUTTON3_MASK);
                int x = MouseInfo.getPointerInfo().getLocation().x;
                int y = MouseInfo.getPointerInfo().getLocation().y;
                peripherals.robot.mouseMove(woundedPlayer.xPosition + 5, woundedPlayer.yPosition + 5);
                peripherals.robot.delay(100);
                peripherals.getMouse().clickLeft();
                peripherals.robot.mouseMove(x, y);
            }
        }
    }
    
    private Player watchOverPlayers(BufferedImage capturedScreen) {
        
        int    capturedRGB;
        int    r, g, b;
        Player mostWoundedPlayer = null;
    
        while ((raid.size() == 0 && System.currentTimeMillis() - lastSearch > 1000) ||
               (System.currentTimeMillis() - lastSearch > 10000)) { raid = lookForHealbotMembers(capturedScreen); }
    
        if (!raid.isEmpty()) {
            for (Player p : raid) {
                
                capturedRGB = capturedScreen.getRGB(p.xPosition, p.yPosition);
                r = (capturedRGB >> 16) & 0xFF;
                g = (capturedRGB >> 8) & 0xFF;
                b = (capturedRGB) & 0xFF;
                
                if (b < 15) {
                    p.damage = r + (255 - g);
                    
                    if (mostWoundedPlayer == null) { mostWoundedPlayer = p; }
                    
                    if (r > 100 || g > 100) {
                        p.lastSeen = System.currentTimeMillis();
                        if (mostWoundedPlayer.damage < p.damage && p.damage > 65) { mostWoundedPlayer = p; }
                    }
                }
            }
            if (mostWoundedPlayer == null) { mostWoundedPlayer = raid.get(0); }
        }
        //		System.out.println("mostWoundedPlayer : " + mostWoundedPlayer.xPosition + " " + mostWoundedPlayer.yPosition);
        return mostWoundedPlayer;
    }
    
    private ArrayList<Player> lookForHealbotMembers(BufferedImage capturedScreen) {
        
        int maxRed = 50, minGreen = 150, maxBlue = 40;
        int red, green, blue;
        
        for (int x = peripherals.getScreen().i_SCREEN_WIDTH / 2; x < peripherals.getScreen().i_SCREEN_WIDTH; x++) {
            for (int y = 0; y < peripherals.getScreen().i_SCREEN_HEIGHT; y++) {
                
                int tempWitdh  = 0;
                int tempHeight = 0;
                
                RGBConverter rgbConverter = new RGBConverter(capturedScreen, x, y).invoke();
                red = rgbConverter.getRed();
                green = rgbConverter.getGreen();
                blue = rgbConverter.getBlue();
                
                if ((red < maxRed) && (green > minGreen) && (blue < maxBlue)) {
                    
                    while ((red < maxRed) && (green > minGreen) && (blue < maxBlue)) {
                        rgbConverter = new RGBConverter(capturedScreen, x + tempWitdh++, y).invoke();
                        red = rgbConverter.getRed();
                        green = rgbConverter.getGreen();
                        blue = rgbConverter.getBlue();
                    }
                    
                    do {
                        rgbConverter = new RGBConverter(capturedScreen, x, y + tempHeight++).invoke();
                        red = rgbConverter.getRed();
                        green = rgbConverter.getGreen();
                        blue = rgbConverter.getBlue();
                    } while ((red < maxRed) && (green > minGreen) && (blue < maxBlue));
                    
                    if (tempWitdh >= hbMinBarWidth - 10 && tempHeight >= hbMinBarHeight - 5) {
                        
                        if (tempWitdh >= hbMinBarWidth) { hbMinBarWidth = tempWitdh; }
                        if (tempHeight >= hbMinBarHeight) { hbMinBarHeight = tempHeight; }
                        
                        boolean exists = false;
                        for (Player player : raid) {
                            if (x >= player.xPosition - 3 && x <= player.xPosition + 3 + hbMinBarWidth &&
                                y >= player.yPosition - 3 && y <= player.yPosition + 3 + hbMinBarHeight) { exists = true; }
                        }
                        if (!exists) {
                            if (hbMaxBarWidth < tempWitdh + 4) { hbMaxBarWidth = tempWitdh + 4; }
                            if (hbMaxBarHeight < tempHeight + 4) { hbMaxBarHeight = tempHeight + 4; }
                            
                            Player player = new Player(x, y, System.currentTimeMillis());
                            player.zone = new ShowZone.Zone(showZone, hbMaxBarWidth, hbMaxBarHeight, x - 2, y - 2);
                            showZone.addZone(player.zone);
                            raid.add(player);
                            System.out.println(hbMinBarWidth + " " + hbMinBarHeight);
                        }
                    }
                }
            }
        }
        
        int iFound = raid.size();
        for (int i = raid.size() - 1; i >= 0; i--) {
            Player player = raid.get(i);
            
            System.out.println("delta lastseen : " + (System.currentTimeMillis() - player.lastSeen));
            if (System.currentTimeMillis() - player.lastSeen > 1000) {
                iFound--;
            }
            if (System.currentTimeMillis() - player.lastSeen > 10000) {
                if (!player.zone.isWarning()) { player.zone.setWarning(); }
            }
            else {
                if (!player.zone.isOK()) { player.zone.setOK(); }
            }
            if (System.currentTimeMillis() - player.lastSeen > 180000) {
                showZone.removeZone(player.zone);
                raid.remove(i);
            }
        }
        
        System.out.println("raid size = " + raid.size() + ", found = " + iFound);
        lastSearch = System.currentTimeMillis();
        return raid;
    }
}

