package fr.ksuto.clockwork.brain.perception;

import java.util.Map;
import java.util.Optional;

/**
 * État du jeu lu dans le QR code v2 : ce que le cerveau connaît pour décider.
 *
 * @param playerHealth     vie du joueur, en %
 * @param playerPower      ressource principale du joueur, en %
 * @param hasTarget        une cible est sélectionnée
 * @param targetHostile    la cible est hostile
 * @param targetHealth     vie de la cible, en %
 * @param targetPower      ressource principale de la cible, en %
 * @param targetInCombat   la cible est en combat
 * @param inCombat         le joueur est en combat
 * @param casting          le joueur incante
 * @param enemies          ennemis en combat à proximité (barres de vie)
 * @param facing           direction du personnage, en radians (0..2π)
 * @param recommendedSpell sort recommandé par Blizzard (0 si aucun)
 * @param aggro            mode aggro de l'addon : attaquer aussi une cible qui n'est pas en combat
 * @param playerDead       le joueur est mort (ou fantôme)
 * @param mounted          le joueur est sur une monture
 * @param targetTapDenied  la cible est déjà marquée par un autre joueur
 * @param form             sort de la forme active (druide : félin, ours, sélénien...), 0 si aucune
 * @param comboPoints      points de combo du joueur
 * @param classId          classe du personnage (identifiant du jeu : 7 = chaman), 0 si inconnue
 * @param specId           spécialisation active (identifiant du jeu : 262 = Élémentaire), 0 si inconnue
 * @param frame            compteur de mises à jour de l'addon (v3, 0 en v2) : inchangé, la grille est figée
 * @param cast             sort en cours d'incantation ou de canalisation ({@link Cast#NONE} si aucun)
 * @param keys             état de chaque touche, par nom de touche
 */
public record GameState(double playerHealth, double playerPower, boolean hasTarget, boolean targetHostile, double targetHealth, double targetPower,
                        boolean targetInCombat, boolean inCombat, boolean casting, int enemies, double facing, int recommendedSpell, boolean aggro,
                        boolean playerDead, boolean mounted, boolean targetTapDenied, int form, int comboPoints, int classId, int specId, int frame, Cast cast, Map<String, KeyState> keys) {

    /**
     * Même règle que l'addon (Clockwork:rotation) : hors mode aggro, on n'attaque pas une cible hors combat
     * quand on est déjà en combat.
     */
    public boolean mayAct() {
        
        return aggro || !inCombat || targetInCombat;
    }
    
    /**
     * Sort en cours.
     *
     * @param spellId    identifiant du sort (0 si inconnu)
     * @param channeling canalisation (Drain de vie...) plutôt qu'incantation (Éclair...)
     * @param remaining  secondes restantes (plafonnées à 10)
     */
    public record Cast(int spellId, boolean channeling, double remaining) {

        public static final Cast NONE = new Cast(0, false, 0);
    }

    /**
     * Garde-fous communs à toutes les rotations, comme l'addon (Clockwork:rotation) : rien sur une monture ou mort.
     * L'incantation en cours est traitée par le cerveau selon la priorité du sort.
     */
    public boolean busy() {

        return mounted || playerDead;
    }

    /**
     * Cible à attaquer : hostile, vivante, et pas déjà marquée par un autre joueur (comme unitExistCanAndShouldDie).
     */
    public boolean attackableTarget() {

        return hasTarget && targetHostile && !targetTapDenied && targetHealth > 0;
    }

    /**
     * Les cases des touches sont remplies : l'addon a fait au moins une mise à jour depuis son activation.
     */
    public boolean keysReady() {
        
        return keys.values().stream().anyMatch(key -> key.spellId() != 0);
    }
    
    /**
     * Touche contenant le sort, s'il est sur une barre d'action.
     */
    public Optional<KeyState> keyForSpell(int spellId) {

        if (spellId == 0) {return Optional.empty();}
        return keys.values().stream().filter(key -> key.spellId() == spellId).findFirst();
    }
}
