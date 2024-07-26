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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.Json5MapperFactory;
import de.medavis.lct.core.JsonPath;
import de.medavis.lct.core.asset.AssetLoader;
import de.medavis.lct.core.license.LicenseLoader;
import de.medavis.lct.core.license.LicenseMappingLoader;
import de.medavis.lct.core.metadata.ComponentMetaDataLoader;

import org.apache.commons.io.output.NullOutputStream;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BomPatcherTest {

    private BomPatcher createBomPatcher() {
        return new BomPatcher(
                new AssetLoader(),
                new ComponentMetaDataLoader(),
                new LicenseLoader(),
                new LicenseMappingLoader(),
                new Configuration() {
                    @Override
                    public Optional<URL> getComponentMetadataUrl() {
                        try {
                            return Optional.of(Path.of("src/test/resources/de/medavis/lct/core/patcher/test-component-metadata.json").toUri().toURL());
                        } catch (MalformedURLException ex) {
                            throw new LicensePatcherException(ex.getMessage(), ex);
                        }
                    }
                }
        );
    }

    @Test
    void testCycloneDXSchema() {
        BomPatcher patcher = createBomPatcher();
        assertThrows(LicensePatcherException.class, () -> patcher.patch(getClass().getResourceAsStream("/asset/test-bom-unsupported-version.json"), NullOutputStream.nullOutputStream()));
        assertThrows(LicensePatcherException.class, () -> patcher.patch(getClass().getResourceAsStream("/asset/test-bom-unsupported-format.json"), NullOutputStream.nullOutputStream()));
    }

    @Test
    void testPatchBOM() throws IOException {
        BomPatcher patcher = createBomPatcher();

        Path testFile = Path.of("target//test-results/test-patched-01.json");
        Files.deleteIfExists(testFile);

        Path sourceFile = Path.of("src/test/resources/de/medavis/lct/core/patcher/test-bom-01.json");

        boolean result = patcher.patch(
                sourceFile,
                testFile
        );

        assertTrue(result);
        assertTrue(Files.exists(testFile));

        ObjectMapper mapper = Json5MapperFactory.create();
        JsonNode rootNode = mapper.readTree(sourceFile.toFile());

        // Validate unpatched file, so that we can be sure that we have patched the BOM
        assertEquals("Apache 2.0", JsonPath.path(rootNode, "components[0].licenses[0].license.id").asText());
        assertFalse(JsonPath.path(rootNode, "components[2].licenses[0].license").has("id"));
        assertTrue(JsonPath.path(rootNode, "components[1].licenses[0]").has("expression"));
        assertFalse(JsonPath.path(rootNode, "components[3].licenses[8].license").has("id"));

        rootNode = mapper.readTree(new File("target//test-results/test-patched-01.json"));

        // Now, validate patched file
        assertEquals("Apache-2.0", JsonPath.path(rootNode, "components[0].licenses[0].license.id").asText());
        // Test, creating of missing licenses node
        assertEquals("BSD-2-Clause", JsonPath.path(rootNode, "components[2].licenses[0].license.id").asText());
        assertTrue(JsonPath.path(rootNode, "components[1].licenses[0]").has("expression"));
    }

}
