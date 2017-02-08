package net.ddns.ksuto.clockwork.entities;

import net.ddns.ksuto.clockwork.tools.ShowZone;

/**
 * Created by Kseniya! on 16/07/2016.
 */
public class Player {
    
    public final int           xPosition;
    public final int           yPosition;
    public       long          lastSeen;
    public       int           damage;
    public       ShowZone.Zone zone;
    
    public Player(int xPosition, int yPosition, long lastSeen) {
        
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.lastSeen = lastSeen;
        this.damage = 0;
    }
}

