package fr.ksuto.clockwork.brain.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * Génère rotation.spells.json : pour chaque classe, la liste des noms de sorts acceptés dans « cast ». Le schéma
 * rotation.schema.json (versionné) y renvoie selon la classe de la rotation, pour l'autocomplétion de l'éditeur.
 */
public final class SpellSchema {

    public static final String FILE_NAME = "rotation.spells.json";

    private SpellSchema() {}

    /**
     * @param spellsByClass noms des sorts de chaque classe (SHAMAN, MAGE...)
     */
    public static void write(Path file, Map<String, SortedSet<String>> spellsByClass) throws IOException {

        Path partial = file.resolveSibling(file.getFileName() + ".part");
        Files.writeString(partial, json(spellsByClass), StandardCharsets.UTF_8);
        Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    static String json(Map<String, SortedSet<String>> spellsByClass) {

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n");
        json.append("  \"$comment\": \"Généré par ClockWork (tables du jeu de wago.tools et sorts exportés par l'addon) : ne pas modifier.\",\n");
        json.append("  \"definitions\": {");

        String classSeparator = "\n";
        for (Map.Entry<String, SortedSet<String>> entry : new TreeMap<>(spellsByClass).entrySet()) {
            json.append(classSeparator).append("    ").append(quote(entry.getKey())).append(": {\n");
            json.append("      \"anyOf\": [\n");
            json.append("        { \"type\": \"integer\", \"description\": \"Identifiant du sort\" },\n");
            json.append("        { \"enum\": [");
            String separator = "\n";
            for (String name : entry.getValue()) {
                json.append(separator).append("          ").append(quote(name));
                separator = ",\n";
            }
            json.append("\n        ] }\n");
            json.append("      ]\n");
            json.append("    }");
            classSeparator = ",\n";
        }
        json.append("\n  }\n}\n");
        return json.toString();
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
