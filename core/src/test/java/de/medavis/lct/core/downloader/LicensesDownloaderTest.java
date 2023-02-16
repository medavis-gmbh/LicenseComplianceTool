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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mock.Strictness;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import de.medavis.lct.core.UserLogger;
import de.medavis.lct.core.license.License;
import de.medavis.lct.core.list.ComponentData;
import de.medavis.lct.core.list.ComponentLister;

@ExtendWith(MockitoExtension.class)
class LicensesDownloaderTest {

    private static final String VIEW_URL = "view";
    private static final String DOWNLOAD_URL = "download";
    private final static String BASE_URL = "http://my-host";

    @Mock
    private ComponentLister componentLister;
    @Mock(strictness = Strictness.LENIENT)
    private LicenseFileDownloader licenseFileDownloader;
    @Mock
    private UserLogger userLogger;

    private LicensesDownloader underTest;

    private Map<String, byte[]> downloads = new LinkedHashMap<>();

    @Test
    void shouldDownloadAllLicensesFromAllComponents() throws IOException {
        setup(
                component(
                        license("A", true, true),
                        license("B", true, true)),
                component(license("C", true, true))
        );

        invokeDownload();

        verifyDownloaded(DOWNLOAD_URL, "A", "B", "C");
    }

    @Test
    void shouldDownloadSameLicenseOnlyOnce() throws IOException {
        setup(
                component(license("A", true, true)),
                component(license("A", true, true))
        );

        invokeDownload();

        verifyDownloaded(DOWNLOAD_URL, "A");
    }

    @Test
    void shouldUseViewUrlIfDownloadUrlIsNotSet() throws IOException {
        setup(component(license("A", true, false)));

        invokeDownload();

        verifyDownloaded(VIEW_URL, "A");
    }

    @Test
    void shouldNotDownloadLicenseIfNoUrlIsSet() throws IOException {
        setup(component(license("A", false, false)));

        invokeDownload();

        verifyNothingDownloaded();
    }

    private void setup(ComponentData... components) {
        when(componentLister.listComponents(any())).thenReturn(Arrays.asList(components));
        underTest = new LicensesDownloader(componentLister, licenseFileDownloader);
    }

    private ComponentData component(License... licenses) {
        return new ComponentData(
                RandomStringUtils.randomAlphabetic(6),
                RandomStringUtils.randomAlphabetic(6),
                RandomStringUtils.randomAlphabetic(6),
                ImmutableSet.copyOf(licenses),
                Collections.emptySet());
    }

    private License license(String name, boolean hasViewUrl, boolean hasDownloadUrl) {
        String viewUrl = stubDownload(hasViewUrl, VIEW_URL, name);
        String downloadUrl = stubDownload(hasDownloadUrl, DOWNLOAD_URL, name);
        return new License(name, viewUrl, downloadUrl);
    }

    private String stubDownload(boolean hasUrl, String prefix, String licenseName) {
        if (hasUrl) {
            String relativeUrl = createUrl(prefix, licenseName);
            return BASE_URL + relativeUrl;
        } else {
            return null;
        }
    }

    private String createUrl(String... parts) {
        return "/" + Joiner.on("/").join(parts);
    }

    private void invokeDownload() {
        underTest.download(userLogger, new ByteArrayInputStream(new byte[0]), Mockito.mock(LicenseFileHandler.class));
    }

    private void verifyDownloaded(String prefix, String... licenses) throws IOException {
        for (String license : licenses) {
            Mockito.verify(licenseFileDownloader).downloadToFile(eq(BASE_URL + createUrl(prefix, license)), any(), any());
        }
    }

    private void verifyNothingDownloaded() {
        Mockito.verifyNoInteractions(licenseFileDownloader);
    }

}
