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

import com.google.common.io.ByteStreams;

import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.asset.AssetLoader;
import de.medavis.lct.core.license.LicenseLoader;
import de.medavis.lct.core.license.LicenseMappingLoader;
import de.medavis.lct.core.list.ComponentData;
import de.medavis.lct.core.list.ComponentLister;
import de.medavis.lct.core.metadata.ComponentMetaDataLoader;

import org.cyclonedx.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.parsers.BomParserFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BomPatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BomPatcher.class);

    private final ComponentLister componentLister;
    private final Configuration configuration;

    private final SpdxLicenseManager spdxLicenseManager;
    private final ComponentMetaDataManager componentMetaDataManager;

    public BomPatcher(
            @NotNull AssetLoader assetLoader,
            @NotNull ComponentMetaDataLoader componentMetaDataLoader,
            @NotNull LicenseLoader licenseLoader,
            @NotNull LicenseMappingLoader licenseMappingLoader,
            @NotNull Configuration configuration) {
        this.configuration = configuration;

        componentLister = new ComponentLister(
                assetLoader,
                componentMetaDataLoader,
                licenseLoader,
                licenseMappingLoader,
                configuration
        );

        spdxLicenseManager = SpdxLicenseManager.create();
        componentMetaDataManager = ComponentMetaDataManager.create();
    }

    private void init() {
        configuration
                .getSpdxLicensesUrl()
                .ifPresentOrElse(
                        url -> spdxLicenseManager.load(URI.create(url.toString())),
                        spdxLicenseManager::loadDefaults
                );

        configuration
                .getComponentMetadataUrl()
                .map(url -> URI.create(url.toString()))
                .ifPresent(componentMetaDataManager::load);

        componentMetaDataManager.logInvalidLicenseIds(spdxLicenseManager.getSupportedLicenseIds());
    }

    /**
     * Patches a BOM file.
     *
     * @param sourceFile Source BOM file
     * @param targetFile Target BOM file. The directory will be created if not exist
     * @return Returns true if patching was successful. False, when BOM leaved untouched.
     */
    public boolean patch(@NotNull Path sourceFile, @NotNull Path targetFile) {
        try {
            Files.createDirectories(targetFile.getParent());
        } catch (IOException ex) {
            throw new LicensePatcherException(ex.getMessage(), ex);
        }

        try (InputStream in = Files.newInputStream(sourceFile);
             OutputStream out = Files.newOutputStream(targetFile, StandardOpenOption.CREATE)) {
            LOGGER.info("Writing patched file '{}'", targetFile);
            return patch(in, out);
        } catch (IOException ex) {
            throw new LicensePatcherException(ex.getMessage(), ex);
        }
    }

    /**
     * Patches a BOM stream.
     *
     * @param in  Source BOM as input stream
     * @param out Target BOM as output stream
     * @return Returns true if patching was successful. False, when BOM leaved untouched.
     */
    public boolean patch(@NotNull InputStream in, @NotNull OutputStream out) {
        init();

        try {
            final byte[] bomBytes = ByteStreams.toByteArray(in);
            Bom bom = BomParserFactory.createParser(bomBytes).parse(bomBytes);
            Version version = Arrays
                    .stream(Version.values())
                    .filter(v -> v.getVersionString().equals(bom.getSpecVersion()))
                    .findFirst()
                    .orElseThrow(() -> new LicensePatcherException("Unsupported version: " + bom.getSpecVersion()));

            if (!"CycloneDX".equals(bom.getBomFormat())) {
                throw new LicensePatcherException("Unsupported BOM format: " + bom.getBomFormat());
            }

            String originalBom = BomGeneratorFactory.createJson(version, bom).toJsonString();

            List<ComponentData> list = componentLister.listComponents(new ByteArrayInputStream(bomBytes));

            // Map licenses back to original BOM
            Map<String, ComponentData> purlMap = list.stream().collect(Collectors.toMap(ComponentData::getPurl, cd -> cd));
            bom.getComponents()
                    .stream()
                    .filter(c -> purlMap.containsKey(c.getPurl()))
                    .forEach(c -> patchLicense(c, purlMap.get(c.getPurl())));

            String patchedBom = BomGeneratorFactory.createJson(version, bom).toJsonString();
            out.write(patchedBom.getBytes(StandardCharsets.UTF_8));

            if (originalBom.equals(patchedBom)) {
                LOGGER.warn("No rules matched. Nothing has been patched in the SBOM.");
                return false;
            }

            return true;
        } catch (IOException | ParseException | GeneratorException ex) {
            throw new LicensePatcherException(ex.getMessage(), ex);
        }
    }

    private void patchLicense(@NotNull Component c, @NotNull ComponentData cd) {
        c.setLicenses(new LicenseChoice());
        cd.getLicenses()
                .stream()
                // Map only valid licenses
                .filter(l -> spdxLicenseManager.getSupportedLicenseIds().contains(l.getName()))
                .map(this::mapLicense)
                .forEach(l -> c.getLicenses().addLicense(l));
    }

    @NotNull
    private License mapLicense(@NotNull de.medavis.lct.core.license.License l) {
        License license = new License();
        // Do not wonder. License object from the asset loader may contain license id or license name.
        license.setId(l.getName());
        license.setUrl(l.getUrl());
        return license;
    }

}
