/*-
 * #%L
 * License Compliance Tool - Command Line Interface
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
package de.medavis.lct.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.medavis.lct.core.Json5MapperFactory;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    void testPatchBOM() throws IOException {

        URI uri = Path.of("src/test/resources/test-component-metadata.json5").toUri();

        Path testFile = Path.of("target/test-results/test-patched-01.json");
        Files.deleteIfExists(testFile);

        int exitCode = new Main().run(new String[] {
                "patch-sbom",
                "--in=src/test/resources/test-bom-01.json",
                "--out=" + testFile,
                "--componentMetadata=" + uri,
        });

        assertEquals(0, exitCode);
        assertTrue(Files.exists(testFile));

        ObjectMapper mapper = Json5MapperFactory.create();
        JsonNode rootNode = mapper.readTree(new File("target/test-results/test-patched-01.json"));

        assertEquals("Apache-2.0", JsonPath.path(rootNode, "components[0].licenses[0].license.id").asText());
        assertEquals("BSD-2-Clause", JsonPath.path(rootNode, "components[2].licenses[0].license.id").asText());
        assertEquals("BSD-2-Clause", JsonPath.path(rootNode, "components[2].licenses[0].license.id").asText());
    }
}
