package de.medavis.lct.core.downloader;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.base.Joiner;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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

    @Mock
    private ComponentLister componentLister;
    private LicenseDownloader downloader;

    @BeforeEach
    void setup(@Mock Configuration configuration, @TempDir Path cachePath) {
        when(configuration.getLicenseCachePath()).thenReturn(cachePath.toString());
        downloader = new LicenseDownloader(componentLister, configuration);
    }

    @Test
    void shouldDownloadAllLicensesFromAllComponents(WireMockRuntimeInfo wiremock, @TempDir Path outputPath) throws MalformedURLException {
        setup(
                component(
                        license("A", true, true, wiremock),
                        license("B", true, true, wiremock)),
                component(license("C", true, true, wiremock))
        );

        invokeDownload(outputPath);

        verifyLicenses(outputPath, "A", "B", "C");
        verifyDownloaded(DOWNLOAD_URL, "A", "B", "C");
    }

    @Test
    void shouldDownloadSameLicenseOnlyOnce() {

    }

    @Test
    void shouldUseViewUrlIfDownloadUrlIsNotSet() {

    }

    @Test
    void shouldNotDownloadLicenseIfNoUrlIsSet() {

    }

    @Test
    void shouldNotDownloadLicensesIfItExistsInCache() {

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

    private License license(String name, boolean hasViewUrl, boolean hasDownloadUrl, WireMockRuntimeInfo wiremock) {
        String viewUrl = getAndStubUrl(hasViewUrl, VIEW_URL, name, wiremock);
        String downloadUrl = getAndStubUrl(hasDownloadUrl, DOWNLOAD_URL, name, wiremock);
        return new License(name, viewUrl, downloadUrl);
    }

    private String getAndStubUrl(boolean hasUrl, String prefix, String name, WireMockRuntimeInfo wiremock) {
        String url = null;
        if (hasUrl) {
            String relativeUrl = createUrl(prefix, name);
            if (stubbings.add(relativeUrl)) {
                stubFor(get(relativeUrl).willReturn(aResponse().withBody(name)));
                url = wiremock.getHttpBaseUrl() + relativeUrl;
            }
        }
        return url;
    }

    private String createUrl(String... parts) {
        return "/" + Joiner.on("/").join(parts);
    }

    private void invokeDownload(Path outputPath) throws MalformedURLException {
        downloader.download(System.out, INPUT, outputPath);
    }

    private void verifyLicenses(Path outputPath, String... licenses) {
        assertSoftly(softly -> Stream.of(licenses).forEach(license ->
                assertThat(outputPath.resolve(license))
                        .exists()
                        .hasContent(license)));
    }

    private void verifyDownloaded(String prefix, String... licenses) {
        assertSoftly(softly -> Stream.of(licenses).forEach(license ->
                verify(1, getRequestedFor(urlEqualTo(createUrl(prefix, license))))));
    }
}