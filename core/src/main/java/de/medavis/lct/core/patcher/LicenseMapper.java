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

import de.medavis.lct.core.patcher.model.Rule;
import de.medavis.lct.core.patcher.model.Rules;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LicenseMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseMapper.class);
    private static final String DEFAULT_MAPPING_RESOURCE = "/de/medavis/lct/core/patcher/DefaultLicenseMapping.json5";
    private final ObjectMapper objectMapper = Json5MapperFactory.create();
    private final Map<String, String> idPatchRulesMap = new HashMap<>();
    private final Map<String, String> namePatchRulesMap = new HashMap<>();
    private final Map<String, String> urlMappingRules = new HashMap<>();
    private final Map<String, Rule> purlMappingRules = new HashMap<>();

    private LicenseMapper() {
        loadEmbeddedMapping();
    }

    public static LicenseMapper create() {
        return new LicenseMapper();
    }

    private void clear() {
        idPatchRulesMap.clear();
        namePatchRulesMap.clear();
        urlMappingRules.clear();
        purlMappingRules.clear();
    }

    private void setMapping(@NotNull Rules rules) {
        clear();

        rules.getIdPatchRules().forEach(r -> idPatchRulesMap.put(r.getMatch(), r.getResolved()));
        rules.getNamePatchRules().forEach(r -> namePatchRulesMap.put(r.getMatch(), r.getResolved()));
        rules.getUrlMappingRules().forEach(r -> urlMappingRules.put(r.getMatch(), r.getId()));
        rules.getPurlMappingRules().forEach(r -> purlMappingRules.put(r.getMatch(), r));
    }

    private void loadEmbeddedMapping() {
        try {
            LOGGER.info("Loading embedded mappings");

            Rules rules = objectMapper.readValue(getClass().getResource(DEFAULT_MAPPING_RESOURCE), Rules.class);
            setMapping(rules);
        } catch (IOException ex) {
            throw new LicensePatcherException(ex.getMessage(), ex);
        }
    }

    public void load(@NotNull Path file) {
        try {
            if (Files.exists(file)) {
                LOGGER.info("Loading custom mappings from '{}'.", file);

                Rules rules = objectMapper.readValue(file.toFile(), Rules.class);
                setMapping(rules);
            } else {
                LOGGER.info("No custom mapping file '{}' found.", file);
            }
        } catch (IOException ex) {
            throw new LicensePatcherException(ex.getMessage(), ex);
        }
    }

    /**
     * Validate Rules ID and Name against official SPDX licenses.
     *
     * @param list SpdxLicenseManager
     */
    public void validateRules(@NotNull SpdxLicenseManager list) {
        idPatchRulesMap.values().forEach(v -> { if (!list.containsId(v)) { throw new LicensePatcherException("Unsupported SPDX ID '" + v + "'"); }});
        namePatchRulesMap.values().forEach(v -> { if (!list.containsName(v)) { throw new LicensePatcherException("Unsupported license name '" + v + "'"); }});
        urlMappingRules.values().forEach(v -> { if (!list.containsId(v)) { throw new LicensePatcherException("Unsupported SPDX ID '" + v + "'"); }});
        purlMappingRules.values().forEach(v -> { if (!list.containsId(v.getId())) { throw new LicensePatcherException("Unsupported SPDX ID '" + v.getId() + "'"); }});
    }

    public void createTemplate(@NotNull Path file) throws IOException {
        if (Files.exists(file)) {
            LOGGER.warn("License mapping file '{}' already exists.", file);
            return;
        }

        Files.createDirectories(file.getParent());

        try (InputStream in = LicenseMapper.class.getResourceAsStream(DEFAULT_MAPPING_RESOURCE)) {
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }

        LOGGER.warn("License mapping file '{}' has been created", file);
    }

    @NotNull
    public Optional<String> patchId(@Nullable String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(idPatchRulesMap.get(id));
    }

    @NotNull
    public Optional<String> patchName(@Nullable String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(namePatchRulesMap.get(name));
    }

    @NotNull
    public Optional<String> mapIdByUrl(@Nullable String url) {
        return url == null ? Optional.empty() : Optional.ofNullable(urlMappingRules.get(url));
    }

    @NotNull
    public Optional<String> mapIdByPURL(@Nullable String purl) {
        if (purl == null) {
            return Optional.empty();
        }

        Rule rule = purlMappingRules.get(purl);
        if (rule != null && !rule.isRegex()) {
            return Optional.ofNullable(rule.getId());
        }

        return purlMappingRules
                .values()
                .stream()
                .filter(Rule::isRegex)
                .filter(v -> {
                    Pattern pattern = Pattern.compile(v.getMatch());
                    Matcher matcher = pattern.matcher(purl);
                    return matcher.find();
                })
                .map(Rule::getId)
                .findFirst();
    }

}
