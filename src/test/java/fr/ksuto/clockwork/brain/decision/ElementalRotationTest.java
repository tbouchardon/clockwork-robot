package fr.ksuto.clockwork.brain.decision;

import fr.ksuto.clockwork.brain.data.SpellDatabase;
import fr.ksuto.clockwork.brain.data.SpellDatabaseFixture;
import fr.ksuto.clockwork.brain.perception.GameState;
import fr.ksuto.clockwork.brain.perception.Group;
import fr.ksuto.clockwork.brain.perception.KeyState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La rotation Élémentaire (rotations/chaman-elementaire.yaml), d'après le guide Method.gg 12.1 : temps de recharge,
 * Horion de flamme, alternance Explosion de lave / dépense (Maître des éléments), zone selon le nombre d'ennemis.
 */
class ElementalRotationTest {

    private static final double NEVER = Double.POSITIVE_INFINITY;
    private static final int    STORMKEEPER = 191634, SWIFTNESS = 443454, ASCENDANCE = 114050, FLAME_SHOCK = 470411, VOLTAIC_BLAZE = 470058,
                                LAVA_BURST = 51505, ELEMENTAL_BLAST = 117014, LIGHTNING_BOLT = 188196, CHAIN_LIGHTNING = 188443,
                                EARTHQUAKE = 61882, WIND_SHEAR = 57994;

    private final Brain         brain     = new Brain();
    private final SpellDatabase spellbook = SpellDatabaseFixture.create();
    private final Rotation      rotation  = brain.parse(Files.readString(Path.of("rotations", "chaman-elementaire.yaml"), StandardCharsets.UTF_8));

    ElementalRotationTest() throws IOException {}

    /**
     * Combat : temps de recharge en cours par défaut (Gardien des tempêtes, Rapidité ancestrale, Ascendance, Brasier
     * voltaïque), Horion de flamme posé il y a 2 s, Maelström à 50 %.
     */
    private final class Fight {

        final Map<Integer, Double>  sinceOnTarget = new LinkedHashMap<>();
        final Map<Integer, Double>  since         = new LinkedHashMap<>();
        final Map<Integer, Double>  cooldown      = new LinkedHashMap<>(Map.of(STORMKEEPER, 30.0, SWIFTNESS, 30.0, ASCENDANCE, 60.0,
                                                                                VOLTAIC_BLAZE, 20.0));
        double  power         = 50;
        int     enemies       = 1;
        boolean multi         = false;
        boolean lavaSurge     = false;
        boolean targetCasting = false;

        Fight() {

            sinceOnTarget.put(FLAME_SHOCK, 2.0);
        }

        Fight cast(int spell, double secondsAgo) {

            since.put(spell, secondsAgo);
            return this;
        }

        int decide() {

            int[] spells = {STORMKEEPER, SWIFTNESS, ASCENDANCE, FLAME_SHOCK, VOLTAIC_BLAZE, LAVA_BURST, ELEMENTAL_BLAST, LIGHTNING_BOLT,
                            CHAIN_LIGHTNING, EARTHQUAKE, WIND_SHEAR};
            Map<String, KeyState> keys = new LinkedHashMap<>();
            for (int i = 0; i < spells.length; i++) {
                int    spell = spells[i];
                String name  = "SHIFT-" + (i + 1 == 10 ? "0" : i + 1 == 11 ? "Q" : String.valueOf(i + 1));
                double ago   = since.getOrDefault(spell, NEVER);
                keys.put(name, new KeyState(name, spell, cooldown.getOrDefault(spell, 0.0), true, KeyState.Range.IN,
                                            sinceOnTarget.getOrDefault(spell, ago), ago, spell == LAVA_BURST && lavaSurge));
            }
            GameState state = new GameState(100, power, true, true, 80, 0, true, true, false, enemies, 0, 0, true, false, false, false, false, 0, 0,
                                            7, 262, 1, GameState.Cast.NONE,
                                            targetCasting ? new GameState.TargetCast(true, 0, true) : GameState.TargetCast.NONE, keys, Group.NONE,
                                            0, 0xA, multi);
            return brain.decide(state, rotation, spellbook).map(Brain.Decision::spellId).orElse(0);
        }
    }

