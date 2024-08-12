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

import de.medavis.lct.core.Json5MapperFactory;
import de.medavis.lct.core.patcher.model.SpdxLicense;
import de.medavis.lct.core.patcher.model.SpdxLicenses;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SpdxLicenseManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpdxLicenseManager.class);
    private final ObjectMapper objectMapper = Json5MapperFactory.create();
    private final Map<String, SpdxLicense> idMap = new HashMap<>();
    private final Map<String, SpdxLicense> nameMap = new HashMap<>();

    public static SpdxLicenseManager create() {
        return new SpdxLicenseManager();
    }

    private void clear() {
        idMap.clear();
        nameMap.clear();
    }

    private void load(@NotNull SpdxLicenses licenses) {
        LOGGER.info("Using SPDX license version {}", licenses.getLicenseListVersion());

        idMap.putAll(licenses
                .getLicenses()
                .stream()
                .collect(Collectors.toMap(SpdxLicense::getLicenseId, l -> l)));
        nameMap.putAll(licenses
                .getLicenses()
                .stream()
                .collect(Collectors.toMap(SpdxLicense::getName, Function.identity(), (existing, replacement) -> existing)));
    }

    @NotNull
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

            load(licenses);
        } catch (IOException ex) {
            throw new LicensePatcherException(ex.getMessage(), ex);
        }

        return this;
    }

    /**
     * Tries to load a list of licenses in specified SPDX format.
     * See <a href="https://github.com/spdx/license-list-data">https://github.com/spdx/license-list-data</a> for more information.
     *
     * @param uri URI of the SPDX list
     * @return Returns this instance
     */
    @NotNull
    public SpdxLicenseManager load(@NotNull URI uri) {
        LOGGER.info("Loading SPDX licenses from {}", uri);
        clear();

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

        load(licenses);

        return this;
    }

    /**
     * Try to match a {@link SpdxLicense} ny license name or license ID.
     *
     * @param licenseId License ID
     * @param licenseName License name
     * @return Returns an {@link Optional} with the matched {@link SpdxLicense} or an empty Optional
     */
    @NotNull
    public Optional<SpdxLicense> match(@Nullable String licenseId, @Nullable String licenseName) {
        return Optional.ofNullable(idMap.get(licenseId))
                .or(() -> Optional.ofNullable(nameMap.get(licenseName)));
    }

    @NotNull
    public Set<String> getSupportedLicenseIds() {
        return Set.copyOf(idMap.keySet());
    }

}
