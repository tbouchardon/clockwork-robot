package fr.ksuto.clockwork.entities;

/**
 * Created by thomas.bouchardon on 09/02/2017!
 */
public class ClkPosition {
    
    public int xPos = 0;
    public int yPos = 0;
    
    public ClkPosition(ClkPosition currenPosition) {
        
        this.xPos = currenPosition.xPos;
        this.yPos = currenPosition.yPos;
    }
    
    public ClkPosition() {
        
    }
    
    public void clear() {
        
        xPos = 0;
        yPos = 0;
    }
}
