package fr.ksuto.clockwork.brain.data;

import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Tous les sorts du jeu (table SpellName), les classes et leurs spécialisations, et les sorts de chacune : capacités de
 * la classe (SkillLineAbility), de la spécialisation (SpecializationSpells) et talents (arbre de talents de la classe ;
 * un nœud réservé à certaines spécialisations par une condition l'est aussi pour ses sorts). Relie aussi chaque sort à
 * ses variantes (un talent remplace souvent un sort par une variante d'un autre identifiant, et Blizzard recommande la
 * forme de base) : c'est le dictionnaire du cerveau pour traduire les noms des règles en identifiants. Les noms des
 * objets (ItemSparse) servent aux règles {@code use} (potions, pierres de soins, leurres...).
 */
public final class SpellDatabase {

    public static final SpellDatabase EMPTY = new SpellDatabase();

    /**
     * Tables utilisées, à télécharger depuis wago.tools.
     */
    public static final String[] TABLES = {"SpellName", "ChrClasses", "ChrSpecialization", "SkillLine", "SkillLineAbility", "SpecializationSpells",
                                           "SkillLineXTraitTree", "TraitNode", "TraitNodeXTraitNodeEntry", "TraitNodeEntry", "TraitDefinition",
                                           "TraitCond", "SpecSetMember", "TraitNodeXTraitCond", "TraitNodeGroupXTraitCond", "TraitNodeGroupXTraitNode",
                                           "ItemSparse"};

    private static final String CLASS_SKILL_CATEGORY = "7";
    private static final String ACQUIRED_THROUGH_ANOTHER_SPELL = "3";

    /**
     * Spécialisation d'une classe.
     *
     * @param id          identifiant (262 = Élémentaire)
     * @param playerClass classe (SHAMAN)
     * @param name        nom affiché (Élémentaire)
     * @param order       rang dans l'interface du jeu
     */
    public record Spec(int id, String playerClass, String name, int order) {}

    private final Map<Integer, String>       names       = new HashMap<>();
    private final Map<String, Set<Integer>>  ids         = new HashMap<>();
    private final Map<Integer, String>       classes     = new HashMap<>();
    private final Map<Integer, Spec>         specs       = new HashMap<>();
    private final Map<String, Set<Integer>>  classSpells = new HashMap<>();
    private final Map<Integer, Set<Integer>> specSpells  = new HashMap<>();
    private final Map<Integer, Set<Integer>> variants    = new HashMap<>();
    private final Map<Integer, String>       itemNames   = new HashMap<>();
    private final Map<String, Set<Integer>>  itemIds     = new HashMap<>();

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

        // Classes et spécialisations ; ligne de compétence de chaque classe (par son nom affiché)
        Map<String, String> classByName = new HashMap<>();
        Csv.read(files.get("ChrClasses"), row -> {
            database.classes.put(Integer.parseInt(row.get("ID")), row.get("Filename"));
            classByName.put(row.get("Name_lang"), row.get("Filename"));
        });
        Csv.read(files.get("ChrSpecialization"), row -> {
            String playerClass = database.classes.get(parse(row.get("ClassID")));
            if (playerClass == null) {return;}
            int id = Integer.parseInt(row.get("ID"));
            database.specs.put(id, new Spec(id, playerClass, row.get("Name_lang"), parse(row.get("OrderIndex"))));
        });

        Map<String, String> classBySkillLine = new HashMap<>();
        Csv.read(files.get("SkillLine"), row -> {
            String playerClass = classByName.get(row.get("DisplayName_lang"));
            if (CLASS_SKILL_CATEGORY.equals(row.get("CategoryID")) && playerClass != null) {classBySkillLine.put(row.get("ID"), playerClass);}
        });

        // Sans les composantes d'un autre sort (dégâts de Frappe-tempête, attaque de la main gauche...) : pas sur les barres
        Csv.read(files.get("SkillLineAbility"), row -> {
            if (!ACQUIRED_THROUGH_ANOTHER_SPELL.equals(row.get("AcquireMethod"))) {
                database.addClassSpell(classBySkillLine.get(row.get("SkillLine")), row.get("Spell"));
            }
        });

        Csv.read(files.get("SpecializationSpells"), row -> {
            Spec spec = database.specs.get(parse(row.get("SpecID")));
            if (spec != null) {
                database.addSpecSpell(spec, row.get("SpellID"));
                database.addSpecSpell(spec, row.get("OverridesSpellID"));
            }
            database.link(row.get("SpellID"), row.get("OverridesSpellID"));
        });

        loadTalents(database, files, classBySkillLine);

        // Objets (une cinquantaine de Mo) : absents, seuls les identifiants numériques sont utilisables dans les règles use
        if (files.get("ItemSparse") != null) {
            Csv.read(files.get("ItemSparse"), row -> {
                String name = row.get("Display_lang");
                if (name == null || name.isBlank()) {return;}
                int id = Integer.parseInt(row.get("ID"));
                database.itemNames.put(id, name);
                database.itemIds.computeIfAbsent(normalize(name), n -> new HashSet<>()).add(id);
            });
        }
        return database;
    }

    /**
     * Talents : arbre de la classe -> nœuds -> entrées -> définitions (sort accordé, sort remplacé, sort affiché). Un nœud
     * soumis à une condition de spécialisation (directement ou par son groupe) n'est proposé qu'à ces spécialisations.
     */
    private static void loadTalents(SpellDatabase database, Map<String, Path> files, Map<String, String> classBySkillLine) throws IOException {

        Map<String, String> classByTree = new HashMap<>();
        Csv.read(files.get("SkillLineXTraitTree"), row -> {
            String playerClass = classBySkillLine.get(row.get("SkillLineID"));
            if (playerClass != null) {classByTree.put(row.get("TraitTreeID"), playerClass);}
        });

        // Conditions de spécialisation : condition -> ensemble de spécialisations -> spécialisations
        Map<String, Set<Integer>> specsBySet = new HashMap<>();
        Csv.read(files.get("SpecSetMember"), row -> specsBySet.computeIfAbsent(row.get("SpecSet"), s -> new HashSet<>())
                                                              .add(parse(row.get("ChrSpecializationID"))));
        Map<String, Set<Integer>> specsByCond = new HashMap<>();
        Csv.read(files.get("TraitCond"), row -> {
            Set<Integer> members = specsBySet.get(row.get("SpecSetID"));
            if (members != null && !"0".equals(row.get("SpecSetID"))) {specsByCond.put(row.get("ID"), members);}
        });
        Map<String, Set<Integer>> specsByNode = new HashMap<>();
        Csv.read(files.get("TraitNodeXTraitCond"), row -> addSpecs(specsByNode, row.get("TraitNodeID"), specsByCond.get(row.get("TraitCondID"))));
        Map<String, Set<Integer>> specsByGroup = new HashMap<>();
        Csv.read(files.get("TraitNodeGroupXTraitCond"), row -> addSpecs(specsByGroup, row.get("TraitNodeGroupID"), specsByCond.get(row.get("TraitCondID"))));
        Csv.read(files.get("TraitNodeGroupXTraitNode"), row -> addSpecs(specsByNode, row.get("TraitNodeID"), specsByGroup.get(row.get("TraitNodeGroupID"))));

        Map<String, String> treeByNode = new HashMap<>();
        Csv.read(files.get("TraitNode"), row -> treeByNode.put(row.get("ID"), row.get("TraitTreeID")));
        Map<String, String> nodeByEntry = new HashMap<>();
        Csv.read(files.get("TraitNodeXTraitNodeEntry"), row -> {
            if (classByTree.containsKey(treeByNode.get(row.get("TraitNodeID")))) {nodeByEntry.put(row.get("TraitNodeEntryID"), row.get("TraitNodeID"));}
        });
        Map<String, Set<String>> nodesByDefinition = new HashMap<>();
        Csv.read(files.get("TraitNodeEntry"), row -> {
            String node = nodeByEntry.get(row.get("ID"));
            if (node != null) {nodesByDefinition.computeIfAbsent(row.get("TraitDefinitionID"), d -> new HashSet<>()).add(node);}
        });

        Csv.read(files.get("TraitDefinition"), row -> {
            database.link(row.get("SpellID"), row.get("OverridesSpellID"));
            for (String node : nodesByDefinition.getOrDefault(row.get("ID"), Set.of())) {
                String       playerClass = classByTree.get(treeByNode.get(node));
                Set<Integer> nodeSpecs   = specsByNode.getOrDefault(node, Set.of());
                for (String column : List.of("SpellID", "OverridesSpellID", "VisibleSpellID")) {
                    if (nodeSpecs.isEmpty()) {database.addClassSpell(playerClass, row.get(column));}
                    for (int specId : nodeSpecs) {
                        Spec spec = database.specs.get(specId);
                        if (spec != null && spec.playerClass().equals(playerClass)) {database.addSpecSpell(spec, row.get(column));}
                    }
                }
            }
        });
    }

    private static void addSpecs(Map<String, Set<Integer>> target, String key, Set<Integer> specs) {

        if (specs != null) {target.computeIfAbsent(key, k -> new HashSet<>()).addAll(specs);}
    }

    private static int parse(String number) {

        return number == null || number.isEmpty() ? 0 : Integer.parseInt(number);
    }

    private void addClassSpell(String playerClass, String spellId) {

        if (playerClass == null || parse(spellId) == 0) {return;}
        classSpells.computeIfAbsent(playerClass, c -> new HashSet<>()).add(parse(spellId));
    }

    private void addSpecSpell(Spec spec, String spellId) {

        if (parse(spellId) == 0) {return;}
        specSpells.computeIfAbsent(spec.id(), s -> new HashSet<>()).add(parse(spellId));
        classSpells.computeIfAbsent(spec.playerClass(), c -> new HashSet<>());
    }

    /**
     * Un sort et la variante qui le remplace (talent, spécialisation), dans les deux sens.
     */
    private void link(String spellId, String overridden) {

        int variant = parse(spellId);
        int base    = parse(overridden);
        if (variant == 0 || base == 0) {return;}
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
     * Objets désignés par une référence de règle {@code use} : un nom d'objet (accents et casse indifférents ; souvent
     * plusieurs identifiants, ex. une vingtaine de « Pierre de soins »), ou directement un identifiant numérique.
     */
    public Set<Integer> itemIdsFor(String reference) {

        if (reference.matches("\\d+")) {return Set.of(Integer.parseInt(reference));}
        return itemIds.getOrDefault(normalize(reference), Set.of());
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
     * @return la classe (SHAMAN...) de cet identifiant de classe (7...), ou null
     */
    public String classOf(int classId) {

        return classes.get(classId);
    }

    /**
     * @return la spécialisation de cet identifiant (262...), ou null
     */
    public Spec spec(int specId) {

        return specs.get(specId);
    }

    /**
     * Spécialisations d'une classe, dans l'ordre du jeu (la spécialisation « Initial » des bas niveaux en dernier).
     */
    public List<Spec> specsOf(String playerClass) {

        return specs.values().stream().filter(spec -> spec.playerClass().equals(playerClass))
                    .sorted(Comparator.comparingInt(Spec::order).thenComparingInt(Spec::id)).toList();
    }

    /**
     * Noms des sorts d'une classe (SHAMAN, MAGE...), toutes spécialisations confondues, triés.
     */
    public SortedSet<String> classSpellNames(String playerClass) {

        SortedSet<String> result = namesOf(classSpells.getOrDefault(playerClass, Set.of()));
        for (Spec spec : specsOf(playerClass)) {result.addAll(namesOf(specSpells.getOrDefault(spec.id(), Set.of())));}
        return result;
    }

    /**
     * Noms des sorts d'une spécialisation : ceux de toute la classe et ceux qui lui sont réservés, triés.
     */
    public SortedSet<String> specSpellNames(int specId) {

        Spec spec = specs.get(specId);
        if (spec == null) {return new TreeSet<>();}
        SortedSet<String> result = namesOf(classSpells.getOrDefault(spec.playerClass(), Set.of()));
        result.addAll(namesOf(specSpells.getOrDefault(specId, Set.of())));
        return result;
    }

    private SortedSet<String> namesOf(Collection<Integer> spellIds) {

        SortedSet<String> result = new TreeSet<>();
        for (int id : spellIds) {
            String name = names.get(id);
            if (name != null) {result.add(name);}
        }
        return result;
    }

    /**
     * Classes ayant des sorts.
     */
    public Set<String> classes() {

        return Collections.unmodifiableSet(classSpells.keySet());
    }

    public int size() {

        return names.size();
    }
}
