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

import de.medavis.lct.core.patcher.model.SpdxLicense;
import de.medavis.lct.core.patcher.model.SpdxLicenses;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SpdxLicenseManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpdxLicenseManager.class);
    private final Set<String> idSet = new HashSet<>();
    private final Set<String> nameSet = new HashSet<>();

    public static SpdxLicenseManager create(@Nullable URI uri) {
        return new SpdxLicenseManager(uri);
    }

    private SpdxLicenseManager(@Nullable URI uri) {
        try {
            SpdxLicenses licenses;
            if (uri == null) {
                LOGGER.info("Loading local copy of SPDX licenses");

                String resource = IOUtils.resourceToString("/de/medavis/lct/core/patcher/SpdxLicenseList.json5", StandardCharsets.UTF_8);
                licenses = Json5MapperFactory
                        .create()
                        .readValue(resource, SpdxLicenses.class);
            } else {
                LOGGER.info("Loading latest SPDX licenses from {}", uri);
                SpdxRestClient client = new SpdxRestClient(uri);
                licenses = client.fetchLicenses();
            }

            LOGGER.info("Using SPDX license version {}", licenses.getLicenseListVersion());

            idSet.addAll(licenses.getLicenses().stream().map(SpdxLicense::getLicenseId).collect(Collectors.toList()));
            nameSet.addAll(licenses.getLicenses().stream().map(SpdxLicense::getName).collect(Collectors.toList()));
         } catch (IOException ex) {
            throw new LicensePatcherException(ex.getMessage(), ex);
        }
    }

    public boolean containsId(String id) {
        return idSet.contains(id);
    }

    public boolean containsName(String name) {
        return nameSet.contains(name);
    }

}