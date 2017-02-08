package autoGrind.helper;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;

public class StringTyper {

	public static void typeString(String strText) throws AWTException {

		Robot robot = new Robot();

		for (char cLetter : strText.toCharArray()) {

			switch (cLetter) {
				case 'a':
					robot.keyPress(KeyEvent.VK_A);
					break;
				case 'b':
					robot.keyPress(KeyEvent.VK_B);
					break;
				case 'c':
					robot.keyPress(KeyEvent.VK_C);
					break;
				case 'd':
					robot.keyPress(KeyEvent.VK_D);
					break;
				case 'e':
					robot.keyPress(KeyEvent.VK_E);
					break;
				case 'f':
					robot.keyPress(KeyEvent.VK_F);
					break;
				case 'g':
					robot.keyPress(KeyEvent.VK_G);
					break;
				case 'h':
					robot.keyPress(KeyEvent.VK_H);
					break;
				case 'i':
					robot.keyPress(KeyEvent.VK_I);
					break;
				case 'j':
					robot.keyPress(KeyEvent.VK_J);
					break;
				case 'k':
					robot.keyPress(KeyEvent.VK_K);
					break;
				case 'l':
					robot.keyPress(KeyEvent.VK_L);
					break;
				case 'm':
					robot.keyPress(KeyEvent.VK_M);
					break;
				case 'n':
					robot.keyPress(KeyEvent.VK_N);
					break;
				case 'o':
					robot.keyPress(KeyEvent.VK_O);
					break;
				case 'p':
					robot.keyPress(KeyEvent.VK_P);
					break;
				case 'q':
					robot.keyPress(KeyEvent.VK_Q);
					break;
				case 'r':
					robot.keyPress(KeyEvent.VK_R);
					break;
				case 's':
					robot.keyPress(KeyEvent.VK_S);
					break;
				case 't':
					robot.keyPress(KeyEvent.VK_T);
					break;
				case 'u':
					robot.keyPress(KeyEvent.VK_U);
					break;
				case 'v':
					robot.keyPress(KeyEvent.VK_V);
					break;
				case 'w':
					robot.keyPress(KeyEvent.VK_W);
					break;
				case 'x':
					robot.keyPress(KeyEvent.VK_X);
					break;
				case 'y':
					robot.keyPress(KeyEvent.VK_Y);
					break;
				case 'z':
					robot.keyPress(KeyEvent.VK_Z);
					break;
				case 'A':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_A);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'B':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_B);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'C':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_C);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'D':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_D);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'E':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_E);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'F':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_F);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'G':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_G);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'H':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_H);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'I':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_I);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'J':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_J);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'K':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_K);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'L':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_L);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'M':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_M);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'N':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_N);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'O':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_O);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'P':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_P);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'Q':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_Q);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'R':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_R);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'S':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_S);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'T':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_T);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'U':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_U);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'V':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_V);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'W':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_W);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'X':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_X);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'Y':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_Y);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case 'Z':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_Z);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '0':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_0);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '1':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_1);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '2':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_2);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '3':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_3);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '4':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_4);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '5':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_5);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '6':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_6);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '7':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_7);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '8':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_8);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '9':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_9);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '-':
					robot.keyPress(KeyEvent.VK_6);
					break;
				case ';':
					robot.keyPress(KeyEvent.VK_SEMICOLON);
					break;
				case ':':
					robot.keyPress(KeyEvent.VK_COLON);
					break;
				case '.':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_SEMICOLON);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case '/':
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_COLON);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					break;
				case ' ':
					robot.keyPress(KeyEvent.VK_SPACE);
					break;
				case '!':
					robot.keyPress(KeyEvent.VK_EXCLAMATION_MARK);
					break;
				case '@':
					robot.keyPress(KeyEvent.VK_CONTROL);
					robot.keyPress(KeyEvent.VK_ALT);
					robot.keyPress(KeyEvent.VK_0);
					robot.keyRelease(KeyEvent.VK_CONTROL);
					robot.keyRelease(KeyEvent.VK_ALT);
					break;
			}
			robot.delay(100);
		}
		robot.delay(400);

	}
	
}
