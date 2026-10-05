package fr.ksuto.clockwork.activity;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Détecte que le personnage se fait frapper sans riposter : sa vie a baissé ces dernières secondes alors que le bot n'a
 * appuyé sur aucune touche depuis aussi longtemps (un monstre non ciblé, dans le dos par exemple).
 * <p>
 * La vie est secrète pour l'addon, qui ne peut pas la comparer ; elle est lue en clair dans la grille par le Java.
 */
final class HitDetector {

    /**
     * Durée sans action, et fenêtre d'observation de la vie.
     */
    static final long WINDOW = 6000;

    /**
     * Baisse de vie minimale, en points de %, au-delà des arrondis de couleur (1 / 255 ≈ 0,4 %).
     */
    static final double MIN_LOSS = 2;

    private record Sample(long time, double health) {}

    private final Deque<Sample> samples = new ArrayDeque<>();

    /**
     * @param now        instant de la mesure, en ms
     * @param health     vie du joueur, en %
     * @param lastAction instant du dernier appui du bot, en ms
     * @return la vie a baissé d'au moins {@value #MIN_LOSS} points sur la fenêtre, sans action du bot depuis
     */
    boolean hitWithoutRetaliating(long now, double health, long lastAction) {

        samples.addLast(new Sample(now, health));
        while (!samples.isEmpty() && now - samples.peekFirst().time() > WINDOW) {samples.removeFirst();}

        if (now - lastAction < WINDOW) {return false;}
        // Maximum de la fenêtre plutôt que valeur précédente : la régénération ne masque pas une perte
        double highest = samples.stream().mapToDouble(Sample::health).max().orElse(health);
        return highest - health >= MIN_LOSS;
    }

    /**
     * Repart de zéro (après un demi-tour : la perte déjà constatée ne doit pas en déclencher un autre).
     */
    void reset() {

        samples.clear();
    }
}
