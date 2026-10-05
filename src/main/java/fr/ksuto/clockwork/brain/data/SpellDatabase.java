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
 * accordent). Permet de nommer n'importe quel sort dans une rotation sans attendre l'export de l'addon.
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
        });

        return database;
    }

    private void addClassSpell(String playerClass, String spellId) {

        if (playerClass == null || spellId == null || spellId.isEmpty() || spellId.equals("0")) {return;}
        classSpells.computeIfAbsent(playerClass, c -> new HashSet<>()).add(Integer.parseInt(spellId));
    }

    /**
     * Identifiants des sorts portant ce nom (accents et casse indifférents) : souvent plusieurs, versions de joueur, de
     * monstre, d'objet...
     */
    public Set<Integer> idsFor(String name) {

        return ids.getOrDefault(normalize(name), Set.of());
    }

    public String nameOf(int id) {

        return names.get(id);
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
