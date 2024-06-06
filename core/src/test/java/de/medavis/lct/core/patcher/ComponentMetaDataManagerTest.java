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

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class ComponentMetaDataManagerTest {

    @Test
    void testLoadAndMatching() throws URISyntaxException {

        ComponentMetaDataManager mapper = ComponentMetaDataManager.create();
        mapper.load(getClass().getClassLoader().getResource("de/medavis/lct/core/patcher/test-component-metadata.json").toURI());

        assertFalse(mapper.findMatch(null, "AspectJ", null).isPresent());
        assertFalse(mapper.findMatch("Bli", "bla", "Blub").isPresent());
        assertTrue(mapper.findMatch("org.aspectj", "AspectJ", null).isPresent());
        assertTrue(mapper.findMatch(null, "BliBlaBlub", "pkg:maven/javax.annotation/jakarta.annotation-api@1.3.2?type=jar").isPresent());
        assertTrue(mapper.findMatch("abc", "BliBlaBlub", "pkg:maven/javax.annotation/jakarta.annotation-api@1.3.2?type=jar").isPresent());

        assertTrue(mapper.validateLicenseMappedNames(SpdxLicenseManager.create().loadDefaults().getSupportedLicenseNames()));

    }

}
