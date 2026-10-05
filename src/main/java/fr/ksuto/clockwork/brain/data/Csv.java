package fr.ksuto.clockwork.brain.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Lecture des exports CSV de wago.tools : première ligne d'en-tête, champs éventuellement entre guillemets (guillemets
 * doublés à l'intérieur, retours à la ligne permis dans les descriptions).
 */
public final class Csv {

    private Csv() {}

    /**
     * Appelle {@code row} pour chaque ligne, sous forme de colonne → valeur.
     */
    public static void read(Path file, Consumer<Map<String, String>> row) throws IOException {

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            read(reader, row);
        }
    }

    static void read(Reader source, Consumer<Map<String, String>> row) throws IOException {

        Reader       reader = source.markSupported() ? source : new BufferedReader(source);
        List<String> header = nextRecord(reader);
        if (header == null) {return;}
        if (!header.isEmpty() && header.getFirst().startsWith("\uFEFF")) {header.set(0, header.getFirst().substring(1));}

        List<String> values;
        while ((values = nextRecord(reader)) != null) {
            Map<String, String> map = HashMap.newHashMap(header.size());
            for (int i = 0; i < header.size(); i++) {map.put(header.get(i), i < values.size() ? values.get(i) : "");}
            row.accept(map);
        }
    }

    /**
     * @return les champs de l'enregistrement suivant, ou null en fin de fichier
     */
    private static List<String> nextRecord(Reader reader) throws IOException {

        List<String>  fields   = new ArrayList<>();
        StringBuilder field    = new StringBuilder();
        boolean       quoted   = false;
        boolean       anything = false;
        int           c;

        while ((c = reader.read()) != -1) {
            anything = true;
            if (quoted) {
                if (c == '"') {
                    reader.mark(1);
                    int next = reader.read();
                    if (next == '"') {field.append('"');}
                    else {
                        quoted = false;
                        if (next != -1) {reader.reset();}
                    }
                }
                else {field.append((char) c);}
            }
            else if (c == '"') {quoted = true;}
            else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
            }
            else if (c == '\n') {
                fields.add(field.toString());
                return fields;
            }
            else if (c != '\r') {field.append((char) c);}
        }

        if (!anything) {return null;}
        fields.add(field.toString());
        return fields;
    }
}
