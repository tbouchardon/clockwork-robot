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
 * La rotation Affliction (rotations/demoniste-affliction.yaml), d'après le guide Method.gg 12.1 : entretien des DoT,
 * éclats gardés pour Regard-noir, dépense, Sombre moisson à court d'éclats, zone, défensifs.
 */
class AfflictionRotationTest {

    private static final double NEVER = Double.POSITIVE_INFINITY;
    private static final int    HAUNT = 48181, AGONY = 980, CORRUPTION = 172, WITHER = 445465, UNSTABLE_AFFLICTION = 1259790,
                                SEED = 27243, DARKGLARE = 205180, DARK_HARVEST = 387016, DRAIN_SOUL = 198590, MALEFIC_GRASP = 1261149,
                                SHADOW_BOLT = 686, UNENDING_RESOLVE = 104773;

    private final Brain         brain     = new Brain();
    private final SpellDatabase spellbook = SpellDatabaseFixture.create();
    private final Rotation      rotation  = brain.parse(Files.readString(Path.of("rotations", "demoniste-affliction.yaml"), StandardCharsets.UTF_8));

    AfflictionRotationTest() throws IOException {}

    /**
     * Situation de combat : par défaut, une cible à 80 %, rien lancé, aucun éclat, tout prêt.
     */
    private final class Fight {

        final Map<String, KeyState> keys = new LinkedHashMap<>();
        double  health    = 100;
        int     shards    = 0;
        int     enemies   = 1;
        boolean multi     = false;
        boolean hellcaller = false;

        /** Secondes depuis le lancement sur la cible, et recharge, par sort. */
        final Map<Integer, Double> since    = new LinkedHashMap<>();
        final Map<Integer, Double> cooldown = new LinkedHashMap<>();
        double sinceDarkglare = NEVER;

        Fight cast(int spell, double secondsAgo) {

            since.put(spell, secondsAgo);
            return this;
        }

        Fight cooldown(int spell, double seconds) {

            cooldown.put(spell, seconds);
            return this;
        }

        int decide() {

            int[] spells = {HAUNT, AGONY, hellcaller ? WITHER : CORRUPTION, UNSTABLE_AFFLICTION, SEED, DARKGLARE, DARK_HARVEST, DRAIN_SOUL,
                            MALEFIC_GRASP, SHADOW_BOLT, UNENDING_RESOLVE};
            String[] names = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "Q"};
            for (int i = 0; i < spells.length; i++) {
                int     spell  = spells[i];
                // Affliction instable et Graine de Corruption coûtent un éclat
                boolean usable = (spell != UNSTABLE_AFFLICTION && spell != SEED) || shards > 0;
                double  ago    = spell == DARKGLARE ? sinceDarkglare : since.getOrDefault(spell, NEVER);
                keys.put(names[i], new KeyState(names[i], spell, cooldown.getOrDefault(spell, 0.0), usable, KeyState.Range.IN, ago, ago, false));
            }
            GameState state = new GameState(health, 100, true, true, 80, 0, true, true, false, enemies, 0, 0, true, false, false, false, false, 0,
                                            shards, 9, 265, 1, GameState.Cast.NONE, GameState.TargetCast.NONE, keys, Group.NONE, 0, 0xA, multi);
            return brain.decide(state, rotation, spellbook).map(Brain.Decision::spellId).orElse(0);
        }
    }

    @Test
    void openerHauntThenAgonyThenCorruption() {

        assertEquals(HAUNT, new Fight().decide());
        assertEquals(AGONY, new Fight().cooldown(HAUNT, 10).decide());
        assertEquals(CORRUPTION, new Fight().cooldown(HAUNT, 10).cast(AGONY, 1).decide());
    }

    @Test
    void refreshesDotsInTheirLastThirty() {

        Fight fresh = new Fight().cooldown(HAUNT, 10).cast(AGONY, 11).cast(CORRUPTION, 9).cooldown(DARKGLARE, 60).cooldown(DARK_HARVEST, 30);
        assertEquals(DRAIN_SOUL, fresh.decide(), "Agonie à 11 s sur 18, Corruption à 9 s sur 14 : remplissage");
        assertEquals(AGONY, new Fight().cooldown(HAUNT, 10).cast(AGONY, 13).cast(CORRUPTION, 9).decide());
        assertEquals(CORRUPTION, new Fight().cooldown(HAUNT, 10).cast(AGONY, 2).cast(CORRUPTION, 11).decide());
    }

    @Test
    void darkglareOnceDotsAreFreshWithShards() {

        Fight ready = new Fight().cooldown(HAUNT, 10).cast(AGONY, 2).cast(CORRUPTION, 2);
        ready.shards = 3;
        assertEquals(DARKGLARE, ready.decide());

        Fight poor = new Fight().cooldown(HAUNT, 10).cast(AGONY, 2).cast(CORRUPTION, 2).cooldown(DARK_HARVEST, 30);
        poor.shards = 2;
        assertEquals(DRAIN_SOUL, poor.decide(), "2 éclats, Regard-noir prêt : on accumule");
    }

    @Test
    void spendsShardsDuringDarkglareThenHarvestsThenGrasps() {

        Fight window = new Fight().cooldown(HAUNT, 10).cast(AGONY, 3).cast(CORRUPTION, 3).cooldown(DARKGLARE, 100);
        window.sinceDarkglare = 3;
        window.shards = 3;
        assertEquals(UNSTABLE_AFFLICTION, window.decide());

        window.shards = 0;
        assertEquals(DARK_HARVEST, window.decide(), "à court d'éclats : Sombre moisson");

        window.cooldown(DARK_HARVEST, 30);
        assertEquals(MALEFIC_GRASP, window.decide(), "pendant Regard-noir : Étreinte maléfique");
    }

    @Test
    void keepsShardsWhenDarkglareComesBackSoon() {

        Fight soon = new Fight().cooldown(HAUNT, 10).cast(AGONY, 3).cast(CORRUPTION, 3).cooldown(DARKGLARE, 10).cooldown(DARK_HARVEST, 30);
        soon.shards = 2;
        assertEquals(DRAIN_SOUL, soon.decide());
        soon.shards = 4;
        assertEquals(UNSTABLE_AFFLICTION, soon.decide(), "presque au maximum : on dépense");

        Fight later = new Fight().cooldown(HAUNT, 10).cast(AGONY, 3).cast(CORRUPTION, 3).cooldown(DARKGLARE, 60).cooldown(DARK_HARVEST, 30);
        later.shards = 2;
        assertEquals(UNSTABLE_AFFLICTION, later.decide());
    }

    @Test
    void seedOfCorruptionFromThreeEnemiesInMultiTargetMode() {

        Fight pack = new Fight().cooldown(HAUNT, 10).cast(AGONY, 3).cast(CORRUPTION, 3).cooldown(DARKGLARE, 60);
        pack.shards = 2;
        pack.enemies = 3;
        assertEquals(UNSTABLE_AFFLICTION, pack.decide(), "multi-cibles éteint");
        pack.multi = true;
        assertEquals(SEED, pack.decide());
        pack.enemies = 2;
        assertEquals(UNSTABLE_AFFLICTION, pack.decide(), "2 ennemis : une cible");
    }

    @Test
    void hellcallerKeepsWitherInsteadOfCorruption() {

        Fight wither = new Fight().cooldown(HAUNT, 10).cast(AGONY, 2);
        wither.hellcaller = true;
        assertEquals(WITHER, wither.decide());
    }

    @Test
    void unendingResolveWhenLow() {

        Fight low = new Fight();
        low.health = 30;
        assertEquals(UNENDING_RESOLVE, low.decide());
    }
}
