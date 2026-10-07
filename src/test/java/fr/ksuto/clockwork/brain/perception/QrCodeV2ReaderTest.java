package fr.ksuto.clockwork.brain.perception;

import fr.ksuto.prh.capture.Frame;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QrCodeV2ReaderTest {

    /**
     * Grille 16x16 encodée comme le fait qrcode_v2.lua (couleurs 0..1 arrondies à l'octet par la texture).
     */
    private static final class Grid {

        final BufferedImage image;

        Grid() {

            this(16);
        }

        Grid(int size) {

            image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        }

        Grid set(int x, int y, double r, double g, double b) {

            image.setRGB(x, y, (int) Math.round(r * 255) << 16 | (int) Math.round(g * 255) << 8 | (int) Math.round(b * 255));
            return this;
        }

        Grid set24(int x, int y, int value) {

            image.setRGB(x, y, value);
            return this;
        }

        Frame frame() {

            return Frame.of(image);
        }
    }

    private static Grid v2() {

        return new Grid().set(8, 13, 2 / 255.0, 0, 0);
    }

    @Test
    void ignoresGridsWithoutVersion2() {

        assertTrue(QrCodeV2Reader.read(new Grid().frame()).isEmpty());
    }

    @Test
    void readsPlayerTargetAndCombat() {

        int facing = (int) Math.round(Math.PI / (2 * Math.PI) * 65535);
        GameState state = QrCodeV2Reader.read(v2()
                .set(12, 2, 0.5, 0, 0).set(13, 2, 0, 0, 0.25)          // PV 50 %, ressource 25 %
                .set(11, 3, 1, 0, 0).set(12, 3, 0.2, 0, 0)             // cible hostile à 20 %
                .set(2, 2, 1, 1, 1)                                    // en combat
                .set(10, 2, 1, 1, 0)                                   // mode aggro, cible en combat
                .set(2, 3, 3 / 255.0, 0, 0)                            // 3 ennemis
                .set(7, 2, (facing >> 8) / 255.0, (facing & 0xFF) / 255.0, 0)
                .set24(6, 2, 51505)                                    // Explosion de lave recommandée
                .set24(8, 4, 768)                                      // forme de félin
                .set(9, 4, 4 / 255.0, 0, 0)                            // 4 points de combo
                .set(13, 4, 0, 1, 1)                                   // cible marquée, monture
                .frame()).orElseThrow();

        assertEquals(50, state.playerHealth(), 0.3);
        assertEquals(25, state.playerPower(), 0.3);
        assertTrue(state.hasTarget());
        assertTrue(state.targetHostile());
        assertEquals(20, state.targetHealth(), 0.3);
        assertTrue(state.inCombat());
        assertTrue(state.aggro());
        assertTrue(state.targetInCombat());
        assertFalse(state.casting());
        assertEquals(3, state.enemies());
        assertEquals(Math.PI, state.facing(), 0.001);
        assertEquals(51505, state.recommendedSpell());
        assertEquals(768, state.form());
        assertEquals(4, state.comboPoints());
        assertFalse(state.playerDead());
        assertTrue(state.targetTapDenied());
        assertTrue(state.mounted());
        assertTrue(state.busy());
        assertFalse(state.attackableTarget());
        assertEquals(18, state.keys().size());
    }

    @Test
    void neutralTargetIsAttackableLikeTheAddonDid() {

        GameState state = QrCodeV2Reader.read(v2().set(11, 3, 1, 1, 0).frame()).orElseThrow();

        assertTrue(state.targetHostile(), "cible neutre (jaune)");
        assertFalse(QrCodeV2Reader.read(v2().set(11, 3, 0, 1, 0).frame()).orElseThrow().targetHostile(), "cible amicale (verte)");
    }

    @Test
    void deadTargetIsNotHostile() {

        GameState state = QrCodeV2Reader.read(v2().set(11, 3, 0.5, 0.5, 0.5).frame()).orElseThrow();

        assertTrue(state.hasTarget());
        assertFalse(state.targetHostile());
    }

    @Test
    void readsKeyStateHistoryAndSpell() {

        // Touche "3" : position 3 -> état (4, 6), historique (4, 9), sort (5, 3)
        GameState state = QrCodeV2Reader.read(v2()
                .set(4, 6, 3 / 60.0, 1, 1)                             // 3 s de recharge, utilisable, à portée
                .set(4, 9, 12 / 60.0, 1 / 3.0, 1)                      // 12 s sur cette cible, proc, jamais ailleurs
                .set24(5, 3, 188389)
                .frame()).orElseThrow();

        KeyState key = state.keys().get("3");
        assertEquals(188389, key.spellId());
        assertEquals(3, key.cooldown(), 0.2);
        assertTrue(key.usable());
        assertEquals(KeyState.Range.IN, key.range());
        assertEquals(12, key.sinceCastOnTarget(), 0.2);
        assertTrue(key.proc());
        assertFalse(key.buffActive());
        assertEquals(Double.POSITIVE_INFINITY, key.sinceCast());
        assertFalse(key.ready(), "en recharge");
        assertEquals(key, state.keyForSpell(188389).orElseThrow());
    }

    @Test
    void castTimeSpellWhileMovingIsNotUsable() {

        GameState state = QrCodeV2Reader.read(v2()
                .set(11, 13, 1, 0, 0)                                  // en mouvement
                .set(4, 6, 0, 0.5, 1)                                  // touche 3 : utilisable, mais à incantation
                .set24(5, 3, 188196)
                .frame()).orElseThrow();

        assertTrue(state.moving());
        assertFalse(state.keys().get("3").usable());
        assertFalse(state.keys().get("3").ready());
    }

    @Test
    void readsTheTargetCast() {

        assertEquals(GameState.TargetCast.NONE, QrCodeV2Reader.read(v2().frame()).orElseThrow().targetCast());

        GameState state = QrCodeV2Reader.read(v2().set(11, 13, 0, 1, 1).set24(12, 13, 8004).frame()).orElseThrow();
        assertEquals(new GameState.TargetCast(true, 8004, true), state.targetCast());
    }

    @Test
    void readsBuffAndProcFlags() {

        GameState buffOnly = QrCodeV2Reader.read(v2().set(4, 9, 1, 2 / 3.0, 1).set24(5, 3, 6673).frame()).orElseThrow();
        assertTrue(buffOnly.keys().get("3").buffActive());
        assertFalse(buffOnly.keys().get("3").proc());

        GameState both = QrCodeV2Reader.read(v2().set(4, 9, 1, 1, 1).set24(5, 3, 6673).frame()).orElseThrow();
        assertTrue(both.keys().get("3").buffActive());
        assertTrue(both.keys().get("3").proc());
    }

    @Test
    void readsLetterKeysOnRow12() {

        // Touche "F" : position 17 -> état (6, 12), historique (12, 12), sort (8, 2)
        GameState state = QrCodeV2Reader.read(v2()
                .set(6, 12, 0, 1, 0.5)                                 // prête, sans portée
                .set24(8, 2, 8004)
                .frame()).orElseThrow();

        KeyState key = state.keys().get("F");
        assertEquals(8004, key.spellId());
        assertEquals(KeyState.Range.NONE, key.range());
        assertTrue(key.ready());
    }

    @Test
    void readsModifierBlocksOfVersion3() {

        // v3 : 32x32, SHIFT-3 dans le bloc (16, 0), CTRL-Q dans le bloc (0, 16), compteur en (11, 2)
        GameState state = QrCodeV2Reader.read(new Grid(32)
                .set(8, 13, 3 / 255.0, 0, 0)
                .set24(11, 2, 70000)
                .set24(5, 3, 188196)                                   // touche 3 : Éclair
                .set24(5 + 16, 3, 51505)                               // SHIFT-3 : Explosion de lave
                .set(4 + 16, 6, 0, 1, 1)                               // SHIFT-3 prête, à portée
                .set24(2, 10 + 16, 8004)                               // CTRL-Q (position 13 -> (2, 10)) : Afflux de soins
                .set(2, 12 + 16, 0, 1, 0.5)                            // CTRL-Q prête, sans portée
                .frame()).orElseThrow();

        assertEquals(70000, state.frame());
        assertEquals(72, state.keys().size());
        assertEquals(188196, state.keys().get("3").spellId());
        assertEquals(51505, state.keys().get("SHIFT-3").spellId());
        assertTrue(state.keys().get("SHIFT-3").ready());
        assertEquals(8004, state.keys().get("CTRL-Q").spellId());
        assertTrue(state.keys().get("CTRL-Q").ready());
        assertEquals("SHIFT-3", state.keyForSpell(51505).orElseThrow().key());
        assertEquals(0, state.keys().get("ALT-3").spellId());
    }

    @Test
    void readsTheGroupOfVersion4() {

        // v4 : mode soigneur en (3, 1) ; membre 1 en (17, 1), 15 en (17, 4), 40 en (28, 5)
        GameState state = QrCodeV2Reader.read(new Grid(32)
                .set(8, 13, 4 / 255.0, 0, 0)
                .set(3, 1, 1, 1, 0)                                    // mode soigneur, en raid
                .set(17, 1, 0.8, 1, (1 + 8 + 16 * 2) / 255.0)          // le joueur, soigneur, 80 %
                .set(17, 4, 0.3, 0, (1 + 16) / 255.0)                  // tank à 30 %, hors de portée
                .set(28, 5, 0, 0.5, (1 + 2 + 4 + 16 * 3) / 255.0)      // mort, déconnecté, portée inconnue
                .frame()).orElseThrow();

        Group group = state.group();
        assertTrue(group.healerMode());
        assertTrue(group.raid());
        assertEquals(3, group.members().size(), "les cases vides (sans le drapeau « existe ») sont ignorées");

        Group.Member self = group.members().get(0);
        assertEquals(1, self.slot());
        assertEquals(80, self.health(), 0.3);
        assertTrue(self.self());
        assertEquals(Group.Role.HEALER, self.role());
        assertTrue(self.healable());

        Group.Member tank = group.members().get(1);
        assertEquals(15, tank.slot());
        assertEquals(Group.Role.TANK, tank.role());
        assertFalse(tank.inRange());
        assertFalse(tank.healable());

        Group.Member gone = group.members().get(2);
        assertEquals(40, gone.slot());
        assertTrue(gone.dead());
        assertTrue(gone.offline());
        assertTrue(gone.inRange(), "portée inconnue : le jeu tranchera");
        assertEquals(Group.Role.DAMAGER, gone.role());
    }

    @Test
    void readsItemsOnKeys() {

        // Touche "3" : Pierre de soins (bit 23 + 5512), 2 possédées, utilisée il y a 6 s ; touche "4" : sort
        GameState state = QrCodeV2Reader.read(new Grid(32)
                .set(8, 13, 4 / 255.0, 0, 0)
                .set24(5, 3, 0x800000 + 5512)
                .set(4, 6, 0, 1, 0.5)                                  // prête, sans portée
                .set(4, 9, 2 / 255.0, 2 / 3.0, 6 / 60.0)               // 2 possédées, aura active, 6 s
                .set24(6, 3, 188196)
                .set(4, 1, 1, 0.5, 0)                                  // leurre sur la canne : 15 min
                .frame()).orElseThrow();

        KeyState stone = state.keys().get("3");
        assertEquals(0, stone.spellId());
        assertEquals(5512, stone.itemId());
        assertEquals(2, stone.count());
        assertTrue(stone.buffActive());
        assertEquals(6, stone.sinceCast(), 0.3);
        assertEquals(Double.POSITIVE_INFINITY, stone.sinceCastOnTarget());
        assertTrue(stone.ready());
        assertEquals(188196, state.keys().get("4").spellId());
        assertEquals(0, state.keys().get("4").itemId());
        assertEquals(15 * 60, state.weaponEnchant(), 10);
        assertTrue(state.keysReady());
    }

    private static void set24(Grid grid, int[] cell, int value) {

        grid.set24(cell[0], cell[1], value);
    }

    private static int fraction(double value) {

        return (int) Math.round(value * 16777215);
    }

    @Test
    void readsTheActiveRoute() {

        // Parcours actif dans les cases libres des blocs 3 et 4 : révision, carte, nombre/boucle/sur la carte, joueur, points
        var  cells = QrCodeV2Reader.routeCells();
        Grid grid  = new Grid(32).set(8, 13, 4 / 255.0, 0, 0);
        set24(grid, cells.get(0), 42);
        set24(grid, cells.get(1), 2023);
        grid.set(cells.get(2)[0], cells.get(2)[1], 3 / 255.0, 1, 1);
        set24(grid, cells.get(3), fraction(0.40));
        set24(grid, cells.get(4), fraction(0.60));
        double[][] points = {{0.10, 0.20}, {0.45, 0.62}, {0.80, 0.90}};
        for (int index = 0; index < points.length; index++) {
            set24(grid, cells.get(5 + index * 2), fraction(points[index][0]));
            set24(grid, cells.get(6 + index * 2), fraction(points[index][1]));
        }

        Route route = QrCodeV2Reader.route(grid.frame());

        assertEquals(42, route.revision());
        assertEquals(2023, route.map());
        assertTrue(route.loop());
        assertEquals(0.40, route.player().x, 1e-6);
        assertEquals(3, route.points().size());
        assertEquals(0.45, route.points().get(1).x, 1e-6);
        assertEquals(0.90, route.points().get(2).y, 1e-6);
        assertEquals(1, route.nearestPoint(), "le joueur (0,40 ; 0,60) est près du 2e point");
    }

    @Test
    void routeCellsAreTheFreeCellsOfBlocks3And4() {

        var cells = QrCodeV2Reader.routeCells();
        assertEquals(2 * (196 - 54), cells.size());
        assertArrayEquals(new int[]{1, 17}, cells.getFirst(), "bloc 3, première case libre");
        assertTrue(cells.stream().allMatch(cell -> cell[1] >= 17 && cell[1] <= 30 && (cell[0] <= 14 || cell[0] >= 17)));
        assertEquals(cells.size(), cells.stream().map(cell -> cell[0] + "," + cell[1]).distinct().count());
    }

    @Test
    void noRouteWithoutMap() {

        Grid grid = new Grid(32).set(8, 13, 4 / 255.0, 0, 0);
        assertFalse(QrCodeV2Reader.route(grid.frame()).exists());
        assertFalse(QrCodeV2Reader.route(new Grid(32).set(8, 13, 3 / 255.0, 0, 0).frame()).exists(), "v3");
    }

    @Test
    void readsTheLootSignal() {

        Grid grid = new Grid(32).set(8, 13, 4 / 255.0, 0, 0);
        assertFalse(QrCodeV2Reader.targetLootable(grid.frame()));
        assertFalse(QrCodeV2Reader.targetLootable(grid.set(8, 1, 0, 1, 0).frame()), "butin, mais mode ramassage éteint");
        assertTrue(QrCodeV2Reader.targetLootable(grid.set(8, 1, 1, 1, 0).frame()));
        assertTrue(QrCodeV2Reader.lootMode(grid.frame()));
        assertFalse(QrCodeV2Reader.interactKeyEnabled(grid.frame()));
        assertTrue(QrCodeV2Reader.interactKeyEnabled(grid.set(8, 1, 1, 1, 1).frame()));
        assertTrue(QrCodeV2Reader.targetDead(grid.set(11, 3, 0.5, 0.5, 0.5).frame()));
        assertFalse(QrCodeV2Reader.targetDead(grid.set(11, 3, 1, 0, 0).frame()), "cible hostile vivante");
    }

    @Test
    void readsTheNotFacingSignal() {

        assertFalse(QrCodeV2Reader.notFacingTarget(new Grid(32).set(8, 13, 4 / 255.0, 0, 0).frame()));
        assertTrue(QrCodeV2Reader.notFacingTarget(new Grid(32).set(8, 13, 4 / 255.0, 0, 0).set(7, 1, 1, 0, 0).frame()));
        assertFalse(QrCodeV2Reader.notFacingTarget(new Grid(32).set(8, 13, 3 / 255.0, 0, 0).set(7, 1, 1, 0, 0).frame()), "v3 : case non définie");
    }

    @Test
    void readsTheLastFishingResult() {

        Grid grid = new Grid(32).set(8, 13, 4 / 255.0, 0, 0);
        assertEquals(new FishingResult(0, FishingResult.Outcome.NONE), QrCodeV2Reader.fishingResult(grid.frame()).orElseThrow());

        grid.set(5, 1, 7 / 255.0, 3 / 255.0, 0);                       // 7e lancer : rien à ferrer
        assertEquals(new FishingResult(7, FishingResult.Outcome.NOT_HOOKED), QrCodeV2Reader.fishingResult(grid.frame()).orElseThrow());

        assertTrue(QrCodeV2Reader.fishingResult(new Grid(32).set(8, 13, 3 / 255.0, 0, 0).frame()).isEmpty(), "v3");
    }

    @Test
    void version3GridHasNoGroup() {

        GameState state = QrCodeV2Reader.read(new Grid(32).set(8, 13, 3 / 255.0, 0, 0).set(17, 1, 1, 1, 1).frame()).orElseThrow();

        assertEquals(Group.NONE, state.group());
    }

    @Test
    void readsTheSpellInProgress() {

        GameState idle = QrCodeV2Reader.read(v2().set24(9, 13, 234153).frame()).orElseThrow();
        assertEquals(GameState.Cast.NONE, idle.cast(), "pas d'incantation (case (3, 2) noire)");

        GameState draining = QrCodeV2Reader.read(v2()
                .set(3, 2, 1, 1, 1)                                    // incantation en cours
                .set24(9, 13, 234153)                                  // Drain de vie
                .set(10, 13, 0.25, 1, 0)                               // 2,5 s restantes, canalisation
                .frame()).orElseThrow();
        assertEquals(234153, draining.cast().spellId());
        assertTrue(draining.cast().channeling());
        assertEquals(2.5, draining.cast().remaining(), 0.05);
    }

    @Test
    void version2GridHasNoModifiersNorCounter() {

        GameState state = QrCodeV2Reader.read(v2().frame()).orElseThrow();

        assertEquals(18, state.keys().size());
        assertEquals(0, state.frame());
    }

    @Test
    void parsesKeyCombos() {

        assertEquals(new KeyCombo("3", true, false, false), KeyCombo.parse("SHIFT-3"));
        assertEquals(new KeyCombo("Q", false, true, false), KeyCombo.parse("CTRL-Q"));
        assertEquals(new KeyCombo("=", false, false, true), KeyCombo.parse("ALT-="));
        assertEquals(new KeyCombo(")", false, false, false), KeyCombo.parse(")"));
    }

    @Test
    void cellsMatchTheAddonLayout() {

        assertArrayEquals(new int[]{2, 6}, QrCodeV2Reader.stateCell(1));
        assertArrayEquals(new int[]{13, 6}, QrCodeV2Reader.stateCell(12));
        assertArrayEquals(new int[]{2, 12}, QrCodeV2Reader.stateCell(13));
        assertArrayEquals(new int[]{8, 12}, QrCodeV2Reader.historyCell(13));
        assertArrayEquals(new int[]{13, 12}, QrCodeV2Reader.historyCell(18));
        assertArrayEquals(new int[]{3, 3}, QrCodeV2Reader.spellCell(1));
        assertArrayEquals(new int[]{2, 7}, QrCodeV2Reader.spellCell(9));
        assertArrayEquals(new int[]{2, 10}, QrCodeV2Reader.spellCell(13));
        assertArrayEquals(new int[]{9, 2}, QrCodeV2Reader.spellCell(18));
        assertArrayEquals(new int[]{30, 1}, QrCodeV2Reader.memberCell(14));
        assertArrayEquals(new int[]{30, 4}, QrCodeV2Reader.memberCell(28));
        assertArrayEquals(new int[]{17, 5}, QrCodeV2Reader.memberCell(29));
    }

    @Test
    void memberCellsAreFreeCellsOfBlock2() {

        Set<String> keyCells = new HashSet<>();
        for (int position = 1; position <= 18; position++) {
            for (int[] cell : new int[][]{QrCodeV2Reader.stateCell(position), QrCodeV2Reader.historyCell(position), QrCodeV2Reader.spellCell(position)}) {
                keyCells.add((cell[0] + 16) + "," + cell[1]);
            }
        }
        Set<String> memberCells = new HashSet<>();
        for (int slot = 1; slot <= QrCodeV2Reader.MEMBERS; slot++) {
            int[] cell = QrCodeV2Reader.memberCell(slot);
            assertTrue(cell[0] >= 17 && cell[0] <= 30 && cell[1] >= 1 && cell[1] <= 14, "intérieur du bloc 2 : membre " + slot);
            assertFalse(keyCells.contains(cell[0] + "," + cell[1]), "case de touche : membre " + slot);
            assertTrue(memberCells.add(cell[0] + "," + cell[1]), "case en double : membre " + slot);
        }
    }
}
