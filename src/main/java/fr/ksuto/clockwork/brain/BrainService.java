package fr.ksuto.clockwork.brain;

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
import java.util.Optional;

/**
 * Cerveau du bot : rotation YAML et dictionnaire des sorts, rechargés à chaud dès que leur fichier change.
 * <p>
 * Fichiers (propriétés système) :
 * <ul>
 *   <li>{@code clockwork.rotation} : la rotation, {@code rotation.yaml} dans le dossier de lancement par défaut ;
 *   sans ce fichier, le cerveau est inactif et l'addon décide seul ;</li>
 *   <li>{@code clockwork.wow} : dossier {@code _retail_} de WoW, pour trouver la SavedVariable CLOCKWORK_SPELLBOOK
 *   (WTF/Account/COMPTE/SavedVariables/ClockWork.lua).</li>
 * </ul>
 */
public final class BrainService {

    private static final Logger logger = LoggerFactory.getLogger(BrainService.class);

    private final Brain brain = new Brain();
    private final Path  rotationFile;
    private final Path  wowFolder;

    private Rotation  rotation;
    private long      rotationModified = -1;
    private Spellbook spellbook        = new Spellbook();
    private Path      spellbookFile;
    private long      spellbookModified = -1;

    public BrainService() {

        this(Paths.get(System.getProperty("clockwork.rotation", "rotation.yaml")),
             Paths.get(System.getProperty("clockwork.wow", "E:/Perso/World of Warcraft/_retail_")));
    }

    public BrainService(Path rotationFile, Path wowFolder) {

        this.rotationFile = rotationFile;
        this.wowFolder = wowFolder;
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
