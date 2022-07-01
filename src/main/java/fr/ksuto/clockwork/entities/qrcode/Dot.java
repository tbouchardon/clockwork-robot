package fr.ksuto.clockwork.entities.qrcode;

import fr.ksuto.clockwork.tools.RGBConverter;

import java.awt.image.BufferedImage;

/**
 * Created by thomas.bouchardon on 07/02/2017!
 */
public class Dot {
    
    public  int          xPosition;
    public  int          yPosition;
    public  boolean      active = false;
    private RGBConverter rgbConverter;
    
    Dot(int xPosition, int yPosition) {
        
        this.xPosition = xPosition;
        this.yPosition = yPosition;
    }
    
    Dot() {
        
    }
    
    public int getBlue(BufferedImage bufferedImage) {
        
        invokeConverter(bufferedImage);
        
        return rgbConverter.getBlue();
    }
    
    public int getGreen(BufferedImage bufferedImage) {
        
        invokeConverter(bufferedImage);
        
        return rgbConverter.getGreen();
    }
    
    public int getRed(BufferedImage bufferedImage) {
        
        invokeConverter(bufferedImage);
        
        return rgbConverter.getRed();
    }
    
    public int getRgb(BufferedImage bufferedImage) {
        
        return bufferedImage.getRGB(xPosition, yPosition);
    }
    
    public void invokeConverter(BufferedImage bufferedImage) {
        
        rgbConverter = new RGBConverter(bufferedImage, xPosition, yPosition);
        rgbConverter.invoke();
    }
    
    public boolean updateActive(BufferedImage bufferedImage) {
        
        active = bufferedImage.getRGB(xPosition, yPosition) == RGBConverter.WHITE;
        
        return active;
    }
}
