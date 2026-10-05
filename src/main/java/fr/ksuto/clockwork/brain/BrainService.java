package fr.ksuto.clockwork.brain;

import fr.ksuto.clockwork.brain.data.GameInstall;
import fr.ksuto.clockwork.brain.data.SpellDatabase;
import fr.ksuto.clockwork.brain.data.SpellDatabaseLoader;
import fr.ksuto.clockwork.brain.data.SpellSchema;
import fr.ksuto.clockwork.brain.decision.Brain;
import fr.ksuto.clockwork.brain.decision.Rotation;
import fr.ksuto.clockwork.brain.decision.Spellbook;
import fr.ksuto.clockwork.brain.perception.GameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Cerveau du bot : rotation YAML et dictionnaire des sorts, rechargés à chaud dès que leur fichier change.
 * <p>
 * Fichiers (propriétés système) :
 * <ul>
 *   <li>{@code clockwork.rotation} : la rotation, {@code rotation.yaml} dans le dossier de lancement par défaut ;
 *   sans ce fichier, le cerveau est inactif et l'addon décide seul ;</li>
 *   <li>{@code clockwork.wow} : dossier {@code _retail_} de WoW, pour trouver la SavedVariable CLOCKWORK_SPELLBOOK
 *   (WTF/Account/COMPTE/SavedVariables/ClockWork.lua) ;</li>
 *   <li>{@code clockwork.cache} : cache des tables du jeu téléchargées depuis wago.tools, {@code ~/.clockwork/wago} par
 *   défaut.</li>
 * </ul>
 * La table complète des sorts est chargée en arrière-plan au démarrage (téléchargée une fois par version du jeu, avec
 * replis sur une version proche ou sur le cache, voir {@link SpellDatabaseLoader}) : elle
 * permet de nommer dans les règles des sorts absents de l'export de l'addon, et sert à générer
 * {@value SpellSchema#FILE_NAME}, la liste des sorts par classe pour l'autocomplétion de l'éditeur.
 */
public final class BrainService {

    private static final Logger logger = LoggerFactory.getLogger(BrainService.class);

    private final Brain brain = new Brain();
    private final Path  rotationFile;
    private final Path  wowFolder;
    private final Path  cacheFolder;

    private Rotation  rotation;
    private long      rotationModified = -1;
    private Spellbook spellbook        = new Spellbook();
    private Path      spellbookFile;
    private long      spellbookModified = -1;

    private volatile SpellDatabase database        = SpellDatabase.EMPTY;
    private          SpellDatabase appliedDatabase = null;
    private          boolean       databaseLoading = false;

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
     * @return la touche à appuyer selon la rotation, ou vide si pas de rotation ou rien à faire
     */
    public Optional<Brain.Decision> decide(GameState state) {

        reloadIfChanged();
        if (rotation == null) {return Optional.empty();}
        return brain.decide(state, rotation, spellbook);
    }

    public boolean isActive() {

        reloadIfChanged();
        return rotation != null;
    }

    private void reloadIfChanged() {

        reloadRotation();
        reloadSpellbook();
        if (rotation != null) {startDatabaseLoading();}
        applyDatabase();
    }

    /**
     * Charge la table complète des sorts dans un fil à part : le premier téléchargement prend quelques secondes.
     */
    private synchronized void startDatabaseLoading() {

        if (databaseLoading) {return;}
        databaseLoading = true;
        Thread loader = new Thread(() -> {
            try {
                Optional<GameInstall> install = GameInstall.detect(wowFolder);
                if (install.isEmpty()) {
                    logger.warn("Version du jeu introuvable dans {} : table complète des sorts non chargée", wowFolder);
                    return;
                }
                SpellDatabaseLoader.Loaded loaded = new SpellDatabaseLoader(cacheFolder).load(install.get());
                logger.info("Table complète des sorts chargée : {} sort(s), {} {} {}, {} classe(s)", loaded.database().size(),
                            loaded.install().product(), loaded.install().build(), loaded.install().locale(), loaded.database().classes().size());
                database = loaded.database();
            }
            catch (IOException | RuntimeException e) {
                logger.warn("Table complète des sorts indisponible, seuls les sorts exportés par l'addon sont connus : {}", e.getMessage());
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "spell-database");
        loader.setDaemon(true);
        loader.start();
    }

    /**
     * Donne la table complète au dictionnaire et régénère la liste des sorts de l'éditeur quand l'un ou l'autre change.
     */
    private void applyDatabase() {

        SpellDatabase current = database;
        if (current == appliedDatabase) {return;}
        appliedDatabase = current;
        spellbook.useDatabase(current);
        writeSpellSchema(current);
    }

    private void writeSpellSchema(SpellDatabase current) {

        Map<String, SortedSet<String>> spellsByClass = new HashMap<>();
        for (String playerClass : current.classes()) {spellsByClass.put(playerClass, current.classSpellNames(playerClass));}
        spellbook.namesByClass().forEach((playerClass, names) -> spellsByClass.computeIfAbsent(playerClass, c -> new TreeSet<>()).addAll(names));
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

    private void reloadSpellbook() {

        try {
            if (spellbookFile == null) {spellbookFile = findSpellbookFile().orElse(null);}
            if (spellbookFile == null) {return;}
            long modified = Files.getLastModifiedTime(spellbookFile).toMillis();
            if (modified == spellbookModified) {return;}
            spellbookModified = modified;
            spellbook = Spellbook.load(spellbookFile);
            appliedDatabase = null; // nouveau dictionnaire : lui redonner la table complète et régénérer la liste
            logger.info("Dictionnaire des sorts chargé : {} sort(s) depuis {}", spellbook.size(), spellbookFile);
        }
        catch (IOException | RuntimeException e) {
            logger.error("Dictionnaire des sorts illisible ({}) : {}", spellbookFile, e.getMessage());
        }
    }

    private Optional<Path> findSpellbookFile() throws IOException {

        Path accounts = wowFolder.resolve("WTF").resolve("Account");
        if (!Files.isDirectory(accounts)) {return Optional.empty();}
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(accounts)) {
            for (Path account : stream) {
                Path file = account.resolve("SavedVariables").resolve("ClockWork.lua");
                if (Files.isRegularFile(file)) {return Optional.of(file);}
            }
        }
        return Optional.empty();
    }
}
