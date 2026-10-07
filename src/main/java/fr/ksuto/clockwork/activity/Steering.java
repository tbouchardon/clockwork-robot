package fr.ksuto.clockwork.activity;

import java.awt.geom.Point2D;

/**
 * Calculs du pilote automatique, sur la carte (x vers l'est, y vers le sud) : de combien tourner pour viser le point, et
 * quand le point est atteint.
 */
final class Steering {

    /**
     * Rotation au clavier de WoW : 180° par seconde.
     */
    static final double DEGREES_PER_MILLISECOND = 0.18;

    /**
     * Écart de cap en dessous duquel on ne tourne pas : la position lue est imprécise, corriger moins ferait zigzaguer.
     */
    static final double TOLERANCE = 8;

    /**
     * Rotation la plus longue en un tour : au-delà, la course aurait dépassé le point avant la fin du virage.
     */
    static final int MAX_TURN = 1000;

    private Steering() {}

    /**
     * @return l'angle, en degrés, entre la direction suivie (de {@code previous} à {@code current}) et celle du point :
     * positif s'il est à droite, négatif à gauche, ±180 derrière
     */
    static double headingError(Point2D previous, Point2D current, Point2D destination) {

        double headingX = current.getX() - previous.getX(), headingY = current.getY() - previous.getY();
        double targetX  = destination.getX() - current.getX(), targetY = destination.getY() - current.getY();
        // y vers le sud : un produit vectoriel positif met le point à droite
        return Math.toDegrees(Math.atan2(headingX * targetY - headingY * targetX, headingX * targetX + headingY * targetY));
    }

    /**
     * @return la durée d'appui sur la flèche pour corriger l'écart (0 sous la tolérance)
     */
    static int turnDuration(double error) {

        double absolute = Math.abs(error);
        if (absolute < TOLERANCE) {return 0;}
        return (int) Math.min(MAX_TURN, Math.round(absolute / DEGREES_PER_MILLISECOND));
    }

    /**
     * Point atteint : assez près (moins d'un tour et demi de course), ou frôlé puis laissé derrière (la distance
     * remonte alors qu'il était à moins de quatre tours de course). Sans ce second cas, un point manqué de peu fait
     * tourner le personnage autour.
     *
     * @param remaining     distance au point
     * @param lastRemaining distance au tour précédent (nulle au premier tour)
     * @param traveled      distance parcourue depuis le tour précédent
     */
    static boolean reached(double remaining, Double lastRemaining, double traveled) {

        if (remaining <= traveled * 1.5) {return true;}
        return lastRemaining != null && remaining > lastRemaining && lastRemaining <= traveled * 4;
    }
}
