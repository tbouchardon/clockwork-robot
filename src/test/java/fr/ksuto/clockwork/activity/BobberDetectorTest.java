package fr.ksuto.clockwork.activity;

import fr.ksuto.prh.capture.Frame;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BobberDetectorTest {

    private static final int       RED   = 0xC82020;
    private static final int       BLUE  = 0x3050D0;
    private static final int       GRASS = 0xB04020; // herbe rougeâtre
    private static final Rectangle ZONE  = new Rectangle(100, 200, 120, 60);

    /**
     * Eau de la couleur donnée, avec un léger bruit (deux images ne sont jamais identiques), capturée en (100, 200).
     */
    private static BufferedImage water(int color, long seed) {

        BufferedImage image = new BufferedImage(120, 60, BufferedImage.TYPE_INT_RGB);
        Random        noise = new Random(seed);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int jitter = noise.nextInt(11) - 5;
                image.setRGB(x, y, clamp((color >> 16 & 0xFF) + jitter) << 16 | clamp((color >> 8 & 0xFF) + jitter) << 8 | clamp((color & 0xFF) + jitter));
            }
        }
        return image;
    }

    private static int clamp(int value) {

        return Math.max(0, Math.min(255, value));
    }

    /**
     * Bouchon : plume rouge de 3 pixels en (x, y) image, plume bleue 2 pixels au-dessus (sur les captures en jeu, les
     * deux plumes se touchent presque).
     */
    private static BufferedImage bobber(BufferedImage image, int x, int y) {

        for (int n = 0; n <= 2; n++) {image.setRGB(x + n, y, RED);}
        image.setRGB(x, y - 2, BLUE);
        return image;
    }

    /**
     * Herbe : une tige verticale de la couleur donnée, en x image.
     */
    private static BufferedImage grass(BufferedImage image, int x, int color) {

        for (int y = 5; y < 55; y++) {
            for (int n = 0; n <= 2; n++) {image.setRGB(x + n, y, color);}
        }
        return image;
    }

    private static Frame frame(BufferedImage image) {

        return Frame.of(image, new Rectangle(100, 200, image.getWidth(), image.getHeight()));
    }

    @Test
    void findsTheBobberWhateverTheWaterColor() {

        for (int waterColor : new int[]{0x204080, 0x406030, 0x705030}) { // bleue, verte, boueuse
            Frame before = frame(water(waterColor, 1));
            Frame after  = frame(bobber(water(waterColor, 2), 50, 30));
            Point found = BobberDetector.locate(after, before, ZONE).orElseThrow(() -> new AssertionError("eau " + Integer.toHexString(waterColor)));
            assertTrue(found.distance(151, 230) < 3, "eau " + Integer.toHexString(waterColor) + " : " + found);
        }
    }

    @Test
    void onLavaTheBlueFeatherAloneFindsTheBobber() {

        int           lava  = 0xD04010;
        BufferedImage after = water(lava, 2);
        for (int x = 40; x < 52; x++) {after.setRGB(x, 30, RED);}  // plume rouge : ne ressort pas sur la lave
        for (int x = 40; x < 52; x++) {after.setRGB(x, 26, BLUE);} // plume bleue

        assertTrue(BobberDetector.locate(frame(after), frame(water(lava, 1)), ZONE).orElseThrow().distance(146, 226) < 2);
    }

    @Test
    void whatWasThereBeforeTheCastIsNeverAFeather() {

        // Herbes rougeâtres et un objet rouge et bleu déjà là : seul le bouchon, apparu, compte
        Frame before = frame(bobber(grass(water(0x406030, 1), 10, GRASS), 80, 20));
        Frame after  = frame(bobber(bobber(grass(water(0x406030, 2), 10, GRASS), 80, 20), 40, 40));

        assertTrue(BobberDetector.locate(after, before, ZONE).orElseThrow().distance(141, 239) < 4);
        assertTrue(BobberDetector.locate(frame(water(0x204080, 3)), frame(water(0x204080, 4)), ZONE).isEmpty(), "bruit seul");
    }

    @Test
    void worksInATinyPuddle() {

        // Flaque de 12 px au milieu de l'herbe : la couleur dominante de la zone est l'herbe, pas l'eau
        BufferedImage before = water(0x30A030, 1);
        BufferedImage after  = water(0x30A030, 2);
        for (int y = 24; y < 36; y++) {
            for (int x = 44; x < 56; x++) {
                before.setRGB(x, y, 0x204080);
                after.setRGB(x, y, 0x204080);
            }
        }
        bobber(after, 48, 32);

        assertTrue(BobberDetector.locate(frame(after), frame(before), ZONE).orElseThrow().distance(149, 231) < 4);
        assertTrue(BobberDetector.measure(frame(after), frame(before), ZONE, new Point(149, 232)).isPresent());
    }

    @Test
    void onLavaTheBlueFeatherCarriesTheMeasure() {

        int           lava  = 0xD04010;
        BufferedImage after = water(lava, 2);
        for (int x = 40; x < 60; x++) {after.setRGB(x, 30, RED);}  // plume rouge : à peine plus rouge que la lave
        for (int x = 40; x < 60; x++) {after.setRGB(x, 26, BLUE);} // plume bleue : très visible

        BobberDetector.Blob feathers = BobberDetector.measure(frame(after), frame(water(lava, 1)), ZONE, new Point(150, 228)).orElseThrow();
        assertEquals(20, feathers.count(), "seule la plume bleue ressort sur la lave");
        assertEquals(226, feathers.y(), 0.01);
    }

    @Test
    void measuresTheCenterOfTheWholeFeather() {

        BufferedImage after = water(0x204080, 7);
        for (int y = 30; y < 34; y++) {
            for (int x = 40; x < 60; x++) {after.setRGB(x, y, RED);}
        }
        Frame before = frame(water(0x204080, 6));

        BobberDetector.Blob feather = BobberDetector.measure(frame(after), before, ZONE, new Point(150, 231)).orElseThrow();
        assertEquals(149.5, feather.x(), 0.01, "centre stable, pas le premier pixel trouvé");
        assertEquals(231.5, feather.y(), 0.01);
        assertEquals(80, feather.count());
        assertEquals(4, feather.height());
        assertTrue(BobberDetector.measure(frame(water(0x204080, 8)), before, ZONE, new Point(150, 231)).isEmpty(), "eau seule");
    }

    @Test
    void grassThatAppearsAwayFromTheBobberIsIgnored() {

        BufferedImage after = water(0x204080, 10);
        for (int x = 40; x < 50; x++) {after.setRGB(x, 30, RED);}   // plume rouge du bouchon
        for (int x = 40; x < 50; x++) {after.setRGB(x, 27, BLUE);}  // plume bleue, 3 px au-dessus : reliée
        grass(after, 80, GRASS);                                    // herbe qui entre dans le champ, à 30 px : ignorée
        Frame before = frame(water(0x204080, 9));

        BobberDetector.Blob feathers = BobberDetector.measure(frame(after), before, ZONE, new Point(145, 229)).orElseThrow();
        assertEquals(20, feathers.count(), "les deux plumes, sans l'herbe");
        assertEquals(144.5, feathers.x(), 0.01);
        assertTrue(BobberDetector.measure(frame(after), before, ZONE, new Point(110, 205)).isEmpty(), "rien près de la position attendue");
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
     * Captures avant et après le lancer : bouchon trouvé seulement après, à l'endroit attendu, et mesuré.
     */
    private static void assertRealBobber(String water, int x, int y, Point expected) throws IOException {

        Frame     before = screenshot(water + "-avant.png", x, y);
        Frame     after  = screenshot(water + "-apres.png", x, y);
        Rectangle zone   = new Rectangle(x, y, before.width(), before.height());

        Optional<Point> bobber = BobberDetector.locate(after, before, zone);
        assertTrue(bobber.isPresent(), water);
        assertTrue(bobber.get().distance(expected) < 25, water + " : bouchon vers " + expected + ", trouvé en " + bobber.get());
        assertTrue(BobberDetector.locate(before, before, zone).isEmpty(), water + " : rien avant le lancer");

        Optional<BobberDetector.Blob> feathers = BobberDetector.measure(after, before, new Rectangle(bobber.get().x - 40, bobber.get().y - 40, 80, 80),
                                                                        bobber.get());
        assertTrue(feathers.isPresent() && feathers.get().distance(bobber.get().x, bobber.get().y) < 20, water + " : plumes mesurées autour du bouchon");
    }

    @Test
    void findsTheRealBobberOnMuddyWaterFromAfar() throws IOException {

        assertRealBobber("boueuse", 860, 300, new Point(961, 372));
    }

    @Test
    void findsTheRealBobberOnGreenGlowingWaterInFirstPerson() throws IOException {

        // Lumière verte : la plume bleue y est gris-vert (69, 107, 84), mais bien plus bleue que l'eau au même endroit
        assertRealBobber("verte", 640, 650, new Point(845, 793));
    }

    private static Optional<BobberDetector.Blob> blob(double x, double y, int count) {

        return Optional.of(new BobberDetector.Blob(x, y, count, 10));
    }

    private static BobberDetector.BiteWatcher calibrated() {

        BobberDetector.BiteWatcher watcher = new BobberDetector.BiteWatcher(new Point(150, 230));
        for (int i = 0; i < BobberDetector.CALIBRATION; i++) {watcher.feed(blob(150, 230, 100));}
        return watcher;
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
    void bothFeathersCountEvenWhenTheyAreNotTouching() {

        // Mesure réelle d'un faux clic : sur certaines images, l'écart entre les plumes dépassait LINK, et une seule
        // plume comptait (surface divisée par deux, prise pour une plongée)
        BufferedImage after = water(0x204080, 11);
        for (int y = 30; y < 34; y++) {
            for (int x = 40; x < 50; x++) {after.setRGB(x, y, RED);}
        }
        for (int y = 20; y < 24; y++) {
            for (int x = 40; x < 50; x++) {after.setRGB(x, y, BLUE);} // 6 px au-dessus de la rouge
        }

        BobberDetector.Blob feathers = BobberDetector.measure(frame(after), frame(water(0x204080, 12)), ZONE, new Point(145, 227)).orElseThrow();
        assertEquals(80, feathers.count(), "les deux plumes");
    }

    @Test
    void landingSplashDoesNotSkewTheRestReference() {

        // Mesure réelle d'un faux clic : l'éclaboussure de l'arrivée doublait la surface des premières images
        BobberDetector.BiteWatcher watcher = new BobberDetector.BiteWatcher(new Point(150, 230));
        for (int i = 0; i < BobberDetector.CALIBRATION; i++) {watcher.feed(blob(150, 230, i < 6 ? 1200 : 600));}
        for (int i = 0; i < 50; i++) {
            assertEquals(BobberDetector.BiteWatcher.Verdict.WAITING, watcher.feed(blob(150, 230, 380 + (i % 5) * 20)), "surface normale, image " + i);
        }
        assertEquals(BobberDetector.BiteWatcher.Verdict.WAITING, watcher.feed(blob(150, 240, 250)));
        assertEquals(BobberDetector.BiteWatcher.Verdict.LOST, watcher.feed(blob(150, 242, 240)), "vraie touche : 0,4 de la surface au repos");
    }

    @Test
    void trackingWindowFollowsTheFeatherSizeWithinBounds() {

        BobberDetector.BiteWatcher watcher = new BobberDetector.BiteWatcher(new Point(150, 230));
        assertEquals(new Rectangle(130, 210, 40, 40), watcher.window(), "avant calibrage : autour du point trouvé");
        for (int i = 0; i < BobberDetector.CALIBRATION; i++) {watcher.feed(Optional.of(new BobberDetector.Blob(160, 240, 300, 12)));}
        assertEquals(new Rectangle(124, 204, 72, 72), watcher.window(), "plumes de 12 px : ± 36 px autour de leur centre");

        BobberDetector.BiteWatcher runaway = new BobberDetector.BiteWatcher(new Point(150, 230));
        for (int i = 0; i < BobberDetector.CALIBRATION; i++) {runaway.feed(Optional.of(new BobberDetector.Blob(150, 230, 100, 400 + i)));}
        assertEquals(2 * BobberDetector.MAX_WINDOW, runaway.window().width, "fenêtre bornée, même si la mesure s'emballe");
    }
}
