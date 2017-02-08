package net.ddns.ksuto.clockwork.entities;

public class Key extends Dot {
    
    public final int    hitKey;
    public       String key;
    public int priority = 0;
    
    public Key(int hitKey, int xPosition, int yPosition, String key) {
        
        this.hitKey = hitKey;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.key = key;
    }
    
    Key(int hitKey) {
        
        super();
        
        this.hitKey = hitKey;
    }
}
