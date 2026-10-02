package fr.ksuto.clockwork.brain.decision;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpellbookTest {

    /**
     * Extrait au format écrit par WoW dans WTF/Account/COMPTE/SavedVariables/ClockWork.lua.
     */
    static final String SAVED_VARIABLES = """

            CLOCKWORK_ROTATIONS = {
            }
            CLOCKWORK_SPELLBOOK = {
            	["SHAMAN-262"] = {
            		["updated"] = "2026-10-02 15:10:00",
            		["spells"] = {
            			[188389] = {
            				["override"] = 470411,
            				["name"] = "Horion de flammes",
            				["base"] = 188389,
            			},
            			[470411] = {
            				["override"] = 470411,
            				["name"] = "Horion de flamme",
            				["base"] = 470411,
            			},
            			[51505] = {
            				["base"] = 73899,
            				["name"] = "Explosion de lave",
            			},
            			[188196] = {
            				["name"] = "Éclair",
            				["base"] = 188196,
            			},
            		},
            	},
            }
            """;

    @Test
    void parsesSavedVariables() {

        Spellbook spellbook = Spellbook.parse(SAVED_VARIABLES);

        assertEquals(4, spellbook.size());
        assertEquals("Horion de flammes", spellbook.nameOf(188389));
    }

    @Test
    void nameDesignatesBaseAndOverride() {

        Spellbook spellbook = Spellbook.parse(SAVED_VARIABLES);

        assertEquals(Set.of(188389, 470411), spellbook.idsFor("Horion de flammes"));
        assertEquals(Set.of(51505, 73899), spellbook.idsFor("Explosion de lave"));
    }

    @Test
    void namesIgnoreCaseAndAccents() {

        Spellbook spellbook = Spellbook.parse(SAVED_VARIABLES);

        assertEquals(Set.of(188196), spellbook.idsFor("eclair"));
        assertEquals(Set.of(188389, 470411), spellbook.idsFor("  HORION DE FLAMMES "));
    }

    @Test
    void numericReferencesAreIds() {

        assertEquals(Set.of(8004), new Spellbook().idsFor("8004"));
    }

    @Test
    void missingSpellbookIsEmpty() {

        assertEquals(0, Spellbook.parse("CLOCKWORK_ROTATIONS = {\n}\n").size());
    }
}
