package fr.ksuto.clockwork.brain.perception;

/**
 * État d'une touche d'action, lu dans le QR code v2.
 *
 * @param key               combinaison : touche ("1".."=", "Q".."G"), éventuellement préfixée de "SHIFT-", "CTRL-" ou "ALT-"
 * @param spellId           identifiant du sort de la touche (0 si aucun, ou si c'est un objet)
 * @param cooldown          temps de recharge restant, en secondes (0 si prêt, plafonné à 60)
 * @param usable            sort utilisable (ressources, conditions ; pas un sort à incantation pendant un déplacement)
 * @param range             portée par rapport à la cible
 * @param sinceCastOnTarget secondes depuis le dernier lancement sur la cible actuelle (infini si jamais ou plus de 60 s)
 * @param sinceCast         secondes depuis le dernier lancement, toutes cibles (infini si jamais ou plus de 60 s)
 * @param proc              bouton en surbrillance (proc)
 * @param buffActive        l'aura du sort (ou du sort de l'objet) est active sur le joueur ; lue hors combat seulement, en
 *                          combat c'est l'état lu juste avant d'y entrer
 * @param itemId            identifiant de l'objet de la touche (potion, pierre de soins, leurre...), 0 si ce n'est pas un
 *                          objet ; {@code sinceCast} est alors le temps depuis sa dernière utilisation
 * @param count             nombre d'objets possédés, charges comprises (0 pour un sort)
 */
public record KeyState(String key, int spellId, double cooldown, boolean usable, Range range, double sinceCastOnTarget, double sinceCast,
                       boolean proc, boolean buffActive, int itemId, int count) {

    public KeyState(String key, int spellId, double cooldown, boolean usable, Range range, double sinceCastOnTarget, double sinceCast,
                    boolean proc, boolean buffActive) {

        this(key, spellId, cooldown, usable, range, sinceCastOnTarget, sinceCast, proc, buffActive, 0, 0);
    }

    public KeyState(String key, int spellId, double cooldown, boolean usable, Range range, double sinceCastOnTarget, double sinceCast,
                    boolean proc) {

        this(key, spellId, cooldown, usable, range, sinceCastOnTarget, sinceCast, proc, false);
    }

    /**
     * Le sort peut être lancé maintenant : utilisable, sans temps de recharge, pas hors de portée.
     */
    public boolean ready() {

        return castable() && range != Range.OUT;
    }

    /**
     * Le sort peut être lancé, portée mise à part : pour un sort lancé sur un membre du groupe, la portée de la touche
     * concerne la cible actuelle, pas lui.
     */
    public boolean castable() {

        return (spellId != 0 || itemId != 0) && usable && cooldown <= 0.05;
    }

    /**
     * La touche porte un sort ou un objet.
     */
    public boolean bound() {

        return spellId != 0 || itemId != 0;
    }

    public enum Range {
        IN,
        OUT,
        NONE
    }
}
