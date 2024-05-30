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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LicenseMapperTest {

    @Test
    void create() {

        LicenseMapper mapper = LicenseMapper.create();

        assertTrue(mapper.mapIdByPURL("TriTraTrullalla").isEmpty());
        assertTrue(mapper.mapIdByUrl("TriTraTrullalla").isEmpty());
        assertTrue(mapper.patchId("TriTraTrullalla").isEmpty());
        assertTrue(mapper.patchName("TriTraTrullalla").isEmpty());

        assertEquals("Apache-2.0", mapper.mapIdByPURL("pkg:maven/com.github.kenglxn.qrgen/core@2.6.0?type=jar").get());
        assertEquals("CC0-1.0", mapper.mapIdByUrl("https://creativecommons.org/publicdomain/zero/1.0/").get());
        assertEquals("MIT", mapper.patchId("Lesser General Public License (LGPL)").get());
        assertEquals("Apache License 2.0", mapper.patchName("Apache License, 2.0").get());

        mapper.validateRules(SpdxLicenseManager.create(null));

    }
}
