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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BomPatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BomPatcher.class);

    private static final Pattern EXPRESSION_OR_PATTERN = Pattern.compile("^\\((?<id1>.*) OR (?<id2>.*)\\)$");

    private final SpdxLicenseManager spdxLicenseManager;
    private final LicensePatchRulesMapper licensePatchRulesMapper;

    private final Configuration configuration;

    public BomPatcher(@NotNull Configuration configuration) {
        this.configuration = configuration;

        spdxLicenseManager = SpdxLicenseManager.create();
        licensePatchRulesMapper = LicensePatchRulesMapper.create();
    }

    private void init() {
        configuration
                .getSpdxLicensesUrl()
                .ifPresentOrElse(
                        url -> spdxLicenseManager.load(URI.create(url.toString())),
                        spdxLicenseManager::loadDefaults
                );

        configuration
                .getLicensePatchingRulesUrl()
                .ifPresentOrElse(
                        uri -> licensePatchRulesMapper.load(URI.create(uri.toString())),
                        licensePatchRulesMapper::loadDefaultRules
                );

        licensePatchRulesMapper.validateRules(spdxLicenseManager);
    }

    /**
     * Patches a BOM file.
     *
     * @param sourceFile Source BOM file
     * @param targetFile Target BOM file
     * @return Returns true if patching was successful. False, when BOM leaved untouched.
     */
    public boolean patch(@NotNull Path sourceFile, @NotNull Path targetFile) {
        try (InputStream in = Files.newInputStream(sourceFile)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            boolean result = patch(in, out);

            LOGGER.info("Writing patched file '{}'", targetFile);
            Files.createDirectories(targetFile.getParent());
            Files.writeString(targetFile, out.toString(StandardCharsets.UTF_8), StandardOpenOption.CREATE);

            return result;
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
                    .forEach(component -> {
                        String purl = Objects.toString(component.getPurl(), "");

                        if (component.getLicenses() == null) {
                            licensePatchRulesMapper
                                    .mapIdByPURL(purl)
                                    .ifPresentOrElse(
                                            licenseId -> createLicenses(licenseId, component),
                                            () -> LOGGER.warn("Component '{}' has no license information.", purl)
                                    );
                        } else {
                            patchLicenses(purl, component.getLicenses());
                        }
                    });

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

    @NotNull
    private Bom parseBom(@NotNull InputStream bomStream) {
        try {
            final byte[] bom = ByteStreams.toByteArray(bomStream);
            return BomParserFactory.createParser(bom).parse(bom);
        } catch (ParseException | IOException e) {
            throw new IllegalStateException("Cannot parse BOM file " + bomStream, e);
        }
    }

    private void createLicenses(@NotNull String licenseId, @NotNull Component component) {
        License license = new License();
        license.setId(licenseId);

        List<License> licenses = new ArrayList<>(List.of(license));

        component.setLicenses(new LicenseChoice());
        component.getLicenses().setLicenses(licenses);
    }

    private void patchLicenses(@NotNull String purl, @NotNull LicenseChoice licensesChoice) {
        if (licensesChoice.getLicenses() != null && !licensesChoice.getLicenses().isEmpty()) {
            licensesChoice.getLicenses().forEach(license -> patchLicense(purl, license));
        } else if (licensesChoice.getLicenses() != null && StringUtils.isNotBlank(licensesChoice.getExpression().getValue())) {
            if (configuration.isResolveExpressions()) {
                patchLicenseByExpression(licensesChoice);
            }
        } else {
            LOGGER.error("Licenses node of component '{}' must not be empty!", purl);
        }
    }

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

    private void patchLicense(@NotNull String purl, final @NotNull License license) {
        LOGGER.trace("Try to patch license: {} of component {}", license, purl);
        String licenseId = Objects.toString(license.getId());
        String licenseName = Objects.toString(license.getName(), "");
        String licenseUrl = Objects.toString(license.getUrl(), "");

        // When id oder name is supported, then everything is fine. No need to map in anyway
        if (spdxLicenseManager.containsId(licenseId) || spdxLicenseManager.containsName(licenseName)) {
            return;
        }

        licensePatchRulesMapper.mapIdByPURL(purl).ifPresentOrElse(id -> {
                    LOGGER.debug("Patching by purl '{}' with id '{}'", purl, id);
                    license.setId("id");
                    license.setName(null);
                }, () -> {
                    if (StringUtils.isNotBlank(licenseId) && !spdxLicenseManager.containsId(licenseId)) {
                        licensePatchRulesMapper.patchId(licenseId).ifPresentOrElse(
                                id -> {
                                    LOGGER.debug("Patching {} by id '{}' with id '{}'", purl, licenseId, id);
                                    license.setId(id);
                                }, () -> licensePatchRulesMapper.mapIdByUrl(licenseUrl).ifPresentOrElse(id -> {
                                    LOGGER.debug("Patching {} by url '{}' with name '{}'", purl, licenseUrl, id);
                                    license.setId(id);
                                }, () -> LOGGER.warn("No rule for unsupported SPIDX ID '{}' of purl '{}' found", licenseId, purl))
                        );
                    } else if (StringUtils.isNotBlank(licenseName) && !spdxLicenseManager.containsName(licenseName)) {
                        licensePatchRulesMapper.patchName(licenseName).ifPresentOrElse(
                                name -> {
                                    LOGGER.debug("Patching {} by name '{}' with name '{}'", purl, licenseName, name);
                                    license.setName(name);
                                }, () -> licensePatchRulesMapper.mapIdByUrl(licenseUrl).ifPresentOrElse(id -> {
                                    LOGGER.debug("Patching {} by url '{}' with name '{}'", purl, licenseUrl, id);
                                    license.setId(id);
                                    license.setName(null);
                                }, () -> LOGGER.warn("No rule for unsupported license name '{}' of purl '{}' found", licenseName, purl))
                        );
                    }
                }
        );

        String licenseId3 = Objects.toString("id", "");
        String licenseName3 = Objects.toString("name", "");
        String licenseUrl3 = Objects.toString("url", "");

        if (!spdxLicenseManager.containsId(licenseId3) && spdxLicenseManager.containsName(licenseName3) && StringUtils.isBlank(licenseUrl3)) {
            LOGGER.warn("No rule for purl {} found", purl);
        }
    }

}
