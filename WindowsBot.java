package autoGrind.helper;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by thomas.bouchardon on 26/11/2015!
 */
@SuppressWarnings("unused")
public class WindowsBot {



	public final int i_DELAY = 100,
			i_SCAN_DELAY     = 100,
			i_TYPING_DELAY   = 50;
	public final  MousePosition mousePosition   = new MousePosition();
	private final Dimension     dim_D           = new Dimension(Toolkit.getDefaultToolkit().getScreenSize());
	private final int           i_SCREEN_WIDTH  = (int) dim_D.getWidth();
	private final int           i_SCREEN_HEIGHT = (int) dim_D.getHeight();


	private final int iX_START = i_SCREEN_WIDTH / 2, iY_START = i_SCREEN_HEIGHT / 2 - 20;

	private final int i_DRAG_DELAY = 20, i_DRAG_SPACE = 1;
	public Robot robot;

	public WindowsBot() {
		try {
			robot = new Robot();
		}
		catch (AWTException e) {
			e.printStackTrace();
		}
	}

	public void altTab() {
		waitIfUserActive();

		robot.keyPress(KeyEvent.VK_ALT);
		robot.keyPress(KeyEvent.VK_TAB);
		robot.keyRelease(KeyEvent.VK_TAB);
		robot.keyRelease(KeyEvent.VK_ALT);

	}

	public void click(int x, int y) {
		waitIfUserActive();

		System.out.println("Method : click(" + x + ", " + y + ")");

		robot.mouseMove(x, y);
		mousePosition.updateMousePosition();
		robot.mousePress(InputEvent.BUTTON1_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_MASK);
		delay(i_DELAY);

	}

	public boolean clickThing(String[] sImage) {
		waitIfUserActive();

		System.out.println("Method : clickThing(String[] sImage)");

		return clickThing(sImage, 0, 0);
	}

	public boolean clickThing(String[] sImage, int xOffset, int yOffset) {
		waitIfUserActive();

		System.out.println("Method : clickThing(sImage, " + xOffset + ", " + yOffset + ")");

		System.out.println("         Trying to click '" + sImage[0] + "', Offsets : x=" + xOffset + ", y=" + yOffset);

		ArrayList<int[]> alFound;

		alFound = scanFor(sImage);
		if (!alFound.isEmpty()) {
			System.out.println("         " + sImage[0] + " Found");
			for (int[] aiCoords : alFound) {
				click(aiCoords[0] + 3 + xOffset, aiCoords[1] + 3 + yOffset);
			}
			return true;
		}
		return false;
	}

	public void delay(int ms) {

		robot.delay(ms);
	}

	public void enter() {
		waitIfUserActive();

		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		delay(i_DELAY);

	}

	public ArrayList<int[]> scanFor(String[] strImages) {
		waitIfUserActive();
		return scanFor(strImages, 0, 1, 0, 1);
	}

	public ArrayList<int[]> scanFor(String[] strImages, double xMin, double xMax, double yMin, double yMax) {
		waitIfUserActive();

		System.out.println("Method : scanFor(String[] strImages, " + xMin + ", " + xMax + ", " + yMin + ", " + yMax + ")");

//		logger.log("test");

		ArrayList<int[]> alCoords = null;

		try {

			if (robot == null) robot = new Robot();

//			System.out.print(".");

			alCoords = new ArrayList<>();

			for (String strImage : strImages) {

				double time = System.currentTimeMillis();

				System.out.print("         " + strImage);

				URL url = this.getClass().getResource(strImage);// + ".png");

				BufferedImage biReferenceImage = ImageIO.read(url);

				BufferedImage biCapturedScreen = robot.createScreenCapture(new Rectangle(0, 0, i_SCREEN_WIDTH, i_SCREEN_HEIGHT));

//				ImageIO.write(biCapturedScreen, "PNG", new File("D:\\tempShot.png"));

				int     iRefRGB = biReferenceImage.getRGB(0, 0);
				int     iCapturedRGB;
				int     iTempCapturedRGB;
				boolean result  = false;

				System.out.print(" - > " + (System.currentTimeMillis() - time) + "ms");
				time = System.currentTimeMillis();

				for (int iXScreen = (int) (biCapturedScreen.getWidth() * xMin);
				     iXScreen < (biCapturedScreen.getWidth() * xMax) - biReferenceImage.getWidth();
				     iXScreen++) {

					for (int iYScreen = (int) (biCapturedScreen.getHeight() * yMin);
					     iYScreen < (biCapturedScreen.getHeight() * yMax) - biReferenceImage.getHeight();
					     iYScreen++) {

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

						// reinitialize values
						iRefRGB = biReferenceImage.getRGB(0, 0);
						result = false;

					}
				}

				System.out.println(" - > " + (System.currentTimeMillis() - time) + "ms");
			}

			if (!alCoords.isEmpty()) return alCoords;

		}
		catch (IOException | AWTException e) {
			e.printStackTrace();
		}

		//		if (strImage.equals("/ressources.pictures/cpasbien/CPasBienDowloadFound.png")) {
		//			logger.log("test");
		//		}

		return alCoords;
	}

