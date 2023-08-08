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
package de.medavis.lct.core.list;

import com.google.common.collect.ImmutableSet;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.medavis.lct.core.license.LicenseLoader;
import de.medavis.lct.core.license.LicenseMappingLoader;
import de.medavis.lct.core.metadata.ComponentMetaDataLoader;
import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.asset.AssetLoader;
import de.medavis.lct.core.license.License;

class ComponentListerTest {

    @Test
    void useWithoutModifications() {
        assertThat(executeTest("metadata-empty", "license-empty", "licensemapping-empty", "test-bom"))
                .containsExactly(
                        new ComponentData("ch.qos.logback.logback-classic", "1.2.11", "https://github.com/ceki/logback", ImmutableSet.of(
                                License.dynamic("EPL-1.0", null),
                                License.dynamic("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        ), Collections.emptySet()),
                        new ComponentData("ch.qos.logback.logback-core", "1.2.11", "https://github.com/ceki/logback", ImmutableSet.of(
                                License.dynamic("EPL-1.0", null),
                                License.dynamic("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        ), Collections.emptySet()),
                        new ComponentData("org.slf4j.slf4j-api", "1.7.32", "https://github.com/qos-ch/slf4j", ImmutableSet.of(
                                License.dynamic("MIT", "https://opensource.org/licenses/MIT")
                        ), Collections.emptySet())
                );
    }

    @Test
    void ignoreEmptyGroup() {
        assertThat(executeTest("metadata-empty", "license-empty", "licensemapping-empty", "test-bom-depWithoutGroup"))
                .containsExactly(
                        new ComponentData("my-dependency", "1.0.0", null, Set.of(
                                License.dynamic("EPL-1.0", null)
                        ), Collections.emptySet())
                );
    }

    @Test
    void canMergeComponentsWithSameMappedName() {
        assertThat(executeTest("metadata-mergeLogback", "license-empty", "licensemapping-empty", "test-bom"))
                .containsExactly(
                        new ComponentData("Logback", "1.2.11", "https://github.com/ceki/logback", ImmutableSet.of(
                                License.dynamic("EPL-1.0", null),
                                License.dynamic("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        ), Collections.emptySet()),
                        new ComponentData("org.slf4j.slf4j-api", "1.7.32", "https://github.com/qos-ch/slf4j", ImmutableSet.of(
                                License.dynamic("MIT", "https://opensource.org/licenses/MIT")
                        ), Collections.emptySet())
                );
    }

    @Test
    void canMergeLicensesWhenMergingComponents() {
        assertThat(executeTest("metadata-mergeLogback", "license-empty", "licensemapping-empty", "test-bom-modifiedLicense"))
                .containsExactly(
                        new ComponentData("Logback", "1.2.11", "https://github.com/ceki/logback", ImmutableSet.of(
                                License.dynamic("EPL-1.0", null),
                                License.dynamic("EPL-1.0-Modified", null),
                                License.dynamic("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html"),
                                License.dynamic("GNU Greater General Public License", "https://greater.gnu.com")
                        ), Collections.emptySet()),
                        new ComponentData("org.slf4j.slf4j-api", "1.7.32", "https://github.com/qos-ch/slf4j", ImmutableSet.of(
                                License.dynamic("MIT", "https://opensource.org/licenses/MIT")
                        ), Collections.emptySet())
                );
    }

    @Test
    void canIgnoreComponents() {
        assertThat(executeTest("metadata-ignoreLogback", "license-empty", "licensemapping-empty", "test-bom"))
                .containsExactly(
                        new ComponentData("org.slf4j.slf4j-api", "1.7.32", "https://github.com/qos-ch/slf4j", ImmutableSet.of(
                                License.dynamic("MIT", "https://opensource.org/licenses/MIT")
                        ), Collections.emptySet())
                );
    }

    @Test
    void canRenameLicenseAndReplaceUrl() {
        assertThat(executeTest("metadata-empty", "license-lgpl", "licensemapping-lgpl", "test-bom"))
                .containsExactly(
                        new ComponentData("ch.qos.logback.logback-classic", "1.2.11", "https://github.com/ceki/logback", ImmutableSet.of(
                                License.dynamic("EPL-1.0", null),
                                License.fromConfig("LGPL", "https://my.lgpl.link", "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt")
                        ), Collections.emptySet()),
                        new ComponentData("ch.qos.logback.logback-core", "1.2.11", "https://github.com/ceki/logback", ImmutableSet.of(
                                License.dynamic("EPL-1.0", null),
                                License.fromConfig("LGPL", "https://my.lgpl.link", "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt")
                        ), Collections.emptySet()),
                        new ComponentData("org.slf4j.slf4j-api", "1.7.32", "https://github.com/qos-ch/slf4j", ImmutableSet.of(
                                License.dynamic("MIT", "https://opensource.org/licenses/MIT")
                        ), Collections.emptySet())
                );
    }

    @Test
    void canRenameLicenseWithOriginalUrl() {
        assertThat(executeTest("metadata-empty", "license-empty", "licensemapping-lgpl", "test-bom"))
                .containsExactly(
                        new ComponentData("ch.qos.logback.logback-classic", "1.2.11", "https://github.com/ceki/logback", ImmutableSet.of(
                                License.dynamic("EPL-1.0", null),
                                License.dynamic("LGPL", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        ), Collections.emptySet()),
                        new ComponentData("ch.qos.logback.logback-core", "1.2.11", "https://github.com/ceki/logback", ImmutableSet.of(
                                License.dynamic("EPL-1.0", null),
                                License.dynamic("LGPL", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        ), Collections.emptySet()),
                        new ComponentData("org.slf4j.slf4j-api", "1.7.32", "https://github.com/qos-ch/slf4j", ImmutableSet.of(
                                License.dynamic("MIT", "https://opensource.org/licenses/MIT")
                        ), Collections.emptySet())
                );
    }

    @Test
    void canOverwriteUrl() {
        assertThat(executeTest("metadata-overwriteSlf4jUrl", "license-empty", "licensemapping-empty", "test-bom"))
                .containsExactly(
                        new ComponentData("ch.qos.logback.logback-classic", "1.2.11", "https://github.com/ceki/logback", ImmutableSet.of(
                                License.dynamic("EPL-1.0", null),
                                License.dynamic("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        ), Collections.emptySet()),
                        new ComponentData("ch.qos.logback.logback-core", "1.2.11", "https://github.com/ceki/logback", ImmutableSet.of(
                                License.dynamic("EPL-1.0", null),
                                License.dynamic("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        ), Collections.emptySet()),
                        new ComponentData("org.slf4j.slf4j-api", "1.7.32", "https://my.slf4j.com/", ImmutableSet.of(
                                License.dynamic("MIT", "https://opensource.org/licenses/MIT")
                        ), Collections.emptySet())
                );
    }

    @Test
    void canOverwriteLicensesForSpecificComponents() {
        assertThat(executeTest("metadata-overwriteMyLicense", "license-empty", "licensemapping-empty", "test-bom"))
                .containsExactly(
                        new ComponentData("ch.qos.logback.logback-classic", "1.2.11", "https://github.com/ceki/logback", ImmutableSet.of(
                                License.dynamic("EPL-1.0", null),
                                License.dynamic("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        ), Collections.emptySet()),
                        new ComponentData("ch.qos.logback.logback-core", "1.2.11", "https://github.com/ceki/logback", ImmutableSet.of(
                                License.dynamic("MYLICENSE", null)
                        ), Collections.emptySet()),
                        new ComponentData("org.slf4j.slf4j-api", "1.7.32", "https://github.com/qos-ch/slf4j", ImmutableSet.of(
                                License.dynamic("MIT", "https://opensource.org/licenses/MIT")
                        ), Collections.emptySet())
                );
    }

    @Test
    void canAddAttributionNotices() {
        assertThat(executeTest("metadata-logbackAttributionNotice", "license-empty", "licensemapping-empty", "test-bom"))
                .containsExactly(
                        new ComponentData("ch.qos.logback.logback-classic", "1.2.11", "https://github.com/ceki/logback", ImmutableSet.of(
                                License.dynamic("EPL-1.0", null),
                                License.dynamic("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        ), ImmutableSet.of("Copyright (c) 2015", "Guaranteed Log4Shell-free")),
                        new ComponentData("ch.qos.logback.logback-core", "1.2.11", "https://github.com/ceki/logback", ImmutableSet.of(
                                License.dynamic("EPL-1.0", null),
                                License.dynamic("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        ), ImmutableSet.of("Copyright (c) 2015", "Guaranteed Log4Shell-free")),
                        new ComponentData("org.slf4j.slf4j-api", "1.7.32", "https://github.com/qos-ch/slf4j", ImmutableSet.of(
                                License.dynamic("MIT", "https://opensource.org/licenses/MIT")
                        ), Collections.emptySet())
                );
    }

    private Collection<ComponentData> executeTest(String metadataFile, String licenseFile, String licenseMappingFile, String bomFile) {
        Configuration configuration = Mockito.mock(Configuration.class);
        when(configuration.getComponentMetadataUrl()).thenReturn(Optional.of(getResourceURL("metadata", metadataFile, "json")));
        when(configuration.getLicensesUrl()).thenReturn(Optional.of(getResourceURL("license", licenseFile, "json")));
        when(configuration.getLicenseMappingsUrl()).thenReturn(Optional.of(getResourceURL("license", licenseMappingFile, "json")));

        ComponentLister componentLister = new ComponentLister(
                new AssetLoader(),
                new ComponentMetaDataLoader(),
                new LicenseLoader(),
                new LicenseMappingLoader(),
                configuration);
        return componentLister.listComponents(getResourceStream("asset", bomFile, "json"));
    }

    private URL getResourceURL(String directory, String filename, String extension) {
        return getClass().getResource(String.format("/%s/%s.%s", directory, filename, extension));
    }

    private InputStream getResourceStream(String directory, String filename, String extension) {
        return getClass().getResourceAsStream(String.format("/%s/%s.%s", directory, filename, extension));
    }
}
