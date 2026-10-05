package fr.ksuto.clockwork.activity;

import fr.ksuto.prh.capture.Frame;
import fr.ksuto.prh.capture.Rgb;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repère le bouchon de pêche et détecte la touche, sur des captures d'écran (coordonnées écran).
 * <p>
 * Un pixel de plume est un pixel devenu nettement plus rouge, ou plus bleu, qu'il ne l'était <b>au même endroit juste
 * avant le lancer</b> (image de référence). Ce qui était déjà là (herbes, rive, fleurs) ne change pas de teinte et ne
 * compte pas, quelle que soit la taille de l'étendue d'eau ; et la couleur de l'eau, même verte, rouge ou de lave, sert
 * de référence pixel par pixel. Vérifié sur des captures en jeu (12.1) : eau boueuse en vue lointaine, eau verte
 * lumineuse en vue à la première personne.
 * <p>
 * Pour le trouver : un amas de pixels de plume apparus, de préférence portant les deux plumes (rouge et bleue).
 */
final class BobberDetector {

    /**
     * Images consécutives nécessaires pour conclure (bouchon écarté ou disparu), contre une image isolée bruitée.
     */
    static final int CONFIRMATIONS = 2;

    /**
     * Plume rouge : rouge moins vert, et rouge moins bleu, dépassent ceux du même pixel avant le lancer d'au moins
     * cette valeur.
     */
    static final int RED_MARGIN = 45;

    /**
     * Plume bleue : bleu moins rouge, et bleu moins vert, dépassent ceux du même pixel avant le lancer d'au moins cette
     * valeur.
     */
    static final int BLUE_MARGIN = 20;

    /**
     * Pixels minimum pour considérer les plumes visibles.
     */
    static final int MIN_PIXELS = 3;

    /**
     * Taille minimale d'un amas d'une seule couleur pour être retenu comme bouchon.
     */
    static final int MIN_CLUSTER = 8;

    /**
     * Le bouchon est cherché à moins de cette distance de sa position attendue.
     */
    static final double SEED_RADIUS = 25;

    /**
     * Pixels des plumes reliés de proche en proche à cette distance près (franchit l'écart entre les deux plumes).
     */
    static final int LINK = 3;

    /**
     * Fenêtre de suivi : demi-côté minimal et maximal, en pixels.
     */
    static final int MIN_WINDOW = 20;
    static final int MAX_WINDOW = 60;

    /**
     * Images de calibrage (~0,4 s) : les plumes au repos (surface et centre médians, insensibles à l'éclaboussure de
     * l'arrivée du bouchon), référence de la touche.
     */
    static final int CALIBRATION = 20;

    /**
     * Touche : la surface visible des plumes tombe sous cette fraction de leur surface au repos (le bouchon plonge).
     * Mesuré en jeu (15 lancers, vue à la première personne) : 0,38 à 0,44 à la touche, rarement sous 0,58 au repos.
     */
    static final double DIP_RATIO = 0.55;

    /**
     * Touche : le centre des plumes s'écarte de sa position au repos de plus que leur hauteur (au moins ce minimum).
     */
    static final double MIN_MOVE = 4;

    private BobberDetector() {}

    /**
     * Cherche le bouchon dans la zone : les pixels de plume apparus depuis le lancer, regroupés en amas reliés de proche
     * en proche. De préférence un amas portant les deux plumes (la signature : rouge et bleue), le plus gros ; à défaut,
     * le plus gros amas d'au moins {@value #MIN_CLUSTER} pixels (sur la lave, seule la plume bleue ressort).
     *
     * @param after     capture après le lancer, couvrant la zone
     * @param reference capture avant le lancer, couvrant la zone
     * @return le centre de l'amas retenu
     */
    static Optional<Point> locate(Frame after, Frame reference, Rectangle zone) {

        boolean[][] red  = new boolean[zone.height][zone.width];
        boolean[][] blue = new boolean[zone.height][zone.width];
        for (int y = 0; y < zone.height; y++) {
            for (int x = 0; x < zone.width; x++) {
                red[y][x] = isRed(after, reference, zone.x + x, zone.y + y);
                blue[y][x] = !red[y][x] && isBlue(after, reference, zone.x + x, zone.y + y);
            }
        }

        boolean[][] seen      = new boolean[zone.height][zone.width];
        Point       bestBoth  = null, bestAny = null;
        int         sizeBoth  = 0, sizeAny = 0;
        for (int y = 0; y < zone.height; y++) {
            for (int x = 0; x < zone.width; x++) {
                if (seen[y][x] || !(red[y][x] || blue[y][x])) {continue;}
                // Amas : parcours de proche en proche
                long                sumX = 0, sumY = 0;
                int                 count = 0, reds = 0, blues = 0;
                ArrayDeque<Integer> queue = new ArrayDeque<>();
                queue.add(y * zone.width + x);
                seen[y][x] = true;
                while (!queue.isEmpty()) {
                    int current = queue.poll(), cx = current % zone.width, cy = current / zone.width;
                    sumX += zone.x + cx;
                    sumY += zone.y + cy;
                    count++;
                    if (red[cy][cx]) {reds++;}
                    else {blues++;}
                    for (int dy = -LINK; dy <= LINK; dy++) {
                        for (int dx = -LINK; dx <= LINK; dx++) {
                            int nx = cx + dx, ny = cy + dy;
                            if (nx < 0 || ny < 0 || nx >= zone.width || ny >= zone.height || seen[ny][nx] || !(red[ny][nx] || blue[ny][nx])) {continue;}
                            seen[ny][nx] = true;
                            queue.add(ny * zone.width + nx);
                        }
                    }
                }
                Point center = new Point((int) Math.round((double) sumX / count), (int) Math.round((double) sumY / count));
                if (reds >= 3 && blues >= 1 && count > sizeBoth) {
                    sizeBoth = count;
                    bestBoth = center;
                }
                if (count >= MIN_CLUSTER && count > sizeAny) {
                    sizeAny = count;
                    bestAny = center;
                }
            }
        }
        return Optional.ofNullable(bestBoth != null ? bestBoth : bestAny);
    }