    @Test
    void cooldownsInOrderStormkeeperBeforeAscendance() {

        Fight fresh = new Fight();
        fresh.cooldown.clear();
        assertEquals(STORMKEEPER, fresh.decide());

        Fight afterStormkeeper = new Fight().cast(STORMKEEPER, 2);
        afterStormkeeper.cooldown.put(STORMKEEPER, 50.0);
        afterStormkeeper.cooldown.put(ASCENDANCE, 0.0);
        assertEquals(ASCENDANCE, afterStormkeeper.decide());

        Fight withoutStormkeeper = new Fight();
        withoutStormkeeper.cooldown.put(ASCENDANCE, 0.0);
        assertNotEquals(ASCENDANCE, withoutStormkeeper.decide(), "Ascendance attend Gardien des tempêtes");
    }

    @Test
    void refreshesFlameShockWithLessThanSixSecondsLeft() {

        Fight late = new Fight();
        late.sinceOnTarget.put(FLAME_SHOCK, 13.0);
        assertEquals(FLAME_SHOCK, late.decide());
    }

    @Test
    void alternatesLavaBurstWithSpenders() {

        assertEquals(LAVA_BURST, new Fight().decide(), "jamais lancée : pas de Maître des éléments");
        assertEquals(ELEMENTAL_BLAST, new Fight().cast(LAVA_BURST, 2).cast(LIGHTNING_BOLT, 5).decide(),
                     "Explosion de lave juste avant : Maître des éléments, on dépense");
        assertEquals(LAVA_BURST, new Fight().cast(LAVA_BURST, 4).cast(ELEMENTAL_BLAST, 2).decide(), "Maître des éléments consommé");

        Fight capped = new Fight().cast(LAVA_BURST, 2).cast(LIGHTNING_BOLT, 5);
        capped.cooldown.put(LAVA_BURST, 6.0);
        capped.cooldown.put(ELEMENTAL_BLAST, 0.0);
        capped.power = 30;
        capped.since.put(LAVA_BURST, 20.0);
        assertEquals(LIGHTNING_BOLT, capped.decide(), "rien de prêt ni d'utile : Éclair");
        capped.power = 90;
        assertEquals(ELEMENTAL_BLAST, capped.decide(), "Maelström presque plein");
    }

    @Test
    void twoTargetsVoltaicBlazeAndChainLightning() {

        Fight two = new Fight().cast(LAVA_BURST, 2).cast(ELEMENTAL_BLAST, 1);
        two.cooldown.put(LAVA_BURST, 6.0);
        two.cooldown.put(VOLTAIC_BLAZE, 0.0);
        two.enemies = 2;
        two.multi = true;
        assertEquals(VOLTAIC_BLAZE, two.decide());
        two.cooldown.put(VOLTAIC_BLAZE, 20.0);
        assertEquals(CHAIN_LIGHTNING, two.decide());
        two.multi = false;
        assertEquals(LIGHTNING_BOLT, two.decide(), "multi-cibles éteint : une cible");
    }

    @Test
    void threeTargetsEarthquakeAndLavaBurstOnlyOnSurge() {

        Fight pack = new Fight();
        pack.enemies = 3;
        pack.multi = true;
        assertEquals(EARTHQUAKE, pack.decide(), "pas d'Explosion de lave sans Vague de lave");
        pack.lavaSurge = true;
        assertEquals(LAVA_BURST, pack.decide());
    }

    @Test
    void interruptsFirst() {

        Fight casting = new Fight();
        casting.targetCasting = true;
        assertEquals(WIND_SHEAR, casting.decide());
    }
}
