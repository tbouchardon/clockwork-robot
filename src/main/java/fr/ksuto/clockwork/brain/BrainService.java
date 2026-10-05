package fr.ksuto.clockwork.brain;

import fr.ksuto.clockwork.brain.data.GameInstall;
import fr.ksuto.clockwork.brain.data.SpellDatabase;
import fr.ksuto.clockwork.brain.data.SpellDatabaseLoader;
import fr.ksuto.clockwork.brain.data.SpellSchema;
import fr.ksuto.clockwork.brain.decision.Brain;
import fr.ksuto.clockwork.brain.decision.Rotation;
import fr.ksuto.clockwork.brain.perception.GameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;

/**
 * Cerveau du bot : rotation YAML, rechargée à chaud dès que son fichier change, et table des sorts du jeu.
 * <p>
 * Propriétés système :
 * <ul>
 *   <li>{@code clockwork.rotation} : la rotation, {@code rotation.yaml} dans le dossier de lancement par défaut ;
 *   sans ce fichier, le cerveau est inactif et l'addon décide seul ;</li>
 *   <li>{@code clockwork.wow} : dossier du client ({@code _retail_}, {@code _classic_era_}...), pour connaître le
 *   produit, la version et la langue du jeu ;</li>
 *   <li>{@code clockwork.cache} : cache des tables du jeu téléchargées depuis wago.tools, {@code ~/.clockwork/wago} par
 *   défaut.</li>
 * </ul>
 * La table des sorts est chargée en arrière-plan au démarrage (téléchargée une fois par version du jeu, avec replis sur
 * une version proche ou sur le cache, voir {@link SpellDatabaseLoader}) : elle traduit les noms des règles en
 * identifiants, et sert à générer {@value SpellSchema#FILE_NAME}, la liste des sorts par classe pour l'autocomplétion de
 * l'éditeur. Le cerveau attend qu'elle soit chargée pour décider.
 */
public final class BrainService {

    private static final Logger logger = LoggerFactory.getLogger(BrainService.class);

    private final Brain brain = new Brain();
    private final Path  rotationFile;
    private final Path  wowFolder;
    private final Path  cacheFolder;

    private Rotation rotation;
    private long     rotationModified = -1;

    /**
     * Table des sorts, nulle tant qu'elle n'est pas chargée (vide si elle n'a pas pu l'être).
     */
    private volatile SpellDatabase database        = null;
    private          boolean       databaseLoading = false;
    private          boolean       waitingReported = false;

    public BrainService() {

        this(Paths.get(System.getProperty("clockwork.rotation", "rotation.yaml")),
             Paths.get(System.getProperty("clockwork.wow", "E:/Perso/World of Warcraft/_retail_")),
             Paths.get(System.getProperty("clockwork.cache", System.getProperty("user.home") + "/.clockwork/wago")));
    }

    public BrainService(Path rotationFile, Path wowFolder, Path cacheFolder) {

        this.rotationFile = rotationFile;
        this.wowFolder = wowFolder;
        this.cacheFolder = cacheFolder;
    }

    /**
     * @return la touche à appuyer selon la rotation, ou vide si pas de rotation, table des sorts pas encore chargée ou
     * rien à faire
     */
    public Optional<Brain.Decision> decide(GameState state) {

        reloadIfChanged();
        SpellDatabase spells = database;
        if (rotation == null) {return Optional.empty();}
        if (spells == null) {
            if (!waitingReported) {logger.info("Cerveau en attente de la table des sorts");}
            waitingReported = true;
            return Optional.empty();
        }
        return brain.decide(state, rotation, spells);
    }

    public boolean isActive() {

        reloadIfChanged();
        return rotation != null;
    }

    private void reloadIfChanged() {

        reloadRotation();
        if (rotation != null) {startDatabaseLoading();}
    }

    /**
     * Charge la table des sorts dans un fil à part : le premier téléchargement prend une dizaine de secondes.
     */
    private synchronized void startDatabaseLoading() {

        if (databaseLoading) {return;}
        databaseLoading = true;
        Thread loader = new Thread(() -> {
            SpellDatabase loaded = SpellDatabase.EMPTY;
            try {
                Optional<GameInstall> install = GameInstall.detect(wowFolder);
                if (install.isEmpty()) {
                    logger.warn("Version du jeu introuvable dans {} : seuls les identifiants de sorts sont utilisables", wowFolder);
                    return;
                }
                SpellDatabaseLoader.Loaded result = new SpellDatabaseLoader(cacheFolder).load(install.get());
                logger.info("Table des sorts chargée : {} sort(s), {} {} {}, {} classe(s)", result.database().size(),
                            result.install().product(), result.install().build(), result.install().locale(), result.database().classes().size());
                loaded = result.database();
                writeSpellSchema(loaded);
            }
            catch (IOException | RuntimeException e) {
                logger.warn("Table des sorts indisponible, seuls les identifiants de sorts sont utilisables : {}", e.getMessage());
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            finally {
                database = loaded;
            }
        }, "spell-database");
        loader.setDaemon(true);
        loader.start();
    }

    private void writeSpellSchema(SpellDatabase spells) {

        Map<String, SortedSet<String>> spellsByClass = new HashMap<>();
        for (String playerClass : spells.classes()) {spellsByClass.put(playerClass, spells.classSpellNames(playerClass));}
        if (spellsByClass.isEmpty()) {return;}

        Path file = rotationFile.toAbsolutePath().resolveSibling(SpellSchema.FILE_NAME);
        try {
            SpellSchema.write(file, spellsByClass);
            logger.info("Liste des sorts pour l'éditeur écrite : {}", file);
        }
        catch (IOException e) {
            logger.warn("Liste des sorts pour l'éditeur non écrite ({}) : {}", file, e.getMessage());
        }
    }

    private void reloadRotation() {

        if (!Files.isRegularFile(rotationFile)) {
            if (rotation != null) {logger.info("Rotation {} supprimée : l'addon décide seul", rotationFile);}
            rotation = null;
            rotationModified = -1;
            return;
        }
        try {
            long modified = Files.getLastModifiedTime(rotationFile).toMillis();
            if (modified == rotationModified) {return;}
            rotationModified = modified;
            rotation = brain.parse(Files.readString(rotationFile, StandardCharsets.UTF_8));
            logger.info("Rotation chargée : {} ({} règle(s), recommandation de Blizzard {})", rotation.spec(), rotation.rules().size(),
                        rotation.followAssisted() ? "en repli, priorité " + rotation.assistedPriority() : "ignorée");
        }
        catch (IOException | RuntimeException e) {
            // On garde la rotation précédente : une faute de frappe en cours d'édition ne doit pas arrêter le bot
            logger.error("Rotation {} invalide, version précédente conservée : {}", rotationFile, e.getMessage());
        }
    }
}
