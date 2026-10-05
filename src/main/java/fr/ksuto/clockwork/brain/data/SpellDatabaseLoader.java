package fr.ksuto.clockwork.brain.data;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Charge la table des sorts du client installé, avec des replis :
 * <ol>
 *   <li>la version exacte du client ;</li>
 *   <li>sinon (version inconnue de wago.tools : client de serveur privé, version sortie dans l'heure) la version connue
 *   la plus proche du même produit ;</li>
 *   <li>sinon (pas de réseau) la version la plus récente déjà en cache pour ce produit et cette langue.</li>
 * </ol>
 * Une fois une version chargée en entier, les autres versions du <b>même produit</b> sont supprimées du cache : retail,
 * vanilla... gardent chacun leur cache.
 */
public final class SpellDatabaseLoader {

    private static final Logger logger = LoggerFactory.getLogger(SpellDatabaseLoader.class);

    /**
     * Présent dans le dossier d'une version dont toutes les tables sont en cache.
     */
    static final String COMPLETE = ".complete";

    /**
     * Source des versions connues de wago.tools (remplaçable pour les tests).
     */
    interface BuildCatalog {

        List<String> builds(String product) throws IOException, InterruptedException;
    }

    /**
     * Source des tables d'une version (remplaçable pour les tests).
     */
    interface TableSource {

        SpellDatabase load(WagoTables tables) throws IOException, InterruptedException;
    }

    private final Path         cache;
    private final BuildCatalog catalog;
    private final TableSource  source;

    public SpellDatabaseLoader(Path cache) {

        this(cache, WagoTables::knownBuilds, SpellDatabase::load);
    }

    SpellDatabaseLoader(Path cache, BuildCatalog catalog, TableSource source) {

        this.cache = cache;
        this.catalog = catalog;
        this.source = source;
    }

    /**
     * @return la table des sorts et la version effectivement chargée
     */
    public Loaded load(GameInstall install) throws IOException, InterruptedException {

        IOException failure;
        try {
            return complete(install);
        }
        catch (IOException e) {
            failure = e;
            logger.warn("Tables du jeu {} {} indisponibles : {}", install.product(), install.build(), e.getMessage());
        }

        if (failure instanceof WagoTables.MissingTableException) {
            try {
                Optional<String> nearest = nearest(install.build(), catalog.builds(install.product()));
                if (nearest.isPresent() && !nearest.get().equals(install.build())) {
                    logger.info("Repli sur la version connue la plus proche : {}", nearest.get());
                    return complete(install.withBuild(nearest.get()));
                }
            }
            catch (IOException e) {
                logger.warn("Repli sur une version proche impossible : {}", e.getMessage());
            }
        }

        Optional<String> cached = latestCached(install);
        if (cached.isPresent()) {
            logger.info("Repli sur la version en cache : {}", cached.get());
            return new Loaded(source.load(new WagoTables(cache, install.withBuild(cached.get()))), install.withBuild(cached.get()));
        }
        throw failure;
    }

    /**
     * @param database table des sorts
     * @param install  version effectivement chargée (celle du client, ou un repli)
     */
    public record Loaded(SpellDatabase database, GameInstall install) {}

    private Loaded complete(GameInstall install) throws IOException, InterruptedException {

        WagoTables    tables   = new WagoTables(cache, install);
        SpellDatabase database = source.load(tables);
        Files.createDirectories(tables.folder());
        if (!Files.exists(tables.folder().resolve(COMPLETE))) {Files.createFile(tables.folder().resolve(COMPLETE));}
        pruneOtherBuilds(install);
        return new Loaded(database, install);
    }

    /**
     * Supprime du cache les autres versions du même produit.
     */
    private void pruneOtherBuilds(GameInstall install) throws IOException {

        Path product = cache.resolve(install.product());
        try (DirectoryStream<Path> builds = Files.newDirectoryStream(product, Files::isDirectory)) {
            for (Path build : builds) {
                if (build.getFileName().toString().equals(install.build())) {continue;}
                logger.info("Suppression du cache de l'ancienne version {} {}", install.product(), build.getFileName());
                deleteRecursively(build);
            }
        }
    }

    private static void deleteRecursively(Path folder) throws IOException {

        try (Stream<Path> paths = Files.walk(folder)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {Files.deleteIfExists(path);}
        }
    }

    /**
     * Version la plus récente du produit entièrement en cache, pour cette langue.
     */
    Optional<String> latestCached(GameInstall install) throws IOException {

        Path product = cache.resolve(install.product());
        if (!Files.isDirectory(product)) {return Optional.empty();}
        List<String> builds = new ArrayList<>();
        try (DirectoryStream<Path> folders = Files.newDirectoryStream(product, Files::isDirectory)) {
            for (Path build : folders) {
                if (Files.exists(build.resolve(install.locale()).resolve(COMPLETE))) {builds.add(build.getFileName().toString());}
            }
        }
        return builds.stream().max(SpellDatabaseLoader::compareVersions);
    }

    /**
     * Version la plus proche : la plus récente qui ne dépasse pas la nôtre, sinon la plus ancienne qui la dépasse.
     */
    static Optional<String> nearest(String build, List<String> known) {

        Optional<String> below = known.stream().filter(v -> compareVersions(v, build) <= 0).max(SpellDatabaseLoader::compareVersions);
        return below.isPresent() ? below : known.stream().min(SpellDatabaseLoader::compareVersions);
    }

    static int compareVersions(String a, String b) {

        int[] left  = Arrays.stream(a.split("\\.")).mapToInt(Integer::parseInt).toArray();
        int[] right = Arrays.stream(b.split("\\.")).mapToInt(Integer::parseInt).toArray();
        return Arrays.compare(left, right);
    }
}
