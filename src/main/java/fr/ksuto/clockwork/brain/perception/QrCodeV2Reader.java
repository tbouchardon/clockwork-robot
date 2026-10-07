package fr.ksuto.clockwork.brain.perception;

import fr.ksuto.prh.capture.Frame;
import fr.ksuto.prh.capture.Rgb;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Lit le QR code de l'addon (qrcode_v2.lua) : la disposition des cases doit rester identique des deux côtés.
 * <ul>
 *   <li>v2 : un bloc de 16x16, touches sans modificateur ;</li>
 *   <li>v3 : carré de 32x32, quatre blocs reprenant les mêmes cases de touches : sans modificateur (0, 0),
 *   Maj (16, 0), Ctrl (0, 16), Alt (16, 16), plus un compteur de mises à jour ;</li>
 *   <li>v4 : v3 + groupe ou raid dans les cases libres du bloc 2, et mode soigneur (group.lua).</li>
 * </ul>
 */
public final class QrCodeV2Reader {

    public static final int VERSION = 4;

    /**
     * Membres du groupe ou du raid décrits par la grille v4.
     */
    public static final int MEMBERS = 40;

    /**
     * Blocs de touches : préfixe de la combinaison et décalage du bloc, identiques à Clockwork.QR_BLOCKS côté addon.
     */
    record Block(String prefix, int x, int y) {}

    static final List<Block> BLOCKS = List.of(new Block("", 0, 0), new Block("SHIFT-", 16, 0),
                                              new Block("CTRL-", 0, 16), new Block("ALT-", 16, 16));

