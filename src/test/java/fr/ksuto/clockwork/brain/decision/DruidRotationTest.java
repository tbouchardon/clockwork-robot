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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La rotation du druide (rotations/druide.yaml), portée depuis rotation_druid.lua : une règle par forme.
 */
class DruidRotationTest {

    private static final double NEVER = Double.POSITIVE_INFINITY;

    private static final int NO_FORM = 0, CAT = 768, BEAR = 5487, MOONKIN = 24858, TRAVEL = 783;
    private static final int MOONFIRE = 8921, WRATH = 190984, MANGLE = 33917, SHRED = 5221, BITE = 22568;
    private static final int REGROWTH = 8936, REJUVENATION = 774;


    private final Brain         brain     = new Brain();
    private final SpellDatabase spellbook = SpellDatabaseFixture.create();
    private final Rotation  rotation  = brain.parse(Files.readString(Path.of("rotations", "druide.yaml"), StandardCharsets.UTF_8));

    DruidRotationTest() throws IOException {}

    private static KeyState key(String key, int spellId, double sinceCastOnTarget) {

        return new KeyState(key, spellId, 0, true, KeyState.Range.IN, sinceCastOnTarget, sinceCastOnTarget, false);
    }

    /**
     * La barre d'action change avec la forme : la touche 1 porte Éclat lunaire, Mutilation ou Lambeau selon la forme.
     */
    private Optional<Brain.Decision> decide(int form, int combo, boolean casting, KeyState... keys) {

        Map<String, KeyState> map = new LinkedHashMap<>();
        for (KeyState key : keys) {map.put(key.key(), key);}
        GameState state = new GameState(100, 100, true, true, 80, 0, true, true, casting, 1, 0, 0, true, false, false, false, false, form, combo, 11, 102, 1, GameState.Cast.NONE, GameState.TargetCast.NONE, map);
        return brain.decide(state, rotation, spellbook);
    }

    private int castIn(int form, int combo, KeyState... keys) {

        return decide(form, combo, false, keys).map(Brain.Decision::spellId).orElse(0);
    }

    @Test
    void casterFormKeepsMoonfireUpThenCastsWrath() {

        assertEquals(MOONFIRE, castIn(NO_FORM, 0, key("1", MOONFIRE, NEVER), key("2", WRATH, NEVER)));
        assertEquals(WRATH, castIn(NO_FORM, 0, key("1", MOONFIRE, 5), key("2", WRATH, NEVER)));
        assertEquals(MOONFIRE, castIn(MOONKIN, 0, key("1", MOONFIRE, 17), key("2", WRATH, 1)), "à renouveler après 16 s");
        assertEquals(WRATH, castIn(MOONKIN, 0, key("1", MOONFIRE, 15), key("2", WRATH, 1)), "encore 3 s de debuff");
    }

    @Test
    void bearFormMangles() {

        assertEquals(MANGLE, castIn(BEAR, 0, key("1", MANGLE, NEVER), key("2", WRATH, NEVER)));
    }

    @Test
    void catFormShredsAndBitesFromThreeComboPoints() {

        assertEquals(SHRED, castIn(CAT, 2, key("1", SHRED, NEVER), key("2", BITE, NEVER)));
        assertEquals(BITE, castIn(CAT, 3, key("1", SHRED, NEVER), key("2", BITE, NEVER)));
    }

    @Test
    void nothingInOtherFormsNorWhileCasting() {

        assertEquals(0, castIn(TRAVEL, 0, key("1", MOONFIRE, NEVER), key("2", WRATH, NEVER)));
        assertEquals(0, castIn(CAT, 0, key("1", MOONFIRE, NEVER), key("2", WRATH, NEVER)), "sorts de lanceur ignorés en félin");
        assertTrue(decide(NO_FORM, 0, true, key("1", MOONFIRE, NEVER)).isEmpty());
    }

    private Optional<Brain.Decision> decideInGroup(boolean healerMode, double woundedHealth, int form, double sinceRegrowth) {

        Map<String, KeyState> map = new LinkedHashMap<>();
        map.put("1", key("1", form == CAT ? SHRED : WRATH, NEVER));
        map.put("4", new KeyState("4", REGROWTH, 0, true, KeyState.Range.IN, NEVER, sinceRegrowth, false));
        map.put("5", key("5", REJUVENATION, NEVER));
        map.put("6", key("6", CAT, NEVER));
        List<Group.Member> members = List.of(new Group.Member(1, 95, true, false, false, true, Group.Role.DAMAGER),
                                             new Group.Member(2, woundedHealth, true, false, false, false, Group.Role.TANK));
        GameState state = new GameState(100, 100, true, true, 80, 0, true, true, false, 1, 0, 0, true, false, false, false, false, form, 0, 11, 103,
                                        1, GameState.Cast.NONE, GameState.TargetCast.NONE, map, new Group(healerMode, false, members));
        return brain.decide(state, rotation, spellbook);
    }

    @Test
    void feralHealsOnlyInAnEmergencyThenGoesBackToCat() {

        assertEquals(SHRED, decideInGroup(false, 50, CAT, NEVER).orElseThrow().spellId(), "hors mode soigneur : on tape");
        Brain.Decision emergency = decideInGroup(false, 20, CAT, NEVER).orElseThrow();
        assertEquals(REGROWTH, emergency.spellId());
        assertEquals(2, emergency.member());
        assertEquals(CAT, decideInGroup(false, 50, NO_FORM, 3).orElseThrow().spellId(), "retour en félin après l'urgence");
    }

    @Test
    void healerModeHealsBeforeDamage() {

        assertEquals(REGROWTH, decideInGroup(true, 50, NO_FORM, NEVER).orElseThrow().spellId());
        assertEquals(REJUVENATION, decideInGroup(true, 80, NO_FORM, NEVER).orElseThrow().spellId());
        assertEquals(WRATH, decideInGroup(true, 100, NO_FORM, NEVER).orElseThrow().spellId(), "personne à soigner");
    }
}
