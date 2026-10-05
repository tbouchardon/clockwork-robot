package fr.ksuto.clockwork.brain.decision;

import fr.ksuto.clockwork.brain.data.SpellDatabase;
import fr.ksuto.clockwork.brain.perception.GameState;
import fr.ksuto.clockwork.brain.perception.KeyState;

import java.util.Optional;
import java.util.Set;

/**
 * Accès aux sorts par leur nom dans les conditions JEXL : {@code spell.cooldown('Horion de flammes') == 0}.
 * Un sort absent des touches décrites est considéré comme jamais prêt et jamais lancé.
 */
public final class SpellView {

    private final GameState state;
    private final SpellDatabase database;

    SpellView(GameState state, SpellDatabase database) {

        this.state = state;
        this.database = database;
    }

    Optional<KeyState> key(String reference) {

        Set<Integer> ids = database.idsFor(reference);
        return state.keys().values().stream().filter(key -> ids.contains(key.spellId())).findFirst();
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

    public boolean inRange(String reference) {

        return key(reference).map(key -> key.range() != KeyState.Range.OUT).orElse(false);
    }

    public double sinceCast(String reference) {

        return key(reference).map(KeyState::sinceCast).orElse(Double.POSITIVE_INFINITY);
    }

    public double sinceCastOnTarget(String reference) {

        return key(reference).map(KeyState::sinceCastOnTarget).orElse(Double.POSITIVE_INFINITY);
    }

    public boolean proc(String reference) {

        return key(reference).map(KeyState::proc).orElse(false);
    }

    /**
     * Le personnage est sous cette forme : {@code spell.form('Forme de félin')}. Contrairement aux autres méthodes, le
     * sort de la forme n'a pas besoin d'être sur une barre.
     */
    public boolean form(String reference) {

        return state.form() != 0 && database.idsFor(reference).contains(state.form());
    }
}
