package fr.ksuto.clockwork.brain.perception;

import java.util.List;

/**
 * Groupe ou raid lu dans la grille v4 (group.lua) : le mode soigneur et les membres présents.
 *
 * @param healerMode mode soigneur de l'addon : le cerveau soigne aussi les autres membres
 * @param raid       le joueur est en raid (emplacement n = raidN) ; sinon emplacement 1 = le joueur, 2..5 = party1..4
 * @param members    membres présents, par emplacement croissant
 */
public record Group(boolean healerMode, boolean raid, List<Member> members) {

    public static final Group NONE = new Group(false, false, List.of());

    /**
     * Rôle attribué dans le groupe.
     */
    public enum Role {
        NONE,
        TANK,
        HEALER,
        DAMAGER
    }

    /**
     * Membre du groupe.
     *
     * @param slot    emplacement 1..40, celui du raccourci qui le cible
     * @param health  vie, en %
     * @param inRange à portée de soin ; vrai si l'addon n'a pas pu le savoir (le jeu refusera le sort s'il est trop loin)
     * @param dead    mort ou fantôme
     * @param offline déconnecté
     * @param self    c'est le joueur
     * @param role    rôle attribué
     */
    public record Member(int slot, double health, boolean inRange, boolean dead, boolean offline, boolean self, Role role) {

        /**
         * Peut recevoir un soin : vivant, connecté et à portée.
         */
        public boolean healable() {

            return !dead && !offline && inRange;
        }
    }
}
