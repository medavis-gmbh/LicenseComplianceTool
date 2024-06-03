/*-
 * #%L
 * License Compliance Tool - Implementation Core
 * %%
 * Copyright (C) 2022 - 2024 medavis GmbH
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
package de.medavis.lct.core.patcher;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.*;

@WireMockTest
class LicensePatchRulesMapperTest {

    private static final String URI_PATH = "/rules.json";

    private String baseUrl;

    @BeforeEach
    void beforeEach(WireMockRuntimeInfo wiremock) {
        baseUrl = wiremock.getHttpBaseUrl();
    }

    @Test
    void testMappings() {

        LicensePatchRulesMapper mapper = LicensePatchRulesMapper.create();
        mapper.loadDefaultRules();

        assertTrue(mapper.mapIdByPURL("TriTraTrullalla").isEmpty());
        assertTrue(mapper.mapIdByUrl("TriTraTrullalla").isEmpty());
        assertTrue(mapper.patchId("TriTraTrullalla").isEmpty());
        assertTrue(mapper.patchName("TriTraTrullalla").isEmpty());

        assertEquals("Apache-2.0", mapper.mapIdByPURL("pkg:maven/com.github.kenglxn.qrgen/core@2.6.0?type=jar").get());
        assertEquals("CC0-1.0", mapper.mapIdByUrl("https://creativecommons.org/publicdomain/zero/1.0/").get());
        assertEquals("MIT", mapper.patchId("Lesser General Public License (LGPL)").get());
        assertEquals("Apache License 2.0", mapper.patchName("Apache License, 2.0").get());

        mapper.validateRules(SpdxLicenseManager.create().loadDefaults());

    }

    @Test
    void testLoadFromURI() throws IOException {
        String licenses = IOUtils.resourceToString("de/medavis/lct/core/patcher/DefaultLicenseMapping.json5", StandardCharsets.UTF_8, SpdxLicenseManagerTest.class.getClassLoader());

        stubFor(get(URI_PATH).
                willReturn(ok(licenses)
                        .withHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(licenses.length()))
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())));

        LicensePatchRulesMapper mapper = LicensePatchRulesMapper.create();
        mapper.load(URI.create(baseUrl + URI_PATH));

        assertEquals("MIT", mapper.patchId("Lesser General Public License (LGPL)").get());
    }
}
