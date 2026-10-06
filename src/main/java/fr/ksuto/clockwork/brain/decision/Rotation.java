package fr.ksuto.clockwork.brain.decision;

import fr.ksuto.clockwork.brain.data.SpellDatabase;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlScript;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Rotation décrite en YAML : une liste de règles « lancer tel sort quand telle condition est vraie », avec une priorité,
 * et éventuellement la recommandation de Blizzard en repli.
 * <pre>
 * name: Chaman Élémentaire
 * class: SHAMAN
 * spec: Élémentaire     # absente : toutes les spécialisations de la classe
 * assisted:
 *   follow: true        # suivre la recommandation de Blizzard quand aucune règle plus prioritaire ne s'applique
 *   priority: 50
 * rules:
 *   - cast: Horion de flammes
 *     when: "target.hostile &amp;&amp; spell.sinceCastOnTarget('Horion de flammes') &gt; 15"
 *     priority: 120
 *   - cast: Afflux de soins
 *     on: lowest            # le membre le plus blessé pour qui la condition est vraie
 *     when: "member.health &lt; 50"
 *     priority: 150
 * </pre>
 *
 * @param name             nom libre, affiché dans le journal
 * @param playerClass      classe visée (SHAMAN, MAGE...), vide si non précisée
 * @param spec             spécialisation visée, nom affiché en jeu (Élémentaire) ou identifiant (262), vide pour toutes
 * @param followAssisted   suivre la recommandation de Blizzard en repli
 * @param assistedPriority priorité de la recommandation de Blizzard
 * @param rules            règles, de la plus prioritaire à la moins prioritaire
 * @param stopCasting      façon d'interrompre sa propre incantation quand une règle plus prioritaire s'applique
 * @param returnToTarget   après un sort lancé sur un membre du groupe, revenir à la cible précédente
 * @param activity         activité hors combat que règle ce fichier ({@code fishing} : préparation de chaque lancer de
 *                         pêche), vide pour une rotation de combat
 */
