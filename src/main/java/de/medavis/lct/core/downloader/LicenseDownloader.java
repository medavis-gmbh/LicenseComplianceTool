package de.medavis.lct.core.downloader;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import joptsimple.internal.Strings;
import org.apache.commons.io.FileUtils;

import static com.google.common.base.MoreObjects.firstNonNull;

import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.license.License;
import de.medavis.lct.core.list.ComponentData;
import de.medavis.lct.core.list.ComponentLister;

public class LicenseDownloader {

    private static final int TIMEOUT_MILLIS = 5000;
    private final ComponentLister componentLister;
    private final String cachePath;

    public LicenseDownloader(ComponentLister componentLister, Configuration configuration) {
        this.componentLister = componentLister;
        this.cachePath = configuration.getLicenseCachePath();
    }

    // TODO Replace logger with a wrapper for both output and error
    public void download(PrintStream logger, Path inputPath, Path outputPath) throws MalformedURLException {
        logger.printf("Downloading licenses from components in %s to %s.%n", inputPath, outputPath);
        final List<ComponentData> components = componentLister.listComponents(inputPath.toUri().toURL());
        Set<License> licenses = components.stream()
                .map(ComponentData::licenses)
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, String> downloadUrls = licenses.stream()
                .filter(license -> !Strings.isNullOrEmpty(license.downloadUrl()) || !Strings.isNullOrEmpty(license.url()))
                .collect(Collectors.toMap(License::name, license -> firstNonNull(license.downloadUrl(), license.url())));
        logger.printf("Will download %d licenses.%n", licenses.size());
        downloadUrls.forEach((name, url) -> download(name, url, logger, outputPath));
    }

    private void download(String name, String source, PrintStream logger, Path outputPath) {
        try {
            File outputFile = outputPath.resolve(name).toFile();
            logger.printf("Downloading from %s into %s... ", source, outputFile);
            URL sourceUrl = new URL(source);
            FileUtils.copyURLToFile(sourceUrl, outputFile, TIMEOUT_MILLIS, TIMEOUT_MILLIS);
            logger.printf("Done.%n");
        } catch (IOException e) {
            // TODO Log error as error
            logger.printf("ERROR: %s.%n", e.getMessage());
        }
    }

}
