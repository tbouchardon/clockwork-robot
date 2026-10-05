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
     * Distance de suivi : le bouchon est recherché à ± cette distance de sa dernière position.
     */
    static final int TRACK_RADIUS = 10;

    /**
     * Écart à la position moyenne, en pixels, au-delà duquel le bouchon a plongé.
     */
    static final double BITE_DISTANCE = 6;

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
     * Cherche le bouchon à ± {@value #TRACK_RADIUS} pixels de sa dernière position.
     */
    static Optional<Point> track(Frame frame, Point last, Background background) {

        return locate(frame, null, new Rectangle(last.x - TRACK_RADIUS, last.y - TRACK_RADIUS, 2 * TRACK_RADIUS, 2 * TRACK_RADIUS), background);
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
        return (r - g) - (water.red() - water.green()) > RED_MARGIN && (r - b) - (water.red() - water.blue()) > RED_MARGIN;
    }

    private static boolean isBlue(Frame frame, int x, int y, Background water) {

        if (!inside(frame, x, y)) {return false;}
        int rgb = at(frame, x, y);
        int r   = Rgb.red(rgb);
        int g   = Rgb.green(rgb);
        int b   = Rgb.blue(rgb);
        return (b - r) - (water.blue() - water.red()) > BLUE_MARGIN && (b - g) - (water.blue() - water.green()) > BLUE_MARGIN;
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
     * Suit le bouchon image après image et signale la touche : le bouchon s'écarte de sa position moyenne d'au moins
     * {@value #BITE_DISTANCE} pixels, ou disparaît (il plonge), sur {@value #CONFIRMATIONS} images consécutives.
     */
    static final class BiteWatcher {

        enum Verdict {WAITING, BITE, LOST}

        private double sumX;
        private double sumY;
        private int    count;
        private int    away;
        private int    missing;

        BiteWatcher(Point initial) {

            add(initial);
        }

        /**
         * @param position position du bouchon sur la nouvelle image, vide s'il n'est plus visible
         */
        Verdict feed(Optional<Point> position) {

            if (position.isEmpty()) {
                away = 0;
                return ++missing >= CONFIRMATIONS ? Verdict.LOST : Verdict.WAITING;
            }
            missing = 0;
            // Moyenne des positions précédentes, sans la mesure courante : le mouvement n'est pas amorti
            double distance = position.get().distance(sumX / count, sumY / count);
            if (distance >= BITE_DISTANCE) {
                return ++away >= CONFIRMATIONS ? Verdict.BITE : Verdict.WAITING;
            }
            away = 0;
            add(position.get());
            return Verdict.WAITING;
        }

        Point average() {

            return new Point((int) Math.round(sumX / count), (int) Math.round(sumY / count));
        }

        private void add(Point point) {

            sumX += point.x;
            sumY += point.y;
            count++;
        }
    }
}
