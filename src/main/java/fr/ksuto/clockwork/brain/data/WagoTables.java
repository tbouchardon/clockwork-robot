package fr.ksuto.clockwork.brain.data;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tables du jeu (DB2) exportées en CSV par wago.tools, téléchargées une fois par version du jeu et par langue, puis
 * gardées en cache : {@code <cache>/<build>/<locale>/<Table>.csv}.
 */
public final class WagoTables {

    private static final Logger logger = LoggerFactory.getLogger(WagoTables.class);

    private static final String URL = "https://wago.tools/db2/%s/csv?build=%s&locale=%s";

    private final Path        folder;
    private final GameInstall install;
    private final HttpClient  http = HttpClient.newBuilder()
                                               .connectTimeout(Duration.ofSeconds(20))
                                               .followRedirects(HttpClient.Redirect.NORMAL)
                                               .build();

    public WagoTables(Path cache, GameInstall install) {

        this.folder = cache.resolve(install.build()).resolve(install.locale());
        this.install = install;
    }

    /**
     * @return le fichier CSV de la table, téléchargé s'il n'est pas encore en cache
     */
    public Path table(String name) throws IOException, InterruptedException {

        Path file = folder.resolve(name + ".csv");
        if (Files.isRegularFile(file)) {return file;}

        Files.createDirectories(folder);
        String url = URL.formatted(name, install.build(), install.locale());
        logger.info("Téléchargement de la table {} ({} {}) depuis wago.tools", name, install.build(), install.locale());

        Path                 partial  = folder.resolve(name + ".csv.part");
        HttpRequest          request  = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(2)).build();
        HttpResponse<Path>   response = http.send(request, HttpResponse.BodyHandlers.ofFile(partial));
        if (response.statusCode() != 200) {
            Files.deleteIfExists(partial);
            throw new IOException("wago.tools a répondu " + response.statusCode() + " pour " + url);
        }
        // Le fichier n'apparaît sous son nom qu'une fois complet : un téléchargement interrompu sera repris
        Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return file;
    }
}
