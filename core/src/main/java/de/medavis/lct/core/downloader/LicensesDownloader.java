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

import com.google.common.base.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.MoreObjects.firstNonNull;

import de.medavis.lct.core.UserLogger;
import de.medavis.lct.core.downloader.LicenseFileDownloader.Result;
import de.medavis.lct.core.license.License;
import de.medavis.lct.core.list.ComponentData;
import de.medavis.lct.core.list.ComponentLister;

public class LicensesDownloader {

    private static final Logger log = LoggerFactory.getLogger(LicensesDownloader.class);

    private final ComponentLister componentLister;
    private final LicenseFileDownloader fileDownloader;

    public LicensesDownloader(ComponentLister componentLister, LicenseFileDownloader fileDownloader) {
        this.componentLister = componentLister;
        this.fileDownloader = fileDownloader;
    }

    public void download(UserLogger userLogger, InputStream inputStream, LicenseFileHandler licenseFileHandler) {
        final List<ComponentData> components = componentLister.listComponents(inputStream);
        Set<License> licenses = components.stream()
                .map(ComponentData::getLicenses)
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));


        Map<String, String> downloadUrls = licenses.stream()
                .filter(license -> !Strings.isNullOrEmpty(license.getDownloadUrl()) || !Strings.isNullOrEmpty(license.getUrl()))
                .collect(Collectors.toMap(License::getName, license -> firstNonNull(license.getDownloadUrl(), license.getUrl())));
        userLogger.info("Will download %d licenses.%n", downloadUrls.size());

        int index = 1;
        for (Entry<String, String> entry : downloadUrls.entrySet()) {
            String name = entry.getKey();
            String url = entry.getValue();
            downloadLicense(name, url, userLogger, licenseFileHandler, index, downloadUrls.size());
            index++;
        }
    }

    private void downloadLicense(String licenseName, String source, UserLogger userLogger, LicenseFileHandler licenseFileHandler, int index, int size) {
        try {
            userLogger.info("(%d/%d) Downloading license %s from %s... ", index, size, licenseName, source);
            var result = fileDownloader.downloadToFile(source, licenseName, licenseFileHandler);
            userLogger.info("%s.%n", result == Result.DOWNLOADED ? "Downloaded" : "Copied from cache");
        } catch (IOException e) {
            log.error(String.format("Could not download license file %s from %s.", licenseName, source), e);
            userLogger.error("%s - %s.%n", e.getClass(), e.getMessage());
        }
    }

}
