package net.ddns.ksuto.clockwork.activity;

import net.ddns.ksuto.clockwork.ClockWork_UI;
import net.ddns.ksuto.clockwork.ClockWork_UI.Status;
import net.ddns.ksuto.clockwork.entities.Key;
import net.ddns.ksuto.clockwork.entities.Position;
import net.ddns.ksuto.clockwork.entities.middleman.MiddleMan;
import net.ddns.ksuto.clockwork.tools.RGBConverter;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class Clockwork {
    
    private static final int iGreyColor = 100;
    private final ClockWork_UI             clockWork_UI;
    private final TBoPeripheralRobotHelper peripherals;
    public Status status = Status.RUN;
    private Robot  robot;
    private TomTom tomtom;
    
    public Clockwork(TBoPeripheralRobotHelper peripherals, ClockWork_UI autoHitControl) {
        
        this.peripherals = peripherals;
        this.clockWork_UI = autoHitControl;
        try {
            tomtom = new TomTom(peripherals);
        }
        catch (AWTException e) {
            e.printStackTrace();
        }
    }
    
    public void play() throws AWTException {
        
        robot = new Robot();
        
        BufferedImage biCapturedScreen;
        
        //noinspection InfiniteLoopStatement
        while (true) {
    
            MiddleMan middleMan = clockWork_UI.getMiddleMan();
    
            robot.delay(100);
    
            biCapturedScreen = robot.createScreenCapture(new Rectangle(0, 0, peripherals.getScreen().i_SCREEN_WIDTH, peripherals.getScreen().i_SCREEN_HEIGHT));
    
            //File outputfile = new File("saved.png");
            //ImageIO.write(biCapturedScreen, "png", outputfile);
    
            if (status == Status.FISHING) {
                fish();
            }
    
            findSomethingToDo(biCapturedScreen, middleMan);
        }
    }
    
    private void findSomethingToDo(BufferedImage biCapturedScreen, MiddleMan middleMan) throws AWTException {
        
        // On ne fait rien si L'addon n'est pas visible
        if (biCapturedScreen.getRGB(middleMan.position.xPosition, middleMan.position.yPosition) != -16711936) {
            return;
        }
        
        middleMan.inCombat.active = biCapturedScreen.getRGB(middleMan.inCombat.xPosition, middleMan.inCombat.yPosition) == RGBConverter.WHITE;
        
        RGBConverter rgbConverter = new RGBConverter(biCapturedScreen, middleMan.health.xPosition, middleMan.health.yPosition);
        rgbConverter.invoke();
        //System.out.println(rgbConverter.getRed());
        double health = 100D / 255D * (double) rgbConverter.getRed();
        rgbConverter = new RGBConverter(biCapturedScreen, middleMan.mana.xPosition, middleMan.mana.yPosition);
        rgbConverter.invoke();
        //System.out.println(rgbConverter.getBlue());
        double mana = 100D / 255D * (double) rgbConverter.getBlue();
        
        middleMan.TOGGLE_ON_OFF.active = biCapturedScreen.getRGB(middleMan.TOGGLE_ON_OFF.xPosition, middleMan.TOGGLE_ON_OFF.yPosition) == RGBConverter.WHITE;
        middleMan.TARGET_NEAREST_ENEMY.active = biCapturedScreen.getRGB(middleMan.TARGET_NEAREST_ENEMY.xPosition, middleMan.TARGET_NEAREST_ENEMY.yPosition) == RGBConverter.WHITE;
        middleMan.ADD_WAYPOINT.active = biCapturedScreen.getRGB(middleMan.ADD_WAYPOINT.xPosition, middleMan.ADD_WAYPOINT.yPosition) == RGBConverter.WHITE;
        middleMan.CLEAR_WAYPOINTS.active = biCapturedScreen.getRGB(middleMan.CLEAR_WAYPOINTS.xPosition, middleMan.CLEAR_WAYPOINTS.yPosition) == RGBConverter.WHITE;
        middleMan.DRIVE_MOD.active = biCapturedScreen.getRGB(middleMan.DRIVE_MOD.xPosition, middleMan.DRIVE_MOD.yPosition) == RGBConverter.WHITE;
        middleMan.DRIVE_LOOP.active = biCapturedScreen.getRGB(middleMan.DRIVE_LOOP.xPosition, middleMan.DRIVE_LOOP.yPosition) == RGBConverter.WHITE;
        middleMan.DEBUG_MOD.active = biCapturedScreen.getRGB(middleMan.DEBUG_MOD.xPosition, middleMan.DEBUG_MOD.yPosition) == RGBConverter.RED;
        
        if (!middleMan.TOGGLE_ON_OFF.active) {
            return;
        }
        
        if (middleMan.ADD_WAYPOINT.active) {
            tomtom.getCoordinates(middleMan, peripherals);
            middleMan.path.add(new Position(middleMan.currenPlayerPosition));
            String output = "";
            for (Position position : middleMan.path) {
                String formattedX = String.format("%06d", position.xPos);
                String formattedY = String.format("%06d", position.yPos);
                output += formattedX.substring(0, 2) + "," + formattedX.substring(2, 4) + "-" +
                          formattedY.substring(0, 2) + "," + formattedY.substring(2, 4) + ";";
            }
            System.out.println(output);
            MiddleMan.typeInChat(peripherals, "/kto wpadded");
            peripherals.robot.delay(500);
        }
        
        if (middleMan.CLEAR_WAYPOINTS.active) {
            middleMan.path.clear();
            MiddleMan.typeInChat(peripherals, "/kto wpcleared");
            peripherals.robot.delay(500);
        }
        
        Key     key2hit       = null;
        boolean shiftModifier = false;
        boolean ctrlModifier  = false;
        boolean altModifier   = false;
        
        for (Key key : middleMan.getKeys()) {
    
            int iCapturedRGB = biCapturedScreen.getRGB(key.xPosition, key.yPosition); //-1 == white && -16777216 == black
    
            if (!ctrlModifier) {
                if (iCapturedRGB == RGBConverter.GREEN) {
                    ctrlModifier = true;
                    key2hit = key;
                }
        
                if (!shiftModifier) {
                    if (iCapturedRGB == RGBConverter.RED) {
                        shiftModifier = true;
                        key2hit = key;
                    }
            
                    if (!altModifier) {
                        if (iCapturedRGB == RGBConverter.BLUE) {
                            altModifier = true;
                            key2hit = key;
                        }
                
                        if (iCapturedRGB == RGBConverter.WHITE && key2hit == null) {
                            key2hit = key;
                        }
                    }
                }
            }
        }
        
        if (key2hit != null) {
            
            if (ctrlModifier) {
                robot.keyPress(Event.CTRL_MASK);
            }
            
            if (shiftModifier) {
                robot.keyPress(Event.SHIFT_MASK);
            }
            
            if (altModifier) {
                robot.keyPress(Event.ALT_MASK);
            }
            
            clockWork_UI.setTextField(key2hit.key);
            clockWork_UI.setiGrey(iGreyColor);
            
            MiddleMan.pressKey(peripherals, key2hit.hitKey);
            
            if (ctrlModifier) {
                robot.keyRelease(Event.CTRL_MASK);
            }
            
            if (shiftModifier) {
                robot.keyRelease(Event.SHIFT_MASK);
            }
            
            if (altModifier) {
                robot.keyRelease(Event.ALT_MASK);
            }
        }
        
        if (middleMan.TARGET_NEAREST_ENEMY.active && key2hit == null) {
            MiddleMan.pressKey(peripherals, KeyEvent.VK_TAB);
        }
        
        if (middleMan.DRIVE_MOD.active && health > 50 && mana > 40 && !middleMan.inCombat.active) {
            if (tomtom == null) {
                tomtom = new TomTom(peripherals);
            }
            tomtom.drive(middleMan, peripherals, key2hit != null);
        }
        
        if (key2hit == null) { peripherals.robot.delay(250); }
        else { peripherals.robot.delay(750); }
    }
    
    private void fish() throws AWTException {
        
        Fisher natPagle = new Fisher(peripherals);
        natPagle.setup();
        boolean bKeepFishing = true;
        while (bKeepFishing) {
            bKeepFishing = natPagle.fish();
        }
        natPagle.leave();
        if (status == Status.FISHING) {
            clockWork_UI.dojButtonFishClick();
        }
    }
}
