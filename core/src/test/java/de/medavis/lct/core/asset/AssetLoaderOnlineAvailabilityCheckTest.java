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
package de.medavis.lct.core.asset;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.AbstractObjectAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.removeAllMappings;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

@WireMockTest
class AssetLoaderOnlineAvailabilityCheckTest {

    private final static String SAMPLE_BOM_TEMPLATE = "asset/test-bom-urlcheck.tmpl.json";
    private static final String URL_PATH_VCS = "/vcs";
    private static final String URL_PATH_WEBSITE = "/website";

    private final AssetLoader underTest = new AssetLoader(true);
    private InputStream bomWithWireMockUrl;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        var bomWithWireMockUrlString = Resources.asCharSource(Resources.getResource(SAMPLE_BOM_TEMPLATE), StandardCharsets.UTF_8)
                                                .lines()
                                                .collect(Collectors.joining("\n"))
                                                .replaceAll("\\Q{{mockUrl}}\\E", wmRuntimeInfo.getHttpBaseUrl());

        bomWithWireMockUrl = IOUtils.toInputStream(bomWithWireMockUrlString, StandardCharsets.UTF_8);
    }

    @AfterEach
    void tearDown() {
        removeAllMappings();
    }

    @Test
    void shouldPreferVCSOverWebsite(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(get(URL_PATH_VCS).willReturn(ok()));
        stubFor(get(URL_PATH_WEBSITE).willReturn(ok()));

        Asset actual = underTest.loadFromBom(bomWithWireMockUrl);

        assertComponentUrl(actual).isEqualTo(wmRuntimeInfo.getHttpBaseUrl() + URL_PATH_VCS);
    }

    @Test
    void shouldUseWebsiteIfVCSIsNotAvailable(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(get(URL_PATH_VCS).willReturn(notFound()));
        stubFor(get(URL_PATH_WEBSITE).willReturn(ok()));

        Asset actual = underTest.loadFromBom(bomWithWireMockUrl);

        assertComponentUrl(actual).isEqualTo(wmRuntimeInfo.getHttpBaseUrl() + URL_PATH_WEBSITE);
    }

    @Test
    void shouldReturnNullWhenBothVCSAndWebsiteAreNotAvailable(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(get(URL_PATH_VCS).willReturn(notFound()));
        stubFor(get(URL_PATH_WEBSITE).willReturn(notFound()));

        Asset actual = underTest.loadFromBom(bomWithWireMockUrl);

        assertComponentUrl(actual).isNull();
    }

    private static AbstractObjectAssert<?, String> assertComponentUrl(final Asset actual) {
        return assertThat(actual.components()).hasSize(1).first().extracting(Component::url);
    }

}
