package fr.ksuto.clockwork.brain.decision;

import fr.ksuto.clockwork.brain.data.SpellDatabase;
import fr.ksuto.clockwork.brain.perception.GameState;
import fr.ksuto.clockwork.brain.perception.KeyState;

import java.util.Optional;
import java.util.Set;

/**
 * Accès aux objets des barres par leur nom dans les conditions JEXL : {@code item.count('Pierre de soins') > 0}.
 * Un objet absent des touches décrites est considéré comme jamais prêt, jamais utilisé, et possédé en 0 exemplaire.
 */
public final class ItemView {

    private final GameState     state;
    private final SpellDatabase database;

    ItemView(GameState state, SpellDatabase database) {

        this.state = state;
        this.database = database;
    }

    Optional<KeyState> key(String reference) {

        Set<Integer> ids = database.itemIdsFor(reference);
        return state.keys().values().stream().filter(key -> key.itemId() != 0 && ids.contains(key.itemId())).findFirst();
    }

    public boolean onBar(String reference) {

        return key(reference).isPresent();
    }

    public boolean ready(String reference) {

        return key(reference).map(KeyState::ready).orElse(false);
    }

    public double cooldown(String reference) {

        return key(reference).map(KeyState::cooldown).orElse(Double.POSITIVE_INFINITY);
    }

    public boolean usable(String reference) {

        return key(reference).map(KeyState::usable).orElse(false);
    }

    /**
     * Nombre possédé, charges comprises (plafonné à 255).
     */
    public int count(String reference) {

        return key(reference).map(KeyState::count).orElse(0);
    }

    /**
     * Secondes depuis la dernière utilisation, infini si jamais ou plus de 60 s.
     */
    public double sinceUse(String reference) {

        return key(reference).map(KeyState::sinceCast).orElse(Double.POSITIVE_INFINITY);
    }

    /**
     * L'aura du sort de l'objet est active sur le joueur (appât, nourriture...). Lue hors combat seulement.
     */
    public boolean buffActive(String reference) {

        return key(reference).map(KeyState::buffActive).orElse(false);
    }
}
