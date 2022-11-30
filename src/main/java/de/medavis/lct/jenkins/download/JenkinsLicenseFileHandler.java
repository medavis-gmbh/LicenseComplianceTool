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

// TODO Add tests
class JenkinsLicenseFileHandler implements LicenseFileHandler {

    private final FilePath workspace;
    private final String outputPath;
    private final String cachePath;

    public JenkinsLicenseFileHandler(FilePath workspace, String outputPath, String cachePath) {
        this.workspace = workspace;
        this.outputPath = outputPath;
        this.cachePath = cachePath;
    }

    @Override
    public boolean isCached(String license) throws IOException {
        return getCachedFile(license).isPresent();
    }

    @Override
    public void save(String license, String extension, byte[] content) throws IOException {
        // TODO Store file in cache
        try (OutputStream outputStream = workspace.child(outputPath).child(license + extension).write()) {
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
                cachedFile.get().child(license).copyTo(workspace.child(outputPath));
            }
        } catch (InterruptedException e) {
            throw rethrowAsIOException(e);
        }
    }

    private Optional<FilePath> getCachedFile(String license) throws IOException {
        try {
            if (!workspace.child(cachePath).exists()) {
                return Optional.empty();
            }

            FilePath[] matches = workspace.child(cachePath).list(license + "*");
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
