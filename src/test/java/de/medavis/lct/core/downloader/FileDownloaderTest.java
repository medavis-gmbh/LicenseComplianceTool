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
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.permanentRedirect;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
class FileDownloaderTest {

    private static final String INITIAL_URL = "/download";
    private static final String REDIRECTED_URL = "/redirected";
    private static final String TARGET_FILENAME = "downloaded";
    private static final String DOWNLOAD_CONTENT = "You should download me.";

    private String baseUrl;

    private final FileDownloader fileDownloader = new FileDownloader();

    @BeforeEach
    void beforeEach(WireMockRuntimeInfo wiremock) {
        baseUrl = wiremock.getHttpBaseUrl();
    }

    @Test
    void shouldCreateEmptyFileOnEmptyContent() throws IOException {
        stubFor(get(INITIAL_URL).willReturn(ok()));

        verifyDownload(TARGET_FILENAME, "");
    }

    @Test
    void shouldThrowExceptionOnServerError() throws IOException {
        stubFor(get(INITIAL_URL).willReturn(serverError()));

        assertThatThrownBy(() -> fileDownloader.downloadToFile(baseUrl + INITIAL_URL, TARGET_FILENAME, (name, content) -> {
        })).isInstanceOf(IOException.class);
    }

    @Test
    void shouldDownloadWithoutExtensionOnEmptyContentType() throws IOException {
        stubFor(get(INITIAL_URL).willReturn(okWithLength(DOWNLOAD_CONTENT)));

        verifyDownload(TARGET_FILENAME, DOWNLOAD_CONTENT);
    }

    @Test
    void shouldDownloadWithHtmlExtensionOnHtmlContentType() throws IOException {
        stubFor(get(INITIAL_URL).willReturn(okWithLengthAndType(DOWNLOAD_CONTENT, ContentType.TEXT_HTML.getMimeType())));

        verifyDownload(TARGET_FILENAME + ".html", DOWNLOAD_CONTENT);
    }

    @Test
    void shouldDownloadWithTxtExtensionOnTextContentType() throws IOException {
        stubFor(get(INITIAL_URL).willReturn(okWithLengthAndType(DOWNLOAD_CONTENT, ContentType.TEXT_PLAIN.getMimeType())));

        verifyDownload(TARGET_FILENAME + ".txt", DOWNLOAD_CONTENT);
    }

    @Test
    void shouldDownloadWithTxtExtensionOnUnexpectedContentType() throws IOException {
        stubFor(get(INITIAL_URL).willReturn(okWithLengthAndType(DOWNLOAD_CONTENT, ContentType.APPLICATION_OCTET_STREAM.getMimeType())));

        verifyDownload(TARGET_FILENAME, DOWNLOAD_CONTENT);
    }

    @Test
    void shouldFollowRedirectWhileDownloading() throws IOException {
        stubFor(get(INITIAL_URL).willReturn(permanentRedirect(REDIRECTED_URL)));
        stubFor(get(REDIRECTED_URL).willReturn(okWithLength(DOWNLOAD_CONTENT)));

        verifyDownload(TARGET_FILENAME, DOWNLOAD_CONTENT);
    }

    private void verifyDownload(String expectedName, String expectedContent) throws IOException {
        fileDownloader.downloadToFile(baseUrl + INITIAL_URL, FilenameUtils.getBaseName(expectedName), (actualName, actualContent) -> {
            assertThat(actualName).isEqualTo(expectedName);
            assertThat(actualContent).isEqualTo(expectedContent.getBytes(StandardCharsets.UTF_8));
        });
    }

    private ResponseDefinitionBuilder okWithLength(String downloadContent) {
        return ok(downloadContent).withHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(downloadContent.length()));
    }

    private ResponseDefinitionBuilder okWithLengthAndType(String downloadContent, String contentType) {
        return okForContentType(contentType, downloadContent).withHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(downloadContent.length()));
    }

}
