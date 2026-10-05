package fr.ksuto.clockwork.brain.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Extraits des tables de wago.tools (12.1.0.69933, frFR) : quelques sorts de chaman, dont des talents.
 */
public final class SpellDatabaseFixture {

    static final Map<String, String> TABLES = Map.ofEntries(
            Map.entry("SpellName", """
                    ID,Name_lang
                    188196,"Éclair"
                    470411,"Horion de flamme"
                    51505,"Explosion de lave"
                    73899,"Frappe primordiale"
                    8004,"Afflux de soins"
                    133,"Boule de feu"
                    999001,"Éclair"
                    470057,"Horion de flamme (talent)"
                    """),
            Map.entry("ChrClasses", """
                    Name_lang,Filename,Description_lang,ID
                    Chaman,SHAMAN,"Les chamans sont des guides spirituels,
                    sur plusieurs lignes ""entre guillemets"".",7
                    Mage,MAGE,,8
                    """),
            Map.entry("ChrSpecialization", """
                    Name_lang,ID,ClassID
                    Élémentaire,262,7
                    Feu,63,8
                    """),
            Map.entry("SkillLine", """
                    DisplayName_lang,ID,CategoryID
                    Chaman,924,7
                    Mage,904,7
                    Epées,43,6
                    """),
            Map.entry("SkillLineAbility", """
                    ID,SkillLine,Spell
                    1,924,188196
                    2,924,8004
                    3,904,133
                    4,43,999001
                    """),
            Map.entry("SpecializationSpells", """
                    Description_lang,ID,SpecID,SpellID,OverridesSpellID
                    ,1,262,73899,0
                    """),
            Map.entry("SkillLineXTraitTree", """
                    ID,SkillLineID,TraitTreeID,Variant
                    1,924,1000,0
                    """),
            Map.entry("TraitNode", """
                    ID,TraitTreeID
                    50,1000
                    """),
            Map.entry("TraitNodeXTraitNodeEntry", """
                    ID,TraitNodeID,TraitNodeEntryID,_Index
                    1,50,60,0
                    2,50,61,1
                    """),
            Map.entry("TraitNodeEntry", """
                    ID,TraitDefinitionID,MaxRanks
                    60,70,1
                    61,71,1
                    """),
            Map.entry("TraitDefinition", """
                    OverrideName_lang,ID,SpellID,OverridesSpellID,VisibleSpellID
                    ,70,470057,470411,0
                    ,71,51505,0,0
                    """));

    private SpellDatabaseFixture() {}

    public static SpellDatabase create(Path folder) throws IOException {

        Map<String, Path> files = new HashMap<>();
        for (Map.Entry<String, String> table : TABLES.entrySet()) {
            Path file = folder.resolve(table.getKey() + ".csv");
            Files.writeString(file, table.getValue(), StandardCharsets.UTF_8);
            files.put(table.getKey(), file);
        }
        return SpellDatabase.load(files);
    }
}
