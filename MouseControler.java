package autoGrind.helper;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Robot;
import java.awt.Toolkit;

@SuppressWarnings({"unused", "Duplicates", "WeakerAccess"})
public class MouseControler {
	
	private final int i_DELAY      = 100;
	private final int i_DRAG_DELAY = 20, i_DRAG_SPACE = 1;
	
	private final Dimension dim_D = new Dimension(Toolkit.getDefaultToolkit().getScreenSize());
	private final int i_SCREEN_WIDTH = (int) dim_D.getWidth();
	private final int i_SCREEN_HEIGHT = (int) dim_D.getHeight();
	
	private final int iX_START = i_SCREEN_WIDTH / 2, iY_START = i_SCREEN_HEIGHT / 2 - 20;


}
