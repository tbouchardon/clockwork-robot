package fr.ksuto.clockwork.brain.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Extraits des tables de wago.tools (12.1.0.69933, frFR) : quelques sorts de chaman (dont des talents et leurs
 * variantes), de druide et de mage.
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
                    188389,"Horion de flammes"
                    8042,"Horion de terre"
                    17364,"Frappe-tempête"
                    8921,"Éclat lunaire"
                    190984,"Colère"
                    33917,"Mutilation"
                    5221,"Lambeau"
                    22568,"Morsure féroce"
                    768,"Forme de félin"
                    5487,"Forme d’ours"
                    24858,"Forme de sélénien"
                    783,"Forme de voyage"
                    """),
            Map.entry("ChrClasses", """
                    Name_lang,Filename,Description_lang,ID
                    Chaman,SHAMAN,"Les chamans sont des guides spirituels,
                    sur plusieurs lignes ""entre guillemets"".",7
                    Mage,MAGE,,8
                    Druide,DRUID,,11
                    """),
            Map.entry("ChrSpecialization", """
                    Name_lang,ID,ClassID
                    Élémentaire,262,7
                    Amélioration,263,7
                    Restauration,264,7
                    Feu,63,8
                    Équilibre,102,11
                    Farouche,103,11
                    """),
            Map.entry("SkillLine", """
                    DisplayName_lang,ID,CategoryID
                    Chaman,924,7
                    Mage,904,7
                    Druide,798,7
                    Epées,43,6
                    """),
            Map.entry("SkillLineAbility", """
                    ID,SkillLine,Spell,AcquireMethod
                    1,924,188196,2
                    2,924,8004,2
                    7,924,17364,3
                    3,904,133
                    4,43,999001
                    5,798,8921
                    6,798,768
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
                    51,1000
                    """),
            Map.entry("TraitNodeXTraitNodeEntry", """
                    ID,TraitNodeID,TraitNodeEntryID,_Index
                    1,50,60,0
                    2,51,61,0
                    """),
            Map.entry("TraitNodeEntry", """
                    ID,TraitDefinitionID,MaxRanks
                    60,70,1
                    61,71,1
                    """),
            Map.entry("TraitDefinition", """
                    OverrideName_lang,ID,SpellID,OverridesSpellID,VisibleSpellID
                    ,70,470057,470411,0
                    ,71,51505,73899,0
                    """),
            // Le nœud 50 est réservé à Élémentaire, le nœud 51 (par son groupe) à Élémentaire et Restauration
            Map.entry("SpecSetMember", """
                    ID,ChrSpecializationID,SpecSet
                    1,262,5
                    2,262,6
                    3,264,6
                    """),
            Map.entry("TraitCond", """
                    ID,CondType,SpecSetID
                    900,1,5
                    901,2,6
                    902,0,0
                    """),
            Map.entry("TraitNodeXTraitCond", """
                    ID,TraitCondID,TraitNodeID
                    1,900,50
                    2,902,51
                    """),
            Map.entry("TraitNodeGroupXTraitCond", """
                    ID,TraitCondID,TraitNodeGroupID
                    1,901,77
                    """),
            Map.entry("TraitNodeGroupXTraitNode", """
                    ID,TraitNodeGroupID,TraitNodeID,_Index
                    1,77,51,0
                    """));

    private SpellDatabaseFixture() {}

    /**
     * Table construite dans un dossier temporaire.
     */
    public static SpellDatabase create() throws IOException {

        Path folder = Files.createTempDirectory("clockwork-spells");
        folder.toFile().deleteOnExit();
        return create(folder);
    }

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
