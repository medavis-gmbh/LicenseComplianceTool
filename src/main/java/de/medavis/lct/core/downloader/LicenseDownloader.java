package de.medavis.lct.core.downloader;

import java.io.PrintStream;
import java.nio.file.Path;

import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.list.ComponentLister;

public class LicenseDownloader {

    private final ComponentLister componentLister;
    private final String cachePath;

    public LicenseDownloader(ComponentLister componentLister, Configuration configuration) {
        this.componentLister = componentLister;
        this.cachePath = configuration.getLicenseCachePath();
    }

    public void download(PrintStream logger, Path inputPath, Path outputPath) {

    }

}
