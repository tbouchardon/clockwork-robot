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
        Optional<BobberDetector.Blob> feather = BobberDetector.measure(after, new Rectangle(bobber.get().x - 40, bobber.get().y - 40, 80, 80), background);
        assertTrue(feather.isPresent() && feather.get().distance(bobber.get().x, bobber.get().y) < 20, water + " : plume mesurée autour du bouchon");
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



    private static Optional<BobberDetector.Blob> blob(double x, double y, int count) {

        return Optional.of(new BobberDetector.Blob(x, y, count, 10));
    }

    @Test
    void measuresTheCenterOfTheWholeRedFeather() {

        BufferedImage image = water(0x204080, 7);
        for (int y = 30; y < 34; y++) {
            for (int x = 40; x < 60; x++) {image.setRGB(x, y, RED);}
        }

        BobberDetector.Blob feather = BobberDetector.measure(frame(image), ZONE, BLUE_WATER).orElseThrow();
        assertEquals(149.5, feather.x(), 0.01, "centre stable, pas le premier pixel trouvé");
        assertEquals(231.5, feather.y(), 0.01);
        assertEquals(80, feather.count());
        assertEquals(4, feather.height());
        assertTrue(BobberDetector.measure(frame(water(0x204080, 8)), ZONE, BLUE_WATER).isEmpty(), "eau seule");
    }

    @Test
    void onRedWaterTheBlueFeatherCarriesTheMeasure() {

        int           lava  = 0xD04010;
        BufferedImage image = water(lava, 9);
        for (int x = 40; x < 60; x++) {image.setRGB(x, 30, RED);}  // plume rouge : à peine plus rouge que la lave
        for (int x = 40; x < 60; x++) {image.setRGB(x, 26, BLUE);} // plume bleue : très visible

        BobberDetector.Blob feathers = BobberDetector.measure(frame(image), ZONE, background(lava)).orElseThrow();
        assertEquals(20, feathers.count(), "seule la plume bleue ressort sur la lave");
        assertEquals(226, feathers.y(), 0.01);
    }

    @Test
    void bobbingIsNotABite() {

        BobberDetector.BiteWatcher watcher = new BobberDetector.BiteWatcher(new Point(150, 230));
        for (int i = 0; i < 100; i++) {
            // Tangage : centre à ±2 px, surface visible à ±20 %
            Optional<BobberDetector.Blob> bobbing = blob(150 + (i % 5) - 2, 230 + (i % 3) - 1, 100 + (i % 9) * 5 - 20);
            assertEquals(BobberDetector.BiteWatcher.Verdict.WAITING, watcher.feed(bobbing), "image " + i);
        }
    }

    @Test
    void biteWhenTheFeatherDipsOrMovesFarOnTwoFrames() {

        BobberDetector.BiteWatcher dip = calibrated();
        assertEquals(BobberDetector.BiteWatcher.Verdict.WAITING, dip.feed(blob(150, 232, 30)), "une image isolée ne suffit pas");
        assertEquals(BobberDetector.BiteWatcher.Verdict.LOST, dip.feed(Optional.empty()), "plongé sous l'eau");

        BobberDetector.BiteWatcher jump = calibrated();
        assertEquals(BobberDetector.BiteWatcher.Verdict.WAITING, jump.feed(blob(150, 245, 100)));
        assertEquals(BobberDetector.BiteWatcher.Verdict.BITE, jump.feed(blob(150, 246, 100)), "écarté de plus que sa hauteur (10 px)");

        BobberDetector.BiteWatcher noise = calibrated();
        assertEquals(BobberDetector.BiteWatcher.Verdict.WAITING, noise.feed(Optional.empty()));
        assertEquals(BobberDetector.BiteWatcher.Verdict.WAITING, noise.feed(blob(150, 230, 100)), "réapparu : fausse alerte");
    }

    @Test
    void trackingWindowFollowsTheFeatherSize() {

        BobberDetector.BiteWatcher watcher = new BobberDetector.BiteWatcher(new Point(150, 230));
        assertEquals(new Rectangle(130, 210, 40, 40), watcher.window(), "avant calibrage : autour du point trouvé");
        for (int i = 0; i < BobberDetector.CALIBRATION; i++) {watcher.feed(Optional.of(new BobberDetector.Blob(160, 240, 300, 12)));}
        assertEquals(new Rectangle(124, 204, 72, 72), watcher.window(), "plume de 12 px : ± 36 px autour de son centre");
    }

    private static BobberDetector.BiteWatcher calibrated() {

        BobberDetector.BiteWatcher watcher = new BobberDetector.BiteWatcher(new Point(150, 230));
        for (int i = 0; i < BobberDetector.CALIBRATION; i++) {watcher.feed(blob(150, 230, 100));}
        return watcher;
    }
}
