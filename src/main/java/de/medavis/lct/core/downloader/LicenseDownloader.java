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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import joptsimple.internal.Strings;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.UserLogger;
import de.medavis.lct.core.license.License;
import de.medavis.lct.core.list.ComponentData;
import de.medavis.lct.core.list.ComponentLister;

public class LicenseDownloader {

    private final ComponentLister componentLister;
    private final Cache cache;
    private final FileDownloader fileDownloader;

    public LicenseDownloader(ComponentLister componentLister, Configuration configuration, FileDownloader fileDownloader) {
        this.componentLister = componentLister;
        this.fileDownloader = fileDownloader;
        this.cache = configuration.getLicenseCachePathOptional()
                .map(FilesystemCache::new)
                .map(Cache.class::cast)
                .orElseGet(CacheDisabled::new);
    }

    public void download(UserLogger userLogger, Path inputPath, Path outputPath) throws IOException {
        userLogger.info("Downloading licenses from components in %s to %s.%n", inputPath, outputPath);
        final List<ComponentData> components = componentLister.listComponents(inputPath.toUri().toURL());
        Set<License> licenses = components.stream()
                .map(ComponentData::licenses)
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, File> cachedLicenses = new LinkedHashMap<>();
        for (License license : licenses) {
            Optional<File> cachedLicenseFile = cache.getCachedFile(license.name());
            cachedLicenseFile.ifPresent(file -> cachedLicenses.put(license.name(), file));
        }
        userLogger.info("Using %d licenses from cache.%n", cachedLicenses.size());
        cachedLicenses.forEach((name, file) -> copyFromCache(name, file, userLogger, outputPath));

        Map<String, String> downloadUrls = licenses.stream()
                .filter(license -> !cachedLicenses.containsKey(license.name()))
                .filter(license -> !Strings.isNullOrEmpty(license.downloadUrl()) || !Strings.isNullOrEmpty(license.url()))
                .collect(Collectors.toMap(License::name, license -> firstNonNull(license.downloadUrl(), license.url())));
        userLogger.info("Will download %d licenses.%n", downloadUrls.size());

        int index = 1;
        for (Entry<String, String> entry : downloadUrls.entrySet()) {
            String name = entry.getKey();
            String url = entry.getValue();
            downloadFile(name, url, userLogger, outputPath, index, downloadUrls.size());
            index++;
        }
    }

    private void copyFromCache(String licenseName, File cachedFile, UserLogger userLogger, Path outputPath) {
        Path outputFile = outputPath.resolve(licenseName);
        try {
            Files.copy(cachedFile.toPath(), outputFile, REPLACE_EXISTING);
        } catch (IOException e) {
            userLogger.error("Could not copy license file from cache: %s.%n", e.getMessage());
        }
    }

    private void downloadFile(String licenseName, String source, UserLogger userLogger, Path outputPath, int index, int size) {
        try {
            userLogger.info("(%d/%d) Downloading license %s from %s... ", index, size, licenseName, source);
            File outputFile = fileDownloader.downloadToFile(source, outputPath, licenseName);
            cache.addCachedFile(licenseName, outputFile);
            userLogger.info("Done.%n");
        } catch (IOException e) {
            userLogger.error("Could not download license file: %s.%n", e.getMessage());
        }
    }

}
