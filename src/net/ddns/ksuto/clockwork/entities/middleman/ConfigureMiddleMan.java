package net.ddns.ksuto.clockwork.entities.middleman;

import static net.ddns.ksuto.prh.properties.Constants.i_DELAY;

import net.ddns.ksuto.clockwork.entities.Dot;
import net.ddns.ksuto.clockwork.entities.Key;
import net.ddns.ksuto.clockwork.tools.Scanner;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;

public class ConfigureMiddleMan {
    
    @SuppressWarnings("FieldCanBeLocal")
    private final int       i_DEBUG_DELAY = 0;
    private final MiddleMan middleMan     = new MiddleMan();
    private       int       iXKsuto       = 0, iYKsuto = 0;
    private TBoPeripheralRobotHelper peripherals;
    
    public ConfigureMiddleMan() throws AWTException {
        
        peripherals = new TBoPeripheralRobotHelper();
    }
    
    public void run() throws AWTException, IOException {
        
        Robot robot = peripherals.robot;
        
        System.out.println("Starting AutoConfig");
        
        Scanner                  scan          = new Scanner(peripherals);
        ArrayList<int[]>         wowPosition   = null;
        ArrayList<Scanner.Block> ksutoPosition = null;
        boolean                  bFound        = false;
        
        for (int j = 10; j >= 0; j--) {
            wowPosition = scan.searchPicture("/Pictures/scan.WoW.vanilla.png");
            //if (wowPosition.isEmpty()) {
            //  wowPosition = scan.searchPicture("/Pictures/scan.WoW.Legion.png");
            //}
            ksutoPosition = scan.searchColorBlocks(0, 255, 0, 16, 16);
            if (!wowPosition.isEmpty()) {
                break;
            }
            robot.delay(1000);
        }
        
        if (!wowPosition.isEmpty() && ksutoPosition.isEmpty()) {
            System.out.println("Found WoW : X = " + wowPosition.get(0)[0] + ", Y = " + wowPosition.get(0)[0]);
            
            for (int i = 10; i >= 0; i--) {
                ksutoPosition = scan.searchColorBlocks(0, 255, 0, 10, 0);
                if (!ksutoPosition.isEmpty()) { break; }
    
                if (i == 5) { MiddleMan.openCloseKsuto(peripherals); }
                
                robot.delay(1000);
            }
        }
        
        if (!wowPosition.isEmpty() && !ksutoPosition.isEmpty()) {
            
            iXKsuto = ksutoPosition.get(0).xPosition;
            iYKsuto = ksutoPosition.get(0).yPosition;
    
            middleMan.position = new Dot(iXKsuto, iYKsuto);
    
            System.out.println("Found MiddleMan : X = " + iXKsuto + ", Y = " + iYKsuto + ", carrying on.");
            peripherals.robot.mouseMove(iXKsuto, iYKsuto);
            
            robot.delay(i_DELAY + i_DEBUG_DELAY);
            bFound = true;
        }
        
        if (bFound) {
            
            robot.delay(i_DELAY);
    
            middleMan.add(new Key(KeyEvent.VK_T, iXKsuto + 6, iYKsuto + 3, "T"));
            middleMan.add(new Key(KeyEvent.VK_G, iXKsuto + 5, iYKsuto + 3, "G"));
            middleMan.add(new Key(KeyEvent.VK_Q, iXKsuto + 4, iYKsuto + 3, "Q"));
            middleMan.add(new Key(KeyEvent.VK_D, iXKsuto + 3, iYKsuto + 3, "D"));
            middleMan.add(new Key(KeyEvent.VK_H, iXKsuto + 2, iYKsuto + 3, "H"));
    
            middleMan.add(new Key(KeyEvent.VK_EQUALS, iXKsuto + 13, iYKsuto + 4, "="));
            middleMan.add(new Key(KeyEvent.VK_RIGHT_PARENTHESIS, iXKsuto + 12, iYKsuto + 4, ")"));
            middleMan.add(new Key(KeyEvent.VK_0, iXKsuto + 11, iYKsuto + 4, "0"));
            middleMan.add(new Key(KeyEvent.VK_9, iXKsuto + 10, iYKsuto + 4, "9"));
            middleMan.add(new Key(KeyEvent.VK_8, iXKsuto + 9, iYKsuto + 4, "8"));
            middleMan.add(new Key(KeyEvent.VK_7, iXKsuto + 8, iYKsuto + 4, "7"));
            middleMan.add(new Key(KeyEvent.VK_6, iXKsuto + 7, iYKsuto + 4, "6"));
            middleMan.add(new Key(KeyEvent.VK_5, iXKsuto + 6, iYKsuto + 4, "5"));
            middleMan.add(new Key(KeyEvent.VK_4, iXKsuto + 5, iYKsuto + 4, "4"));
            middleMan.add(new Key(KeyEvent.VK_3, iXKsuto + 4, iYKsuto + 4, "3"));
            middleMan.add(new Key(KeyEvent.VK_2, iXKsuto + 3, iYKsuto + 4, "2"));
            middleMan.add(new Key(KeyEvent.VK_1, iXKsuto + 2, iYKsuto + 4, "1"));
    
            middleMan.TOGGLE_ON_OFF = new Dot(iXKsuto + 2, iYKsuto + 13);
            middleMan.TARGET_NEAREST_ENEMY = new Dot(iXKsuto + 3, iYKsuto + 13);
            middleMan.ADD_WAYPOINT = new Dot(iXKsuto + 4, iYKsuto + 13);
            middleMan.CLEAR_WAYPOINTS = new Dot(iXKsuto + 5, iYKsuto + 13);
            middleMan.DRIVE_MOD = new Dot(iXKsuto + 6, iYKsuto + 13);
            middleMan.DRIVE_LOOP = new Dot(iXKsuto + 7, iYKsuto + 13);
            middleMan.DEBUG_MOD = new Dot(iXKsuto + 13, iYKsuto + 13);
    
            MiddleMan.startKsuto(peripherals, wowPosition);
            
            System.out.println("AutoConfig Done");
        }
        else {
            System.out.println("AutoConfig Failed");
        }
    }
    
    public MiddleMan getMiddleMan() {
        
        return middleMan;
    }
}