    /**
     * Ordre des touches, identique à Clockwork.KEY_ORDER côté addon.
     */
    public static final List<String> KEY_ORDER = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", ")", "=", "Q", "D", "R", "T", "F", "G");

    private static final double HORIZON = 60;

    /**
     * Réaction de la cible : jaune = neutre (sanglier, monture sauvage...), attaquable comme une cible rouge, ainsi que
     * le faisait l'addon (unitExistCanAndShouldDie).
     */
    private static final int ARGB_YELLOW = 0xFFFFFF00;

    /**
     * Secondes encodées pour le temps restant d'une incantation, identique à CAST_HORIZON côté addon.
     */
    private static final double CAST_HORIZON = 10;

    /**
     * Secondes encodées pour l'enchantement temporaire de l'arme, identique à WEAPON_ENCHANT_HORIZON côté addon.
     */
    private static final double WEAPON_ENCHANT_HORIZON = 30 * 60;

    /**
     * Bit 23 de la case du sort : la touche porte un objet, identique à ITEM_FLAG côté addon.
     */
    private static final int ITEM_FLAG = 0x800000;

    private QrCodeV2Reader() {}

    /**
     * @param qr capture de 32x32 (v3) ou au moins 16x16 (v2) dont le coin haut gauche est celui du QR code
     * @return l'état du jeu, ou vide si la grille n'est pas en v2, v3 ou v4 (addon plus ancien)
     */
    public static Optional<GameState> read(Frame qr) {

        int version = qr.red(8, 13);
        if (version < 2 || version > VERSION) {return Optional.empty();}
        boolean     full   = qr.width() >= 32 && qr.height() >= 32;
        List<Block> blocks = version == 2 || !full ? BLOCKS.subList(0, 1) : BLOCKS;

        Map<String, KeyState> keys = new LinkedHashMap<>();
        for (Block block : blocks) {
            for (int position = 1; position <= KEY_ORDER.size(); position++) {
                String key = block.prefix() + KEY_ORDER.get(position - 1);
                keys.put(key, readKey(qr, block, key, position));
            }
        }

        int reaction = qr.rgb(11, 3);

        return Optional.of(new GameState(
                percent(qr.red(12, 2)),
                percent(qr.blue(13, 2)),
                reaction != Rgb.ARGB_BLACK,
                reaction == Rgb.ARGB_RED || reaction == ARGB_YELLOW,
                percent(qr.red(12, 3)),
                percent(qr.blue(13, 3)),
                qr.green(10, 2) > 127,
                qr.rgb(2, 2) == Rgb.ARGB_WHITE,
                qr.rgb(3, 2) == Rgb.ARGB_WHITE,
                qr.red(2, 3),
                (qr.red(7, 2) * 256 + qr.green(7, 2)) / 65535.0 * 2 * Math.PI,
                read24(qr, 6, 2),
                qr.red(10, 2) > 127,
                qr.red(13, 4) > 127,
                qr.blue(13, 4) > 127,
                qr.green(13, 4) > 127,
                qr.red(11, 13) > 127,
                read24(qr, 8, 4),
                qr.red(9, 4),
                qr.red(10, 4),
                qr.red(11, 4) << 8 | qr.green(11, 4),
                version >= 3 ? read24(qr, 11, 2) : 0,
                qr.rgb(3, 2) == Rgb.ARGB_WHITE ? new GameState.Cast(read24(qr, 9, 13), qr.green(10, 13) > 127, qr.red(10, 13) * CAST_HORIZON / 255)
                                               : GameState.Cast.NONE,
                qr.green(11, 13) > 127 ? new GameState.TargetCast(true, read24(qr, 12, 13), qr.blue(11, 13) > 127) : GameState.TargetCast.NONE,
                keys,
                version >= 4 && full ? readGroup(qr) : Group.NONE,
                version >= 4 && qr.red(4, 1) > 127 ? qr.green(4, 1) * WEAPON_ENCHANT_HORIZON / 255 : 0,
                version >= 4 ? read24(qr, 6, 1) : 0,
                qr.blue(10, 2) > 127));
    }

    /**
     * Ramassage (v4, case (8, 1)) : mode ramassage actif et un ennemi ciblé récemment est un cadavre avec du butin pour
     * le joueur (même s'il n'est plus ciblé).
     */
    public static boolean targetLootable(Frame qr) {

        return lootMode(qr) && qr.green(8, 1) > 127;
    }

    /**
     * Mode ramassage du menu de l'addon (v4, case (8, 1)).
     */
    public static boolean lootMode(Frame qr) {

        return qr.red(8, 13) >= 4 && qr.red(8, 1) > 127;
    }

    /**
     * Touche d'interaction de WoW active (option « Activer la touche d'interaction », v4, case (8, 1) bleu) : sans elle,
     * Interagir avec la cible ne fait rien sans cible.
     */
    public static boolean interactKeyEnabled(Frame qr) {

        return qr.red(8, 13) >= 4 && qr.blue(8, 1) > 127;
    }

    /**
     * La cible est morte : réaction grise en (11, 3).
     */
    public static boolean targetDead(Frame qr) {

        int r = qr.red(11, 3), g = qr.green(11, 3), b = qr.blue(11, 3);
        return r > 100 && r < 160 && Math.abs(r - g) < 10 && Math.abs(r - b) < 10;
    }

    /**
     * Un sort vient d'être refusé parce que la cible n'est pas devant le personnage (v4, case (7, 1)) : elle est dans
     * son dos.
     */
    public static boolean notFacingTarget(Frame qr) {

        return qr.red(8, 13) >= 4 && qr.red(7, 1) > 127;
    }

    /**
     * Cases libres du bloc 3 (0, 16), dans l'ordre de la grille (ligne par ligne) : celles du parcours actif, identiques à
     * routeCellPositions dans routes.lua. Le bloc 4 reste en réserve.
     */
    static List<int[]> routeCells() {

        Set<String> keyCells = new HashSet<>();
        for (int position = 1; position <= KEY_ORDER.size(); position++) {
            for (int[] cell : new int[][]{stateCell(position), historyCell(position), spellCell(position)}) {keyCells.add(cell[0] + "," + cell[1]);}
        }
        List<int[]> cells = new ArrayList<>();
        for (int[] block : new int[][]{{0, 16}}) {
            for (int y = 1; y <= 14; y++) {
                for (int x = 1; x <= 14; x++) {
                    if (!keyCells.contains(x + "," + y)) {cells.add(new int[]{block[0] + x, block[1] + y});}
                }
            }
        }
        return List.copyOf(cells);
    }

    private static final List<int[]> ROUTE_CELLS = routeCells();

    /**
     * Parcours actif (v4) : révision, carte, nombre de points et boucle, position du joueur, puis les points.
     *
     * @return {@link Route#NONE} si la grille n'est pas en v4 ou s'il n'y a pas de parcours actif
     */
    public static Route route(Frame qr) {

        if (qr.red(8, 13) < 4 || qr.width() < 32 || qr.height() < 32) {return Route.NONE;}
        int revision = read24(qr, ROUTE_CELLS.get(0));
        int map      = read24(qr, ROUTE_CELLS.get(1));
        if (map == 0) {return new Route(revision, 0, false, null, List.of());}
        int[] header = ROUTE_CELLS.get(2);
        int   count  = Math.min(qr.red(header[0], header[1]), (ROUTE_CELLS.size() - 5) / 2);
        Point2D.Double player = qr.blue(header[0], header[1]) > 127 ? fraction(qr, 3) : null;
        List<Point2D.Double> points = new ArrayList<>();
        for (int index = 0; index < count; index++) {points.add(fraction(qr, 5 + index * 2));}
        return new Route(revision, map, qr.green(header[0], header[1]) > 127, player, List.copyOf(points));
    }

    private static Point2D.Double fraction(Frame qr, int cell) {

        return new Point2D.Double(read24(qr, ROUTE_CELLS.get(cell)) / 16777215.0, read24(qr, ROUTE_CELLS.get(cell + 1)) / 16777215.0);
    }

    private static int read24(Frame qr, int[] cell) {

        return read24(qr, cell[0], cell[1]);
    }

    /**
     * Résultat du dernier lancer de pêche (v4) : case (5, 1), R = compteur, G = résultat.
     *
     * @return vide si la grille n'est pas en v4
     */
    public static Optional<FishingResult> fishingResult(Frame qr) {

        if (qr.red(8, 13) < 4) {return Optional.empty();}
        int code = qr.green(5, 1);
        FishingResult.Outcome[] outcomes = FishingResult.Outcome.values();
        return Optional.of(new FishingResult(qr.red(5, 1), code < outcomes.length ? outcomes[code] : FishingResult.Outcome.NONE));
    }

    /**
     * Groupe (v4) : mode soigneur en (3, 1), un membre par case du bloc 2, identique à group.lua.
     */
    private static Group readGroup(Frame qr) {

        List<Group.Member> members = new ArrayList<>();
        for (int slot = 1; slot <= MEMBERS; slot++) {
            int[] cell  = memberCell(slot);
            int   flags = qr.blue(cell[0], cell[1]);
            if ((flags & 1) == 0) {continue;}
            int range = qr.green(cell[0], cell[1]);
            members.add(new Group.Member(slot, percent(qr.red(cell[0], cell[1])), range > 63, (flags & 2) != 0, (flags & 4) != 0,
                                         (flags & 8) != 0, Group.Role.values()[flags >> 4 & 3]));
        }
        return new Group(qr.red(3, 1) > 127, qr.green(3, 1) > 127, List.copyOf(members));
    }

    /**
     * Case d'un membre (bloc 2) : 1..14 en ligne 1, 15..28 en ligne 4, 29..40 en ligne 5.
     */
    static int[] memberCell(int slot) {

        if (slot <= 14) {return new int[]{16 + slot, 1};}
        if (slot <= 28) {return new int[]{16 + slot - 14, 4};}
        return new int[]{16 + slot - 28, 5};
    }

    private static KeyState readKey(Frame qr, Block block, String key, int position) {

        int[] state   = offset(stateCell(position), block);
        int[] history = offset(historyCell(position), block);
        int[] spell   = offset(spellCell(position), block);

        int rangeValue = qr.blue(state[0], state[1]);
        // Historique, vert : proc + 2 x buff actif, sur 3
        int flags      = (int) Math.round(qr.green(history[0], history[1]) / 85.0);
        KeyState.Range range = rangeValue > 191 ? KeyState.Range.IN : rangeValue < 64 ? KeyState.Range.OUT : KeyState.Range.NONE;

        int     content  = read24(qr, spell[0], spell[1]);
        boolean item     = (content & ITEM_FLAG) != 0;
        double  cooldown = seconds(qr.red(state[0], state[1]), false);
        // 1 = utilisable ; 0,5 = sort à incantation pendant un déplacement, que WoW refuserait
        boolean usable   = qr.green(state[0], state[1]) > 191;
        double  since    = seconds(qr.blue(history[0], history[1]), true);
        if (item) {
            // Objet : l'historique porte le nombre possédé (rouge) à la place du temps sur la cible
            return new KeyState(key, 0, cooldown, usable, range, Double.POSITIVE_INFINITY, since, false, (flags & 2) != 0,
                                content & ~ITEM_FLAG, qr.red(history[0], history[1]));
        }
        return new KeyState(key, content, cooldown, usable, range, seconds(qr.red(history[0], history[1]), true), since, (flags & 1) != 0,
                            (flags & 2) != 0);
    }

    private static int[] offset(int[] cell, Block block) {

        return new int[]{cell[0] + block.x(), cell[1] + block.y()};
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
