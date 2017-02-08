package net.ddns.ksuto.clockwork.ksuto;

import static net.ddns.ksuto.prh.properties.Constants.i_DELAY;

import net.ddns.ksuto.clockwork.entities.Dot;
import net.ddns.ksuto.clockwork.entities.Key;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrateur on 19/05/15!
 */
public class Ksuto {
    
    private final ArrayList<Key> alKeys = new ArrayList<>();
    public Dot position;
    public Dot TOGGLE_ON_OFF;
    public Dot TARGET_NEAREST_ENEMY;
    public Dot ADD_WAYPOINT;
    public Dot CLEAR_WAYPOINTS;
    public Dot DRIVE_MOD;
    public Dot DRIVE_LOOP;
    public Dot DEBUG_MOD;
    public Position       currenPosition = new Position();
    public List<Position> path           = new ArrayList<>();
    
    public static void typeInChat(TBoPeripheralRobotHelper peripherals, String s) {
        
        
        peripherals.robot.keyPress(KeyEvent.VK_ENTER);
        peripherals.robot.keyRelease(KeyEvent.VK_ENTER);
        peripherals.robot.delay(i_DELAY);
        peripherals.getKeyboard().typeString(s);
        peripherals.robot.delay(i_DELAY);
        peripherals.robot.keyPress(KeyEvent.VK_ENTER);
        peripherals.robot.keyRelease(KeyEvent.VK_ENTER);
    }
    
    static void startKsuto(TBoPeripheralRobotHelper peripherals, ArrayList<int[]> wowPosition) {
        
        peripherals.robot.mouseMove(wowPosition.get(0)[0] + 50, wowPosition.get(0)[1] + 50);
        peripherals.robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        peripherals.robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        peripherals.robot.delay(i_DELAY);
        
        peripherals.robot.keyPress(KeyEvent.VK_ENTER);
        peripherals.robot.keyRelease(KeyEvent.VK_ENTER);
        peripherals.robot.delay(i_DELAY);
        peripherals.getKeyboard().typeString("/kto");
        peripherals.robot.delay(i_DELAY);
        peripherals.robot.keyPress(KeyEvent.VK_ENTER);
        peripherals.robot.keyRelease(KeyEvent.VK_ENTER);
        
        openCloseKsuto(peripherals);
    }
    
    static void openCloseKsuto(TBoPeripheralRobotHelper peripherals) {
        
        typeInChat(peripherals, "/kto");
    }
    
    void add(Key key) {
        
        alKeys.add(key);
    }
    
    public ArrayList<Key> getKeys() {
        
        return alKeys;
    }
    
    public class Position {
        
        public int xPos_Xxxx = 0;
        public int xPos_xXxx = 0;
        public int xPos_xxXx = 0;
        public int xPos_xxxX = 0;
        
        public int yPos_Xxxx = 0;
        public int yPos_xXxx = 0;
        public int yPos_xxXx = 0;
        public int yPos_xxxX = 0;
        
        public void clear() {
            
            xPos_Xxxx = 0;
            xPos_xXxx = 0;
            xPos_xxXx = 0;
            xPos_xxxX = 0;
            
            yPos_Xxxx = 0;
            yPos_xXxx = 0;
            yPos_xxXx = 0;
            yPos_xxxX = 0;
        }
        
        public double[] getCoordinates() {
            
            double[] cooridnates = new double[2];
            
            double x = 0L, y = 0L;
            
            x += (double) xPos_Xxxx * 10L;
            x += (double) xPos_xXxx;
            x += (double) xPos_xxXx / 10L;
            x += (double) xPos_xxxX / 100L;
            
            y += (double) yPos_Xxxx * 10L;
            y += (double) yPos_xXxx;
            y += (double) yPos_xxXx / 10L;
            y += (double) yPos_xxxX / 100L;
            
            cooridnates[0] = x;
            cooridnates[1] = y;
            
            return cooridnates;
        }
    }
}

