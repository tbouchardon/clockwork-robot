package net.ddns.ksuto.clockwork.entities.qrcode;

import net.ddns.ksuto.clockwork.tools.RGBConverter;

import java.awt.image.BufferedImage;

/**
 * Created by thomas.bouchardon on 07/02/2017!
 */
public class Dot {
    
    public int xPosition;
    public int yPosition;
    public boolean active = false;
    private RGBConverter rgbConverter;
    
    Dot(int xPosition, int yPosition) {
        
        this.xPosition = xPosition;
        this.yPosition = yPosition;
    }
    
    Dot() {
        
    }
    
    public void invokeConverter(BufferedImage bufferedImage) {
        
        rgbConverter = new RGBConverter(bufferedImage, xPosition, yPosition);
        rgbConverter.invoke();
    }
    
    public int getRgb(BufferedImage bufferedImage) {
        
        return bufferedImage.getRGB(xPosition, yPosition);
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
    
    public boolean updateActive(BufferedImage bufferedImage) {
        
        active = bufferedImage.getRGB(xPosition, yPosition) == RGBConverter.WHITE;
        
        return active;
    }
}
