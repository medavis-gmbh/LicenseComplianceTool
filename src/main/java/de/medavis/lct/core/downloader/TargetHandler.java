package de.medavis.lct.core.downloader;

import java.io.IOException;

@FunctionalInterface
public interface TargetHandler {

    void handle(String name, String extension, byte[] content) throws IOException;

}
