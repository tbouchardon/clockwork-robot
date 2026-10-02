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
 * @param inCombat         le joueur est en combat
 * @param casting          le joueur incante
 * @param enemies          ennemis en combat à proximité (barres de vie)
 * @param facing           direction du personnage, en radians (0..2π)
 * @param recommendedSpell sort recommandé par Blizzard (0 si aucun)
 * @param keys             état de chaque touche, par nom de touche
 */
public record GameState(double playerHealth, double playerPower, boolean hasTarget, boolean targetHostile, double targetHealth, double targetPower,
                        boolean inCombat, boolean casting, int enemies, double facing, int recommendedSpell, Map<String, KeyState> keys) {

    /**
     * Touche contenant le sort, s'il est sur une barre d'action.
     */
    public Optional<KeyState> keyForSpell(int spellId) {

        if (spellId == 0) {return Optional.empty();}
        return keys.values().stream().filter(key -> key.spellId() == spellId).findFirst();
    }
}
