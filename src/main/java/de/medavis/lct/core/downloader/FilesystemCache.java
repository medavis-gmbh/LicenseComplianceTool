package de.medavis.lct.core.downloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

class FilesystemCache {

    private final Path path;

    public FilesystemCache(Path path) {
        this.path = path;
    }

    public Optional<File> getCachedFile(String licenseName) {
        return Optional.of(path.resolve(licenseName).toFile()).filter(File::isFile);
    }

    public void addCachedFile(String licenseName, File source) throws IOException {
        Files.copy(source.toPath(), path.resolve(licenseName), REPLACE_EXISTING);
    }
}
