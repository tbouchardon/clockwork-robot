package fr.ksuto.clockwork.brain.perception;

/**
 * État d'une touche d'action, lu dans le QR code v2.
 *
 * @param key               combinaison : touche ("1".."=", "Q".."G"), éventuellement préfixée de "SHIFT-", "CTRL-" ou "ALT-"
 * @param spellId           identifiant du sort de la touche (0 si aucun)
 * @param cooldown          temps de recharge restant, en secondes (0 si prêt, plafonné à 60)
 * @param usable            sort utilisable (ressources, conditions)
 * @param range             portée par rapport à la cible
 * @param sinceCastOnTarget secondes depuis le dernier lancement sur la cible actuelle (infini si jamais ou plus de 60 s)
 * @param sinceCast         secondes depuis le dernier lancement, toutes cibles (infini si jamais ou plus de 60 s)
 * @param proc              bouton en surbrillance (proc)
 */
public record KeyState(String key, int spellId, double cooldown, boolean usable, Range range, double sinceCastOnTarget, double sinceCast,
                       boolean proc) {

    /**
     * Le sort peut être lancé maintenant : utilisable, sans temps de recharge, pas hors de portée.
     */
    public boolean ready() {

        return spellId != 0 && usable && cooldown <= 0.05 && range != Range.OUT;
    }

    public enum Range {
        IN,
        OUT,
        NONE
    }
}
