package fr.ksuto.clockwork.brain.data;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tables du jeu (DB2) exportées en CSV par wago.tools, téléchargées une fois par produit, version et langue, puis
 * gardées en cache : {@code <cache>/<produit>/<build>/<langue>/<Table>.csv}.
 */
public final class WagoTables {

    private static final Logger logger = LoggerFactory.getLogger(WagoTables.class);

    private static final String  TABLE_URL  = "https://wago.tools/db2/%s/csv?build=%s&locale=%s";
    private static final String  BUILDS_URL = "https://wago.tools/api/builds";
    private static final Pattern BUILD      = Pattern.compile("\"product\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"version\"\\s*:\\s*\"([\\d.]+)\"");

    private static final HttpClient HTTP = HttpClient.newBuilder()
                                                     .connectTimeout(Duration.ofSeconds(20))
                                                     .followRedirects(HttpClient.Redirect.NORMAL)
                                                     .build();

    /**
     * La table n'existe pas pour cette version (ou la version est inconnue de wago.tools) : réponse 4xx.
     */
    public static final class MissingTableException extends IOException {

        MissingTableException(String message) {

            super(message);
        }
    }

    private final Path        folder;
    private final GameInstall install;

    public WagoTables(Path cache, GameInstall install) {

        this.folder = folder(cache, install);
        this.install = install;
    }

    static Path folder(Path cache, GameInstall install) {

        return cache.resolve(install.product()).resolve(install.build()).resolve(install.locale());
    }

    public GameInstall install() {

        return install;
    }

    public Path folder() {

        return folder;
    }

    /**
     * @return le fichier CSV de la table, téléchargé s'il n'est pas encore en cache
     * @throws MissingTableException si wago.tools ne connaît pas cette table pour cette version
     */
    public Path table(String name) throws IOException, InterruptedException {

        Path file = folder.resolve(name + ".csv");
        if (Files.isRegularFile(file)) {return file;}

        Files.createDirectories(folder);
        String url = TABLE_URL.formatted(name, install.build(), install.locale());
        logger.info("Téléchargement de la table {} ({} {} {}) depuis wago.tools", name, install.product(), install.build(), install.locale());

        Path               partial  = folder.resolve(name + ".csv.part");
        HttpRequest        request  = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(2)).build();
        HttpResponse<Path> response = HTTP.send(request, HttpResponse.BodyHandlers.ofFile(partial));
        if (response.statusCode() != 200) {
            Files.deleteIfExists(partial);
            String message = "wago.tools a répondu " + response.statusCode() + " pour " + url;
            if (response.statusCode() >= 400 && response.statusCode() < 500) {throw new MissingTableException(message);}
            throw new IOException(message);
        }
        // Le fichier n'apparaît sous son nom qu'une fois complet : un téléchargement interrompu sera repris
        Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return file;
    }

    /**
     * Note une table absente de cette version (ex. talents en vanilla) : un fichier vide, pour ne pas la redemander.
     */
    public Path markAbsent(String name) throws IOException {

        Files.createDirectories(folder);
        return Files.writeString(folder.resolve(name + ".csv"), "", StandardCharsets.UTF_8);
    }

    /**
     * Versions connues de wago.tools pour un produit.
     */
    public static List<String> knownBuilds(String product) throws IOException, InterruptedException {

        HttpRequest          request  = HttpRequest.newBuilder(URI.create(BUILDS_URL)).timeout(Duration.ofSeconds(30)).build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {throw new IOException("wago.tools a répondu " + response.statusCode() + " pour " + BUILDS_URL);}
        return buildsOf(response.body(), product);
    }

    static List<String> buildsOf(String json, String product) {

        List<String> builds  = new ArrayList<>();
        Matcher      matcher = BUILD.matcher(json);
        while (matcher.find()) {
            if (matcher.group(1).equals(product)) {builds.add(matcher.group(2));}
        }
        return builds;
    }
}
