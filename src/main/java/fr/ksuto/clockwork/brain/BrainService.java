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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Cerveau du bot : rotations YAML, rechargées à chaud dès que leur fichier change, et table des sorts du jeu.
 * <p>
 * La rotation suit le personnage : parmi les fichiers du dossier des rotations, celle de sa classe et de sa
 * spécialisation (lues dans la grille), sinon celle de toute sa classe. Sans rotation pour lui, l'addon décide seul.
 * <p>
 * Propriétés système :
 * <ul>
 *   <li>{@code clockwork.rotations} : dossier des rotations, {@code rotations} dans le dossier de lancement par défaut ;</li>
 *   <li>{@code clockwork.rotation} : rotation imposée quel que soit le personnage, {@code rotation.yaml} dans le dossier
 *   de lancement par défaut (ignorée si absente) ;</li>
 *   <li>{@code clockwork.wow} : dossier du client ({@code _retail_}, {@code _classic_era_}...), pour connaître le
 *   produit, la version et la langue du jeu ;</li>
 *   <li>{@code clockwork.cache} : cache des tables du jeu téléchargées depuis wago.tools, {@code ~/.clockwork/wago} par
 *   défaut.</li>
 * </ul>
 * La table des sorts est chargée en arrière-plan au démarrage (téléchargée une fois par version du jeu, avec replis sur
 * une version proche ou sur le cache, voir {@link SpellDatabaseLoader}) : elle traduit les noms des règles en
 * identifiants, donne les noms des classes et des spécialisations, et sert à générer {@value SpellSchema#FILE_NAME} pour
 * l'autocomplétion de l'éditeur. Le cerveau attend qu'elle soit chargée pour décider.
 */
public final class BrainService {

    private static final Logger logger = LoggerFactory.getLogger(BrainService.class);

    /**
     * Les fichiers ne sont réexaminés qu'une fois par seconde : l'automate décide toutes les 100 ms.
     */
    private static final long RELOAD_INTERVAL = 1000;

    private final Brain brain = new Brain();
    private final Path  rotationsFolder;
    private final Path  forcedFile;
    private final Path  wowFolder;
    private final Path  cacheFolder;

    /**
     * Rotation lue dans un fichier, avec sa date de modification.
     */
    private record Loaded(Path file, long modified, Rotation rotation) {}

    private final Map<Path, Loaded> rotations  = new HashMap<>();
    private       Loaded            forced     = null;
    private       long              lastReload = 0;
    private       String            lastChoice = null;

    /**
     * Table des sorts, nulle tant qu'elle n'est pas chargée (vide si elle n'a pas pu l'être).
     */
    private volatile SpellDatabase database        = null;
    private          boolean       databaseLoading = false;
    private          boolean       waitingReported = false;

    public BrainService() {

        this(Paths.get(System.getProperty("clockwork.rotations", "rotations")),
             Paths.get(System.getProperty("clockwork.rotation", "rotation.yaml")),
             Paths.get(System.getProperty("clockwork.wow", "E:/Perso/World of Warcraft/_retail_")),
             Paths.get(System.getProperty("clockwork.cache", System.getProperty("user.home") + "/.clockwork/wago")));
    }

    public BrainService(Path rotationsFolder, Path forcedFile, Path wowFolder, Path cacheFolder) {

        this.rotationsFolder = rotationsFolder;
        this.forcedFile = forcedFile;
        this.wowFolder = wowFolder;
        this.cacheFolder = cacheFolder;
    }

    /**
     * Table des sorts fournie (tests) : pas de chargement depuis le jeu.
     */
    BrainService(Path rotationsFolder, Path forcedFile, SpellDatabase database) {

        this(rotationsFolder, forcedFile, Path.of("."), Path.of("."));
        this.database = database;
        this.databaseLoading = true;
    }

    /**
     * @return au moins une rotation est disponible (le cerveau peut prendre la main sur l'addon)
     */
    public boolean hasRotations() {

        reloadIfChanged();
        return forced != null || !rotations.isEmpty();
    }

    /**
     * @return le cerveau décide pour ce personnage : une rotation lui correspond (ou la table des sorts, nécessaire pour
     * le savoir, est en cours de chargement)
     */
    public boolean handles(GameState state) {

        if (!hasRotations()) {return false;}
        if (forced != null || database == null) {return true;}
        return rotationFor(state).isPresent();
    }

    /**
     * @return la touche à appuyer selon la rotation du personnage, ou vide si pas de rotation, table des sorts pas
     * encore chargée ou rien à faire
     */
    public Optional<Brain.Decision> decide(GameState state) {

        reloadIfChanged();
        SpellDatabase spells = database;
        if (spells == null) {
            if (!waitingReported) {logger.info("Cerveau en attente de la table des sorts");}
            waitingReported = true;
            return Optional.empty();
        }
        return rotationFor(state).flatMap(rotation -> brain.decide(state, rotation, spells));
    }

    /**
     * Rotation imposée, sinon celle de la spécialisation du personnage, sinon celle de toute sa classe.
     */
    private Optional<Rotation> rotationFor(GameState state) {

        if (forced != null) {return report(Optional.of(forced), "tout personnage (rotation imposée)");}

        SpellDatabase spells = database;
        if (spells == null) {return Optional.empty();}
        String             playerClass = spells.classOf(state.classId());
        SpellDatabase.Spec spec        = spells.spec(state.specId());
        String             character   = playerClass == null ? "classe inconnue (" + state.classId() + ")"
                                                             : playerClass + (spec == null ? "" : " " + spec.name());

        Optional<Loaded> chosen = rotations.values().stream()
                                           .filter(loaded -> loaded.rotation().appliesTo(playerClass, spec))
                                           .min(Comparator.comparing((Loaded loaded) -> loaded.rotation().forWholeClass())
                                                          .thenComparing(loaded -> loaded.file().getFileName().toString()));
        return report(chosen, character);
    }

    /**
     * Journalise le choix de rotation quand il change.
     */
    private Optional<Rotation> report(Optional<Loaded> chosen, String character) {

        String choice = chosen.map(loaded -> loaded.rotation().label() + " (" + loaded.file() + ")").orElse(null);
        String key    = character + " -> " + choice;
        if (!Objects.equals(key, lastChoice)) {
            lastChoice = key;
            if (choice == null) {logger.info("Aucune rotation pour {} : l'addon décide seul", character);}
            else {logger.info("Rotation pour {} : {}", character, choice);}
        }
        return chosen.map(Loaded::rotation);
    }

    private void reloadIfChanged() {

        long now = System.currentTimeMillis();
        if (now - lastReload < RELOAD_INTERVAL) {return;}
        lastReload = now;

        startDatabaseLoading();
        forced = Files.isRegularFile(forcedFile) ? reload(forcedFile, forced) : null;

        List<Path> files = new ArrayList<>();
        if (Files.isDirectory(rotationsFolder)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(rotationsFolder, "*.{yaml,yml}")) {
                stream.forEach(files::add);
            }
            catch (IOException e) {
                logger.warn("Dossier des rotations illisible ({}) : {}", rotationsFolder, e.getMessage());
            }
        }
        rotations.keySet().retainAll(files);
        for (Path file : files) {
            Loaded loaded = reload(file, rotations.get(file));
            if (loaded == null) {rotations.remove(file);}
            else {rotations.put(file, loaded);}
        }
    }

    /**
     * @return la rotation du fichier, relue s'il a changé ; la version précédente si la nouvelle est invalide
     */
    private Loaded reload(Path file, Loaded previous) {

        try {
            long modified = Files.getLastModifiedTime(file).toMillis();
            if (previous != null && previous.modified() == modified) {return previous;}
            Rotation rotation = brain.parse(Files.readString(file, StandardCharsets.UTF_8));
            logger.info("Rotation chargée : {} ({}, {} règle(s), recommandation de Blizzard {})", rotation.label(), file, rotation.rules().size(),
                        rotation.followAssisted() ? "en repli, priorité " + rotation.assistedPriority() : "ignorée");
            return new Loaded(file, modified, rotation);
        }
        catch (IOException | RuntimeException e) {
            // On garde la rotation précédente : une faute de frappe en cours d'édition ne doit pas arrêter le bot
            logger.error("Rotation {} invalide{} : {}", file, previous != null ? ", version précédente conservée" : "", e.getMessage());
            return previous;
        }
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

    /**
     * Écrit le schéma des rotations, avec les spécialisations et les sorts du jeu, dans le dossier de lancement : les
     * fichiers de rotations/ y renvoient par leur première ligne.
     */
    private void writeSpellSchema(SpellDatabase spells) {

        if (spells.classes().isEmpty()) {return;}
        Path file = forcedFile.toAbsolutePath().resolveSibling(SpellSchema.FILE_NAME);
        try {
            SpellSchema.write(file, spells);
            logger.info("Schéma des rotations écrit : {}", file);
        }
        catch (IOException e) {
            logger.warn("Schéma des rotations non écrit ({}) : {}", file, e.getMessage());
        }
    }
}
