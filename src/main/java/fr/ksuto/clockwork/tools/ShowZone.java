package fr.ksuto.clockwork.tools;

import fr.ksuto.clockwork.activity.Fisherman;
import fr.ksuto.prh.PeripheralRobotHelper;
import fr.ksuto.prh.peripherals.Screen;
import lombok.Data;

import java.awt.*;
import java.util.ArrayList;

import javax.swing.*;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("serial")
public class ShowZone extends JFrame {
    
    //private JPanel panel;
    public  ArrayList<Zone>       zoneList = new ArrayList<>();
    private PeripheralRobotHelper peripherals;
    private PaintPane             paintPane;
    
    public ShowZone(PeripheralRobotHelper peripherals) {
        
        this.peripherals = peripherals;
        
        setAlwaysOnTop(true);
        setUndecorated(true);
        setSize(Screen.SCREEN_WIDTH, Screen.SCREEN_HEIGHT);
        setLocation(0, 0);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setBackground(new Color(0, 0, 0, 0));
        paintPane = new PaintPane();
        add(paintPane);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    public static void main(String[] args) throws AWTException {
        
        PeripheralRobotHelper peripheralRobotHelper = new PeripheralRobotHelper();
        
        ShowZone show = new ShowZone(peripheralRobotHelper);
        ShowZone.Zone zone = new ShowZone.Zone("Fishing Zone",
                                               Fisherman.STARTING_WIDTH,
                                               Fisherman.STARTING_HEIGHT,
                                               Screen.SCREEN_WIDTH / 2 - Fisherman.STARTING_WIDTH / 2,
                                               Screen.SCREEN_HEIGHT / 2 - Fisherman.STARTING_HEIGHT / 2 + Fisherman.Y_OFFSET,
                                               150, 150, 200, 0);
        show.addZone(zone);
        
        //        while (true) {
        //            peripheralRobotHelper.robot.delay(500);
        //            int offset = new Random().ints(50, 200)
        //                                 .findFirst()
        //                                 .getAsInt();
        //            show.changeZoneSize(startWidth + offset,
        //                                startHeight + offset,
        //                                Screen.SCREEN_WIDTH / 2 - (startWidth / 2) - offset / 2,
        //                                Screen.SCREEN_HEIGHT / 2 - (startHeight) + Fisherman.Y_OFFSET - offset / 2);
        //        }
    }
    
    public void addZone(Zone zone) {
        
        zoneList.add(zone);
        paintPane.repaint();
    }
    
    public void changeZoneSize(int width, int height, int xPosition, int yPosition) {
        
        if (zoneList.size() != 1) {return;}
        
        Zone zone = zoneList.get(0);
        
        changeZoneSize(zone, width, height, xPosition, yPosition);
    }
    
    public void changeZoneSize(Zone zone, int width, int height, int xPosition, int yPosition) {
        //panel.setPreferredSize(new Dimension(iWidth, iHeight));
        zone.width = width;
        zone.height = height;
        zone.xPosition = xPosition;
        zone.yPosition = yPosition;
        repaint();
    }
    
    public void removeZone(Zone zone) {
        
        zoneList.remove(zone);
        paintPane.repaint();
    }
    
    public void setError(Zone zone) {
        
        zone.status = Zone.ZoneStatus.ERROR;
        paintPane.repaint();
    }
    
    public void setOK(Zone zone) {
        
        zone.status = Zone.ZoneStatus.OK;
        paintPane.repaint();
    }
    
    public void setWarning(Zone zone) {
        
        zone.status = Zone.ZoneStatus.WARNING;
        paintPane.repaint();
    }
    
    @Data
    public static class Zone {
        
        ZoneStatus status = ZoneStatus.OK;
        String     name   = "";
        int        xPosition;
        int        yPosition;
        int        red    = 100;
        int        green  = 255;
        int        blue   = 255;
        int        alpha  = 1;
        private int width;
        private int height;
        
        public Zone(int width, int height, int xPosition, int yPosition) {
            
            this.width = width;
            this.height = height;
            this.xPosition = xPosition;
            this.yPosition = yPosition;
        }
        
        public Zone(String name, int width, int height, int xPosition, int yPosition) {
            
            this.name = name;
            this.width = width;
            this.height = height;
            this.xPosition = xPosition;
            this.yPosition = yPosition;
        }
        
        public Zone(String name, int width, int height, int xPosition, int yPosition, int red, int green, int blue, int alpha) {
            
            this.name = name;
            this.width = width;
            this.height = height;
            this.xPosition = xPosition;
            this.yPosition = yPosition;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
        }
        
        public int getX1() {
            
            return xPosition;
        }
        
        public int getX2() {
            
            return xPosition + width;
        }
        
        public int getY1() {
            
            return yPosition;
        }
        
        public int getY2() {
            
            return yPosition + height;
        }
        
        public boolean isError() {
            
            return status == Zone.ZoneStatus.ERROR;
        }
        
        public boolean isOK() {
            
            return status == Zone.ZoneStatus.OK;
        }
        
        public boolean isWarning() {
            
            return status == Zone.ZoneStatus.WARNING;
        }
        
        private enum ZoneStatus {
            OK,
            WARNING,
            ERROR
        }
    }
    
    private class PaintPane extends JPanel {
        
        PaintPane() {
            
            setOpaque(false);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(new Color(0, 0, 0, 0));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setStroke(new BasicStroke(2));
            
            for (Zone zone : zoneList) {
    
                if (zone.isOK()) {g2d.setColor(new Color(zone.red, zone.green, zone.blue));}
                if (zone.isWarning()) {g2d.setColor(Color.ORANGE);}
                if (zone.isError()) {g2d.setColor(Color.RED);}
    
                g2d.drawLine(zone.xPosition, zone.yPosition, zone.xPosition + zone.width, zone.yPosition);
                g2d.drawLine(zone.xPosition + zone.width, zone.yPosition, zone.xPosition + zone.width, zone.yPosition + zone.height);
                g2d.drawLine(zone.xPosition + zone.width, zone.yPosition + zone.height, zone.xPosition, zone.yPosition + zone.height);
                g2d.drawLine(zone.xPosition, zone.yPosition + zone.height, zone.xPosition, zone.yPosition);
    
                if (StringUtils.isNotBlank(zone.name)) {
                    g2d.drawString(zone.name, zone.xPosition, zone.yPosition - 5);
                }
            }
            
            g2d.dispose();
        }
        
        @Override
        public Dimension getPreferredSize() {
            
            return new Dimension(peripherals.getScreen().SCREEN_WIDTH, peripherals.getScreen().SCREEN_HEIGHT);
        }
    }
}
