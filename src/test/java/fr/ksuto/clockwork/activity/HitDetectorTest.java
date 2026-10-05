package fr.ksuto.clockwork.activity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HitDetectorTest {

    @Test
    void healthLossWithoutActionForSixSeconds() {

        HitDetector detector = new HitDetector();
        long        lastAction = 0;

        assertFalse(detector.hitWithoutRetaliating(10_000, 100, lastAction));
        assertFalse(detector.hitWithoutRetaliating(11_000, 99, lastAction), "perte sous le seuil (arrondis)");
        assertTrue(detector.hitWithoutRetaliating(12_000, 95, lastAction), "5 points perdus, aucune touche depuis 12 s");
    }

    @Test
    void noAlertWhileTheBotActs() {

        HitDetector detector = new HitDetector();

        detector.hitWithoutRetaliating(10_000, 100, 9_000);
        assertFalse(detector.hitWithoutRetaliating(12_000, 80, 9_000), "le bot a appuyé il y a 3 s : il riposte");
    }

    @Test
    void regenerationDoesNotHideALossButOldLossesExpire() {

        HitDetector detector = new HitDetector();

        detector.hitWithoutRetaliating(10_000, 100, 0);
        detector.hitWithoutRetaliating(11_000, 90, 0);
        assertTrue(detector.hitWithoutRetaliating(12_000, 92, 0), "comparé au maximum de la fenêtre, pas à la mesure précédente");
        assertFalse(detector.hitWithoutRetaliating(20_000, 92, 0), "le maximum de 100 % a quitté la fenêtre de 6 s");
    }

    @Test
    void resetForgetsTheLossAlreadySeen() {

        HitDetector detector = new HitDetector();

        detector.hitWithoutRetaliating(10_000, 100, 0);
        assertTrue(detector.hitWithoutRetaliating(11_000, 90, 0));
        detector.reset();
        assertFalse(detector.hitWithoutRetaliating(11_500, 90, 0));
    }
}
