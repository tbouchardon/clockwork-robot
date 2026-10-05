package fr.ksuto.clockwork.brain.data;

import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Tous les sorts du jeu (table SpellName), et ceux de chaque classe : capacités de la classe et de ses spécialisations
 * (SkillLineAbility, SpecializationSpells) et talents (arbres de talents de la classe, sorts et variantes qu'ils
 * accordent). Relie aussi chaque sort à ses variantes (un talent remplace souvent un sort par une variante d'un autre
 * identifiant, et Blizzard recommande la forme de base) : c'est le dictionnaire du cerveau pour traduire les noms des
 * règles en identifiants.
 */
public final class SpellDatabase {

    public static final SpellDatabase EMPTY = new SpellDatabase();

    /**
     * Tables utilisées, à télécharger depuis wago.tools.
     */
    public static final String[] TABLES = {"SpellName", "ChrClasses", "ChrSpecialization", "SkillLine", "SkillLineAbility", "SpecializationSpells",
                                    "SkillLineXTraitTree", "TraitNode", "TraitNodeXTraitNodeEntry", "TraitNodeEntry", "TraitDefinition"};

    private static final String CLASS_SKILL_CATEGORY = "7";

    private final Map<Integer, String>       names       = new HashMap<>();
    private final Map<String, Set<Integer>>  ids         = new HashMap<>();
    private final Map<String, Set<Integer>>  classSpells = new HashMap<>();
    private final Map<Integer, Set<Integer>> variants    = new HashMap<>();

    private SpellDatabase() {}

