package de.medavis.lct.core.downloader;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.license.License;
import de.medavis.lct.core.list.ComponentData;
import de.medavis.lct.core.list.ComponentLister;

@WireMockTest
@ExtendWith(MockitoExtension.class)
class LicenseDownloaderTest {

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
    void shouldDownloadAllLicensesFromAllComponents(WireMockRuntimeInfo wiremock, @TempDir Path outputPath) {
        setup(
                component(
                        license("A", true, true, wiremock),
                        license("B", true, true, wiremock)),
                component(license("C", true, true, wiremock))
        );

        downloader.download(System.out, INPUT, outputPath);
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
        String viewUrl = getAndStubUrl(hasViewUrl, "view", name, wiremock);
        String downloadUrl = getAndStubUrl(hasDownloadUrl, "download", name, wiremock);
        return new License(name, viewUrl, downloadUrl);
    }

    private String getAndStubUrl(boolean hasUrl, String prefix, String name, WireMockRuntimeInfo wiremock) {
        String url = null;
        if (hasUrl) {
            String relativeUrl = prefix + "/" + name;
            if (stubbings.add(relativeUrl)) {
                stubFor(get(relativeUrl).willReturn(aResponse().withBody(name)));
                url = wiremock.getHttpBaseUrl() + "/" + relativeUrl;
            }
        }
        return url;
    }
}