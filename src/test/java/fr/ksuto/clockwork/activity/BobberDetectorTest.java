package fr.ksuto.clockwork.activity;

import fr.ksuto.prh.capture.Frame;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BobberDetectorTest {

    private static final int RED  = 0xC82020;
    private static final int BLUE = 0x3050D0;

    /**
     * Eau de la couleur donnée, avec un léger bruit (deux images ne sont jamais identiques), capturée en (100, 200).
     */
    private static BufferedImage water(int color, long seed) {

        BufferedImage image = new BufferedImage(120, 60, BufferedImage.TYPE_INT_RGB);
        Random        noise = new Random(seed);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int jitter = noise.nextInt(11) - 5;
                int r = clamp((color >> 16 & 0xFF) + jitter), g = clamp((color >> 8 & 0xFF) + jitter), b = clamp((color & 0xFF) + jitter);
                image.setRGB(x, y, r << 16 | g << 8 | b);
            }
        }
        return image;
    }

    private static int clamp(int value) {

        return Math.max(0, Math.min(255, value));
    }

    /**
     * Bouchon dont la plume rouge commence en (x, y) image, la plume bleue 5 pixels au-dessus.
     */
    private static BufferedImage bobber(BufferedImage image, int x, int y) {

        for (int n = 0; n <= 2; n++) {image.setRGB(x + n, y, RED);}
        image.setRGB(x, y - 5, BLUE);
        return image;
    }

    private static Frame frame(BufferedImage image) {

        return Frame.of(image, new Rectangle(100, 200, image.getWidth(), image.getHeight()));
    }

    private static final Rectangle ZONE = new Rectangle(100, 200, 120, 60);

    @Test
    void findsTheBobberByItsTwoFeathersWhateverTheWater() {

        for (int waterColor : new int[]{0x204080, 0x406030, 0x705030}) { // bleue, verte, boueuse
            Optional<Point> found = BobberDetector.locate(frame(bobber(water(waterColor, 1), 50, 30)), null, ZONE);
            assertEquals(Optional.of(new Point(151, 230)), found, "eau " + Integer.toHexString(waterColor));
        }
    }

    @Test
    void ignoresARedAndBlueThingAlreadyThereBeforeTheCast() {

        Frame before = frame(bobber(water(0x204080, 1), 10, 20));                // décor rouge et bleu
        Frame after  = frame(bobber(bobber(water(0x204080, 2), 10, 20), 70, 40)); // même décor, bruit différent, et le bouchon

        assertEquals(Optional.of(new Point(171, 240)), BobberDetector.locate(after, before, ZONE), "le décor, inchangé malgré le bruit, est écarté");
        assertEquals(Optional.of(new Point(111, 220)), BobberDetector.locate(after, null, ZONE), "sans l'image d'avant : le premier trouvé");
    }

    @Test
    void noiseAloneIsNeverABobber() {

        assertTrue(BobberDetector.locate(frame(water(0x204080, 3)), frame(water(0x204080, 4)), ZONE).isEmpty());
    }

    @Test
    void tracksTheBobberAroundItsLastPosition() {

        Frame frame = frame(bobber(water(0x204080, 5), 52, 31));

        assertEquals(Optional.of(new Point(153, 231)), BobberDetector.track(frame, new Point(151, 230)));
        assertTrue(BobberDetector.track(frame, new Point(120, 210)).isEmpty(), "hors du rayon de suivi");
    }

    @Test
    void biteWhenTheBobberMovesAwayOrDisappearsOnTwoFrames() {

        Point origin = new Point(150, 230);

        BobberDetector.BiteWatcher calm = new BobberDetector.BiteWatcher(origin);
        for (int i = 0; i < 20; i++) {
            assertEquals(BobberDetector.BiteWatcher.Verdict.WAITING, calm.feed(Optional.of(new Point(150 + i % 3 - 1, 230 + i % 2))), "tangage");
        }

        BobberDetector.BiteWatcher dive = new BobberDetector.BiteWatcher(origin);
        assertEquals(BobberDetector.BiteWatcher.Verdict.WAITING, dive.feed(Optional.of(new Point(150, 238))), "une image isolée ne suffit pas");
        assertEquals(BobberDetector.BiteWatcher.Verdict.BITE, dive.feed(Optional.of(new Point(150, 239))), "touche : clic immédiat");

        BobberDetector.BiteWatcher lost = new BobberDetector.BiteWatcher(origin);
        assertEquals(BobberDetector.BiteWatcher.Verdict.WAITING, lost.feed(Optional.empty()));
        assertEquals(BobberDetector.BiteWatcher.Verdict.WAITING, lost.feed(Optional.of(origin)), "réapparu : fausse alerte");
        assertEquals(BobberDetector.BiteWatcher.Verdict.WAITING, lost.feed(Optional.empty()));
        assertEquals(BobberDetector.BiteWatcher.Verdict.LOST, lost.feed(Optional.empty()), "plongé sous l'eau");
    }
}
