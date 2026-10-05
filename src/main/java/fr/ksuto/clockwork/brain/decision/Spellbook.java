package fr.ksuto.clockwork.brain.decision;

import fr.ksuto.clockwork.brain.data.SpellDatabase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Dictionnaire des sorts exporté par l'addon (SavedVariable CLOCKWORK_SPELLBOOK) : relie les noms utilisés dans les
 * règles aux identifiants que l'addon transmet pour chaque touche. Un nom désigne aussi la forme de base et la
 * variante du sort (ex. Horion de flammes 188389 et sa variante 470411).
 * <p>
 * Un nom absent de l'export (sort pas encore sur une barre au dernier /reload) est cherché dans la table complète des
 * sorts du jeu ({@link SpellDatabase}), si elle est chargée.
 */
public final class Spellbook {

    private final Map<Integer, String>      names = new HashMap<>();
    private final Map<String, Set<Integer>> ids   = new HashMap<>();
    private final Map<String, SortedSet<String>> classNames = new HashMap<>();
    private       SpellDatabase             database = SpellDatabase.EMPTY;

    /**
     * Charge le fichier SavedVariables de l'addon (WTF/Account/COMPTE/SavedVariables/ClockWork.lua).
     */
    public static Spellbook load(Path savedVariables) throws IOException {

        return parse(Files.readString(savedVariables, StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    static Spellbook parse(String savedVariables) {

        Spellbook spellbook = new Spellbook();
        Object    root      = LuaTableParser.parse(savedVariables).get("CLOCKWORK_SPELLBOOK");
        if (!(root instanceof Map<?, ?> specs)) {return spellbook;}

        for (Map.Entry<?, ?> specEntry : specs.entrySet()) {
            if (!(specEntry.getValue() instanceof Map<?, ?> specTable) || !(specTable.get("spells") instanceof Map<?, ?> spells)) {continue;}
            // Clé « CLASSE-spécialisation », ex. SHAMAN-262
            String playerClass = String.valueOf(specEntry.getKey()).split("-")[0];
            for (Map.Entry<?, ?> entry : spells.entrySet()) {
                if (!(entry.getKey() instanceof Long id) || !(entry.getValue() instanceof Map<?, ?> spell)) {continue;}
                Map<Object, Object> fields = (Map<Object, Object>) spell;
                spellbook.add(id.intValue(), (String) fields.get("name"), asInt(fields.get("base")), asInt(fields.get("override")));
                if (fields.get("name") instanceof String name) {spellbook.classNames.computeIfAbsent(playerClass, c -> new TreeSet<>()).add(name);}
            }
        }
        return spellbook;
    }

    private static int asInt(Object value) {

        return value instanceof Number number ? number.intValue() : 0;
    }

    /**
     * Nom sans accents ni casse, pour que « horion de flammes » désigne « Horion de flammes ».
     */
    static String normalize(String name) {

        return SpellDatabase.normalize(name);
    }

    /**
     * Table complète des sorts du jeu, consultée pour les noms absents de l'export.
     */
    public void useDatabase(SpellDatabase database) {

        this.database = database;
    }

    void add(int id, String name, int base, int override) {

        if (name == null) {return;}
        names.put(id, name);
        Set<Integer> related = ids.computeIfAbsent(normalize(name), n -> new HashSet<>());
        related.add(id);
        if (base != 0) {related.add(base);}
        if (override != 0) {related.add(override);}
    }

    /**
     * Identifiants désignés par une référence de règle : un nom de sort, ou directement un identifiant numérique.
     */
    public Set<Integer> idsFor(String reference) {

        if (reference.matches("\\d+")) {return Set.of(Integer.parseInt(reference));}
        Set<Integer> exported = ids.get(normalize(reference));
        return exported != null ? exported : database.idsFor(reference);
    }

    /**
     * Le sort et ceux qui lui sont liés (forme de base, variante) : la recommandation de Blizzard désigne la forme de
     * base (ex. 73899) alors que le bouton contient la variante active (51505, Explosion de lave).
     */
    public Set<Integer> related(int id) {

        Set<Integer> related = new HashSet<>(Set.of(id));
        for (Set<Integer> group : ids.values()) {
            if (group.contains(id)) {related.addAll(group);}
        }
        return related;
    }

    public String nameOf(int id) {

        String name = names.get(id);
        if (name == null) {name = database.nameOf(id);}
        return name != null ? name : String.valueOf(id);
    }

    /**
     * Noms des sorts exportés par l'addon, par classe (SHAMAN, MAGE...).
     */
    public Map<String, SortedSet<String>> namesByClass() {

        return classNames;
    }

    public int size() {

        return names.size();
    }
}
