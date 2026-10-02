package fr.ksuto.clockwork.brain.decision;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lit le format des SavedVariables de WoW : affectations {@code NOM = valeur} dont les valeurs sont des tables,
 * chaînes, nombres, booléens ou nil. Les clés de table sont {@code ["texte"]}, {@code [nombre]} ou implicites (liste).
 */
final class LuaTableParser {

    private final String text;
    private       int    position;

    private LuaTableParser(String text) {

        this.text = text;
    }

    /**
     * @return les variables du fichier, par nom ; les tables deviennent des Map (clés String, Long ou index Long)
     */
    static Map<String, Object> parse(String text) {

        LuaTableParser parser    = new LuaTableParser(text);
        Map<String, Object> variables = new LinkedHashMap<>();
        parser.skipSpaces();
        while (parser.position < text.length()) {
            String name = parser.identifier();
            parser.expect('=');
            variables.put(name, parser.value());
            parser.skipSpaces();
        }
        return variables;
    }

    private Object value() {

        skipSpaces();
        char c = text.charAt(position);
        if (c == '{') {return table();}
        if (c == '"') {return string();}
        if (c == '-' || Character.isDigit(c)) {return number();}
        String word = identifier();
        return switch (word) {
            case "true" -> Boolean.TRUE;
            case "false" -> Boolean.FALSE;
            case "nil" -> null;
            default -> throw error("valeur inattendue '" + word + "'");
        };
    }

    private Map<Object, Object> table() {

        expect('{');
        Map<Object, Object> table = new HashMap<>();
        long index = 1;
        skipSpaces();
        while (text.charAt(position) != '}') {
            Object key;
            if (text.charAt(position) == '[') {
                position++;
                skipSpaces();
                key = text.charAt(position) == '"' ? string() : number();
                expect(']');
                expect('=');
                table.put(key, value());
            }
            else {
                table.put(index++, value());
            }
            skipSpaces();
            if (text.charAt(position) == ',' || text.charAt(position) == ';') {position++;}
            skipSpaces();
        }
        position++;
        return table;
    }

    private String string() {

        expect('"');
        StringBuilder sb = new StringBuilder();
        while (text.charAt(position) != '"') {
            char c = text.charAt(position++);
            if (c == '\\') {
                char escaped = text.charAt(position++);
                sb.append(switch (escaped) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    default -> escaped;
                });
            }
            else {
                sb.append(c);
            }
        }
        position++;
        return sb.toString();
    }

    private Object number() {

        int start = position;
        if (text.charAt(position) == '-') {position++;}
        while (position < text.length() && (Character.isDigit(text.charAt(position)) || ".eE+-".indexOf(text.charAt(position)) >= 0)) {
            position++;
        }
        String number = text.substring(start, position);
        return number.matches("-?\\d+") ? (Object) Long.parseLong(number) : (Object) Double.parseDouble(number);
    }

    private String identifier() {

        skipSpaces();
        int start = position;
        while (position < text.length() && (Character.isLetterOrDigit(text.charAt(position)) || text.charAt(position) == '_')) {
            position++;
        }
        if (start == position) {throw error("identifiant attendu");}
        return text.substring(start, position);
    }

    private void expect(char expected) {

        skipSpaces();
        if (position >= text.length() || text.charAt(position) != expected) {throw error("'" + expected + "' attendu");}
        position++;
    }

    private void skipSpaces() {

        while (position < text.length()) {
            char c = text.charAt(position);
            if (Character.isWhitespace(c)) {
                position++;
            }
            else if (text.startsWith("--", position)) {
                while (position < text.length() && text.charAt(position) != '\n') {position++;}
            }
            else {
                return;
            }
        }
    }

    private IllegalArgumentException error(String message) {

        return new IllegalArgumentException(message + " (position " + position + ")");
    }
}
