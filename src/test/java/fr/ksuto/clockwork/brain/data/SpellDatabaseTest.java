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
        assertEquals("1", database.nameOf(1), "sort inconnu : son identifiant");
        assertEquals(Set.of(8004), database.idsFor("8004"), "identifiant numérique");
        assertEquals(Set.of(), database.idsFor("Sort inconnu"));
    }

    @Test
    void relatesSpellsToTheirVariants() throws IOException {

        SpellDatabase database = SpellDatabaseFixture.create(folder);

        // Blizzard recommande la forme de base 73899 ; le bouton contient la variante Explosion de lave 51505
        assertTrue(database.related(73899).contains(51505));
        assertTrue(database.related(51505).contains(73899));
        assertTrue(database.related(470057).contains(470411));
        assertEquals(Set.of(188196, 999001), database.related(188196), "homonymes");
    }

    @Test
    void classSpellsIncludeAbilitiesSpecializationAndTalents() throws IOException {

        SpellDatabase database = SpellDatabaseFixture.create(folder);

        assertEquals(new TreeSet<>(List.of("Afflux de soins", "Explosion de lave", "Frappe primordiale", "Horion de flamme",
                                           "Horion de flamme (talent)", "Éclair")),
                     database.classSpellNames("SHAMAN"));
        assertEquals(new TreeSet<>(List.of("Boule de feu")), database.classSpellNames("MAGE"));
        assertEquals(new TreeSet<>(List.of("Forme de félin", "Éclat lunaire")), database.classSpellNames("DRUID"));
    }

    @Test
    void specsHaveTheWholeClassSpellsAndTheirOwn() throws IOException {

        SpellDatabase database = SpellDatabaseFixture.create(folder);

        assertEquals("SHAMAN", database.classOf(7));
        assertEquals(new SpellDatabase.Spec(262, "SHAMAN", "Élémentaire", 0), database.spec(262));
        assertEquals(List.of("Élémentaire", "Amélioration", "Restauration"), database.specsOf("SHAMAN").stream().map(SpellDatabase.Spec::name).toList());
        assertEquals(new TreeSet<>(List.of("Afflux de soins", "Explosion de lave", "Frappe primordiale", "Horion de flamme",
                                           "Horion de flamme (talent)", "Éclair")),
                     database.specSpellNames(262), "talents réservés à Élémentaire, directement ou par leur groupe");
        assertEquals(new TreeSet<>(List.of("Afflux de soins", "Explosion de lave", "Frappe primordiale", "Éclair")),
                     database.specSpellNames(264), "Explosion de lave et sa forme de base : groupe Élémentaire et Restauration");
        assertEquals(new TreeSet<>(List.of("Afflux de soins", "Éclair")), database.specSpellNames(263));
        assertEquals(Set.of("SHAMAN", "MAGE", "DRUID"), database.classes());
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

        assertEquals(new GameInstall("wow", "12.1.0.69933", "frFR"), GameInstall.detect(retail).orElseThrow());

        // Vanilla : le produit vient de .flavor.info du dossier du client
        Path vanilla = Files.createDirectories(retail.getParent().resolve("_classic_era_"));
        Files.writeString(vanilla.resolve(".flavor.info"), "Product Flavor!STRING:0\nwow_classic_era\n", StandardCharsets.UTF_8);
        assertEquals(new GameInstall("wow_classic_era", "1.15.7.61582", "enUS"), GameInstall.detect(vanilla).orElseThrow());
        assertTrue(GameInstall.detect(folder.resolve("ailleurs").resolve("_retail_")).isEmpty());
    }

    @Test
    void editorSchemaProposesSpecsAndSpellsByClassAndSpec() throws IOException {

        String json = SpellSchema.json(SpellDatabaseFixture.create(folder));

        assertTrue(json.contains("\"byClass\""));
        assertTrue(json.contains("\"spec\": { \"enum\": [\n          \"Élémentaire\",\n          \"Amélioration\",\n          \"Restauration\"\n        ] }"));
        assertTrue(json.contains("\"spec\": { \"const\": \"Élémentaire\" }"));
        assertTrue(json.contains("\"$ref\": \"#/definitions/SHAMAN-262\""));
        assertTrue(json.contains("\"SHAMAN-262\": {"));
        assertTrue(json.contains("\"not\": { \"required\": [\"spec\"] }"), "sans spec : sorts de toute la classe");
        assertTrue(json.indexOf("\"DRUID\"") < json.indexOf("\"SHAMAN\""), "classes triées");
        assertEquals("\"a\\\\b\\u0001\\\"c\\\"\"", SpellSchema.quote("a\\b\u0001\"c\""));
    }

    @Test
    void writesTheEditorSchema() throws IOException {

        Path file = folder.resolve(SpellSchema.FILE_NAME);
        SpellSchema.write(file, SpellDatabaseFixture.create(Files.createDirectories(folder.resolve("tables"))));

        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("\"Éclair\""));
        assertFalse(Files.exists(folder.resolve(SpellSchema.FILE_NAME + ".part")));
    }
}
