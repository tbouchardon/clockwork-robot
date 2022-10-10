package fr.ksuto.clockwork.entities.qrcode;

public class Key extends Dot {
    
    public int    hitKey;
    public String key;
    public int    priority = 0;
    
    public Key() {
    
    }
    
    public Key(int hitKey, int xPosition, int yPosition, String key) {
        
        this.hitKey = hitKey;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.key = key;
    }
    
    public Key(int hitKey) {
        
        this.hitKey = hitKey;
    }
    
    public Key(int hitKey, String key) {
        
        this.hitKey = hitKey;
        this.key = key;
    }
}
