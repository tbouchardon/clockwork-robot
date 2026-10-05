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
 * @param form             sort de la forme active (druide : félin, ours, sélénien...), 0 si aucune
 * @param comboPoints      points de combo du joueur
 * @param frame            compteur de mises à jour de l'addon (v3, 0 en v2) : inchangé, la grille est figée
 * @param keys             état de chaque touche, par nom de touche
 */
public record GameState(double playerHealth, double playerPower, boolean hasTarget, boolean targetHostile, double targetHealth, double targetPower,
                        boolean targetInCombat, boolean inCombat, boolean casting, int enemies, double facing, int recommendedSpell, boolean aggro,
                        int form, int comboPoints, int frame, Map<String, KeyState> keys) {

    /**
     * Même règle que l'addon (Clockwork:rotation) : hors mode aggro, on n'attaque pas une cible hors combat
     * quand on est déjà en combat.
     */
    public boolean mayAct() {
        
        return aggro || !inCombat || targetInCombat;
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
