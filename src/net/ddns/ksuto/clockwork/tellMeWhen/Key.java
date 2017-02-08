package net.ddns.ksuto.clockwork.tellMeWhen;

public class Key {
    
    public final int     hitKey;
    public       int     xPosition;
    public       int     yPosition;
    public       int     color;
    public       boolean ShiftModifier;
    public       String  key;
    
    Key(int hitKey) {
        
        this.hitKey = hitKey;
        this.ShiftModifier = false;
    }
}
