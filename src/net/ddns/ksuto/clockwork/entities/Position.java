package net.ddns.ksuto.clockwork.entities;

/**
 * Created by thomas.bouchardon on 09/02/2017!
 */
public class Position {
    
    public int xPos = 0;
    public int yPos = 0;
    
    public Position(Position currenPosition) {
    
        this.xPos = currenPosition.xPos;
        this.yPos = currenPosition.yPos;
    }
    
    public Position() {
        
    }
    
    public void clear() {
    
        xPos = 0;
        yPos = 0;
    }
}
