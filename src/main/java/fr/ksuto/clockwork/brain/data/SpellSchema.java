package fr.ksuto.clockwork.brain.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Génère rotation.spells.json, la partie du schéma de rotation.yaml tirée des tables du jeu ; rotation.schema.json
 * (versionné) y renvoie par {@code #/definitions/byClass} :
 * <ul>
 *   <li>selon {@code class} : les spécialisations proposées pour {@code spec} ;</li>
 *   <li>selon {@code class} et {@code spec} : les sorts proposés pour {@code cast}, ceux de la spécialisation (sorts de
 *   toute la classe et sorts réservés à la spécialisation), ou de toute la classe sans {@code spec}.</li>
 * </ul>
 */
public final class SpellSchema {

    public static final String FILE_NAME = "rotation.spells.json";

    private SpellSchema() {}

    public static void write(Path file, SpellDatabase database) throws IOException {

        Path partial = file.resolveSibling(file.getFileName() + ".part");
        Files.writeString(partial, json(database), StandardCharsets.UTF_8);
        Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    static String json(SpellDatabase database) {

        List<String> conditions  = new ArrayList<>();
        List<String> definitions = new ArrayList<>();

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

        return "{\n"
               + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n"
               + "  \"$comment\": \"Généré par ClockWork d'après les tables du jeu (wago.tools) : ne pas modifier.\",\n"
               + "  \"definitions\": {\n"
               + "    \"byClass\": {\n"
               + "      \"allOf\": [\n" + String.join(",\n", conditions) + "\n      ]\n"
               + "    },\n"
               + String.join(",\n", definitions) + "\n"
               + "  }\n"
               + "}\n";
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
