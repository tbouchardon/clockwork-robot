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

        final BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);

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
        assertEquals(18, state.keys().size());
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
