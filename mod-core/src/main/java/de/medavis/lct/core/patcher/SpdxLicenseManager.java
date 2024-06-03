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

import com.fasterxml.jackson.databind.ObjectMapper;

import de.medavis.lct.core.patcher.model.SpdxLicense;
import de.medavis.lct.core.patcher.model.SpdxLicenses;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SpdxLicenseManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpdxLicenseManager.class);
    private final ObjectMapper objectMapper = Json5MapperFactory.create();
    private final Set<String> idSet = new HashSet<>();
    private final Set<String> nameSet = new HashSet<>();

    public static SpdxLicenseManager create() {
        return new SpdxLicenseManager();
    }

    public SpdxLicenseManager loadDefaults() {
        try {
            LOGGER.info("Loading local copy of SPDX licenses");
            String resource = IOUtils.resourceToString(
                    "de/medavis/lct/core/patcher/SpdxLicenseList.json5",
                    StandardCharsets.UTF_8,
                    ClassLoader.getSystemClassLoader()
            );
            SpdxLicenses licenses = Json5MapperFactory
                    .create()
                    .readValue(resource, SpdxLicenses.class);

            LOGGER.info("Using SPDX license version {}", licenses.getLicenseListVersion());

            idSet.addAll(licenses.getLicenses().stream().map(SpdxLicense::getLicenseId).collect(Collectors.toList()));
            nameSet.addAll(licenses.getLicenses().stream().map(SpdxLicense::getName).collect(Collectors.toList()));
        } catch (IOException ex) {
            throw new LicensePatcherException(ex.getMessage(), ex);
        }

        return this;
    }

    public SpdxLicenseManager load(@NotNull URI uri) {
        LOGGER.info("Loading SPDX licenses from {}", uri);
        idSet.clear();
        nameSet.clear();

        SpdxLicenses licenses;

        if ("file".equals(uri.getScheme())) {
            try {
                licenses = objectMapper.readValue(uri.toURL(), SpdxLicenses.class);
            } catch (IOException ex) {
                throw new LicensePatcherException(ex.getMessage(), ex);
            }
        } else {
            SpdxRestClient client = new SpdxRestClient(uri);
            licenses = client.fetchLicenses();
        }
        LOGGER.info("Using SPDX license version {}", licenses.getLicenseListVersion());

        idSet.addAll(licenses.getLicenses().stream().map(SpdxLicense::getLicenseId).collect(Collectors.toList()));
        nameSet.addAll(licenses.getLicenses().stream().map(SpdxLicense::getName).collect(Collectors.toList()));

        return this;
    }

    public boolean containsId(String id) {
        return idSet.contains(id);
    }

    public boolean containsName(String name) {
        return nameSet.contains(name);
    }

}
