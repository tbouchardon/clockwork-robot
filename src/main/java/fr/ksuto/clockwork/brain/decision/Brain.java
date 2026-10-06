package fr.ksuto.clockwork.brain.decision;

import fr.ksuto.clockwork.brain.data.SpellDatabase;
import fr.ksuto.clockwork.brain.perception.GameState;
import fr.ksuto.clockwork.brain.perception.Group;
import fr.ksuto.clockwork.brain.perception.KeyState;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * Choisit la touche à appuyer : la règle applicable de plus haute priorité, ou la recommandation de Blizzard en repli.
 * Une règle s'applique si son sort est sur une barre, prêt (utilisable, sans temps de recharge, à portée) et si sa
 * condition est vraie.
 * <p>
 * Une règle {@code on} vise un membre du groupe (grille v4) : le plus blessé de ceux qui conviennent (vivants,
 * connectés, à portée, du bon rôle) pour qui la condition est vraie, avec {@code member} dans le contexte. Soigner les
 * autres n'a lieu qu'en mode soigneur, sauf règle {@code always}.
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
     * Touche de la décision « cible suivante » (règle {@code action: next-target}).
     */
    public static final String NEXT_TARGET = "TAB";

    /**
     * Délai minimal entre deux changements de cible : la grille (mise à jour toutes les 0,2 s par l'addon) doit d'abord
     * décrire la nouvelle cible.
     */
    public static final long NEXT_TARGET_DELAY = 500;

    private long lastNextTarget = Long.MIN_VALUE / 2;

    /**
     * Derniers lancements sur chaque ennemi (identifiant de cible → sort → instant en ms), pour {@code spell.dotted}.
     */
    private final Map<Integer, Map<Integer, Long>> targetCasts = new HashMap<>();

    /**
     * Derniers lancements sur chaque membre (emplacement → sort → instant en ms), pour {@code member.sinceCast}.
     */
    private final Map<Integer, Map<Integer, Long>> memberCasts = new HashMap<>();
    private final LongSupplier                     clock;

    public Brain() {

        this(System::currentTimeMillis);
    }

    /**
     * @param clock horloge en ms (tests)
     */
    Brain(LongSupplier clock) {

        this.clock = clock;
    }

    /**
     * @param key      touche à appuyer
     * @param spellId  sort lancé
     * @param priority priorité de la règle retenue
     * @param reason   règle ou recommandation à l'origine du choix
     * @param stopFirst interrompre d'abord sa propre incantation (NONE si inutile)
     * @param member    emplacement du membre à cibler avant le sort (1..40), 0 pour la cible actuelle
     * @param returnToTarget revenir ensuite à la cible précédente
     */
    public record Decision(String key, int spellId, int priority, String reason, Rotation.StopCasting stopFirst, int member,
                           boolean returnToTarget) {

        Decision(String key, int spellId, int priority, String reason) {

            this(key, spellId, priority, reason, Rotation.StopCasting.NONE, 0, false);
        }
    }

    public Rotation parse(String yaml) {

        return Rotation.parse(yaml, jexl);
    }

    public Optional<Decision> decide(GameState state, Rotation rotation, SpellDatabase database) {

        // Grille pas encore remplie (addon tout juste activé), monture ou joueur mort : rien à faire. Une cible hors combat
        // sans mode aggro n'empêche que les sorts sur la cible, pas les soins sur le groupe
        if (!state.keysReady() || state.busy()) {return Optional.empty();}

        // Sort en cours : seule une règle plus prioritaire que lui peut agir
        OptionalInt floor = priorityFloor(state, database);
        if (floor.isEmpty()) {return Optional.empty();}
        int minimum = floor.getAsInt();
        // Couper sa propre incantation (hors fin d'incantation, où le sort suivant part en file d'attente)
        Rotation.StopCasting stop = state.casting() && !state.cast().channeling() && state.cast().remaining() > QUEUE_WINDOW
                                    ? rotation.stopCasting() : Rotation.StopCasting.NONE;

        long                now       = clock.getAsLong();
        SpellView           spells    = new SpellView(state, database, targetCasts, now);
        Map<String, Object> variables = variables(state, database, spells);
        MapContext          context   = new MapContext(variables);
        ItemView            items     = new ItemView(state, database);

        for (Rotation.Rule rule : rotation.rules()) {

            if (rule.priority() <= minimum) {break;} // règles triées par priorité décroissante

            if (rotation.followAssisted() && rotation.assistedPriority() > rule.priority() && rotation.assistedPriority() > minimum && state.mayAct()) {
                Optional<Decision> assisted = assisted(state, rotation, database);
                if (assisted.isPresent()) {return remember(assisted, stop, state);}
            }

            if (rule.kind() == Rotation.Kind.NEXT_TARGET) {
                // Pas de mayAct : quitter une cible hors combat est justement permis
                if (now - lastNextTarget < NEXT_TARGET_DELAY || !holds(rule, context)) {continue;}
                lastNextTarget = now;
                return Optional.of(new Decision(NEXT_TARGET, 0, rule.priority(), "règle " + rule.label()));
            }

            Optional<KeyState> key = rule.item() ? items.key(rule.cast()) : spells.key(rule.cast());
            if (key.isEmpty()) {
                reportOnce("absent:" + rule.label(), "Règle ignorée : " + rule.label() + " n'est sur aucune touche de la grille");
                continue;
            }
            if (rule.on() != Rotation.On.TARGET) {
                Optional<Decision> onMember = onMember(rule, key.get(), state, rotation, variables, database);
                if (onMember.isPresent()) {return remember(onMember, stop, state);}
                continue;
            }
            if (!state.mayAct() || !key.get().ready() || !holds(rule, context)) {continue;}

            return remember(Optional.of(new Decision(key.get().key(), key.get().spellId(), rule.priority(), "règle " + rule.label())), stop, state);
        }

        if (!rotation.followAssisted() || rotation.assistedPriority() <= minimum || !state.mayAct()) {return Optional.empty();}
        return remember(assisted(state, rotation, database), stop, state);
    }

    /**
     * Règle {@code on} : le membre le plus blessé parmi ceux qui conviennent, pour qui la condition est vraie.
     */
    private Optional<Decision> onMember(Rotation.Rule rule, KeyState key, GameState state, Rotation rotation, Map<String, Object> variables,
                                        SpellDatabase database) {

        if (rule.on().others() && !state.group().healerMode() && !rule.always()) {return Optional.empty();}
        if (!key.castable()) {return Optional.empty();}
        if (state.group().members().isEmpty()) {
            reportOnce("groupe:" + rule.cast(), "Règle " + rule.label() + " ignorée : la grille ne décrit pas le groupe (addon en v4 requis)");
            return Optional.empty();
        }

        long now = clock.getAsLong();
        return state.group().members().stream()
                    .filter(Group.Member::healable)
                    .filter(member -> switch (rule.on()) {
                        case SELF -> member.self();
                        case TANK -> member.role() == Group.Role.TANK;
                        case HEALER -> member.role() == Group.Role.HEALER;
                        default -> true;
                    })
                    .sorted(Comparator.comparingDouble(Group.Member::health).thenComparingInt(Group.Member::slot))
                    .filter(member -> {
                        MapContext context = new MapContext(new HashMap<>(variables));
                        context.set("member", new MemberView(member, database, memberCasts.getOrDefault(member.slot(), Map.of()), now));
                        return holds(rule, context);
                    })
                    .findFirst()
                    .map(member -> new Decision(key.key(), key.spellId(), rule.priority(),
                                                "règle " + rule.label() + " sur le membre " + member.slot() + " (" + Math.round(member.health()) + " %)",
                                                Rotation.StopCasting.NONE, member.slot(), rotation.returnToTarget() && state.hasTarget()));
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

    private Optional<Decision> remember(Optional<Decision> decision, Rotation.StopCasting stop, GameState state) {

        Optional<Decision> result = decision.map(d -> new Decision(d.key(), d.spellId(), d.priority(), d.reason(), stop, d.member(), d.returnToTarget()));
        result.ifPresent(d -> {
            lastDecision = d;
            long now = clock.getAsLong();
            if (d.member() > 0) {memberCasts.computeIfAbsent(d.member(), slot -> new HashMap<>()).put(d.spellId(), now);}
            else if (state.targetId() != 0 && d.spellId() != 0) {
                targetCasts.values().forEach(casts -> casts.values().removeIf(time -> now - time > 60_000));
                targetCasts.values().removeIf(Map::isEmpty);
                targetCasts.computeIfAbsent(state.targetId(), id -> new HashMap<>()).put(d.spellId(), now);
            }
        });
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
            reportOnce("erreur:" + rule.when(), "Condition invalide pour " + rule.label() + " (" + rule.when() + ") : " + e.getMessage());
            return false;
        }
    }

    /**
     * Variables des conditions ; {@code member} s'y ajoute pour une règle {@code on}.
     */
    private static Map<String, Object> variables(GameState state, SpellDatabase database, SpellView spells) {

        Map<String, Object> variables = new HashMap<>();
        MapContext          context   = new MapContext(variables); // écrit dans variables
        context.set("player", Map.ofEntries(Map.entry("health", state.playerHealth()), Map.entry("power", state.playerPower()),
                                            Map.entry("combat", state.inCombat()), Map.entry("casting", state.casting()),
                                            Map.entry("aggro", state.aggro()), Map.entry("moving", state.moving()),
                                            Map.entry("castSpell", state.cast().spellId() == 0 ? "" : database.nameOf(state.cast().spellId())),
                                            Map.entry("channeling", state.cast().channeling()), Map.entry("castRemaining", state.cast().remaining()),
                                            Map.entry("form", state.form() == 0 ? "" : database.nameOf(state.form())),
                                            Map.entry("combo", state.comboPoints()), Map.entry("weaponEnchant", state.weaponEnchant())));
        context.set("target", Map.of("exists", state.hasTarget(), "hostile", state.attackableTarget(), "combat", state.targetInCombat(),
                                     "health", state.targetHealth(), "power", state.targetPower(),
                                     "casting", state.targetCast().casting(), "interruptible", state.targetCast().interruptible(),
                                     "castSpell", state.targetCast().spellId() == 0 ? "" : database.nameOf(state.targetCast().spellId())));
        context.set("enemies", state.enemies());
        context.set("assisted", state.recommendedSpell() == 0 ? "" : database.nameOf(state.recommendedSpell()));
        context.set("spell", spells);
        context.set("item", new ItemView(state, database));
        context.set("healer", state.group().healerMode());
        context.set("group", new GroupView(state.group()));
        return variables;
    }

    private void reportOnce(String id, String message) {

        if (reportedProblems.add(id)) {logger.warn(message);}
    }
}
