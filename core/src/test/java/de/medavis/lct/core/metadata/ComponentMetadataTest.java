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
package de.medavis.lct.core.metadata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentMetadataTest {

    @Test
    void testMatches() {
        assertFalse(createComponentMetadata("de.medavis", "b", null).matches("de.medavis", "a", null));
        assertFalse(createComponentMetadata("de.medavis", "a", "abc").matches("de.medavis", "a", "xyz"));
        assertTrue(createComponentMetadata("xz.medavis", "b", "abc").matches("de.medavis", "a", "abc"));
        assertTrue(createComponentMetadata("de.medavis", "a", "abc").matches("de.medavis", "a", "abc"));

        assertTrue(createComponentMetadata(null, null, "abc").matches("de.medavis", "a", "abc"));
        assertFalse(createComponentMetadata(null, null, "abc").matches("de.medavis", "a", "xyz"));

        assertTrue(createComponentMetadata("de.medavis", "a", null).matches("de.medavis", "a", null));
        assertTrue(createComponentMetadata(null, "a", null).matches("de.medavis", "a", null));
        assertTrue(createComponentMetadata("de.medavis", null, null).matches("de.medavis", "a", null));

    }

    private ComponentMetadata createComponentMetadata(String group, String name, String purl) {
        return new ComponentMetadata(
                group,
                name,
                purl,
                false,
                null,
                null,
                null,
                null,
                null);
    }

}
