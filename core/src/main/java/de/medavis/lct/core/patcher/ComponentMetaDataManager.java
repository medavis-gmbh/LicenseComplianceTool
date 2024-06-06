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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ComponentMetaDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentMetaDataManager.class);
    private Collection<ComponentMetadata> componentMetaDataList = List.of();

    public static ComponentMetaDataManager create() {
        return new ComponentMetaDataManager();
    }

    private void clear() {
    }

    private void setComponentMetaDataList(@NotNull Collection<ComponentMetadata> list) {
        clear();
        this.componentMetaDataList = List.copyOf(list);
    }

    public void load(@NotNull URI uri) {
        LOGGER.info("Loading custom rules from '{}'.", uri);

        try {
            ComponentMetaDataLoader loader = new ComponentMetaDataLoader();
            if ("file".equals(uri.getScheme())) {
                setComponentMetaDataList(loader.load(uri.toURL()));
            } else {

                setComponentMetaDataList(loader.load(uri.toURL()));
            }
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
     * If an unsupported SPDX license was found, a log warning will be written.
     *
     * @param supportedLicenseNames Set od (SPDX) supported license names
     * @return Returns true, when unsupported warnings was found
     */
    public boolean validateLicenseMappedNames(@NotNull Set<String> supportedLicenseNames) {
        AtomicBoolean result = new AtomicBoolean(false);

        componentMetaDataList
                .stream()
                .filter(cm -> StringUtils.isNotBlank(cm.mappedName()) && !supportedLicenseNames.contains(cm.mappedName()))
                .forEach(cm -> {
                    result.set(true);
                    LOGGER.warn("Your component meta data configuration contains an unsupported to be mapped SPDX name '{}'.", cm.mappedName());
                });

        return result.get();
    }

    /**
     * Try to match an {@link ComponentMetadata} by following condition: "group && name || purl".
     *
     * @param group Group to match or null. If null then it will be ignored in the match. According to CycloneDX Spec this is not a mandatory field
     * @param name Name to match. According to CycloneDX Spec this is a mandatory field
     * @param purl Package URL or null. If null then it will be ignored in the match. According to CycloneDX Spec this is not a mandatory field

     * @return Returns always an {@link Optional} with the {@link ComponentMetadata} if match.
     */
    @NotNull
    public Optional<ComponentMetadata> findMatch(@Nullable String group, @NotNull String name, @Nullable String purl) {
        return componentMetaDataList
                .stream()
                .filter(cm -> cm.matches(group, name, purl))
                .findFirst();
    }

}
