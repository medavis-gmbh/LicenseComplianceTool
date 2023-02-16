package de.medavis.lct.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import de.medavis.lct.core.downloader.LicenseFileHandler;

class FilesystemLicenseFileHandler implements LicenseFileHandler {

    private final Path target;

    public FilesystemLicenseFileHandler(Path target) {
        this.target = target;
        final File targetFile = target.toFile();
        if (!targetFile.exists()) {
            if (!targetFile.mkdirs()) {
                throw new IllegalArgumentException(target + " could not be created.");
            }
        } else {
            if (targetFile.isFile()) {
                throw new IllegalArgumentException(target + " must not point to a file.");
            }
        }
    }

    @Override
    public boolean isCached(String license) throws IOException {
        return getCachedFile(license).isPresent();
    }

    @Override
    public void save(String license, String extension, byte[] content) throws IOException {
        Files.write(target.resolve(license + extension), content);
    }

    @Override
    public void copyFromCache(String license) {
        // Nothing to do - the cache is identical to the output file
    }

    private Optional<Path> getCachedFile(String license) throws IOException {
        try (Stream<Path> cached = Files.find(target, 1, (name, attributes) -> name.startsWith(license) && attributes.isRegularFile())) {
            return cached.findFirst();
        }
    }
}
