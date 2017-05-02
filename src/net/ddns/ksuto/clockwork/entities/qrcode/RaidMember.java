package net.ddns.ksuto.clockwork.entities.qrcode;

/**
 * Created by thomas.bouchardon on 02/05/2017!
 */
public class RaidMember extends Key {
    
    public int index;
    public boolean alt   = false;
    public boolean ctrl  = false;
    public boolean shift = false;
    
    public RaidMember(int hitKey, int xPosition, int yPosition, String key, boolean alt, boolean ctrl, boolean shift, int index) {
        
        super(hitKey, xPosition, yPosition, key);
        this.alt = alt;
        this.ctrl = ctrl;
        this.shift = shift;
        this.index = index;
    }
}
