package fr.ksuto.clockwork.activity;

import fr.ksuto.clockwork.brain.perception.Route;

import java.awt.geom.Point2D;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pilote automatique : cap, durée de virage, point atteint, point de départ.
 */
class SteeringTest {

    private static Point2D.Double p(double x, double y) {

        return new Point2D.Double(x, y);
    }

    @Test
    void headingErrorIsSignedOnTheMapAxes() {

        // Vers l'est (x croissant) ; y croît vers le sud : le sud est à droite, le nord à gauche
        assertEquals(0, Steering.headingError(p(0, 0), p(1, 0), p(5, 0)), 1e-9);
        assertEquals(90, Steering.headingError(p(0, 0), p(1, 0), p(1, 5)), 1e-9, "point au sud : à droite");
        assertEquals(-90, Steering.headingError(p(0, 0), p(1, 0), p(1, -5)), 1e-9, "point au nord : à gauche");
        assertEquals(180, Math.abs(Steering.headingError(p(0, 0), p(1, 0), p(-5, 0))), 1e-9, "derrière");
        assertEquals(45, Steering.headingError(p(0, 0), p(1, 0), p(3, 2)), 1e-9);
    }

    @Test
    void turnDurationFollowsWowTurnRate() {

        assertEquals(0, Steering.turnDuration(5), "sous la tolérance : tout droit");
        assertEquals(500, Steering.turnDuration(90), "90° à 180°/s");
        assertEquals(500, Steering.turnDuration(-90));
        assertEquals(Steering.MAX_TURN, Steering.turnDuration(180));
    }

    @Test
    void pointIsReachedWhenCloseOrPassed() {

        assertTrue(Steering.reached(100, 200d, 100), "à moins d'un tour et demi de course");
        assertFalse(Steering.reached(1000, 1100d, 100), "encore loin, et on s'en approche");
        assertTrue(Steering.reached(320, 300d, 100), "frôlé puis laissé derrière : la distance remonte");
        assertFalse(Steering.reached(2100, 2000d, 100), "s'éloigner loin du point n'est pas l'atteindre");
        assertFalse(Steering.reached(1000, null, 100), "premier tour");
    }

    @Test
    void startPointSkipsAPointAlreadyPassed() {

        List<Point2D.Double> square = List.of(p(0, 0), p(1, 0), p(1, 1), p(0, 1));

        // Juste après le point 2 (indice 1), en route vers le 3 : ne pas revenir en arrière
        assertEquals(2, new Route(1, 1, true, p(1, 0.2), square).startPoint());
        // Juste avant le point 2, en venant du 1
        assertEquals(1, new Route(1, 1, true, p(0.8, 0), square).startPoint());
        // Dernier point dépassé : en boucle, le premier ; sinon le dernier
        assertEquals(0, new Route(1, 1, true, p(0, 0.8), square).startPoint());
        assertEquals(3, new Route(1, 1, false, p(0, 0.8), square).startPoint());
    }
}
