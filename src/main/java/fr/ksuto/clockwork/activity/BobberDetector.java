package fr.ksuto.clockwork.activity;

import fr.ksuto.prh.capture.Frame;
import fr.ksuto.prh.capture.Rgb;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repère le bouchon de pêche et détecte la touche, sur des captures d'écran (coordonnées écran).
 * <p>
 * Signature du bouchon : sa plume rouge (3 pixels consécutifs) avec sa plume bleue juste au-dessus (2 à 7 pixels plus
 * haut, à un pixel près en largeur : l'écart change quand le bouchon tangue). Les couleurs sont jugées <b>par rapport à
 * l'eau</b> ({@link Background}, couleur médiane de la zone) : sous une lumière verte, la plume bleue devient gris-vert
 * (69, 107, 84) et ne serait plus « bleue » dans l'absolu, mais elle reste bien plus bleue que l'eau. Vérifiée sur des
 * captures en jeu (12.1) : eau boueuse en vue lointaine, eau verte lumineuse en vue à la première personne.
 */
final class BobberDetector {

    /**
     * Pixels minimum pour considérer les plumes visibles.
     */
    static final int MIN_PIXELS = 3;

    /**
     * Images de calibrage : la plume au repos (surface et centre moyens), référence de la touche.
     */
    static final int CALIBRATION = 8;

    /**
     * Touche : la surface visible de la plume tombe sous cette fraction de sa surface au repos (le bouchon plonge).
     */
    static final double DIP_RATIO = 0.5;

    /**
     * Touche : le centre de la plume s'écarte de sa position au repos de plus que sa hauteur (au moins ce minimum).
     */
    static final double MIN_MOVE = 4;

    /**
     * Images consécutives nécessaires pour conclure (bouchon écarté ou disparu), contre une image isolée bruitée.
     */
    static final int CONFIRMATIONS = 2;

    /**
     * Écart de couleur (sur un canal) entre avant et après le lancer à partir duquel un pixel est « apparu ».
     */
    static final int CHANGE_THRESHOLD = 40;

    /**
     * Plume rouge : rouge moins vert, et rouge moins bleu, dépassent ceux de l'eau d'au moins cette valeur.
     */
    static final int RED_MARGIN = 45;

    /**
     * Plume bleue : bleu moins rouge, et bleu moins vert, dépassent ceux de l'eau d'au moins cette valeur.
     */
    static final int BLUE_MARGIN = 20;

    /**
     * Couleur de l'eau, référence des couleurs des plumes.
     */
    record Background(int red, int green, int blue) {

        /**
         * Couleur médiane de la zone (un pixel sur 4 dans chaque sens) : l'eau, le bouchon étant petit.
         */
        static Background of(Frame frame, Rectangle zone) {

            List<Integer> reds = new ArrayList<>(), greens = new ArrayList<>(), blues = new ArrayList<>();
            for (int y = zone.y; y < zone.y + zone.height; y += 4) {
                for (int x = zone.x; x < zone.x + zone.width; x += 4) {
                    if (!inside(frame, x, y)) {continue;}
                    int rgb = at(frame, x, y);
                    reds.add(Rgb.red(rgb));
                    greens.add(Rgb.green(rgb));
                    blues.add(Rgb.blue(rgb));
                }
            }
            if (reds.isEmpty()) {return new Background(0, 0, 0);}
            return new Background(median(reds), median(greens), median(blues));
        }

        private static int median(List<Integer> values) {

            Collections.sort(values);
            return values.get(values.size() / 2);
        }
    }

    private BobberDetector() {}

    /**
     * Cherche le bouchon dans la zone.
     *
     * @param after      capture après le lancer, couvrant la zone
     * @param before     capture avant le lancer, de la même étendue (null : pas de restriction)
     * @param background couleur de l'eau
     * @return la position du bouchon : seuls comptent les pixels apparus depuis {@code before} (la ligne lancée), ce qui
     * écarte une plume rouge et bleue du décor ou un reflet
     */
    static Optional<Point> locate(Frame after, Frame before, Rectangle zone, Background background) {

        for (int y = zone.y; y < zone.y + zone.height; y++) {
            for (int x = zone.x; x < zone.x + zone.width; x++) {
                if (isSignature(after, x, y, background) && (before == null || appeared(before, after, x + 1, y))) {
                    return Optional.of(new Point(x + 1, y));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Plumes du bouchon dans la fenêtre : leurs pixels (rouges ou bleus par rapport à l'eau), leur centre et leur
     * hauteur. Les deux plumes comptent : sur une eau rouge, orange ou de lave, la plume rouge ne ressort plus mais la
     * bleue, très fortement ; sur une eau bleue, c'est l'inverse. Le centre de tous les pixels est stable d'une image à
     * l'autre, contrairement au premier pixel trouvé : en vue à la première personne, les plumes font des dizaines de
     * pixels.
     *
     * @return vide si moins de {@value #MIN_PIXELS} pixels (bouchon sous l'eau ou hors de la fenêtre)
     */
    static Optional<Blob> measure(Frame frame, Rectangle window, Background water) {

        long sumX = 0, sumY = 0;
        int  count = 0, top = Integer.MAX_VALUE, bottom = Integer.MIN_VALUE;
        for (int y = window.y; y < window.y + window.height; y++) {
            for (int x = window.x; x < window.x + window.width; x++) {
                if (!isRed(frame, x, y, water) && !isBlue(frame, x, y, water)) {continue;}
                sumX += x;
                sumY += y;
                count++;
                top = Math.min(top, y);
                bottom = Math.max(bottom, y);
            }
        }
        if (count < MIN_PIXELS) {return Optional.empty();}
        return Optional.of(new Blob((double) sumX / count, (double) sumY / count, count, bottom - top + 1));
    }

    /**
     * Plumes mesurées sur une image.
     *
     * @param x      centre, abscisse écran
     * @param y      centre, ordonnée écran
     * @param count  nombre de pixels des plumes
     * @param height hauteur, en pixels
     */
    record Blob(double x, double y, int count, int height) {

        double distance(double otherX, double otherY) {

            return Math.hypot(x - otherX, y - otherY);
        }
    }

    /**
     * Plume rouge en (x..x+2, y) et plume bleue dans (x-1..x+3, y-7..y-2).
     */
    static boolean isSignature(Frame frame, int x, int y, Background background) {

        for (int n = 0; n <= 2; n++) {
            if (!isRed(frame, x + n, y, background)) {return false;}
        }
        for (int dy = 2; dy <= 7; dy++) {
            for (int dx = -1; dx <= 3; dx++) {
                if (isBlue(frame, x + dx, y - dy, background)) {return true;}
            }
        }
        return false;
    }

    private static boolean isRed(Frame frame, int x, int y, Background water) {

        if (!inside(frame, x, y)) {return false;}
        int rgb = at(frame, x, y);
        int r   = Rgb.red(rgb);
        int g   = Rgb.green(rgb);
        int b   = Rgb.blue(rgb);
        // Plus rouge que l'eau, et rouge dominant : sur une eau verte, la plume reste rouge (120, 60, 20)
        return r > g && r > b && (r - g) - (water.red() - water.green()) > RED_MARGIN && (r - b) - (water.red() - water.blue()) > RED_MARGIN;
    }

    private static boolean isBlue(Frame frame, int x, int y, Background water) {

        if (!inside(frame, x, y)) {return false;}
        int rgb = at(frame, x, y);
        int r   = Rgb.red(rgb);
        int g   = Rgb.green(rgb);
        int b   = Rgb.blue(rgb);
        // Plus bleu que l'eau, et plus bleu que rouge : sur la lave, une plume rouge plus sombre que l'eau serait sinon
        // « plus bleue » ; sous une lumière verte, la plume bleue reste plus bleue que rouge (69, 107, 84)
        return b > r && (b - r) - (water.blue() - water.red()) > BLUE_MARGIN && (b - g) - (water.blue() - water.green()) > BLUE_MARGIN;
    }

    private static boolean appeared(Frame before, Frame after, int x, int y) {

        if (!inside(before, x, y)) {return true;}
        int was = at(before, x, y);
        int now = at(after, x, y);
        return Math.abs(Rgb.red(was) - Rgb.red(now)) > CHANGE_THRESHOLD
               || Math.abs(Rgb.green(was) - Rgb.green(now)) > CHANGE_THRESHOLD
               || Math.abs(Rgb.blue(was) - Rgb.blue(now)) > CHANGE_THRESHOLD;
    }

    private static boolean inside(Frame frame, int x, int y) {

        return frame.contains(x - frame.x(), y - frame.y());
    }

    private static int at(Frame frame, int x, int y) {

        return frame.rgb(x - frame.x(), y - frame.y());
    }

    /**
     * Suit la plume rouge image après image et signale la touche, par rapport à la plume au repos (moyenne des
     * {@value #CALIBRATION} premières images) : sa surface visible tombe sous {@value #DIP_RATIO} fois celle au repos ou
     * disparaît (le bouchon plonge), ou son centre s'écarte de plus que sa hauteur, sur {@value #CONFIRMATIONS} images
     * consécutives.
     */
    static final class BiteWatcher {

        enum Verdict {WAITING, BITE, LOST}

        private final Point start;

        private double sumX, sumY, sumCount;
        private int    calibrated, height, suspicious;

        /**
         * @param start position du bouchon trouvée par {@link #locate}
         */
        BiteWatcher(Point start) {

            this.start = start;
        }

        /**
         * Fenêtre de suivi : autour de la plume au repos, assez grande pour sa taille et ses mouvements.
         */
        Rectangle window() {

            int    radius = Math.max(20, 3 * height);
            double x      = calibrated == 0 ? start.x : sumX / calibrated;
            double y      = calibrated == 0 ? start.y : sumY / calibrated;
            return new Rectangle((int) Math.round(x) - radius, (int) Math.round(y) - radius, 2 * radius, 2 * radius);
        }

        Verdict feed(Optional<Blob> measured) {

            if (calibrated < CALIBRATION) {
                measured.ifPresent(blob -> {
                    sumX += blob.x();
                    sumY += blob.y();
                    sumCount += blob.count();
                    height = Math.max(height, blob.height());
                    calibrated++;
                });
                return Verdict.WAITING;
            }

            double restCount = sumCount / calibrated;
            Verdict verdict;
            if (measured.isEmpty() || measured.get().count() < DIP_RATIO * restCount) {verdict = Verdict.LOST;}
            else if (measured.get().distance(sumX / calibrated, sumY / calibrated) > Math.max(MIN_MOVE, height)) {verdict = Verdict.BITE;}
            else {verdict = Verdict.WAITING;}

            if (verdict == Verdict.WAITING) {
                suspicious = 0;
                return verdict;
            }
            return ++suspicious >= CONFIRMATIONS ? verdict : Verdict.WAITING;
        }
    }
}