    /**
     * Nom sans accents ni casse, apostrophe typographique comprise, pour que « horion de flamme » désigne « Horion de
     * flamme » et « chaine d'eclairs » « Chaîne d’éclairs ».
     */
    public static String normalize(String name) {

        return Normalizer.normalize(name.trim().replace('\u2019', '\''), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                         .toLowerCase(Locale.ROOT);
    }

    /**
     * Seule SpellName est indispensable : une autre table absente de cette version du jeu (ex. talents en vanilla) est
     * traitée comme vide.
     *
     * @throws WagoTables.MissingTableException si la version est inconnue de wago.tools (SpellName introuvable)
     */
    public static SpellDatabase load(WagoTables tables) throws IOException, InterruptedException {

        Map<String, Path> files = new HashMap<>();
        for (String table : TABLES) {
            try {
                files.put(table, tables.table(table));
            }
            catch (WagoTables.MissingTableException e) {
                if (table.equals("SpellName")) {throw e;}
                files.put(table, tables.markAbsent(table));
            }
        }
        return load(files);
    }

    /**
     * @param files fichier CSV de chaque table de {@link #TABLES}
     */
    public static SpellDatabase load(Map<String, Path> files) throws IOException {

        SpellDatabase database = new SpellDatabase();

        Csv.read(files.get("SpellName"), row -> {
            String name = row.get("Name_lang");
            if (name == null || name.isBlank()) {return;}
            int id = Integer.parseInt(row.get("ID"));
            database.names.put(id, name);
            database.ids.computeIfAbsent(normalize(name), n -> new HashSet<>()).add(id);
        });

        // Classes : identifiant -> nom de fichier (SHAMAN), et ligne de compétence de chaque classe (par son nom affiché)
        Map<String, String> classById   = new HashMap<>();
        Map<String, String> classByName = new HashMap<>();
        Csv.read(files.get("ChrClasses"), row -> {
            classById.put(row.get("ID"), row.get("Filename"));
            classByName.put(row.get("Name_lang"), row.get("Filename"));
        });

        Map<String, String> classBySkillLine = new HashMap<>();
        Csv.read(files.get("SkillLine"), row -> {
            String playerClass = classByName.get(row.get("DisplayName_lang"));
            if (CLASS_SKILL_CATEGORY.equals(row.get("CategoryID")) && playerClass != null) {classBySkillLine.put(row.get("ID"), playerClass);}
        });

        Csv.read(files.get("SkillLineAbility"), row -> database.addClassSpell(classBySkillLine.get(row.get("SkillLine")), row.get("Spell")));

        Map<String, String> classBySpec = new HashMap<>();
        Csv.read(files.get("ChrSpecialization"), row -> {
            String playerClass = classById.get(row.get("ClassID"));
            if (playerClass != null) {classBySpec.put(row.get("ID"), playerClass);}
        });
        Csv.read(files.get("SpecializationSpells"), row -> {
            String playerClass = classBySpec.get(row.get("SpecID"));
            database.addClassSpell(playerClass, row.get("SpellID"));
            database.addClassSpell(playerClass, row.get("OverridesSpellID"));
            database.link(row.get("SpellID"), row.get("OverridesSpellID"));
        });

        // Talents : arbre de la classe -> nœuds -> entrées -> définitions (sort accordé, sort remplacé, sort affiché)
        Map<String, String> classByTree = new HashMap<>();
        Csv.read(files.get("SkillLineXTraitTree"), row -> {
            String playerClass = classBySkillLine.get(row.get("SkillLineID"));
            if (playerClass != null) {classByTree.put(row.get("TraitTreeID"), playerClass);}
        });
        Map<String, String> treeByNode = new HashMap<>();
        Csv.read(files.get("TraitNode"), row -> treeByNode.put(row.get("ID"), row.get("TraitTreeID")));
        Map<String, String> classByEntry = new HashMap<>();
        Csv.read(files.get("TraitNodeXTraitNodeEntry"), row -> {
            String playerClass = classByTree.get(treeByNode.get(row.get("TraitNodeID")));
            if (playerClass != null) {classByEntry.put(row.get("TraitNodeEntryID"), playerClass);}
        });
        Map<String, String> classByDefinition = new HashMap<>();
        Csv.read(files.get("TraitNodeEntry"), row -> {
            String playerClass = classByEntry.get(row.get("ID"));
            if (playerClass != null) {classByDefinition.put(row.get("TraitDefinitionID"), playerClass);}
        });
        Csv.read(files.get("TraitDefinition"), row -> {
            String playerClass = classByDefinition.get(row.get("ID"));
            database.addClassSpell(playerClass, row.get("SpellID"));
            database.addClassSpell(playerClass, row.get("OverridesSpellID"));
            database.addClassSpell(playerClass, row.get("VisibleSpellID"));
            database.link(row.get("SpellID"), row.get("OverridesSpellID"));
        });

        return database;
    }

    private void addClassSpell(String playerClass, String spellId) {

        if (playerClass == null || spellId == null || spellId.isEmpty() || spellId.equals("0")) {return;}
        classSpells.computeIfAbsent(playerClass, c -> new HashSet<>()).add(Integer.parseInt(spellId));
    }

    /**
     * Un sort et la variante qui le remplace (talent, spécialisation), dans les deux sens.
     */
    private void link(String spellId, String overridden) {

        if (spellId == null || overridden == null || spellId.isEmpty() || overridden.isEmpty() || spellId.equals("0") || overridden.equals("0")) {return;}
        int variant = Integer.parseInt(spellId);
        int base    = Integer.parseInt(overridden);
        variants.computeIfAbsent(variant, v -> new HashSet<>()).add(base);
        variants.computeIfAbsent(base, b -> new HashSet<>()).add(variant);
    }

    /**
     * Identifiants désignés par une référence de règle : un nom de sort (accents et casse indifférents, souvent plusieurs
     * identifiants : versions de joueur, de monstre, d'objet...), ou directement un identifiant numérique.
     */
    public Set<Integer> idsFor(String reference) {

        if (reference.matches("\\d+")) {return Set.of(Integer.parseInt(reference));}
        return ids.getOrDefault(normalize(reference), Set.of());
    }

    /**
     * Le sort, ses variantes et ses homonymes : la recommandation de Blizzard désigne la forme de base (ex. 73899) alors
     * que le bouton contient la variante active (51505, Explosion de lave).
     */
    public Set<Integer> related(int id) {

        Set<Integer> related = new HashSet<>(Set.of(id));
        related.addAll(variants.getOrDefault(id, Set.of()));
        String name = names.get(id);
        if (name != null) {related.addAll(idsFor(name));}
        return related;
    }

    /**
     * @return le nom du sort, ou son identifiant s'il est inconnu
     */
    public String nameOf(int id) {

        return names.getOrDefault(id, String.valueOf(id));
    }

    /**
     * Noms des sorts d'une classe (SHAMAN, MAGE...), triés.
     */
    public SortedSet<String> classSpellNames(String playerClass) {

        SortedSet<String> result = new TreeSet<>();
        for (int id : classSpells.getOrDefault(playerClass, Set.of())) {
            String name = names.get(id);
            if (name != null) {result.add(name);}
        }
        return result;
    }

    public Set<String> classes() {

        return Collections.unmodifiableSet(classSpells.keySet());
    }

    public int size() {

        return names.size();
    }
}
