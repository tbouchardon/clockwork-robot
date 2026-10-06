package fr.ksuto.clockwork.brain.decision;

import fr.ksuto.clockwork.brain.data.SpellDatabase;
import fr.ksuto.clockwork.brain.perception.GameState;
import fr.ksuto.clockwork.brain.perception.KeyState;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Accès aux sorts par leur nom dans les conditions JEXL : {@code spell.cooldown('Horion de flammes') == 0}.
 * Un sort absent des touches décrites est considéré comme jamais prêt et jamais lancé.
 */
public final class SpellView {

    private final GameState                        state;
    private final SpellDatabase                    database;
    private final Map<Integer, Map<Integer, Long>> targetCasts;
    private final long                             now;

    /**
     * @param targetCasts lancements du cerveau sur chaque ennemi (identifiant de cible → sort → instant en ms)
     * @param now         instant de la décision, en ms
     */
    SpellView(GameState state, SpellDatabase database, Map<Integer, Map<Integer, Long>> targetCasts, long now) {

        this.state = state;
        this.database = database;
        this.targetCasts = targetCasts;
        this.now = now;
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

    /**
     * Nombre d'ennemis différents sur lesquels le sort a été lancé depuis moins de {@code seconds} secondes : ceux qui
     * portent encore ce DoT, s'il dure autant. Comparé à {@code enemies}, il dit s'il reste des ennemis à affliger :
     * {@code spell.dotted('Corruption', 14) < enemies}. La cible actuelle compte aussi si on l'a affligée à la main.
     */
    public int dotted(String reference, double seconds) {

        Set<Integer> ids     = database.idsFor(reference);
        Set<Integer> targets = new HashSet<>();
        targetCasts.forEach((target, casts) -> {
            if (casts.entrySet().stream().anyMatch(cast -> ids.contains(cast.getKey()) && (now - cast.getValue()) / 1000.0 < seconds)) {
                targets.add(target);
            }
        });
        // Cible actuelle, même sans identifiant (grille v3) : d'après l'addon, qui compte aussi les lancers à la main
        if (sinceCastOnTarget(reference) < seconds) {targets.add(state.targetId() != 0 ? state.targetId() : -1);}
        return targets.size();
    }

    public boolean proc(String reference) {

        return key(reference).map(KeyState::proc).orElse(false);
    }

    /**
     * L'aura du sort est active sur le joueur : {@code !spell.buffActive('Cri de guerre')}. Lue hors combat seulement
     * (auras inaccessibles en combat) : en combat, c'est l'état lu juste avant d'y entrer.
     */
    public boolean buffActive(String reference) {

        return key(reference).map(KeyState::buffActive).orElse(false);
    }

    /**
     * Le personnage est sous cette forme : {@code spell.form('Forme de félin')}. Contrairement aux autres méthodes, le
     * sort de la forme n'a pas besoin d'être sur une barre.
     */
    public boolean form(String reference) {

        return state.form() != 0 && database.idsFor(reference).contains(state.form());
    }
}
