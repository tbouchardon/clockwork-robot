package net.ddns.ksuto.clockwork.entities;

/**
 * Created by thomas.bouchardon on 09/02/2017!
 */
public class Position {
    
    public int xPos_Xxxx = 0;
    public int xPos_xXxx = 0;
    public int xPos_xxXx = 0;
    public int xPos_xxxX = 0;
    
    public int yPos_Xxxx = 0;
    public int yPos_xXxx = 0;
    public int yPos_xxXx = 0;
    public int yPos_xxxX = 0;
    
    public Position(Position currenPosition) {
        
        this.xPos_Xxxx = currenPosition.xPos_Xxxx;
        this.xPos_xXxx = currenPosition.xPos_xXxx;
        this.xPos_xxXx = currenPosition.xPos_xxXx;
        this.xPos_xxxX = currenPosition.xPos_xxxX;
        
        this.yPos_Xxxx = currenPosition.yPos_Xxxx;
        this.yPos_xXxx = currenPosition.yPos_xXxx;
        this.yPos_xxXx = currenPosition.yPos_xxXx;
        this.yPos_xxxX = currenPosition.yPos_xxxX;
    }
    
    public Position() {
        
    }
    
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
