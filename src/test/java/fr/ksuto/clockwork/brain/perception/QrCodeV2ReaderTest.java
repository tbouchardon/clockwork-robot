package fr.ksuto.clockwork.brain.perception;

import fr.ksuto.prh.capture.Frame;

import java.awt.image.BufferedImage;

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
                .set(4, 9, 12 / 60.0, 1, 1)                            // 12 s sur cette cible, proc, jamais ailleurs
                .set24(5, 3, 188389)
                .frame()).orElseThrow();

        KeyState key = state.keys().get("3");
        assertEquals(188389, key.spellId());
        assertEquals(3, key.cooldown(), 0.2);
        assertTrue(key.usable());
        assertEquals(KeyState.Range.IN, key.range());
        assertEquals(12, key.sinceCastOnTarget(), 0.2);
        assertTrue(key.proc());
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
    }
}
