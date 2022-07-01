package fr.ksuto.clockwork.tools;

import java.awt.image.BufferedImage;

/**
 * Created by Kseniya! on 16/07/2016.
 */
public class RGBConverter {
    
    public static final int BLACK = -16777216;
    public static final int BLUE  = -16776961;
    public static final int GREEN = -16711936;
    public static final int RED   = -65536;
    public static final int WHITE = -1;
    
    private final BufferedImage capturedScreen;
    private final int           x;
    private final int           y;
    private       int           blue;
    private       int           green;
    private       int           red;
    
    public RGBConverter(BufferedImage capturedScreen, int x, int y) {
        
        this.capturedScreen = capturedScreen;
        this.x = x;
        this.y = y;
    }
    
    public RGBConverter invoke() {
        
        int capturedRGB;
        //Debug.sout(x + " " + y);
        capturedRGB = capturedScreen.getRGB(x, y);
        red = (capturedRGB >> 16) & 0xFF;
        green = (capturedRGB >> 8) & 0xFF;
        blue = (capturedRGB) & 0xFF;
        return this;
    }
    
    public int getBlue() {
        
        return blue;
    }
    
    public int getGreen() {
        
        return green;
    }
    
    public int getRed() {
        
        return red;
    }
}
