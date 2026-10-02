package fr.ksuto.clockwork.brain.perception;

import fr.ksuto.prh.capture.Frame;
import fr.ksuto.prh.capture.Rgb;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lit le QR code v2 de l'addon (qrcode_v2.lua) : la disposition des cases doit rester identique des deux côtés.
 */
public final class QrCodeV2Reader {

    public static final int VERSION = 2;

    /**
     * Ordre des touches, identique à Clockwork.KEY_ORDER côté addon.
     */
    public static final List<String> KEY_ORDER = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", ")", "=", "Q", "D", "R", "T", "F", "G");

    private static final double HORIZON = 60;

    private QrCodeV2Reader() {}

    /**
     * @return l'état du jeu, ou vide si la grille n'est pas en version 2 (addon plus ancien)
     */
    public static Optional<GameState> read(Frame qr) {

        if (qr.red(8, 13) != VERSION) {return Optional.empty();}

        Map<String, KeyState> keys = new LinkedHashMap<>();
        for (int position = 1; position <= KEY_ORDER.size(); position++) {
            String key = KEY_ORDER.get(position - 1);
            keys.put(key, readKey(qr, key, position));
        }

        int reaction = qr.rgb(11, 3);

        return Optional.of(new GameState(
                percent(qr.red(12, 2)),
                percent(qr.blue(13, 2)),
                reaction != Rgb.ARGB_BLACK,
                reaction == Rgb.ARGB_RED,
                percent(qr.red(12, 3)),
                percent(qr.blue(13, 3)),
                qr.green(10, 2) > 127,
                qr.rgb(2, 2) == Rgb.ARGB_WHITE,
                qr.rgb(3, 2) == Rgb.ARGB_WHITE,
                qr.red(2, 3),
                (qr.red(7, 2) * 256 + qr.green(7, 2)) / 65535.0 * 2 * Math.PI,
                read24(qr, 6, 2),
                qr.red(10, 2) > 127,
                keys));
    }

    private static KeyState readKey(Frame qr, String key, int position) {

        int[] state   = stateCell(position);
        int[] history = historyCell(position);
        int[] spell   = spellCell(position);

        int rangeValue = qr.blue(state[0], state[1]);
        KeyState.Range range = rangeValue > 191 ? KeyState.Range.IN : rangeValue < 64 ? KeyState.Range.OUT : KeyState.Range.NONE;

        return new KeyState(key,
                            read24(qr, spell[0], spell[1]),
                            seconds(qr.red(state[0], state[1]), false),
                            qr.green(state[0], state[1]) > 127,
                            range,
                            seconds(qr.red(history[0], history[1]), true),
                            seconds(qr.blue(history[0], history[1]), true),
                            qr.green(history[0], history[1]) > 127);
    }

    /**
     * Case d'état : sous la touche (ligne 6) pour 1..=, ligne 12 x 2..7 pour Q..G.
     */
    static int[] stateCell(int position) {

        return position <= 12 ? new int[]{position + 1, 6} : new int[]{position - 11, 12};
    }

    /**
     * Case d'historique : ligne 9 pour 1..=, ligne 12 x 8..13 pour Q..G.
     */
    static int[] historyCell(int position) {

        return position <= 12 ? new int[]{position + 1, 9} : new int[]{position - 5, 12};
    }

    /**
     * Case de l'identifiant du sort, identique à spellIdCell côté addon.
     */
    static int[] spellCell(int position) {

        if (position <= 8) {return new int[]{position + 2, 3};}
        if (position <= 12) {return new int[]{position - 7, 7};}
        if (position <= 16) {return new int[]{position - 11, 10};}
        return new int[]{position - 9, 2};
    }

    private static int read24(Frame qr, int x, int y) {

        return qr.red(x, y) << 16 | qr.green(x, y) << 8 | qr.blue(x, y);
    }

    private static double percent(int channel) {

        return channel * 100.0 / 255;
    }

    /**
     * @param saturatedIsInfinite vrai si 255 (plafond) signifie « jamais ou plus de 60 s »
     */
    private static double seconds(int channel, boolean saturatedIsInfinite) {

        if (saturatedIsInfinite && channel == 255) {return Double.POSITIVE_INFINITY;}
        return channel * HORIZON / 255;
    }
}
