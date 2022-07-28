/*-
 * #%L
 * License Compliance Tool
 * %%
 * Copyright (C) 2022 medavis GmbH
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

import com.google.common.collect.ImmutableSet;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ComponentMetaDataLoaderTest {

    private final ComponentMetaDataLoader underTest = new ComponentMetaDataLoader();

    @Test
    void shouldLoadCompleteRecords() {
        final URL metadataUrl = getClass().getResource("/metadata/metadata-complete.json");

        final Collection<ComponentMetadata> actual = underTest.load(metadataUrl);

        Assertions.assertThat(actual).containsExactly(
                new ComponentMetadata("my\\.group", "keep", false, "KEEP!", "https://keep.com", "Keep component",
                        ImmutableSet.of("LIC-1.0")),
                new ComponentMetadata("my\\.group", "ignore", true, "IGNORE!", "https://ignore.com", "Ignore component",
                        ImmutableSet.of("LIC1-1.0", "LIC2-1.0"))
        );
    }

    @Test
    void shouldLoadMinimalRecords() {
        final URL metadataUrl = getClass().getResource("/metadata/metadata-minimal.json");

        final Collection<ComponentMetadata> actual = underTest.load(metadataUrl);

        Assertions.assertThat(actual).containsExactly(
                new ComponentMetadata("my\\.group", null, false, null, null, null, Collections.emptySet()),
                new ComponentMetadata(null, "my\\.name", false, null, null, null, Collections.emptySet())
        );
    }
}
