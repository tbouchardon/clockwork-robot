package net.ddns.ksuto.clockwork.tools;

import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class Scanner {
    
    private final TBoPeripheralRobotHelper peripherals;
    
    public Scanner(TBoPeripheralRobotHelper peripherals) {
        
        this.peripherals = peripherals;
    }
    
    public static void main(String[] args) throws AWTException {
        
        TBoPeripheralRobotHelper peripheralRobotHelper = new TBoPeripheralRobotHelper();
        Scanner                  scanner               = new Scanner(peripheralRobotHelper);
        try {
            scanner.searchColorBlocks(0, 255, 0, 10, 0);
        }
        catch (AWTException e) {
            e.printStackTrace();
        }
    }
    
    public ArrayList<int[]> searchPicture(String strImage) throws IOException, AWTException {
        
        System.out.println("Searching for : " + strImage);
        
        ArrayList<int[]> alCoords = new ArrayList<>();
        
        //noinspection ConstantConditions
        BufferedImage biReferenceImage = ImageIO.read(getClass().getResource(strImage));
        
        Robot robot = new Robot();
        
        BufferedImage biCapturedScreen = robot.createScreenCapture(new Rectangle(0, 0, peripherals.getScreen().i_SCREEN_WIDTH, peripherals.getScreen().i_SCREEN_HEIGHT));
        
        int     iRefRGB = biReferenceImage.getRGB(0, 0);
        int     iCapturedRGB;
        int     iTempCapturedRGB;
        boolean result  = false;
        
        for (int iXScreen = 0; iXScreen < biCapturedScreen.getWidth() - biReferenceImage.getWidth(); iXScreen++) {
            for (int iYScreen = 0; iYScreen < biCapturedScreen.getHeight() - biReferenceImage.getHeight(); iYScreen++) {
                
                iCapturedRGB = biCapturedScreen.getRGB(iXScreen, iYScreen);
                
                if (iCapturedRGB == iRefRGB) {
                    
                    result = true;
                    
                    for (int iXRef = 0; iXRef < biReferenceImage.getWidth(); iXRef++) {
                        for (int iYRef = 0; iYRef < biReferenceImage.getHeight(); iYRef++) {
                            
                            iTempCapturedRGB = biCapturedScreen.getRGB(iXRef + iXScreen, iYRef + iYScreen);
                            iRefRGB = biReferenceImage.getRGB(iXRef, iYRef);
                            if (iTempCapturedRGB != iRefRGB) {
                                result = false;
                                break;
                            }
                        }
                        
                        if (!result) {
                            break;
                        }
                    }
                }
                
                if (result) {
                    int[] iTemp = new int[]{iXScreen, iYScreen};
                    alCoords.add(iTemp);
                }
                
                iRefRGB = biReferenceImage.getRGB(0, 0);
                result = false;
            }
        }
        
        System.out.println("Found : " + alCoords.size());
        return alCoords;
    }
    
    public ArrayList<Block> searchColorBlocks(int iRed, int iGreen, int iBlue, int iMinSize, int iMaxSize) throws AWTException {
        
        Robot         robot            = new Robot();
        BufferedImage biCapturedScreen = robot.createScreenCapture(new Rectangle(0, 0, peripherals.getScreen().i_SCREEN_WIDTH, peripherals.getScreen().i_SCREEN_HEIGHT));
        
        ArrayList<Block> alBlocks = new ArrayList<>();
        int              iCapturedRGB;
        int              r, g, b;
        boolean          bFound;
        System.out.println(iRed + " " + iGreen + " " + iBlue);
        
        for (int iX = 0; iX < peripherals.getScreen().i_SCREEN_WIDTH; iX++) {
            for (int iY = 0; iY < peripherals.getScreen().i_SCREEN_HEIGHT; iY++) {
                
                if (!alBlocks.isEmpty()) {
                    for (Block block : alBlocks) {
                        if (iY >= block.yPosition && iY <= block.yPosition + block.ySize && iX >= block.xPosition && iX <= block.xPosition + block.xSize) { iY = block.yPosition + block.ySize; }
                    }
                }
                
                
                Block block = new Block();
                
                iCapturedRGB = biCapturedScreen.getRGB(iX, iY);
                b = (iCapturedRGB) & 0xFF;
                g = (iCapturedRGB >> 8) & 0xFF;
                r = (iCapturedRGB >> 16) & 0xFF;
                
                int xDelta = 0;
                int yDelta = 0;
                
                if (r == iRed && g == iGreen && b == iBlue) {
                    bFound = true;
                    while (bFound) {
                        iCapturedRGB = biCapturedScreen.getRGB(iX + ++xDelta, iY + yDelta);
                        b = (iCapturedRGB) & 0xFF;
                        g = (iCapturedRGB >> 8) & 0xFF;
                        r = (iCapturedRGB >> 16) & 0xFF;
                        if (!(r == iRed && g == iGreen && b == iBlue)) {
                            block.ySize = yDelta;
                            yDelta++;
                            if (xDelta != 1) { block.xSize = xDelta; }
                            else { bFound = false; }
                            xDelta = 0;
                        }
                    }
                    
                    block.xPosition = iX;
                    block.yPosition = iY;
                    alBlocks.add(block);
                }
            }
        }
        
        if (!alBlocks.isEmpty()) {
            int iSize = alBlocks.size();
            for (int i = 0; i < iSize; i++) {
                Block block = alBlocks.get(i);
                if (((block.xSize < iMinSize || block.ySize < iMinSize) && iMinSize != 0) || ((block.xSize > iMaxSize || block.ySize > iMaxSize) && iMaxSize != 0)) {
                    alBlocks.remove(block);
                    i--;
                    iSize--;
                }
            }
        }
        
        return alBlocks;
    }
    
    public class Block {
        
        public int xPosition;
        public int yPosition;
        int xSize;
        int ySize;
    }
}
