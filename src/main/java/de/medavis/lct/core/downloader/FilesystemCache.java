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
package de.medavis.lct.core.downloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;

class FilesystemCache implements Cache {

    private final Path path;

    public FilesystemCache(Path path) {
        this.path = path;
    }

    @Override
    public Optional<File> getCachedFile(String name) throws IOException {
        if (!path.toFile().exists()) {
            return Optional.empty();
        }

        try (Stream<Path> files = Files.walk(path, 1)) {
            return files.map(Path::toFile)
                    .filter(file -> FilenameUtils.removeExtension(file.getName()).equalsIgnoreCase(name))
                    .findFirst();
        }
    }

    @Override
    public void addCachedFile(String name, String ext, byte[] content) throws IOException {
        Files.createDirectories(path);
        Files.write(path.resolve(name + ext), content);
    }
}
