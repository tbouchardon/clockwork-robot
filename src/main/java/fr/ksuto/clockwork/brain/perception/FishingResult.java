package fr.ksuto.clockwork.brain.perception;

/**
 * Résultat du dernier lancer de pêche terminé, publié par l'addon (fishing.lua) : le compteur change à chaque lancer
 * terminé.
 *
 * @param counter compteur de lancers terminés, modulo 256
 * @param outcome résultat du dernier
 */
public record FishingResult(int counter, Outcome outcome) {

    public enum Outcome {
        /**
         * Aucun lancer terminé depuis le chargement de l'addon.
         */
        NONE,
        /**
         * Butin de pêche ouvert : un poisson (ou un objet) ramené.
         */
        CAUGHT,
        /**
         * « Votre poisson s'est échappé » : clic trop tardif.
         */
        ESCAPED,
        /**
         * « Aucun poisson n'a mordu » : clic trop tôt, faux clic.
         */
        NOT_HOOKED,
        /**
         * Fin de la canalisation sans rien : pas de clic, ou clic hors du bouchon.
         */
        NOTHING
    }
}
