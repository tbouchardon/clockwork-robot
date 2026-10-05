package fr.ksuto.clockwork.brain.data;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class SpellDatabaseTest {

    @TempDir
    Path folder;

    @Test
    void csvHandlesQuotesAndLineBreaks() throws IOException {

        List<Map<String, String>> rows = new ArrayList<>();
        Csv.read(new StringReader("\uFEFFID,Name\r\n1,\"a, \"\"b\"\"\nc\"\r\n2,\r\n3"), rows::add);

        assertEquals(3, rows.size());
        assertEquals("1", rows.getFirst().get("ID"));
        assertEquals("a, \"b\"\nc", rows.getFirst().get("Name"));
        assertEquals("", rows.get(1).get("Name"));
        assertEquals("", rows.get(2).get("Name"));
    }

    @Test
    void namesAreFoundWithoutAccentsAmongAllSpells() throws IOException {

        SpellDatabase database = SpellDatabaseFixture.create(folder);

        assertEquals(Set.of(188196, 999001), database.idsFor("eclair"));
        assertEquals(Set.of(133), database.idsFor("Boule de feu"));
        assertEquals(SpellDatabase.normalize("Chaîne d\u2019éclairs"), SpellDatabase.normalize("chaine d'eclairs"));
        assertEquals("Explosion de lave", database.nameOf(51505));
        assertNull(database.nameOf(1));
    }

    @Test
    void classSpellsIncludeAbilitiesSpecializationAndTalents() throws IOException {

        SpellDatabase database = SpellDatabaseFixture.create(folder);

        assertEquals(new TreeSet<>(List.of("Afflux de soins", "Explosion de lave", "Frappe primordiale", "Horion de flamme",
                                           "Horion de flamme (talent)", "Éclair")),
                     database.classSpellNames("SHAMAN"));
        assertEquals(new TreeSet<>(List.of("Boule de feu")), database.classSpellNames("MAGE"));
        assertEquals(Set.of("SHAMAN", "MAGE"), database.classes());
    }

    @Test
    void readsBuildAndLocaleOfTheInstalledGame() throws IOException {

        Path retail = Files.createDirectories(folder.resolve("World of Warcraft").resolve("_retail_"));
        Files.createDirectories(retail.resolve("WTF"));
        Files.writeString(retail.getParent().resolve(".build.info"), """
                Branch!STRING:0|Active!DEC:1|Version!STRING:0|Product!STRING:0
                eu|1|1.15.7.61582|wow_classic_era
                eu|1|12.1.0.69933|wow
                """, StandardCharsets.UTF_8);
        Files.writeString(retail.resolve("WTF").resolve("Config.wtf"), "SET locale \"x\"\nSET textLocale \"frFR\"\n", StandardCharsets.UTF_8);

        assertEquals(new GameInstall("12.1.0.69933", "frFR"), GameInstall.detect(retail).orElseThrow());
        assertTrue(GameInstall.detect(folder.resolve("ailleurs").resolve("_retail_")).isEmpty());
    }

    @Test
    void spellListForTheEditorIsValidJsonPerClass() {

        SortedSet<String> shaman = new TreeSet<>(List.of("Éclair", "Nom \"cité\""));
        String            json   = SpellSchema.json(Map.of("SHAMAN", shaman, "MAGE", new TreeSet<>(List.of("Boule de feu"))));

        assertTrue(json.contains("\"definitions\""));
        assertTrue(json.indexOf("\"MAGE\"") < json.indexOf("\"SHAMAN\""), "classes triées");
        assertTrue(json.contains("\"Nom \\\"cité\\\"\""));
        assertTrue(json.contains("\"Éclair\""));
        assertEquals("\"a\\\\b\\u0001\"", SpellSchema.quote("a\\b\u0001"));
    }

    @Test
    void writesTheSpellListNextToTheRotation() throws IOException {

        Path file = folder.resolve(SpellSchema.FILE_NAME);
        SpellSchema.write(file, Map.of("SHAMAN", new TreeSet<>(List.of("Éclair"))));

        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("\"Éclair\""));
        assertFalse(Files.exists(folder.resolve(SpellSchema.FILE_NAME + ".part")));
    }
}
