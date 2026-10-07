package fr.ksuto.clockwork.brain.perception;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * Parcours actif du pilote automatique, publié par l'addon (routes.lua) : l'addon garde les parcours, le Java les suit.
 * Coordonnées en fraction de la carte du parcours (0 à 1).
 *
 * @param revision change à chaque modification du parcours ou changement de parcours
 * @param map      carte du parcours (0 = aucun parcours)
 * @param loop     recommencer une fois le dernier point atteint
 * @param player   position du joueur sur la carte du parcours, nulle s'il n'y est pas
 * @param points   points du parcours, dans l'ordre
 */
public record Route(int revision, int map, boolean loop, Point2D.Double player, List<Point2D.Double> points) {

    public static final Route NONE = new Route(0, 0, false, null, List.of());

    public boolean exists() {

        return map != 0;
    }

    /**
     * @return l'indice du point le plus proche du joueur (0 si sa position est inconnue)
     */
    public int nearestPoint() {

        if (player == null || points.isEmpty()) {return 0;}
        int nearest = 0;
        for (int index = 1; index < points.size(); index++) {
            if (points.get(index).distance(player) < points.get(nearest).distance(player)) {nearest = index;}
        }
        return nearest;
    }

    /**
     * Point à rejoindre au départ : le plus proche, ou le suivant si le joueur l'a déjà dépassé (plus près du suivant
     * que ne l'est le point lui-même), pour ne pas revenir en arrière.
     */
    public int startPoint() {

        int nearest = nearestPoint();
        if (player == null || points.size() < 2) {return nearest;}
        int next = nearest + 1;
        if (next >= points.size()) {
            if (!loop) {return nearest;}
            next = 0;
        }
        return player.distance(points.get(next)) < points.get(nearest).distance(points.get(next)) ? next : nearest;
    }
}
