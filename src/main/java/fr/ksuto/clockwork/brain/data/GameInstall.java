package fr.ksuto.clockwork.brain.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version et langue du client installé, pour télécharger les tables du jeu correspondantes.
 *
 * @param build  version complète, ex. 12.1.0.69933 (colonne Version de .build.info, produit « wow »)
 * @param locale langue des textes, ex. frFR (textLocale de WTF/Config.wtf)
 */
public record GameInstall(String build, String locale) {

    private static final Pattern TEXT_LOCALE = Pattern.compile("SET textLocale \"(\\w+)\"");

    /**
     * @param retail dossier _retail_ de WoW ; .build.info est dans son dossier parent
     */
    public static Optional<GameInstall> detect(Path retail) throws IOException {

        Path buildInfo = retail.toAbsolutePath().getParent().resolve(".build.info");
        Path config    = retail.resolve("WTF").resolve("Config.wtf");
        if (!Files.isRegularFile(buildInfo)) {return Optional.empty();}

        Optional<String> build = readBuild(Files.readAllLines(buildInfo, StandardCharsets.UTF_8));
        if (build.isEmpty()) {return Optional.empty();}

        String locale = "enUS";
        if (Files.isRegularFile(config)) {
            Matcher matcher = TEXT_LOCALE.matcher(Files.readString(config, StandardCharsets.UTF_8));
            if (matcher.find()) {locale = matcher.group(1);}
        }
        return Optional.of(new GameInstall(build.get(), locale));
    }

    /**
     * .build.info : en-tête « Nom!TYPE:taille|… », puis une ligne par produit installé (wow, wow_classic…).
     */
    static Optional<String> readBuild(List<String> lines) {

        if (lines.isEmpty()) {return Optional.empty();}
        List<String> header  = Arrays.stream(lines.getFirst().split("\\|")).map(column -> column.split("!")[0]).toList();
        int          version = header.indexOf("Version");
        int          product = header.indexOf("Product");
        int          active  = header.indexOf("Active");
        if (version < 0) {return Optional.empty();}

        for (String line : lines.subList(1, lines.size())) {
            String[] values = line.split("\\|", -1);
            if (values.length <= version) {continue;}
            if (product >= 0 && product < values.length && !values[product].equals("wow")) {continue;}
            if (active >= 0 && active < values.length && values[active].equals("0")) {continue;}
            return Optional.of(values[version]);
        }
        return Optional.empty();
    }
}
