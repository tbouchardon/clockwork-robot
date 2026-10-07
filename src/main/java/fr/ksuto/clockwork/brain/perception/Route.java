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
}
