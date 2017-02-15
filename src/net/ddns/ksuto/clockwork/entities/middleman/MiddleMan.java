package net.ddns.ksuto.clockwork.entities.middleman;

import static net.ddns.ksuto.prh.properties.Constants.i_DELAY;

import net.ddns.ksuto.clockwork.entities.Dot;
import net.ddns.ksuto.clockwork.entities.Key;
import net.ddns.ksuto.clockwork.entities.Position;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrateur on 19/05/15!
 */
public class MiddleMan {
    
    private final ArrayList<Key> alKeys = new ArrayList<>();
    public Dot position;
    public Dot TOGGLE_ON_OFF;
    public Dot TARGET_NEAREST_ENEMY;
    public Dot ADD_WAYPOINT;
    public Dot CLEAR_WAYPOINTS;
    public Dot DRIVE_MOD;
    public Dot DRIVE_LOOP;
    public Dot DEBUG_MOD;
    public Dot inCombat;
    public Dot health;
    public Dot mana;
    public Position       currenPlayerPosition = new Position();
    public List<Position> path                 = new ArrayList<>();
    
    public static void typeInChat(TBoPeripheralRobotHelper peripherals, String s) {
        
        
        peripherals.robot.keyPress(KeyEvent.VK_ENTER);
        peripherals.robot.keyRelease(KeyEvent.VK_ENTER);
        peripherals.robot.delay(i_DELAY);
        peripherals.getKeyboard().typeString(s);
        peripherals.robot.delay(i_DELAY);
        peripherals.robot.keyPress(KeyEvent.VK_ENTER);
        peripherals.robot.keyRelease(KeyEvent.VK_ENTER);
    }
    
    public static void pressKey(TBoPeripheralRobotHelper peripherals, int iKey) {
        
        peripherals.robot.keyPress(iKey);
        peripherals.robot.delay(i_DELAY);
        peripherals.robot.keyRelease(iKey);
        peripherals.robot.delay(i_DELAY);
    }
    
    static void startKsuto(TBoPeripheralRobotHelper peripherals, Dot dot) {
        
        peripherals.robot.mouseMove(dot.xPosition, dot.yPosition);
        peripherals.robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        peripherals.robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        peripherals.robot.delay(i_DELAY);
        
        openCloseKsuto(peripherals);
    }
    
    static void openCloseKsuto(TBoPeripheralRobotHelper peripherals) {
    
        typeInChat(peripherals, "/kto toggle");
    }
    
    void addKey(Key key) {
        
        alKeys.add(key);
    }
    
    public ArrayList<Key> getKeys() {
        
        return alKeys;
    }
}

