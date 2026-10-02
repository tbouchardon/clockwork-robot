package fr.ksuto.clockwork.entities.qrcode;

import fr.ksuto.prh.capture.Frame;
import fr.ksuto.prh.capture.Rgb;

/**
 * Created by thomas.bouchardon on 07/02/2017!
 */
public class Dot {

    public int     xPosition;
    public int     yPosition;
    public boolean active = false;

    Dot(int xPosition, int yPosition) {

        this.xPosition = xPosition;
        this.yPosition = yPosition;
    }

    Dot() {

    }

    public int getBlue(Frame frame) {

        return frame.blue(xPosition, yPosition);
    }

    public int getGreen(Frame frame) {

        return frame.green(xPosition, yPosition);
    }

    public int getRed(Frame frame) {

        return frame.red(xPosition, yPosition);
    }

    public int getRgb(Frame frame) {

        return frame.rgb(xPosition, yPosition);
    }

    public boolean updateActive(Frame frame) {

        active = frame.rgb(xPosition, yPosition) == Rgb.ARGB_WHITE;

        return active;
    }
}
