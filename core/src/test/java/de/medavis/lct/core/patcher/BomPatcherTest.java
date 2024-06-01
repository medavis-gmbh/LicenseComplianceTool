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

import de.medavis.lct.core.Configuration;

import org.apache.commons.io.output.NullOutputStream;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BomPatcherTest {

    private final Configuration c = new Configuration() {
        @Override
        public boolean isResolveExpressions() {
            return true;
        }
    };

    @Test
    void testCycloneDXSchema() {
        BomPatcher patcher = new BomPatcher(c);
        assertThrows(LicensePatcherException.class, () -> patcher.patch(getClass().getResourceAsStream("/asset/test-bom-unsupported-version.json"), NullOutputStream.nullOutputStream()));
        assertThrows(LicensePatcherException.class, () -> patcher.patch(getClass().getResourceAsStream("/asset/test-bom-unsupported-format.json"), NullOutputStream.nullOutputStream()));
    }

}
