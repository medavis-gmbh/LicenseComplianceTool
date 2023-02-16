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

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.permanentRedirect;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WireMockTest
@ExtendWith(MockitoExtension.class)
class LicenseFileDownloaderTest {

    private static final String INITIAL_URL = "/download";
    private static final String REDIRECTED_URL = "/redirected";
    private static final String LICENSE = "downloaded";
    private static final String DOWNLOAD_CONTENT = "You should download me.";

    private String baseUrl;

    @Mock
    private LicenseFileHandler handlerMock;

    private final LicenseFileDownloader fileDownloader = new LicenseFileDownloader();

    @BeforeEach
    void beforeEach(WireMockRuntimeInfo wiremock) {
        baseUrl = wiremock.getHttpBaseUrl();
    }

    @Test
    void shouldCreateEmptyFileOnEmptyContent() throws IOException {
        stubFor(get(INITIAL_URL).willReturn(ok()));

        download();

        verifyDownload(LICENSE, "", "");
    }

    @Test
    void shouldThrowExceptionOnServerError() {
        stubFor(get(INITIAL_URL).willReturn(serverError()));

        assertThatThrownBy(this::download).isInstanceOf(IOException.class);
    }

    @Test
    void shouldDownloadWithoutExtensionOnEmptyContentType() throws IOException {
        stubFor(get(INITIAL_URL).willReturn(okWithLength(DOWNLOAD_CONTENT)));

        download();

        verifyDownload(LICENSE, "", DOWNLOAD_CONTENT);
    }

    @Test
    void shouldDownloadWithHtmlExtensionOnHtmlContentType() throws IOException {
        stubFor(get(INITIAL_URL).willReturn(okWithLengthAndType(DOWNLOAD_CONTENT, ContentType.TEXT_HTML.getMimeType())));

        download();

        verifyDownload(LICENSE, ".html", DOWNLOAD_CONTENT);
    }

    @Test
    void shouldDownloadWithTxtExtensionOnTextContentType() throws IOException {
        stubFor(get(INITIAL_URL).willReturn(okWithLengthAndType(DOWNLOAD_CONTENT, ContentType.TEXT_PLAIN.getMimeType())));

        download();

        verifyDownload(LICENSE, ".txt", DOWNLOAD_CONTENT);
    }

    @Test
    void shouldDownloadWithTxtExtensionOnUnexpectedContentType() throws IOException {
        stubFor(get(INITIAL_URL).willReturn(okWithLengthAndType(DOWNLOAD_CONTENT, ContentType.APPLICATION_OCTET_STREAM.getMimeType())));

        download();

        verifyDownload(LICENSE, "", DOWNLOAD_CONTENT);
    }

    @Test
    void shouldFollowRedirectWhileDownloading() throws IOException {
        stubFor(get(INITIAL_URL).willReturn(permanentRedirect(REDIRECTED_URL)));
        stubFor(get(REDIRECTED_URL).willReturn(okWithLength(DOWNLOAD_CONTENT)));

        download();

        verifyDownload(LICENSE, "", DOWNLOAD_CONTENT);
    }

    @Test
    void shouldNotDownloadCachedLicenseFile() throws IOException {
        when(handlerMock.isCached(LICENSE)).thenReturn(true);

        download();

        verify(handlerMock).copyFromCache(LICENSE);
        verify(handlerMock, never()).save(any(), any(), any());
    }

    private void download() throws IOException {
        fileDownloader.downloadToFile(baseUrl + INITIAL_URL, LICENSE, handlerMock);
    }

    private void verifyDownload(String license, String extension, String expectedContent) throws IOException {
        verify(handlerMock).save(license, extension, expectedContent.getBytes(StandardCharsets.UTF_8));
    }

    private ResponseDefinitionBuilder okWithLength(String downloadContent) {
        return ok(downloadContent).withHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(downloadContent.length()));
    }

    private ResponseDefinitionBuilder okWithLengthAndType(String downloadContent, String contentType) {
        return okForContentType(contentType, downloadContent).withHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(downloadContent.length()));
    }

}
