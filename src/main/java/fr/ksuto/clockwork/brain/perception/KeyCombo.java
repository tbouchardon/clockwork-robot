package fr.ksuto.clockwork.brain.perception;

/**
 * Combinaison de touches telle que la nomme le QR code v3 : "3", "SHIFT-3", "CTRL-Q", "ALT-="...
 *
 * @param key   touche sans modificateur
 * @param shift avec Maj
 * @param ctrl  avec Ctrl
 * @param alt   avec Alt
 */
public record KeyCombo(String key, boolean shift, boolean ctrl, boolean alt) {

    public static KeyCombo parse(String combo) {

        if (combo.startsWith("SHIFT-")) {return new KeyCombo(combo.substring(6), true, false, false);}
        if (combo.startsWith("CTRL-")) {return new KeyCombo(combo.substring(5), false, true, false);}
        if (combo.startsWith("ALT-")) {return new KeyCombo(combo.substring(4), false, false, true);}
        return new KeyCombo(combo, false, false, false);
    }
}
