package fr.ksuto.clockwork.brain.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Génère rotation.schema.json, le schéma des fichiers de rotation, à partir du modèle versionné (ressource
 * {@value #TEMPLATE}) complété par les tables du jeu. Un seul fichier, sans référence externe : certains éditeurs ne
 * suivent pas un {@code $ref} vers un autre fichier.
 * <ul>
 *   <li>{@code #/definitions/specs} et {@code #/definitions/spells} : toutes les spécialisations et tous les sorts de
 *   joueur, pour l'autocomplétion (certains éditeurs n'appliquent les conditions qu'à la validation) ;</li>
 *   <li>{@code #/definitions/byClass} : conditions qui restreignent, selon {@code class}, les spécialisations proposées
 *   pour {@code spec}, et selon {@code class} et {@code spec}, les sorts proposés pour {@code cast} (sorts de toute la
 *   classe et sorts réservés à la spécialisation), ou ceux de toute la classe sans {@code spec}.</li>
 * </ul>
 */
public final class SpellSchema {

    public static final String FILE_NAME = "rotation.schema.json";

    /**
     * Modèle : structure, documentation et références internes ; ses {@code definitions} vides sont remplacées.
     */
    static final String TEMPLATE = "/rotation.schema.json";

    private static final String PLACEHOLDER = "\"definitions\": {}";

    private SpellSchema() {}

    public static void write(Path file, SpellDatabase database) throws IOException {

        Path partial = file.resolveSibling(file.getFileName() + ".part");
        Files.writeString(partial, json(database), StandardCharsets.UTF_8);
        Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    static String json(SpellDatabase database) throws IOException {

        String template;
        try (InputStream stream = SpellSchema.class.getResourceAsStream(TEMPLATE)) {
            if (stream == null) {throw new IOException("Modèle de schéma introuvable : " + TEMPLATE);}
            template = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (!template.contains(PLACEHOLDER)) {throw new IOException("Modèle de schéma sans " + PLACEHOLDER);}
        return template.replace(PLACEHOLDER, definitions(database));
    }

    private static String definitions(SpellDatabase database) {

        List<String> conditions  = new ArrayList<>();
        List<String> definitions = new ArrayList<>();

        // Listes complètes pour l'autocomplétion : certains éditeurs n'appliquent les conditions qu'à la validation
        TreeSet<String> allSpecs  = new TreeSet<>();
        TreeSet<String> allSpells = new TreeSet<>();
        for (String playerClass : database.classes()) {
            database.specsOf(playerClass).forEach(spec -> allSpecs.add(spec.name()));
            allSpells.addAll(database.classSpellNames(playerClass));
        }
        definitions.add("    \"specs\": { \"enum\": " + array(allSpecs) + " }");
        definitions.add(definition("spells", allSpells));

        for (String playerClass : new TreeSet<>(database.classes())) {
            List<SpellDatabase.Spec> specs = database.specsOf(playerClass);

            conditions.add(condition("{ \"required\": [\"class\"], \"properties\": { \"class\": { \"const\": " + quote(playerClass) + " } } }",
                                     "{ \"properties\": { \"spec\": { \"enum\": " + array(specs.stream().map(SpellDatabase.Spec::name).toList()) + " } } }"));
            conditions.add(condition("{ \"required\": [\"class\"], \"properties\": { \"class\": { \"const\": " + quote(playerClass)
                                     + " } }, \"not\": { \"required\": [\"spec\"] } }", cast(playerClass)));
            definitions.add(definition(playerClass, database.classSpellNames(playerClass)));

            for (SpellDatabase.Spec spec : specs) {
                String key = playerClass + "-" + spec.id();
                conditions.add(condition("{ \"required\": [\"class\", \"spec\"], \"properties\": { \"class\": { \"const\": " + quote(playerClass)
                                         + " }, \"spec\": { \"const\": " + quote(spec.name()) + " } } }", cast(key)));
                definitions.add(definition(key, database.specSpellNames(spec.id())));
            }
        }

        return "\"definitions\": {\n"
               + "    \"byClass\": {\n"
               + "      \"allOf\": [\n" + String.join(",\n", conditions) + "\n      ]\n"
               + "    },\n"
               + String.join(",\n", definitions) + "\n"
               + "  }";
    }

    private static String condition(String ifSchema, String thenSchema) {

        return "        { \"if\": " + ifSchema + ",\n          \"then\": " + thenSchema + " }";
    }

    private static String cast(String definition) {

        return "{ \"properties\": { \"rules\": { \"items\": { \"properties\": { \"cast\": { \"$ref\": \"#/definitions/" + definition + "\" } } } } } }";
    }

    private static String definition(String key, Collection<String> names) {

        return "    " + quote(key) + ": {\n"
               + "      \"anyOf\": [\n"
               + "        { \"type\": \"integer\", \"description\": \"Identifiant du sort\" },\n"
               + "        { \"enum\": " + array(names) + " }\n"
               + "      ]\n"
               + "    }";
    }

    private static String array(Collection<String> values) {

        List<String> quoted = values.stream().map(SpellSchema::quote).toList();
        return quoted.isEmpty() ? "[]" : "[\n          " + String.join(",\n          ", quoted) + "\n        ]";
    }

    static String quote(String text) {

        StringBuilder quoted = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> quoted.append("\\\"");
                case '\\' -> quoted.append("\\\\");
                case '\n' -> quoted.append("\\n");
                case '\r' -> quoted.append("\\r");
                case '\t' -> quoted.append("\\t");
                default -> {
                    if (c < 0x20) {quoted.append("\\u%04x".formatted((int) c));}
                    else {quoted.append(c);}
                }
            }
        }
        return quoted.append('"').toString();
    }
}
