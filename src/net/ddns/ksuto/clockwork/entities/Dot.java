package net.ddns.ksuto.clockwork.entities;

/**
 * Created by thomas.bouchardon on 07/02/2017!
 */
public class Dot {
    
    public int xPosition;
    public int yPosition;
    public boolean active = false;
    
    public Dot(int xPosition, int yPosition) {
        
        this.xPosition = xPosition;
        this.yPosition = yPosition;
    }
    
    Dot() {
        
    }
}
