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
class SpdxLicenseManagerTest {

    private static final String SPDX_LICENSE_PATH = "/spdx/licenses.json";

    private String baseUrl;

    @BeforeEach
    void beforeEach(WireMockRuntimeInfo wiremock) {
        baseUrl = wiremock.getHttpBaseUrl();
    }

    @Test
    void createWithExternal() throws IOException {
        String licenses = IOUtils.resourceToString("de/medavis/lct/core/patcher/SpdxLicenseList.json5", StandardCharsets.UTF_8, SpdxLicenseManagerTest.class.getClassLoader());

        stubFor(get(SPDX_LICENSE_PATH).
                willReturn(ok(licenses)
                        .withHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(licenses.length()))
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())));

        SpdxLicenseManager manager = SpdxLicenseManager.create(URI.create(baseUrl + SPDX_LICENSE_PATH));

        assertTrue(manager.containsId("Apache-2.0"));
        assertFalse(manager.containsId("Commercial-42"));
    }

    @Test
    void createWithInternal() throws IOException {
        String licenses = IOUtils.resourceToString("de/medavis/lct/core/patcher/SpdxLicenseList.json5", StandardCharsets.UTF_8, SpdxLicenseManagerTest.class.getClassLoader());
        assertNotNull(licenses);
        SpdxLicenseManager manager = SpdxLicenseManager.create(null);

        assertTrue(manager.containsId("Apache-2.0"));
        assertFalse(manager.containsId("Commercial-42"));

        assertTrue(manager.containsName("BSD 3-Clause No Nuclear License"));
        assertFalse(manager.containsName("Very Commercial License 42"));
    }

}
