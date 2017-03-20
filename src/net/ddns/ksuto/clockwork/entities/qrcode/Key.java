package net.ddns.ksuto.clockwork.entities.qrcode;

public class Key extends Dot {
    
    public final int    hitKey;
    public       String key;
    public int priority = 0;
    
    Key(int hitKey, int xPosition, int yPosition, String key) {
        
        this.hitKey = hitKey;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.key = key;
    }
    
    Key(int hitKey) {
        
        this.hitKey = hitKey;
    }
}
