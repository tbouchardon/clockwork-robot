package fr.ksuto.clockwork.activity;

import fr.ksuto.prh.capture.Frame;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
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

    private static final BobberDetector.Background BLUE_WATER = background(0x204080);

    private static BobberDetector.Background background(int color) {

        return new BobberDetector.Background(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF);
    }

    @Test
    void findsTheBobberByItsTwoFeathersWhateverTheWater() {

        for (int waterColor : new int[]{0x204080, 0x406030, 0x705030}) { // bleue, verte, boueuse
            Optional<Point> found = BobberDetector.locate(frame(bobber(water(waterColor, 1), 50, 30)), null, ZONE, background(waterColor));
            assertEquals(Optional.of(new Point(151, 230)), found, "eau " + Integer.toHexString(waterColor));
        }
    }

    @Test
    void ignoresARedAndBlueThingAlreadyThereBeforeTheCast() {

        Frame before = frame(bobber(water(0x204080, 1), 10, 20));                // décor rouge et bleu
        Frame after  = frame(bobber(bobber(water(0x204080, 2), 10, 20), 70, 40)); // même décor, bruit différent, et le bouchon

        assertEquals(Optional.of(new Point(171, 240)), BobberDetector.locate(after, before, ZONE, BLUE_WATER), "le décor, inchangé malgré le bruit, est écarté");
        assertEquals(Optional.of(new Point(111, 220)), BobberDetector.locate(after, null, ZONE, BLUE_WATER), "sans l'image d'avant : le premier trouvé");
    }

    /**
     * Recadrage d'une capture en jeu (12.1), placé à sa position d'origine dans l'écran.
     */
    private static Frame screenshot(String name, int x, int y) throws IOException {

        try (InputStream stream = BobberDetectorTest.class.getResourceAsStream("/fishing/" + name)) {
            BufferedImage image = ImageIO.read(stream);
            return Frame.of(image, new Rectangle(x, y, image.getWidth(), image.getHeight()));
        }
    }

    /**
     * Captures avant et après le lancer : bouchon trouvé seulement après, à l'endroit attendu, et suivi.
     */
    private static void assertRealBobber(String water, int x, int y, Point expected) throws IOException {

        Frame                     before     = screenshot(water + "-avant.png", x, y);
        Frame                     after      = screenshot(water + "-apres.png", x, y);
        Rectangle                 zone       = new Rectangle(x, y, before.width(), before.height());
        BobberDetector.Background background = BobberDetector.Background.of(before, zone);

        Optional<Point> bobber = BobberDetector.locate(after, before, zone, background);
        assertTrue(bobber.isPresent(), water);
        assertTrue(bobber.get().distance(expected) < 25, water + " : bouchon vers " + expected + ", trouvé en " + bobber.get());
        assertTrue(BobberDetector.locate(before, null, zone, background).isEmpty(), water + " : pas de bouchon avant le lancer");
        assertEquals(bobber, BobberDetector.track(after, bobber.get(), background), water + " : suivi");
    }

    @Test
    void findsTheRealBobberOnMuddyWaterFromAfar() throws IOException {

        assertRealBobber("boueuse", 860, 300, new Point(961, 372));
    }

    @Test
    void findsTheRealBobberOnGreenGlowingWaterInFirstPerson() throws IOException {

        // Lumière verte : la plume bleue y est gris-vert (69, 107, 84), plus « bleue » dans l'absolu, mais plus bleue que l'eau
        assertRealBobber("verte", 640, 650, new Point(845, 793));
    }

    @Test
    void toleratesTheBlueFeatherMovingWhenTheBobberTilts() {

        for (int gap = 2; gap <= 7; gap++) {
            BufferedImage image = water(0x204080, gap);
            for (int n = 0; n <= 2; n++) {image.setRGB(50 + n, 30, RED);}
            image.setRGB(51, 30 - gap, BLUE);
            assertTrue(BobberDetector.locate(frame(image), null, ZONE, BLUE_WATER).isPresent(), "plume bleue " + gap + " px au-dessus");
        }
    }

    @Test
    void noiseAloneIsNeverABobber() {

        assertTrue(BobberDetector.locate(frame(water(0x204080, 3)), frame(water(0x204080, 4)), ZONE, BLUE_WATER).isEmpty());
    }

    @Test
    void tracksTheBobberAroundItsLastPosition() {

        Frame frame = frame(bobber(water(0x204080, 5), 52, 31));

        assertEquals(Optional.of(new Point(153, 231)), BobberDetector.track(frame, new Point(151, 230), BLUE_WATER));
        assertTrue(BobberDetector.track(frame, new Point(120, 210), BLUE_WATER).isEmpty(), "hors du rayon de suivi");
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
