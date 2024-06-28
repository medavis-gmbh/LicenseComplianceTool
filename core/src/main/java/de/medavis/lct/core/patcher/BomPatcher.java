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

import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.Version;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BomPatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BomPatcher.class);

    private static final Pattern EXPRESSION_OR_PATTERN = Pattern.compile("^\\((?<id1>.*) OR (?<id2>.*)\\)$");

    private final SpdxLicenseManager spdxLicenseManager;
    private final ComponentMetaDataManager componentMetaDataManager;

    private final Configuration configuration;

    public BomPatcher(@NotNull Configuration configuration) {
        this.configuration = configuration;

        spdxLicenseManager = SpdxLicenseManager.create();
        componentMetaDataManager = new ComponentMetaDataManager();
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

        componentMetaDataManager.validateLicenseMappedNames(spdxLicenseManager.getSupportedLicenseNames());
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

            Optional.ofNullable(bom.getComponents())
                    .orElse(List.of())
                    .forEach(this::patchLicenses);

            String patchedBom = BomGeneratorFactory.createJson(version, bom).toJsonString();
            out.write(patchedBom.getBytes(StandardCharsets.UTF_8));

            if (originalBom.equals(patchedBom)) {
                LOGGER.warn("No rules matched. Nothing has been patched in the SBOM.");
                return false;
            }

            return true;
        } catch (IOException | ParseException ex) {
            throw new LicensePatcherException(ex.getMessage(), ex);
        }
    }

    /**
     * Creates a {@link LicenseChoice} instance, add the given license names as
     * {@link de.medavis.lct.core.license.License}s and return the {@link LicenseChoice} instance.
     *
     * @param licenseNames Name of licenses to add
     * @return Returns the {@link LicenseChoice} instance but never null
     */
    @NotNull
    private LicenseChoice createLicenses(@NotNull Set<String> licenseNames) {
        LicenseChoice licenseChoice = new LicenseChoice();
        licenseChoice.setLicenses(
                licenseNames
                        .stream()
                        .map(n -> {
                            License license = new License();
                            license.setId(n);
                            return license;
                        })
                        .collect(Collectors.toList()));

        return licenseChoice;
    }

    /**
     * Patch the licenses of a {@link Component} if configured
     *
     * @param component {@link Component} of the BOM model.
     */
    private void patchLicenses(@NotNull Component component) {
        String purl =  Objects.toString(component.getPurl(), "");
        LicenseChoice licensesChoice = component.getLicenses();
        if (licensesChoice == null) {
            // When no license node in component found then...Yes, this can be happened
            componentMetaDataManager
                    // Find licenses bei group name or PURL
                    .findMatch(component.getGroup(), component.getName(), purl)
                    .ifPresentOrElse(cm -> component.setLicenses(createLicenses(cm.licenses())),
                            () -> LOGGER.warn("Component '{}' has no license information because unable to resolve", purl));
        } else if (licensesChoice.getLicenses() != null && !licensesChoice.getLicenses().isEmpty()) {
            // OK. License node includes one or more licenses
            licensesChoice.getLicenses().forEach(license -> patchLicense(component, license));
        } else if (licensesChoice.getExpression() != null && StringUtils.isNotBlank(licensesChoice.getExpression().getValue())) {
            // Fine. License node includes expression of two or more licenses
            if (configuration.isResolveExpressions()) {
                patchLicenseByExpression(licensesChoice);
            }
        } else {
            LOGGER.error("Invalid SBOM model. Licenses node of component '{}' must not be empty!", purl);
        }
    }

    /**
     * Resolve license expressions.
     * <p/>
     * <b>Note: Currently only one "or" expression supported</b>
     *
     * @param licenseChoice License choice element from the BOM model.
     */
    private void patchLicenseByExpression(@NotNull LicenseChoice licenseChoice) {
        String expression = licenseChoice.getExpression().getValue();

        Matcher matcher = EXPRESSION_OR_PATTERN.matcher(expression);
        if (matcher.find()) {
            // Create node 1
            License licenseId1 = new License();
            licenseId1.setId(matcher.group("id1"));

            // Create node 2
            License licenseId2 = new License();
            licenseId2.setId(matcher.group("id2"));

            // Remove all licenses and Add new licenses (Removing of expression will automatically be done by adding licenses)
            licenseChoice.addLicense(licenseId1);
            licenseChoice.addLicense(licenseId2);
        } else {
            LOGGER.warn("Expressions like '{}' currently not supported.", expression);
        }
    }

    /**
     * Patch the license element ob using "purl", license "id" and license "name" and the {@link ComponentMetaDataManager}.
     *
     * @param component Component of the BOM model
     * @param license License to patch. This one {@link License} of the component {@link LicenseChoice} model.
     */
    private void patchLicense(
            @NotNull Component component,
            final @NotNull License license) {
        String purl = component.getPurl();
        LOGGER.trace("Try to patch license: {} of component {}", license, purl);
        String licenseId = Objects.toString(license.getId());
        String licenseName = Objects.toString(license.getName(), "");

        if (spdxLicenseManager.match(licenseId, licenseName).isEmpty()) {
            LOGGER.info("Package URL '{}' has an unsupported license id '{}' and/or license name '{}'.", purl, licenseId, licenseName);
        }

        componentMetaDataManager.findMatch(
                component.getGroup(),
                component.getName(),
                component.getPurl()
        ).ifPresentOrElse(cm -> {
            if (cm.licenses() == null || cm.licenses().isEmpty()) {
                LOGGER.warn("No license to set for purl '{}'.", purl);
            } else if (cm.licenses().size() == 1) {
                // Enrich license
                license.setId(cm.licenses().stream().findFirst().orElse(""));
                license.setName(null);
                Optional.ofNullable(cm.url()).ifPresent(license::setUrl);
            } else {
                // Recreate licenses node with all licenses
                component.setLicenses(createLicenses(cm.licenses()));
            }
        }, () -> LOGGER.warn("No match for purl '{}' found", purl));
    }

}
