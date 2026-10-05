package fr.ksuto.clockwork.brain.decision;

import fr.ksuto.clockwork.brain.perception.GameState;
import fr.ksuto.clockwork.brain.perception.KeyState;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Choisit la touche à appuyer : la règle applicable de plus haute priorité, ou la recommandation de Blizzard en repli.
 * Une règle s'applique si son sort est sur une barre, prêt (utilisable, sans temps de recharge, à portée) et si sa
 * condition est vraie.
 */
public final class Brain {

    private static final Logger logger = LoggerFactory.getLogger(Brain.class);

    // Les conditions n'accèdent qu'aux classes de ce paquet (SpellView) et aux types de base : pas de réflexion, de fichiers...
    private final JexlEngine  jexl            = new JexlBuilder()
            .permissions(JexlPermissions.RESTRICTED.compose("fr.ksuto.clockwork.brain.decision.*"))
            .strict(true).silent(false).cache(256).create();
    private final Set<String> reportedProblems = new HashSet<>();

    /**
     * @param key      touche à appuyer
     * @param spellId  sort lancé
     * @param priority priorité de la règle retenue
     * @param reason   règle ou recommandation à l'origine du choix
     */
    public record Decision(String key, int spellId, int priority, String reason) {}

    public Rotation parse(String yaml) {

        return Rotation.parse(yaml, jexl);
    }

    public Optional<Decision> decide(GameState state, Rotation rotation, Spellbook spellbook) {

        // Grille pas encore remplie (addon tout juste activé) ou cible hors combat sans mode aggro : rien à faire
        if (!state.keysReady() || !state.mayAct()) {return Optional.empty();}

        MapContext context = context(state, spellbook);
        SpellView  spells  = new SpellView(state, spellbook);

        for (Rotation.Rule rule : rotation.rules()) {

            if (rotation.followAssisted() && rotation.assistedPriority() > rule.priority()) {
                Optional<Decision> assisted = assisted(state, rotation, spellbook);
                if (assisted.isPresent()) {return assisted;}
            }

            Optional<KeyState> key = spells.key(rule.cast());
            if (key.isEmpty()) {
                reportOnce("absent:" + rule.cast(), "Règle ignorée : « " + rule.cast() + " » n'est sur aucune touche de la grille");
                continue;
            }
            if (!key.get().ready() || !holds(rule, context)) {continue;}

            return Optional.of(new Decision(key.get().key(), key.get().spellId(), rule.priority(), "règle « " + rule.cast() + " »"));
        }

        return rotation.followAssisted() ? assisted(state, rotation, spellbook) : Optional.empty();
    }

    private Optional<Decision> assisted(GameState state, Rotation rotation, Spellbook spellbook) {

        // Comme la rotation assistée de l'addon : seulement contre une cible ennemie vivante (Blizzard recommande
        // aussi des sorts offensifs sans cible, et un sort sans cible n'est pas « hors de portée »)
        if (state.recommendedSpell() == 0 || !state.hasTarget() || !state.targetHostile() || state.targetHealth() <= 0) {
            return Optional.empty();
        }
        Set<Integer> recommended = spellbook.related(state.recommendedSpell());
        return state.keys().values().stream()
                    .filter(key -> recommended.contains(key.spellId()))
                    .filter(KeyState::ready)
                    .findFirst()
                    .map(key -> new Decision(key.key(), key.spellId(), rotation.assistedPriority(),
                                             "recommandation de Blizzard (" + spellbook.nameOf(key.spellId()) + ")"));
    }

    private boolean holds(Rotation.Rule rule, MapContext context) {

        if (rule.script() == null) {return true;}
        try {
            return Boolean.TRUE.equals(rule.script().execute(context));
        }
        catch (JexlException e) {
            reportOnce("erreur:" + rule.when(), "Condition invalide pour « " + rule.cast() + " » (" + rule.when() + ") : " + e.getMessage());
            return false;
        }
    }

    private static MapContext context(GameState state, Spellbook spellbook) {

        MapContext context = new MapContext();
        context.set("player", Map.of("health", state.playerHealth(), "power", state.playerPower(),
                                     "combat", state.inCombat(), "casting", state.casting(), "aggro", state.aggro(),
                                     "form", state.form() == 0 ? "" : spellbook.nameOf(state.form()), "combo", state.comboPoints()));
        context.set("target", Map.of("exists", state.hasTarget(), "hostile", state.targetHostile(), "combat", state.targetInCombat(),
                                     "health", state.targetHealth(), "power", state.targetPower()));
        context.set("enemies", state.enemies());
        context.set("assisted", state.recommendedSpell() == 0 ? "" : spellbook.nameOf(state.recommendedSpell()));
        context.set("spell", new SpellView(state, spellbook));
        return context;
    }

    private void reportOnce(String id, String message) {

        if (reportedProblems.add(id)) {logger.warn(message);}
    }
}
