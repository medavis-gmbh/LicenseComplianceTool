package de.medavis.lct.core.downloader;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.base.Joiner;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.license.License;
import de.medavis.lct.core.list.ComponentData;
import de.medavis.lct.core.list.ComponentLister;

@WireMockTest
@ExtendWith(MockitoExtension.class)
class LicenseDownloaderTest {

    private static final String VIEW_URL = "view";
    private static final String DOWNLOAD_URL = "download";
    private static final Path INPUT = Paths.get("DOES_NOT_MATTER");

    private final Set<String> stubbings = new HashSet<>();

    @TempDir
    private Path outputPath;
    @TempDir
    private Path cachePath;
    @Mock
    private ComponentLister componentLister;
    private LicenseDownloader downloader;
    private String baseUrl;

    @BeforeEach
    void setup(@Mock Configuration configuration, WireMockRuntimeInfo wiremock) {
        when(configuration.getLicenseCachePath()).thenReturn(cachePath);
        downloader = new LicenseDownloader(componentLister, configuration);
        baseUrl = wiremock.getHttpBaseUrl();
    }

    @Test
    void shouldDownloadAllLicensesFromAllComponents(WireMockRuntimeInfo wiremock) throws MalformedURLException {
        setup(
                component(
                        license("A", true, true),
                        license("B", true, true)),
                component(license("C", true, true))
        );

        invokeDownload();

        verifyLicenses("A", "B", "C");
        verifyDownloaded(DOWNLOAD_URL, "A", "B", "C");
    }

    @Test
    void shouldDownloadSameLicenseOnlyOnce(WireMockRuntimeInfo wiremock) throws MalformedURLException {
        setup(
                component(license("A", true, true)),
                component(license("A", true, true))
        );

        invokeDownload();

        verifyLicenses("A");
        verifyDownloaded(DOWNLOAD_URL, "A");
    }

    @Test
    void shouldUseViewUrlIfDownloadUrlIsNotSet() throws MalformedURLException {
        setup(component(license("A", true, false)));

        invokeDownload();

        verifyLicenses("A");
        verifyDownloaded(VIEW_URL, "A");

    }

    @Test
    void shouldNotDownloadLicenseIfNoUrlIsSet() throws MalformedURLException {
        setup(component(license("A", false, false)));

        invokeDownload();

        verifyEmptyLicenses(outputPath);
        verifyNothingDownloaded();

    }

    @Test
    void shouldPopulateCacheAfterDownload() throws IOException {
        setup(component(license("A", true, true)));

        invokeDownload();

        verifyCache("A");
    }

    @Test
    void shouldNotDownloadLicensesIfItExistsInCache() throws IOException {
        initializeCache("A");
        setup(component(license("A", true, true)));

        invokeDownload();

        verifyLicenses("A");
        verifyNothingDownloaded();
    }

    private void initializeCache(String licenseName) throws IOException {
        Files.write(cachePath.resolve(licenseName), Collections.singletonList(licenseName));
    }

    private void setup(ComponentData... components) {
        when(componentLister.listComponents(any())).thenReturn(Arrays.asList(components));
    }

    private ComponentData component(License... licenses) {
        return new ComponentData(
                RandomStringUtils.randomAlphabetic(6),
                RandomStringUtils.randomAlphabetic(6),
                RandomStringUtils.randomAlphabetic(6),
                Stream.of(licenses).collect(Collectors.toSet()));
    }

    private License license(String name, boolean hasViewUrl, boolean hasDownloadUrl) {
        String viewUrl = getAndStubUrl(hasViewUrl, VIEW_URL, name);
        String downloadUrl = getAndStubUrl(hasDownloadUrl, DOWNLOAD_URL, name);
        return new License(name, viewUrl, downloadUrl);
    }

    private String getAndStubUrl(boolean hasUrl, String prefix, String name) {
        String url = null;
        if (hasUrl) {
            String relativeUrl = createUrl(prefix, name);
            if (stubbings.add(relativeUrl)) {
                stubFor(get(relativeUrl).willReturn(aResponse().withBody(name)));
                url = baseUrl + relativeUrl;
            }
        }
        return url;
    }

    private String createUrl(String... parts) {
        return "/" + Joiner.on("/").join(parts);
    }

    private void invokeDownload() throws MalformedURLException {
        downloader.download(System.out, INPUT, outputPath);
    }

    private void verifyLicenses(String... licenses) {
        assertSoftly(softly -> Stream.of(licenses).forEach(license ->
                assertThat(outputPath.resolve(license))
                        .exists()
                        .hasContent(license)));
    }

    private void verifyEmptyLicenses(Path outputPath) {
        assertThat(outputPath).isEmptyDirectory();
    }

    private void verifyDownloaded(String prefix, String... licenses) {
        Stream.of(licenses).forEach(license ->
                verify(1, getRequestedFor(urlEqualTo(createUrl(prefix, license)))));
    }

    private void verifyNothingDownloaded() {
        verify(0, getRequestedFor(anyUrl()));
    }

    private void verifyCache(String... licenses) {
        assertSoftly(softly -> Stream.of(licenses).forEach(license ->
                assertThat(cachePath.resolve(license))
                        .exists()
                        .hasContent(license)));
    }
}