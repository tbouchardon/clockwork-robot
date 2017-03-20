package net.ddns.ksuto.clockwork.activity;

import net.ddns.ksuto.clockwork.ClockWork_UI;
import net.ddns.ksuto.clockwork.ClockWork_UI.Status;
import net.ddns.ksuto.clockwork.entities.Key;
import net.ddns.ksuto.clockwork.entities.Position;
import net.ddns.ksuto.clockwork.entities.middleman.QrCode;
import net.ddns.ksuto.clockwork.tools.RGBConverter;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class Clockwork_Figure {
    
    private static final int iGreyColor = 100;
    private final ClockWork_UI             clockWork_UI;
    private final TBoPeripheralRobotHelper peripherals;
    public Status status = Status.RUN;
    private Robot  robot;
    private TomTom tomtom;
    private boolean wasInCombat = false;
    
    public Clockwork_Figure(TBoPeripheralRobotHelper peripherals, ClockWork_UI autoHitControl) {
        
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
    
            QrCode qrCode = clockWork_UI.getQrCode();
    
            robot.delay(100);
    
            biCapturedScreen = robot.createScreenCapture(new Rectangle(0, 0, peripherals.getScreen().i_SCREEN_WIDTH, peripherals.getScreen().i_SCREEN_HEIGHT));
    
            //File outputfile = new File("saved.png");
            //ImageIO.write(biCapturedScreen, "png", outputfile);
    
            if (status == Status.FISHING) {
                fish();
            }
    
            findSomethingToDo(biCapturedScreen, qrCode);
        }
    }
    
    private void findSomethingToDo(BufferedImage biCapturedScreen, QrCode qrCode) throws AWTException {
        
        // On ne fait rien si L'addon n'est pas visible
        if (biCapturedScreen.getRGB(qrCode.position.xPosition, qrCode.position.yPosition) != -16711936) {
            return;
        }
        
        qrCode.inCombat.active = biCapturedScreen.getRGB(qrCode.inCombat.xPosition, qrCode.inCombat.yPosition) == RGBConverter.WHITE;
        
        RGBConverter rgbConverter = new RGBConverter(biCapturedScreen, qrCode.health.xPosition, qrCode.health.yPosition);
        rgbConverter.invoke();
        //System.out.println(rgbConverter.getRed());
        double health = 100D / 255D * (double) rgbConverter.getRed();
        rgbConverter = new RGBConverter(biCapturedScreen, qrCode.mana.xPosition, qrCode.mana.yPosition);
        rgbConverter.invoke();
        //System.out.println(rgbConverter.getBlue());
        double mana = 100D / 255D * (double) rgbConverter.getBlue();
        
        qrCode.TOGGLE_ON_OFF.active = biCapturedScreen.getRGB(qrCode.TOGGLE_ON_OFF.xPosition, qrCode.TOGGLE_ON_OFF.yPosition) == RGBConverter.WHITE;
        qrCode.TARGET_NEAREST_ENEMY.active = biCapturedScreen.getRGB(qrCode.TARGET_NEAREST_ENEMY.xPosition, qrCode.TARGET_NEAREST_ENEMY.yPosition) == RGBConverter.WHITE;
        qrCode.ADD_WAYPOINT.active = biCapturedScreen.getRGB(qrCode.ADD_WAYPOINT.xPosition, qrCode.ADD_WAYPOINT.yPosition) == RGBConverter.WHITE;
        qrCode.CLEAR_WAYPOINTS.active = biCapturedScreen.getRGB(qrCode.CLEAR_WAYPOINTS.xPosition, qrCode.CLEAR_WAYPOINTS.yPosition) == RGBConverter.WHITE;
        qrCode.DRIVE_MOD.active = biCapturedScreen.getRGB(qrCode.DRIVE_MOD.xPosition, qrCode.DRIVE_MOD.yPosition) == RGBConverter.WHITE;
        qrCode.DRIVE_LOOP.active = biCapturedScreen.getRGB(qrCode.DRIVE_LOOP.xPosition, qrCode.DRIVE_LOOP.yPosition) == RGBConverter.WHITE;
        qrCode.DEBUG_MOD.active = biCapturedScreen.getRGB(qrCode.DEBUG_MOD.xPosition, qrCode.DEBUG_MOD.yPosition) == RGBConverter.RED;
        
        if (!qrCode.TOGGLE_ON_OFF.active) {
            return;
        }
        
        if (qrCode.ADD_WAYPOINT.active) {
            tomtom.getCoordinates(qrCode, peripherals);
            qrCode.path.add(new Position(qrCode.currenPlayerPosition));
            String output = "";
            for (Position position : qrCode.path) {
                String formattedX = String.format("%06d", position.xPos);
                String formattedY = String.format("%06d", position.yPos);
                output += formattedX.substring(0, 2) + "," + formattedX.substring(2, 4) + "-" +
                          formattedY.substring(0, 2) + "," + formattedY.substring(2, 4) + ";";
            }
            System.out.println(output);
            QrCode.typeInChat(peripherals, "/kto wpadded");
            peripherals.robot.delay(500);
        }
        
        if (qrCode.CLEAR_WAYPOINTS.active) {
            qrCode.path.clear();
            QrCode.typeInChat(peripherals, "/kto wpcleared");
            peripherals.robot.delay(500);
        }
        
        Key     key2hit       = null;
        boolean shiftModifier = false;
        boolean ctrlModifier  = false;
        boolean altModifier   = false;
        
        for (Key key : qrCode.getKeys()) {
    
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
                robot.keyPress(KeyEvent.VK_CONTROL);
            }
            
            if (shiftModifier) {
                robot.keyPress(KeyEvent.VK_SHIFT);
            }
            
            if (altModifier) {
                robot.keyPress(KeyEvent.VK_ALT);
            }
            
            clockWork_UI.setTextField(key2hit.key);
            clockWork_UI.setiGrey(iGreyColor);
    
            QrCode.pressKey(peripherals, key2hit.hitKey);
            
            if (ctrlModifier) {
                robot.keyRelease(KeyEvent.VK_CONTROL);
            }
            
            if (shiftModifier) {
                robot.keyRelease(KeyEvent.VK_SHIFT);
            }
            
            if (altModifier) {
                robot.keyRelease(KeyEvent.VK_ALT);
            }
        }
        
        if (qrCode.TARGET_NEAREST_ENEMY.active && key2hit == null) {
            QrCode.pressKey(peripherals, KeyEvent.VK_TAB);
        }
        
        if (wasInCombat && !qrCode.inCombat.active) { tryToLoot();}
        
        if (qrCode.DRIVE_MOD.active && health > 50 && !qrCode.inCombat.active) { // && mana > 40
            if (tomtom == null) {
                tomtom = new TomTom(peripherals);
            }
            tomtom.drive(qrCode, peripherals, key2hit != null);
        }
        
        wasInCombat = qrCode.inCombat.active;
    
        if (key2hit == null) { peripherals.robot.delay(200); }
        else { peripherals.robot.delay(750); }
    }
    
    private void tryToLoot() {
        
        peripherals.robot.delay(500);
        
        int hitZoneX = peripherals.getScreen().i_SCREEN_WIDTH / 2 + (int) ((double) peripherals.getScreen().i_SCREEN_WIDTH / 100d * 4.6875);
        int hitZoneY = peripherals.getScreen().i_SCREEN_HEIGHT / 2 + (int) ((double) peripherals.getScreen().i_SCREEN_HEIGHT / 100d * 14.8148);
        
        peripherals.robot.keyPress(KeyEvent.VK_SHIFT);
        peripherals.getMouse().clickRight(hitZoneX, hitZoneY);
        peripherals.getMouse().clickRight(hitZoneX, hitZoneY - 100);
        peripherals.getMouse().clickRight(hitZoneX, hitZoneY + 100);
        peripherals.getMouse().clickRight(hitZoneX - 100, hitZoneY);
        peripherals.getMouse().clickRight(hitZoneX + 100, hitZoneY);
        peripherals.robot.keyRelease(KeyEvent.VK_SHIFT);
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
