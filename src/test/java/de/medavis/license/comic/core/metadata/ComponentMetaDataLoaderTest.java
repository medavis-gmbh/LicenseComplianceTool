/*-
 * #%L
 * CoMiC - Component Manifest Creator
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
package de.medavis.license.comic.core.metadata;

import java.util.Collections;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentMetaDataLoaderTest {

    private final ComponentMetaDataLoader underTest = new ComponentMetaDataLoader();

    @Test
    public void shouldLoadCompleteRecords() {
        final var metadataUrl = getClass().getResource("/metadata/metadata-complete.json");

        final var actual = underTest.load(metadataUrl);

        Assertions.assertThat(actual).containsExactly(
                new ComponentMetadata("my\\.group", "keep", false, "KEEP!", "https://keep.com", "Keep component", Set.of("LIC-1.0")),
                new ComponentMetadata("my\\.group", "ignore", true, "IGNORE!", "https://ignore.com", "Ignore component", Set.of("LIC1-1.0", "LIC2-1.0"))
        );
    }

    @Test
    public void shouldLoadMinimalRecords() {
        final var metadataUrl = getClass().getResource("/metadata/metadata-minimal.json");

        final var actual = underTest.load(metadataUrl);

        Assertions.assertThat(actual).containsExactly(
                new ComponentMetadata("my\\.group", null, false, null, null, null, Collections.emptySet()),
                new ComponentMetadata(null, "my\\.name", false, null, null, null, Collections.emptySet())
        );
    }
}