    /**
     * Plumes du bouchon dans la fenêtre : leurs pixels, leur centre et leur hauteur. Les deux plumes comptent : selon
     * l'eau, c'est l'une ou l'autre qui ressort le mieux. Seuls les pixels reliés au bouchon comptent, de proche en
     * proche à {@value #LINK} pixels près, en partant de tous les pixels de plume à moins de {@value #SEED_RADIUS} pixels
     * de sa position attendue. Le centre de tous ces pixels est stable d'une image à l'autre.
     *
     * @param reference capture avant le lancer, couvrant la fenêtre
     * @param seed      position attendue du bouchon
     * @return vide si moins de {@value #MIN_PIXELS} pixels près de la position attendue (bouchon sous l'eau)
     */
    static Optional<Blob> measure(Frame frame, Frame reference, Rectangle window, Point seed) {

        // Départs : tous les pixels de plume proches de la position attendue (les deux plumes, même si l'écart entre
        // elles dépasse LINK sur certaines images : sinon une seule serait comptée, et sa surface prise pour une plongée)
        boolean[][]         feather = new boolean[window.height][window.width];
        boolean[][]         seen    = new boolean[window.height][window.width];
        ArrayDeque<Integer> queue   = new ArrayDeque<>();
        for (int y = 0; y < window.height; y++) {
            for (int x = 0; x < window.width; x++) {
                int screenX = window.x + x, screenY = window.y + y;
                if (!isFeather(frame, reference, screenX, screenY)) {continue;}
                feather[y][x] = true;
                if (seed.distance(screenX, screenY) <= SEED_RADIUS) {
                    seen[y][x] = true;
                    queue.add(y * window.width + x);
                }
            }
        }
        if (queue.isEmpty()) {return Optional.empty();}

        long sumX  = 0, sumY = 0;
        int  count = 0, top = Integer.MAX_VALUE, bottom = Integer.MIN_VALUE;
        while (!queue.isEmpty()) {
            int current = queue.poll(), cx = current % window.width, cy = current / window.width;
            sumX += window.x + cx;
            sumY += window.y + cy;
            count++;
            top = Math.min(top, cy);
            bottom = Math.max(bottom, cy);
            for (int dy = -LINK; dy <= LINK; dy++) {
                for (int dx = -LINK; dx <= LINK; dx++) {
                    int nx = cx + dx, ny = cy + dy;
                    if (nx < 0 || ny < 0 || nx >= window.width || ny >= window.height || seen[ny][nx] || !feather[ny][nx]) {continue;}
                    seen[ny][nx] = true;
                    queue.add(ny * window.width + nx);
                }
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

    static boolean isFeather(Frame frame, Frame reference, int x, int y) {

        return isRed(frame, reference, x, y) || isBlue(frame, reference, x, y);
    }

    /**
     * Devenu nettement plus rouge qu'avant le lancer, et rouge dominant (sous une lumière verte, la plume reste rouge :
     * 120, 60, 20).
     */
    private static boolean isRed(Frame frame, Frame reference, int x, int y) {

        if (!inside(frame, x, y) || !inside(reference, x, y)) {return false;}
        int now = at(frame, x, y), was = at(reference, x, y);
        int r   = Rgb.red(now), g = Rgb.green(now), b = Rgb.blue(now);
        int r0  = Rgb.red(was), g0 = Rgb.green(was), b0 = Rgb.blue(was);
        return r > g && r > b && (r - g) - (r0 - g0) > RED_MARGIN && (r - b) - (r0 - b0) > RED_MARGIN;
    }

    /**
     * Devenu nettement plus bleu qu'avant le lancer, et plus bleu que rouge (sur la lave, une plume rouge plus sombre
     * que l'eau serait sinon « plus bleue » ; sous une lumière verte, la plume bleue reste plus bleue que rouge :
     * 69, 107, 84).
     */
    private static boolean isBlue(Frame frame, Frame reference, int x, int y) {

        if (!inside(frame, x, y) || !inside(reference, x, y)) {return false;}
        int now = at(frame, x, y), was = at(reference, x, y);
        int r   = Rgb.red(now), g = Rgb.green(now), b = Rgb.blue(now);
        int r0  = Rgb.red(was), g0 = Rgb.green(was), b0 = Rgb.blue(was);
        return b > r && (b - r) - (b0 - r0) > BLUE_MARGIN && (b - g) - (b0 - g0) > BLUE_MARGIN;
    }

    private static boolean inside(Frame frame, int x, int y) {

        return frame.contains(x - frame.x(), y - frame.y());
    }

    private static int at(Frame frame, int x, int y) {

        return frame.rgb(x - frame.x(), y - frame.y());
    }

    /**
     * Suit les plumes image après image et signale la touche, par rapport à la plume au repos (moyenne des
     * {@value #CALIBRATION} premières images) : sa surface visible tombe sous {@value #DIP_RATIO} fois celle au repos ou
     * disparaît (le bouchon plonge), ou son centre s'écarte de plus que sa hauteur, sur {@value #CONFIRMATIONS} images
     * consécutives.
     */
    static final class BiteWatcher {

        enum Verdict {WAITING, BITE, LOST}

        private final Point        start;
        private final List<Blob>   calibration = new ArrayList<>();
        private       Blob         rest;
        private       int          suspicious;
        private       double       lastX, lastY;

        /**
         * @param start position du bouchon trouvée par {@link #locate}
         */
        BiteWatcher(Point start) {

            this.start = start;
            this.lastX = start.x;
            this.lastY = start.y;
        }

        /**
         * Hauteur des plumes au repos (médiane du calibrage), ou la plus récente pendant le calibrage.
         */
        int restHeight() {

            if (rest != null) {return rest.height();}
            return calibration.isEmpty() ? 0 : median(calibration.stream().map(Blob::height).toList()).intValue();
        }

        /**
         * Position attendue du bouchon : au repos une fois calibré, sinon la dernière mesurée.
         */
        Point seed() {

            if (rest != null) {return new Point((int) Math.round(rest.x()), (int) Math.round(rest.y()));}
            return new Point((int) Math.round(lastX), (int) Math.round(lastY));
        }

        /**
         * Fenêtre de suivi : autour de la position attendue, de demi-côté 3 fois la hauteur des plumes, bornée entre
         * {@value #MIN_WINDOW} et {@value #MAX_WINDOW} pixels (elle ne peut pas s'emballer).
         */
        Rectangle window() {

            int   radius = Math.clamp(3L * restHeight(), MIN_WINDOW, MAX_WINDOW);
            Point center = seed();
            return new Rectangle(center.x - radius, center.y - radius, 2 * radius, 2 * radius);
        }

        Verdict feed(Optional<Blob> measured) {

            measured.ifPresent(blob -> {
                lastX = blob.x();
                lastY = blob.y();
            });
            if (rest == null) {
                measured.ifPresent(calibration::add);
                if (calibration.size() >= CALIBRATION) {
                    rest = new Blob(median(calibration.stream().map(Blob::x).toList()), median(calibration.stream().map(Blob::y).toList()),
                                    median(calibration.stream().map(Blob::count).toList()).intValue(),
                                    median(calibration.stream().map(Blob::height).toList()).intValue());
                }
                return Verdict.WAITING;
            }

            Verdict verdict;
            if (measured.isEmpty() || measured.get().count() < DIP_RATIO * rest.count()) {verdict = Verdict.LOST;}
            else if (measured.get().distance(rest.x(), rest.y()) > Math.max(MIN_MOVE, rest.height())) {verdict = Verdict.BITE;}
            else {verdict = Verdict.WAITING;}

            if (verdict == Verdict.WAITING) {
                suspicious = 0;
                return verdict;
            }
            return ++suspicious >= CONFIRMATIONS ? verdict : Verdict.WAITING;
        }

        private static <T extends Number & Comparable<T>> Double median(List<T> values) {

            List<T> sorted = new ArrayList<>(values);
            Collections.sort(sorted);
            return sorted.get(sorted.size() / 2).doubleValue();
        }
    }
}
