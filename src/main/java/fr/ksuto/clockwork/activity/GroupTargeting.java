package fr.ksuto.clockwork.activity;

import java.awt.event.KeyEvent;

/**
 * Raccourcis de ciblage des membres du groupe, identiques à group.lua côté addon : boutons sécurisés reliés à
 * Alt+Maj+A..T (membres 1..20), Alt+Ctrl+A..T (21..40), et Alt+Maj+U pour revenir à la cible précédente.
 */
final class GroupTargeting {

    /**
     * Touche avec ses modificateurs.
     */
    record Shortcut(int key, boolean alt, boolean ctrl, boolean shift) {}

    static final Shortcut LAST_TARGET = new Shortcut(KeyEvent.VK_U, true, false, true);

    private GroupTargeting() {}

    /**
     * @param slot emplacement du membre, 1..40
     */
    static Shortcut member(int slot) {

        if (slot < 1 || slot > 40) {throw new IllegalArgumentException("Emplacement de membre hors de 1..40 : " + slot);}
        int letter = KeyEvent.VK_A + (slot - 1) % 20;
        return slot <= 20 ? new Shortcut(letter, true, false, true) : new Shortcut(letter, true, true, false);
    }
}
