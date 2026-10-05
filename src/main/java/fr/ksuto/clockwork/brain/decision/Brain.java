package fr.ksuto.clockwork.brain.decision;

import fr.ksuto.clockwork.brain.data.SpellDatabase;
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
import java.util.OptionalInt;
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
     * Fenêtre de fin d'incantation pendant laquelle WoW accepte déjà le sort suivant (file d'attente des sorts).
     */
    static final double QUEUE_WINDOW = 0.4;

    /**
     * Dernière décision : le sort en cours garde la priorité de la règle qui l'a lancé.
     */
    private Decision lastDecision;

    /**
     * @param key      touche à appuyer
     * @param spellId  sort lancé
     * @param priority priorité de la règle retenue
     * @param reason   règle ou recommandation à l'origine du choix
     * @param stopFirst interrompre d'abord sa propre incantation (NONE si inutile)
     */
    public record Decision(String key, int spellId, int priority, String reason, Rotation.StopCasting stopFirst) {

        Decision(String key, int spellId, int priority, String reason) {

            this(key, spellId, priority, reason, Rotation.StopCasting.NONE);
        }
    }

    public Rotation parse(String yaml) {

        return Rotation.parse(yaml, jexl);
    }

    public Optional<Decision> decide(GameState state, Rotation rotation, SpellDatabase database) {

        // Grille pas encore remplie (addon tout juste activé), cible hors combat sans mode aggro, monture ou joueur mort :
        // rien à faire
        if (!state.keysReady() || !state.mayAct() || state.busy()) {return Optional.empty();}

        // Sort en cours : seule une règle plus prioritaire que lui peut agir
        OptionalInt floor = priorityFloor(state, database);
        if (floor.isEmpty()) {return Optional.empty();}
        int minimum = floor.getAsInt();
        // Couper sa propre incantation (hors fin d'incantation, où le sort suivant part en file d'attente)
        Rotation.StopCasting stop = state.casting() && !state.cast().channeling() && state.cast().remaining() > QUEUE_WINDOW
                                    ? rotation.stopCasting() : Rotation.StopCasting.NONE;

        MapContext context = context(state, database);
        SpellView  spells  = new SpellView(state, database);

        for (Rotation.Rule rule : rotation.rules()) {

            if (rule.priority() <= minimum) {break;} // règles triées par priorité décroissante

            if (rotation.followAssisted() && rotation.assistedPriority() > rule.priority() && rotation.assistedPriority() > minimum) {
                Optional<Decision> assisted = assisted(state, rotation, database);
                if (assisted.isPresent()) {return remember(assisted, stop);}
            }

            Optional<KeyState> key = spells.key(rule.cast());
            if (key.isEmpty()) {
                reportOnce("absent:" + rule.cast(), "Règle ignorée : « " + rule.cast() + " » n'est sur aucune touche de la grille");
                continue;
            }
            if (!key.get().ready() || !holds(rule, context)) {continue;}

            return remember(Optional.of(new Decision(key.get().key(), key.get().spellId(), rule.priority(), "règle « " + rule.cast() + " »")), stop);
        }

        if (!rotation.followAssisted() || rotation.assistedPriority() <= minimum) {return Optional.empty();}
        return remember(assisted(state, rotation, database), stop);
    }

    /**
     * Priorité qu'une règle doit dépasser pour agir maintenant :
     * <ul>
     *   <li>rien en cours : aucune limite ;</li>
     *   <li>dernières {@value #QUEUE_WINDOW} s d'une incantation : aucune limite, le sort suivant est mis en file
     *   d'attente ;</li>
     *   <li>incantation (Éclair) ou canalisation (Drain de vie) : la priorité de la règle qui l'a lancée ; une règle plus
     *   prioritaire l'interrompt (la canalisation est coupée par le jeu, l'incantation selon {@link Rotation#stopCasting()}),
     *   sa propre règle ne la relance pas ;</li>
     *   <li>sort inconnu ou lancé à la main : on ne le coupe pas.</li>
     * </ul>
     *
     * @return la priorité à dépasser, ou vide s'il faut attendre
     */
    private OptionalInt priorityFloor(GameState state, SpellDatabase database) {

        if (!state.casting()) {return OptionalInt.of(Integer.MIN_VALUE);}
        GameState.Cast cast = state.cast();
        if (cast.spellId() == 0) {return OptionalInt.empty();}
        if (!cast.channeling() && cast.remaining() <= QUEUE_WINDOW) {return OptionalInt.of(Integer.MIN_VALUE);}
        if (lastDecision != null && database.related(cast.spellId()).contains(lastDecision.spellId())) {return OptionalInt.of(lastDecision.priority());}
        return OptionalInt.empty();
    }

    private Optional<Decision> remember(Optional<Decision> decision, Rotation.StopCasting stop) {

        Optional<Decision> result = decision.map(d -> new Decision(d.key(), d.spellId(), d.priority(), d.reason(), stop));
        result.ifPresent(d -> lastDecision = d);
        return result;
    }

    private Optional<Decision> assisted(GameState state, Rotation rotation, SpellDatabase database) {

        // Comme la rotation assistée de l'addon : seulement contre une cible ennemie vivante, non marquée par un autre
        // joueur (Blizzard recommande
        // aussi des sorts offensifs sans cible, et un sort sans cible n'est pas « hors de portée »)
        if (state.recommendedSpell() == 0 || !state.attackableTarget()) {
            return Optional.empty();
        }
        Set<Integer> recommended = database.related(state.recommendedSpell());
        return state.keys().values().stream()
                    .filter(key -> recommended.contains(key.spellId()))
                    .filter(KeyState::ready)
                    .findFirst()
                    .map(key -> new Decision(key.key(), key.spellId(), rotation.assistedPriority(),
                                             "recommandation de Blizzard (" + database.nameOf(key.spellId()) + ")"));
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

    private static MapContext context(GameState state, SpellDatabase database) {

        MapContext context = new MapContext();
        context.set("player", Map.ofEntries(Map.entry("health", state.playerHealth()), Map.entry("power", state.playerPower()),
                                            Map.entry("combat", state.inCombat()), Map.entry("casting", state.casting()),
                                            Map.entry("aggro", state.aggro()), Map.entry("moving", state.moving()),
                                            Map.entry("castSpell", state.cast().spellId() == 0 ? "" : database.nameOf(state.cast().spellId())),
                                            Map.entry("channeling", state.cast().channeling()), Map.entry("castRemaining", state.cast().remaining()),
                                            Map.entry("form", state.form() == 0 ? "" : database.nameOf(state.form())),
                                            Map.entry("combo", state.comboPoints())));
        context.set("target", Map.of("exists", state.hasTarget(), "hostile", state.attackableTarget(), "combat", state.targetInCombat(),
                                     "health", state.targetHealth(), "power", state.targetPower()));
        context.set("enemies", state.enemies());
        context.set("assisted", state.recommendedSpell() == 0 ? "" : database.nameOf(state.recommendedSpell()));
        context.set("spell", new SpellView(state, database));
        return context;
    }

    private void reportOnce(String id, String message) {

        if (reportedProblems.add(id)) {logger.warn(message);}
    }
}
