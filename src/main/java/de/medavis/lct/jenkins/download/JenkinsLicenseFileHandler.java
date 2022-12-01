/*-
 * #%L
 * License Compliance Tool
 * %%
 * Copyright (C) 2022 medavis GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package de.medavis.lct.jenkins.download;

import hudson.FilePath;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;

import de.medavis.lct.core.downloader.LicenseFileHandler;

class JenkinsLicenseFileHandler implements LicenseFileHandler {

    private static final String CACHE_PATH = ".lct/cache/licenses";

    private final FilePath workspace;
    private final String outputPath;

    public JenkinsLicenseFileHandler(FilePath workspace, String outputPath) {
        this.workspace = workspace;
        this.outputPath = outputPath;
    }

    @Override
    public boolean isCached(String license) throws IOException {
        return getCachedFile(license).isPresent();
    }

    @Override
    public void save(String license, String extension, byte[] content) throws IOException {
        writeFile(outputPath, license, extension, content);
        writeFile(CACHE_PATH, license, extension, content);
    }

    private void writeFile(String targetPath, String license, String extension, byte[] content) throws IOException {
        final FilePath target = workspace.child(targetPath);
        try (OutputStream outputStream = target.child(license + extension).write()) {
            outputStream.write(content);
        } catch (InterruptedException e) {
            throw rethrowAsIOException(e);
        }
    }

    @Override
    public void copyFromCache(String license) throws IOException {
        try {
            Optional<FilePath> cachedFile = getCachedFile(license);
            if (cachedFile.isPresent()) {
                cachedFile.get().copyTo(workspace.child(outputPath).child(cachedFile.get().getName()));
            }
        } catch (InterruptedException e) {
            throw rethrowAsIOException(e);
        }
    }

    private Optional<FilePath> getCachedFile(String license) throws IOException {
        try {
            if (!workspace.child(CACHE_PATH).exists()) {
                return Optional.empty();
            }

            FilePath[] matches = workspace.child(CACHE_PATH).list(license + "*");
            return Stream.of(matches)
                    .filter(file -> FilenameUtils.removeExtension(file.getName()).equalsIgnoreCase(license))
                    .findFirst();
        } catch (InterruptedException e) {
            throw rethrowAsIOException(e);
        }
    }

    private IOException rethrowAsIOException(InterruptedException e) {
        Thread.currentThread().interrupt();
        return new IOException("Thread has been interrupted.", e);
    }
}
