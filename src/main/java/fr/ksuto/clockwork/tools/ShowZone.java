package fr.ksuto.clockwork.tools;

import fr.ksuto.prh.PeripheralRobotHelper;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
public class ShowZone extends JFrame {
    
    private PeripheralRobotHelper peripherals;
    //private JPanel panel;
    private ArrayList<Zone>       zoneList = new ArrayList<>();
    private PaintPane             paintPane;
    
    public ShowZone(PeripheralRobotHelper peripherals) {
        
        this.peripherals = peripherals;
        
        setAlwaysOnTop(true);
        setUndecorated(true);
        try {
            Method method;
            method = Class.forName("com.sun.awt.AWTUtilities").getMethod("setWindowOpaque", Window.class, boolean.class);
            method.invoke(null, this, false);
        }
        catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        
        setSize(peripherals.getScreen().SCREEN_WIDTH, peripherals.getScreen().SCREEN_HEIGHT);
        setLocation(0, 0);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        paintPane = new PaintPane();
        add(paintPane);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    public static void main(String[] args) throws InterruptedException, IOException, AWTException {
        
        final Dimension dim_D         = new Dimension(Toolkit.getDefaultToolkit().getScreenSize());
        int             SCREEN_WIDTH  = (int) dim_D.getWidth();
        int             SCREEN_HEIGHT = (int) dim_D.getHeight();
        
        int      iWidth  = 100;
        int      iHeight = 50;
        ShowZone show    = new ShowZone(new PeripheralRobotHelper());
        show.addZone(new Zone(show, 100, 50, SCREEN_WIDTH / 2 - iWidth / 2, SCREEN_HEIGHT / 2 - iHeight));
    }
    
    public void addZone(Zone zone) {
        
        zoneList.add(zone);
        paintPane.repaint();
    }
    
    public void changeSize(int iWidth, int iHeight, int iXPosition, int iYPosition) {
        //panel.setPreferredSize(new Dimension(iWidth, iHeight));
        setSize(iWidth, iHeight);
        setLocation(iXPosition, iYPosition);
        // repaint();
    }
    
    public void removeZone(Zone zone) {
        
        zoneList.remove(zone);
        paintPane.repaint();
    }
    
    public void zone(String sName, int iWidth, int iHeight, int iXPosition, int iYPosition, int iRColor, int iGColor, int iBColor, int iOpacity) {
        
        setLayout(new GridBagLayout());
        setAlwaysOnTop(true);
        setUndecorated(true);
        setLocationRelativeTo(null);
        setLocation(iXPosition, iYPosition);
        
        TitledBorder tb = new TitledBorder(sName);
        tb.setBorder(new LineBorder(new Color(iRColor, iGColor, iBColor)));
        tb.setTitleColor(new Color(iRColor, iGColor, iBColor));
        
        setVisible(true);
        
        setBackground(new Color(0, 0, 0, 0));
        setSize(iWidth, iHeight);
    }
    
    public static class Zone {
        
        ZoneStatus zoneStatus = ZoneStatus.OK;
        ShowZone   showZone;
        
        String name    = "";
        int    width;
        int    height;
        int    xPosition;
        int    yPosition;
        int    rColor  = 100;
        int    gColor  = 255;
        int    bColor  = 255;
        int    opacity = 1;
        
        public Zone(ShowZone showZone, int width, int height, int xPosition, int yPosition) {
            
            this.showZone = showZone;
            this.width = width;
            this.height = height;
            this.xPosition = xPosition;
            this.yPosition = yPosition;
        }
        
        public Zone(ShowZone showZone, String name, int width, int height, int xPosition, int yPosition) {
            
            this.showZone = showZone;
            this.name = name;
            this.width = width;
            this.height = height;
            this.xPosition = xPosition;
            this.yPosition = yPosition;
        }
        
        public Zone(ShowZone showZone, String name, int width, int height, int xPosition, int yPosition, int rColor, int gColor, int bColor, int opacity) {
            
            this.showZone = showZone;
            this.name = name;
            this.width = width;
            this.height = height;
            this.xPosition = xPosition;
            this.yPosition = yPosition;
            this.rColor = rColor;
            this.gColor = gColor;
            this.bColor = bColor;
            this.opacity = opacity;
        }
        
        public void setError() {
            
            zoneStatus = ZoneStatus.ERROR;
            showZone.paintPane.repaint();
        }
        
        public void setOK() {
            
            zoneStatus = ZoneStatus.OK;
            showZone.paintPane.repaint();
        }
        
        public void setWarning() {
            
            zoneStatus = ZoneStatus.WARNING;
            showZone.paintPane.repaint();
        }
        
        public boolean isError() {
            
            return zoneStatus == ZoneStatus.ERROR;
        }
        
        public boolean isOK() {
            
            return zoneStatus == ZoneStatus.OK;
        }
        
        public boolean isWarning() {
            
            return zoneStatus == ZoneStatus.WARNING;
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
                
                if (zone.isOK()) {g2d.setColor(new Color(zone.rColor, zone.gColor, zone.bColor));}
                if (zone.isWarning()) {g2d.setColor(Color.ORANGE);}
                if (zone.isError()) {g2d.setColor(Color.RED);}
                
                g2d.drawLine(zone.xPosition, zone.yPosition, zone.xPosition + zone.width, zone.yPosition);
                g2d.drawLine(zone.xPosition + zone.width, zone.yPosition, zone.xPosition + zone.width, zone.yPosition + zone.height);
                g2d.drawLine(zone.xPosition + zone.width, zone.yPosition + zone.height, zone.xPosition, zone.yPosition + zone.height);
                g2d.drawLine(zone.xPosition, zone.yPosition + zone.height, zone.xPosition, zone.yPosition);
            }
            
            g2d.dispose();
        }
        
        @Override
        public Dimension getPreferredSize() {
            
            return new Dimension(peripherals.getScreen().SCREEN_WIDTH, peripherals.getScreen().SCREEN_HEIGHT);
        }
    }
}
