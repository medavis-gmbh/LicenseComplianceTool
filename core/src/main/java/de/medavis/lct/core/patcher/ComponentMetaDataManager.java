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

import de.medavis.lct.core.metadata.ComponentMetaDataLoader;
import de.medavis.lct.core.metadata.ComponentMetadata;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ComponentMetaDataManager {

    private final Logger LOGGER = LoggerFactory.getLogger(ComponentMetaDataManager.class);
    private Collection<ComponentMetadata> componentMetaDataList = List.of();

    private ComponentMetaDataManager() { }

    public static ComponentMetaDataManager create() {
        return new ComponentMetaDataManager();
    }

    private void setComponentMetaDataList(@NotNull Collection<ComponentMetadata> list) {
        this.componentMetaDataList = List.copyOf(list);
    }

    public void load(@NotNull URI uri) {
        LOGGER.info("Loading custom rules from '{}'.", uri);

        try {
            ComponentMetaDataLoader loader = new ComponentMetaDataLoader();
            setComponentMetaDataList(loader.load(uri.toURL()));
        } catch (IOException ex) {
            throw new LicensePatcherException(ex.getMessage(), ex);
        }
    }

    public void load(@NotNull Path file) {
        if (Files.exists(file)) {
            load(file.toUri());
        } else {
            LOGGER.info("No custom rules file '{}' found.", file);
        }
    }

    /**
     * Validate to be mapped license names against an (official SPDX) set of license names.
     * <p>
     * If an unsupported SPDX license was found, a log warning will be written into the log.
     *
     * @param supportedLicenseIds Set od (SPDX) supported license IDs
     */
    public void logInvalidLicenseIds(@NotNull Set<String> supportedLicenseIds) {
        List<String> findings = componentMetaDataList
                .stream()
                .flatMap(cm -> cm.licenses().stream())
                .filter(l -> StringUtils.isNotBlank(l) && !supportedLicenseIds.contains(l))
                .collect(Collectors.toList());

        if (!findings.isEmpty()) {
            LOGGER.warn("Your component meta data configuration contains an unsupported to be mapped SPDX name '{}'.", findings);
        }
    }

}
