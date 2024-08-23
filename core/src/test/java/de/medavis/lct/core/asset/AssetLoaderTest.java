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
package de.medavis.lct.core.asset;

import com.google.common.collect.ImmutableSet;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

import de.medavis.lct.core.license.License;

class AssetLoaderTest {

    private final static String SAMPLE_BOM = "/asset/test-bom";

    private final AssetLoader underTest = new AssetLoader();

    @ParameterizedTest
    @ValueSource(strings = {"-1.4", "-1.5"})
    void shouldLoadAssetFromBOM(String bomSuffix) {
        InputStream sampleBomStream = getClass().getResourceAsStream(SAMPLE_BOM  + bomSuffix + ".json");

        Asset actual = underTest.loadFromBom(sampleBomStream);

        assertThat(actual.name()).isEqualTo("de.medavis.license-compliance-tool-core");
        assertThat(actual.version()).isEqualTo("1.4.0");
        assertThat(actual.components()).contains(
                new Component("org.cyclonedx", "cyclonedx-core-java", "9.0.0", "https://github.com/CycloneDX/cyclonedx-core-java.git", ImmutableSet.of(
                        License.dynamic("Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0")
                )),
                new Component("org.codehaus.woodstox", "stax2-api", "4.2.2", "http://github.com/FasterXML/stax2-api", ImmutableSet.of(
                        License.dynamic("BSD-2-Clause", null)
                )),
                new Component(null, "slf4j-api", "2.0.13", "https://github.com/qos-ch/slf4j/slf4j-parent/slf4j-api", ImmutableSet.of(
                        License.dynamic("MIT", "https://opensource.org/licenses/MIT"),
                        License.dynamic("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")))
        );
    }


}