	public void selectAll() {

		System.out.println("Method : selectAll()");
		waitIfUserActive();

		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		delay(i_DELAY);
	}

	public void typeString(String strText) {
		waitIfUserActive();

		System.out.println("Method : typeString(" + strText + ")");

		for (char cLetter : strText.toCharArray()) {

			switch (cLetter) {
				case 'a':
					robot.keyPress(KeyEvent.VK_A);
					robot.keyRelease(KeyEvent.VK_A);
					break;
				case 'b':
					robot.keyPress(KeyEvent.VK_B);
					robot.keyRelease(KeyEvent.VK_B);
					break;
				case 'c':
					robot.keyPress(KeyEvent.VK_C);
					robot.keyRelease(KeyEvent.VK_C);
					break;
				case 'd':
					robot.keyPress(KeyEvent.VK_D);
					robot.keyRelease(KeyEvent.VK_D);
					break;
				case 'e':
					robot.keyPress(KeyEvent.VK_E);
					robot.keyRelease(KeyEvent.VK_E);
					break;
				case 'f':
					robot.keyPress(KeyEvent.VK_F);
					robot.keyRelease(KeyEvent.VK_F);
					break;
				case 'g':
					robot.keyPress(KeyEvent.VK_G);
					robot.keyRelease(KeyEvent.VK_G);
					break;
				case 'h':
					robot.keyPress(KeyEvent.VK_H);
					robot.keyRelease(KeyEvent.VK_H);
					break;
				case 'i':
					robot.keyPress(KeyEvent.VK_I);
					robot.keyRelease(KeyEvent.VK_I);
					break;
				case 'j':
					robot.keyPress(KeyEvent.VK_J);
					robot.keyRelease(KeyEvent.VK_J);
					break;
				case 'k':
					robot.keyPress(KeyEvent.VK_K);
					robot.keyRelease(KeyEvent.VK_K);
					break;
				case 'l':
					robot.keyPress(KeyEvent.VK_L);
					robot.keyRelease(KeyEvent.VK_L);
					break;
				case 'm':
					robot.keyPress(KeyEvent.VK_M);
					robot.keyRelease(KeyEvent.VK_M);
					break;
				case 'n':
					robot.keyPress(KeyEvent.VK_N);
					robot.keyRelease(KeyEvent.VK_N);
					break;
				case 'o':
					robot.keyPress(KeyEvent.VK_O);
					robot.keyRelease(KeyEvent.VK_O);
					break;
				case 'p':
					robot.keyPress(KeyEvent.VK_P);
					robot.keyRelease(KeyEvent.VK_P);
					break;
				case 'q':
					robot.keyPress(KeyEvent.VK_Q);
					robot.keyRelease(KeyEvent.VK_Q);
					break;
				case 'r':
					robot.keyPress(KeyEvent.VK_R);
					robot.keyRelease(KeyEvent.VK_R);
					break;
				case 's':
					robot.keyPress(KeyEvent.VK_S);
					robot.keyRelease(KeyEvent.VK_S);
					break;
				case 't':
					robot.keyPress(KeyEvent.VK_T);
					robot.keyRelease(KeyEvent.VK_T);
					break;
				case 'u':
					robot.keyPress(KeyEvent.VK_U);
					robot.keyRelease(KeyEvent.VK_U);
					break;
				case 'v':
					robot.keyPress(KeyEvent.VK_V);
					robot.keyRelease(KeyEvent.VK_V);
					break;
				case 'w':
					robot.keyPress(KeyEvent.VK_W);
					robot.keyRelease(KeyEvent.VK_W);
					break;
				case 'x':
					robot.keyPress(KeyEvent.VK_X);
					robot.keyRelease(KeyEvent.VK_X);
					break;
				case 'y':
					robot.keyPress(KeyEvent.VK_Y);
					robot.keyRelease(KeyEvent.VK_Y);
					break;
				case 'z':
					robot.keyPress(KeyEvent.VK_Z);
					robot.keyRelease(KeyEvent.VK_Z);
					break;
				case 'A':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_A);
					robot.keyRelease(KeyEvent.VK_A);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'B':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_B);
					robot.keyRelease(KeyEvent.VK_B);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'C':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_C);
					robot.keyRelease(KeyEvent.VK_C);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'D':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_D);
					robot.keyRelease(KeyEvent.VK_D);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'E':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_E);
					robot.keyRelease(KeyEvent.VK_E);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'F':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_F);
					robot.keyRelease(KeyEvent.VK_F);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'G':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_G);
					robot.keyRelease(KeyEvent.VK_G);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'H':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_H);
					robot.keyRelease(KeyEvent.VK_H);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'I':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_I);
					robot.keyRelease(KeyEvent.VK_I);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'J':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_J);
					robot.keyRelease(KeyEvent.VK_J);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'K':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_K);
					robot.keyRelease(KeyEvent.VK_K);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'L':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_L);
					robot.keyRelease(KeyEvent.VK_L);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'M':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_M);
					robot.keyRelease(KeyEvent.VK_M);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'N':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_N);
					robot.keyRelease(KeyEvent.VK_N);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'O':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_O);
					robot.keyRelease(KeyEvent.VK_O);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'P':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_P);
					robot.keyRelease(KeyEvent.VK_P);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'Q':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_Q);
					robot.keyRelease(KeyEvent.VK_Q);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'R':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_R);
					robot.keyRelease(KeyEvent.VK_R);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'S':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_S);
					robot.keyRelease(KeyEvent.VK_S);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'T':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_T);
					robot.keyRelease(KeyEvent.VK_T);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'U':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_U);
					robot.keyRelease(KeyEvent.VK_U);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'V':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_V);
					robot.keyRelease(KeyEvent.VK_V);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'W':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_W);
					robot.keyRelease(KeyEvent.VK_W);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'X':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_X);
					robot.keyRelease(KeyEvent.VK_X);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'Y':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_Y);
					robot.keyRelease(KeyEvent.VK_Y);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'Z':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_Z);
					robot.keyRelease(KeyEvent.VK_Z);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '0':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_0);
					robot.keyRelease(KeyEvent.VK_0);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '1':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_1);
					robot.keyRelease(KeyEvent.VK_1);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '2':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_2);
					robot.keyRelease(KeyEvent.VK_2);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '3':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_3);
					robot.keyRelease(KeyEvent.VK_3);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '4':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_4);
					robot.keyRelease(KeyEvent.VK_4);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '5':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_5);
					robot.keyRelease(KeyEvent.VK_5);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '6':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_6);
					robot.keyRelease(KeyEvent.VK_6);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '7':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_7);
					robot.keyRelease(KeyEvent.VK_7);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '8':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_8);
					robot.keyRelease(KeyEvent.VK_8);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '9':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_9);
					robot.keyRelease(KeyEvent.VK_9);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '&':
					robot.keyPress(KeyEvent.VK_1);
					robot.keyRelease(KeyEvent.VK_1);
					break;
				case '"':
					robot.keyPress(KeyEvent.VK_3);
					robot.keyRelease(KeyEvent.VK_3);
					break;
				case '\'':
					robot.keyPress(KeyEvent.VK_4);
					robot.keyRelease(KeyEvent.VK_4);
					break;
				case '-':
					robot.keyPress(KeyEvent.VK_6);
					robot.keyRelease(KeyEvent.VK_6);
					break;
				case ';':
					robot.keyPress(KeyEvent.VK_SEMICOLON);
					robot.keyRelease(KeyEvent.VK_SEMICOLON);
					break;
				case ':':
					robot.keyPress(KeyEvent.VK_COLON);
					robot.keyRelease(KeyEvent.VK_COLON);
					break;
				case '.':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_SEMICOLON);
					robot.keyRelease(KeyEvent.VK_SEMICOLON);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '/':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_COLON);
					robot.keyRelease(KeyEvent.VK_COLON);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case ' ':
					robot.keyPress(KeyEvent.VK_SPACE);
					robot.keyRelease(KeyEvent.VK_SPACE);
					break;
				case '!':
					robot.keyPress(KeyEvent.VK_EXCLAMATION_MARK);
					robot.keyRelease(KeyEvent.VK_EXCLAMATION_MARK);
					break;
				case '@':
					robot.keyPress(KeyEvent.VK_CONTROL);
					robot.keyPress(KeyEvent.VK_ALT);
					robot.keyPress(KeyEvent.VK_0);
					robot.keyRelease(KeyEvent.VK_0);
					robot.keyRelease(KeyEvent.VK_CONTROL);
					robot.keyRelease(KeyEvent.VK_ALT);
					break;
			}
			delay(i_TYPING_DELAY);
		}
		delay(i_DELAY);

	}

	public boolean waitFor(String[] strImages, int seconds) {
		waitIfUserActive();

		System.out.println("Method : waitFor(strImages[], " + seconds + ")");

		return waitFor(strImages, seconds, 0, 1, 0, 1);
	}

	public boolean waitFor(String[] strImages, int seconds, double xMin, double xMax, double yMin, double yMax) {
		waitIfUserActive();

		System.out.println("Method : waitFor(String[] strImages, " + seconds + ", " + xMin + ", " + xMax + ", " + yMin + ", " + yMax + ")");

		double startTime = System.currentTimeMillis();

		System.out.println("         Waiting for strImages[]");

		while (scanFor(strImages, xMin, xMax, yMin, yMax).isEmpty()) {
			if (seconds != -1 && (System.currentTimeMillis() - startTime) > (seconds * 1000)) {
				System.out.println("         /!\\ Not Found /!\\");
				return false;
			}

			if (System.currentTimeMillis() - startTime > 10000) {
				System.out.println("         Time > 10s, looking for everywhere");
				xMin = 0;
				xMax = 1;
				yMin = 0;
				yMax = 1;
			}

//			System.out.print(".");
			delay(i_SCAN_DELAY);

		}

		System.out.println("         Found !");

		return true;
	}

	public boolean waitFor(String[] strImages) {
		waitIfUserActive();

		System.out.println("Method : waitFor(strImages[])");

		return waitFor(strImages, -1, 0, 1, 0, 1);
	}

	public boolean waitFor(String[] strImages, double xMin, double xMax, double yMin, double yMax) {
		waitIfUserActive();

		System.out.println("Method : waitFor(String[] strImages, " + xMin + ", " + xMax + ", " + yMin + ", " + yMax + ")");

		return waitFor(strImages, -1, xMin, xMax, yMin, yMax);
	}

	public void waitIfUserActive() {

		System.out.println("Method : waitIfUserActive()");

		while (mousePosition.hasMoved()) {
			mousePosition.updateMousePosition();

			JFrame frame = new JFrame("test");
			frame.setAlwaysOnTop(true);
			frame.setUndecorated(true);
			frame.setPreferredSize(new Dimension(150, 90));
			frame.setBackground(new Color(0, 0, 0, 100));
			frame.getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 2, 2));
			frame.setLocation(i_SCREEN_WIDTH / 2 - 75, i_SCREEN_HEIGHT / 2 - 45);

			JLabel waiting = new JLabel("<html><body>Activity detected :<br>waiting...</body></html>", SwingConstants.CENTER);
			waiting.setPreferredSize(new Dimension(148, 40));
			waiting.setFont(waiting.getFont().deriveFont(15f));
			waiting.setForeground(new Color(255, 225, 225));

			JLabel countDown = new JLabel("5", SwingConstants.CENTER);
			countDown.setPreferredSize(new Dimension(148, 40));
			countDown.setFont(countDown.getFont().deriveFont(50f));
			countDown.setForeground(new Color(255, 225, 225));

			frame.add(waiting);
			frame.add(countDown);
			frame.pack();
			frame.setVisible(true);

			for (int i = 5; i != 0; i--) {

				if (mousePosition.hasMoved()) {
					mousePosition.updateMousePosition();
					i = 5;
				}

				countDown.setText(String.valueOf(i));
				delay(1000);
			}

			frame.dispose();
		}

	}

	class MousePosition {

		public int xPos;
		public int yPos;

		MousePosition() {
			updateMousePosition();
		}

		public void updateMousePosition() {
			Point point = MouseInfo.getPointerInfo().getLocation();
			xPos = point.x;
			yPos = point.y;
		}

		MousePosition(int xPos, int yPos) {
			this.xPos = xPos;
			this.yPos = yPos;
		}

		public boolean hasMoved() {

			MousePosition currentMousePosition = new MousePosition();

			return this.equals(currentMousePosition);
		}

		@SuppressWarnings("SimplifiableIfStatement")
		@Override
		public boolean equals(Object other) {

			if (other == null) return false;
			if (other == this) return true;
			if (!(other instanceof MousePosition)) return false;

			//noinspection UnnecessaryLocalVariable
			boolean hasMoved = (xPos != ((MousePosition) other).xPos) || (yPos != ((MousePosition) other).yPos);

			return hasMoved;
		}

	}
}