public record Rotation(String name, String playerClass, String spec, boolean followAssisted, int assistedPriority, List<Rule> rules,
                       StopCasting stopCasting, boolean returnToTarget, String activity) {

    /**
     * Activité de la pêche : leurre, appât... avant chaque lancer.
     */
    public static final String FISHING = "fishing";

    /**
     * Interrompre sa propre incantation : WoW refuse un autre sort pendant une incantation (pas pendant une
     * canalisation, qu'il coupe de lui-même). Se déplacer l'interrompt.
     */
    public enum StopCasting {
        /**
         * Sauter (par défaut) : le personnage reste en place ; pendant le saut, seuls les sorts instantanés partent.
         */
        JUMP,
        /**
         * Petit pas en arrière : immédiat, mais le personnage bouge un peu.
         */
        BACK,
        /**
         * Rien : appuyer directement (sorts utilisables pendant une incantation, macros /stopcasting sur la barre).
         */
        NONE
    }

    /**
     * Sur qui lancer le sort.
     */
    public enum On {
        /**
         * La cible actuelle (par défaut).
         */
        TARGET,
        /**
         * Le joueur lui-même.
         */
        SELF,
        /**
         * Le membre le plus blessé pour qui la condition est vraie.
         */
        LOWEST,
        /**
         * Le tank le plus blessé pour qui la condition est vraie.
         */
        TANK,
        /**
         * Le soigneur le plus blessé pour qui la condition est vraie.
         */
        HEALER;

        /**
         * Le sort vise un autre membre : seulement en mode soigneur, sauf règle {@code always}.
         */
        public boolean others() {

            return this == LOWEST || this == TANK || this == HEALER;
        }
    }

    /**
     * @param cast     nom du sort ou de l'objet (ou identifiant numérique)
     * @param when     condition JEXL, nulle si toujours vraie
     * @param priority priorité (la plus haute l'emporte)
     * @param script   condition compilée, nulle si toujours vraie
     * @param on       sur qui lancer le sort
     * @param always   un soin sur les autres qui s'applique même hors mode soigneur (urgence)
     * @param item     la règle utilise un objet ({@code use}) plutôt qu'un sort ({@code cast})
     */
    public record Rule(String cast, String when, int priority, JexlScript script, On on, boolean always, boolean item) {

        /**
         * Nom affiché dans le journal.
         */
        public String label() {

            return item ? "objet « " + cast + " »" : "« " + cast + " »";
        }
    }

    @SuppressWarnings("unchecked")
    static Rotation parse(String yaml, JexlEngine jexl) {

        Object root = new Yaml().load(yaml);
        if (!(root instanceof Map<?, ?> map)) {throw new IllegalArgumentException("La rotation doit être un objet YAML (name, class, spec, assisted, rules)");}

        String name        = map.get("name") == null ? "" : String.valueOf(map.get("name"));
        String playerClass = map.get("class") == null ? "" : String.valueOf(map.get("class"));
        String spec        = map.get("spec") == null ? "" : String.valueOf(map.get("spec"));

        boolean follow   = false;
        int     priority = 50;
        if (map.get("assisted") instanceof Map<?, ?> assisted) {
            follow = Boolean.TRUE.equals(assisted.get("follow"));
            if (assisted.get("priority") instanceof Number number) {priority = number.intValue();}
        }

        List<Rule> rules = new ArrayList<>();
        if (map.get("rules") instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> rule) || (rule.get("cast") == null) == (rule.get("use") == null)) {
                    throw new IllegalArgumentException("Règle sans « cast » (sort) ni « use » (objet), ou avec les deux : " + item);
                }
                boolean    useItem = rule.get("use") != null;
                String     cast   = String.valueOf(useItem ? rule.get("use") : rule.get("cast"));
                String     when   = rule.get("when") == null ? null : String.valueOf(rule.get("when"));
                int        ruleP  = rule.get("priority") instanceof Number number ? number.intValue() : 1;
                JexlScript script = when == null ? null : jexl.createScript(when);
                // YAML 1.1 (SnakeYAML) lit la clé « on » comme le booléen true, comme le fait GitHub Actions
                Object     onKey  = rule.containsKey("on") ? rule.get("on") : rule.get(Boolean.TRUE);
                On         on     = onKey == null ? On.TARGET : switch (String.valueOf(onKey).toLowerCase()) {
                    case "target" -> On.TARGET;
                    case "self" -> On.SELF;
                    case "lowest" -> On.LOWEST;
                    case "tank" -> On.TANK;
                    case "healer" -> On.HEALER;
                    default -> throw new IllegalArgumentException("on inconnu pour « " + cast + " » : " + onKey
                                                                  + " (target, self, lowest, tank ou healer)");
                };
                rules.add(new Rule(cast, when, ruleP, script, on, Boolean.TRUE.equals(rule.get("always")), useItem));
            }
        }
        // Tri stable : à priorité égale, l'ordre du fichier est conservé
        rules.sort(Comparator.comparingInt(Rule::priority).reversed());

        StopCasting stopCasting = map.get("stopCasting") == null ? StopCasting.JUMP : switch (String.valueOf(map.get("stopCasting")).toLowerCase()) {
            case "jump" -> StopCasting.JUMP;
            case "back" -> StopCasting.BACK;
            case "none" -> StopCasting.NONE;
            default -> throw new IllegalArgumentException("stopCasting inconnu : " + map.get("stopCasting") + " (jump, back ou none)");
        };

        boolean returnToTarget = !Boolean.FALSE.equals(map.get("returnToTarget"));

        String activity = map.get("activity") == null ? "" : String.valueOf(map.get("activity"));
        if (!activity.isEmpty() && !activity.equals(FISHING)) {throw new IllegalArgumentException("activity inconnue : " + activity + " (fishing)");}

        return new Rotation(name, playerClass, spec, follow, priority, List.copyOf(rules), stopCasting, returnToTarget, activity);
    }

    /**
     * @param playerClass classe du personnage (SHAMAN...)
     * @param spec        spécialisation du personnage, ou null si inconnue
     * @return la rotation convient : même classe, et même spécialisation ou rotation de toute la classe
     */
    public boolean appliesTo(String playerClass, SpellDatabase.Spec spec) {

        if (!activity.isEmpty() || !this.playerClass.equals(playerClass)) {return false;}
        return forWholeClass() || (spec != null && (this.spec.equals(String.valueOf(spec.id()))
                                                    || SpellDatabase.normalize(this.spec).equals(SpellDatabase.normalize(spec.name()))));
    }

    /**
     * La rotation vaut pour toutes les spécialisations de sa classe.
     */
    public boolean forWholeClass() {

        return spec.isBlank();
    }

    /**
     * Nom affiché dans le journal : le nom libre, sinon classe et spécialisation.
     */
    public String label() {

        if (!name.isBlank()) {return name;}
        if (!activity.isEmpty()) {return activity;}
        return (playerClass + " " + spec).trim();
    }
}
