package net.ddns.ksuto.clockwork.entities.qrcode;

import static net.ddns.ksuto.prh.properties.Constants.i_DELAY;

import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;
import net.ddns.ksuto.prh.entities.ColorBlock;
import net.ddns.ksuto.tools.TboTools_Debug;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;

public class ConfigureQrCode {
    
    @SuppressWarnings("FieldCanBeLocal")
    private final int    i_DEBUG_DELAY = 0;
    private final QrCode qrCode        = new QrCode();
    private       int    iXKsuto       = 0, iYKsuto = 0;
    private TBoPeripheralRobotHelper peripherals;
    
    public ConfigureQrCode() throws AWTException {
        
        peripherals = new TBoPeripheralRobotHelper();
    }
    
    public void run() throws AWTException, IOException {
        
        Robot robot = peripherals.robot;
    
        TboTools_Debug.sout("Starting AutoConfig");
    
        ArrayList<ColorBlock> ksutoPosition = null;
        boolean               bFound        = false;

        for (int i = 7; i >= 0; i--) {
            ksutoPosition = peripherals.getScreen().searchColorBlocks(0, 255, 0, 14, 0);
            if (!ksutoPosition.isEmpty()) {
                break;
            }

            if (i == 4) {
                QrCode.openCloseKsuto(peripherals);
            }

            robot.delay(750);
        }
        
        if (!ksutoPosition.isEmpty()) {
            
            iXKsuto = ksutoPosition.get(0).xPosition;
            iYKsuto = ksutoPosition.get(0).yPosition;
    
            qrCode.position = new Dot(iXKsuto, iYKsuto);
    
            TboTools_Debug.sout("Found QrCode : X = " + iXKsuto + ", Y = " + iYKsuto + ", carrying on.");
            peripherals.robot.mouseMove(iXKsuto, iYKsuto);
            
            robot.delay(i_DELAY + i_DEBUG_DELAY);
            bFound = true;
        }
        
        if (bFound) {
            
            robot.delay(i_DELAY);

            // L'ordre d'ajout correspond à l'ordre de priorité. L'interface WoW et celui-ci doivent correspondre.
            qrCode.addKey(new Key(KeyEvent.VK_T, iXKsuto + 6, iYKsuto + 4, "T"));
            qrCode.addKey(new Key(KeyEvent.VK_G, iXKsuto + 5, iYKsuto + 4, "G"));
            qrCode.addKey(new Key(KeyEvent.VK_Q, iXKsuto + 4, iYKsuto + 4, "Q"));
            qrCode.addKey(new Key(KeyEvent.VK_D, iXKsuto + 3, iYKsuto + 4, "D"));
            qrCode.addKey(new Key(KeyEvent.VK_H, iXKsuto + 2, iYKsuto + 4, "H"));
    
            qrCode.addKey(new Key(KeyEvent.VK_EQUALS, iXKsuto + 13, iYKsuto + 5, "="));
            qrCode.addKey(new Key(KeyEvent.VK_RIGHT_PARENTHESIS, iXKsuto + 12, iYKsuto + 5, ")"));
            qrCode.addKey(new Key(KeyEvent.VK_0, iXKsuto + 11, iYKsuto + 5, "0"));
            qrCode.addKey(new Key(KeyEvent.VK_9, iXKsuto + 10, iYKsuto + 5, "9"));
            qrCode.addKey(new Key(KeyEvent.VK_8, iXKsuto + 9, iYKsuto + 5, "8"));
            qrCode.addKey(new Key(KeyEvent.VK_7, iXKsuto + 8, iYKsuto + 5, "7"));
            qrCode.addKey(new Key(KeyEvent.VK_6, iXKsuto + 7, iYKsuto + 5, "6"));
            qrCode.addKey(new Key(KeyEvent.VK_5, iXKsuto + 6, iYKsuto + 5, "5"));
            qrCode.addKey(new Key(KeyEvent.VK_4, iXKsuto + 5, iYKsuto + 5, "4"));
            qrCode.addKey(new Key(KeyEvent.VK_3, iXKsuto + 4, iYKsuto + 5, "3"));
            qrCode.addKey(new Key(KeyEvent.VK_2, iXKsuto + 3, iYKsuto + 5, "2"));
            qrCode.addKey(new Key(KeyEvent.VK_1, iXKsuto + 2, iYKsuto + 5, "1"));
    
            qrCode.inCombat = new Dot(iXKsuto + 2, iYKsuto + 2);
            qrCode.health = new Dot(iXKsuto + 12, iYKsuto + 2);
            qrCode.mana = new Dot(iXKsuto + 13, iYKsuto + 2);
            qrCode.TOGGLE_ON_OFF = new Dot(iXKsuto + 2, iYKsuto + 13);
            qrCode.TARGET_NEAREST_ENEMY = new Dot(iXKsuto + 3, iYKsuto + 13);
            qrCode.ADD_WAYPOINT = new Dot(iXKsuto + 4, iYKsuto + 13);
            qrCode.CLEAR_WAYPOINTS = new Dot(iXKsuto + 5, iYKsuto + 13);
            qrCode.DRIVE_MOD = new Dot(iXKsuto + 6, iYKsuto + 13);
            qrCode.DRIVE_LOOP = new Dot(iXKsuto + 7, iYKsuto + 13);
            qrCode.DEBUG_MOD = new Dot(iXKsuto + 13, iYKsuto + 13);
    
            QrCode.startKsuto(peripherals, qrCode.position);
    
            TboTools_Debug.sout("AutoConfig Done");
    
            //            QrCode.pressKey(peripherals, KeyEvent.VK_ESCAPE);
        }
        else {
            TboTools_Debug.sout("AutoConfig Failed");
        }
    }
    
    public QrCode getQrCode() {
        
        return qrCode;
    }
}
