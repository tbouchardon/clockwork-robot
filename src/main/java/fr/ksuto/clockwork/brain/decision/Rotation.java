package fr.ksuto.clockwork.brain.decision;

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
 * spec: Chaman Élémentaire
 * assisted:
 *   follow: true        # suivre la recommandation de Blizzard quand aucune règle plus prioritaire ne s'applique
 *   priority: 50
 * rules:
 *   - cast: Horion de flammes
 *     when: "target.hostile &amp;&amp; spell.sinceCastOnTarget('Horion de flammes') &gt; 15"
 *     priority: 120
 * </pre>
 *
 * @param spec             description libre de la spécialisation visée
 * @param followAssisted   suivre la recommandation de Blizzard en repli
 * @param assistedPriority priorité de la recommandation de Blizzard
 * @param rules            règles, de la plus prioritaire à la moins prioritaire
 */
public record Rotation(String spec, boolean followAssisted, int assistedPriority, List<Rule> rules) {

    /**
     * @param cast     nom du sort (ou identifiant numérique)
     * @param when     condition JEXL, nulle si toujours vraie
     * @param priority priorité (la plus haute l'emporte)
     * @param script   condition compilée, nulle si toujours vraie
     */
    public record Rule(String cast, String when, int priority, JexlScript script) {}

    @SuppressWarnings("unchecked")
    static Rotation parse(String yaml, JexlEngine jexl) {

        Object root = new Yaml().load(yaml);
        if (!(root instanceof Map<?, ?> map)) {throw new IllegalArgumentException("La rotation doit être un objet YAML (spec, assisted, rules)");}

        String spec = map.get("spec") == null ? "" : String.valueOf(map.get("spec"));

        boolean follow   = false;
        int     priority = 50;
        if (map.get("assisted") instanceof Map<?, ?> assisted) {
            follow = Boolean.TRUE.equals(assisted.get("follow"));
            if (assisted.get("priority") instanceof Number number) {priority = number.intValue();}
        }

        List<Rule> rules = new ArrayList<>();
        if (map.get("rules") instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> rule) || rule.get("cast") == null) {
                    throw new IllegalArgumentException("Règle sans « cast » : " + item);
                }
                String     cast   = String.valueOf(rule.get("cast"));
                String     when   = rule.get("when") == null ? null : String.valueOf(rule.get("when"));
                int        ruleP  = rule.get("priority") instanceof Number number ? number.intValue() : 1;
                JexlScript script = when == null ? null : jexl.createScript(when);
                rules.add(new Rule(cast, when, ruleP, script));
            }
        }
        // Tri stable : à priorité égale, l'ordre du fichier est conservé
        rules.sort(Comparator.comparingInt(Rule::priority).reversed());

        return new Rotation(spec, follow, priority, List.copyOf(rules));
    }
}
