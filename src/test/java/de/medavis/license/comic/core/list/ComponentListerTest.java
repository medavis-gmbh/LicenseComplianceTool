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
package de.medavis.license.comic.core.list;

import java.net.URL;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.medavis.license.comic.core.Configuration;
import de.medavis.license.comic.core.asset.AssetLoader;
import de.medavis.license.comic.core.license.License;
import de.medavis.license.comic.core.license.LicenseLoader;
import de.medavis.license.comic.core.license.LicenseMappingLoader;
import de.medavis.license.comic.core.metadata.ComponentMetaDataLoader;

public class ComponentListerTest {

    @Test
    public void useWithoutModifications() {
        assertThat(executeTest("metadata-empty", "license-empty", "licensemapping-empty", "test-bom"))
                .containsExactly(
                        new ComponentData("ch.qos.logback.logback-classic", "1.2.11", "https://github.com/ceki/logback", Set.of(
                                new License("EPL-1.0", null),
                                new License("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        )),
                        new ComponentData("ch.qos.logback.logback-core", "1.2.11", "https://github.com/ceki/logback", Set.of(
                                new License("EPL-1.0", null),
                                new License("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        )),
                        new ComponentData("org.slf4j.slf4j-api", "1.7.32", "https://github.com/qos-ch/slf4j", Set.of(
                                new License("MIT", "https://opensource.org/licenses/MIT")
                        ))
                );
    }

    @Test
    public void canMergeComponentsWithSameMappedName() {
        assertThat(executeTest("metadata-mergeLogback", "license-empty", "licensemapping-empty", "test-bom"))
                .containsExactly(
                        new ComponentData("Logback", "1.2.11", "https://github.com/ceki/logback", Set.of(
                                new License("EPL-1.0", null),
                                new License("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        )),
                        new ComponentData("org.slf4j.slf4j-api", "1.7.32", "https://github.com/qos-ch/slf4j", Set.of(
                                new License("MIT", "https://opensource.org/licenses/MIT")
                        ))
                );
    }

    @Test
    public void canMergeLicensesWhenMergingComponents() {
        assertThat(executeTest("metadata-mergeLogback", "license-empty", "licensemapping-empty", "test-bom-modifiedLicense"))
                .containsExactly(
                        new ComponentData("Logback", "1.2.11", "https://github.com/ceki/logback", Set.of(
                                new License("EPL-1.0", null),
                                new License("EPL-1.0-Modified", null),
                                new License("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html"),
                                new License("GNU Greater General Public License", "https://greater.gnu.com")
                        )),
                        new ComponentData("org.slf4j.slf4j-api", "1.7.32", "https://github.com/qos-ch/slf4j", Set.of(
                                new License("MIT", "https://opensource.org/licenses/MIT")
                        ))
                );
    }

    @Test
    public void canIgnoreComponents() {
        assertThat(executeTest("metadata-ignoreLogback", "license-empty", "licensemapping-empty", "test-bom"))
                .containsExactly(
                        new ComponentData("org.slf4j.slf4j-api", "1.7.32", "https://github.com/qos-ch/slf4j", Set.of(
                                new License("MIT", "https://opensource.org/licenses/MIT")
                        ))
                );
    }

    @Test
    public void canRenameLicenseAndReplaceUrl() {
        assertThat(executeTest("metadata-empty", "license-lgpl", "licensemapping-lgpl", "test-bom"))
                .containsExactly(
                        new ComponentData("ch.qos.logback.logback-classic", "1.2.11", "https://github.com/ceki/logback", Set.of(
                                new License("EPL-1.0", null),
                                new License("LGPL", "https://my.lgpl.link", "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt")
                        )),
                        new ComponentData("ch.qos.logback.logback-core", "1.2.11", "https://github.com/ceki/logback", Set.of(
                                new License("EPL-1.0", null),
                                new License("LGPL", "https://my.lgpl.link", "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt")
                        )),
                        new ComponentData("org.slf4j.slf4j-api", "1.7.32", "https://github.com/qos-ch/slf4j", Set.of(
                                new License("MIT", "https://opensource.org/licenses/MIT")
                        ))
                );
    }

    @Test
    public void canRenameLicenseWithOriginalUrl() {
        assertThat(executeTest("metadata-empty", "license-empty", "licensemapping-lgpl", "test-bom"))
                .containsExactly(
                        new ComponentData("ch.qos.logback.logback-classic", "1.2.11", "https://github.com/ceki/logback", Set.of(
                                new License("EPL-1.0", null),
                                new License("LGPL", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        )),
                        new ComponentData("ch.qos.logback.logback-core", "1.2.11", "https://github.com/ceki/logback", Set.of(
                                new License("EPL-1.0", null),
                                new License("LGPL", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        )),
                        new ComponentData("org.slf4j.slf4j-api", "1.7.32", "https://github.com/qos-ch/slf4j", Set.of(
                                new License("MIT", "https://opensource.org/licenses/MIT")
                        ))
                );
    }

    @Test
    public void canOverwriteUrl() {
        assertThat(executeTest("metadata-overwriteSlf4jUrl", "license-empty", "licensemapping-empty", "test-bom"))
                .containsExactly(
                        new ComponentData("ch.qos.logback.logback-classic", "1.2.11", "https://github.com/ceki/logback", Set.of(
                                new License("EPL-1.0", null),
                                new License("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        )),
                        new ComponentData("ch.qos.logback.logback-core", "1.2.11", "https://github.com/ceki/logback", Set.of(
                                new License("EPL-1.0", null),
                                new License("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        )),
                        new ComponentData("org.slf4j.slf4j-api", "1.7.32", "https://my.slf4j.com/", Set.of(
                                new License("MIT", "https://opensource.org/licenses/MIT")
                        ))
                );
    }

    @Test
    public void canOverwriteLicensesForSpecificComponents() {
        assertThat(executeTest("metadata-overwriteMyLicense", "license-empty", "licensemapping-empty", "test-bom"))
                .containsExactly(
                        new ComponentData("ch.qos.logback.logback-classic", "1.2.11", "https://github.com/ceki/logback", Set.of(
                                new License("EPL-1.0", null),
                                new License("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                        )),
                        new ComponentData("ch.qos.logback.logback-core", "1.2.11", "https://github.com/ceki/logback", Set.of(
                                new License("MYLICENSE", null)
                        )),
                        new ComponentData("org.slf4j.slf4j-api", "1.7.32", "https://github.com/qos-ch/slf4j", Set.of(
                                new License("MIT", "https://opensource.org/licenses/MIT")
                        ))
                );
    }

    private Collection<ComponentData> executeTest(String metadataFile, String licenseFile, String licenseMappingFile, String bomFile) {
        Configuration configuration = Mockito.mock(Configuration.class);
        when(configuration.getComponentMetadataUrl()).thenReturn(Optional.of(getResourceURL("metadata", metadataFile, "json")));
        when(configuration.getLicensesUrl()).thenReturn(Optional.of(getResourceURL("license", licenseFile, "json")));
        when(configuration.getLicenseMappingsUrl()).thenReturn(Optional.of(getResourceURL("license", licenseMappingFile, "json")));

        var componentLister = new ComponentLister(
                new AssetLoader(),
                new ComponentMetaDataLoader(),
                new LicenseLoader(),
                new LicenseMappingLoader(),
                configuration);
        return componentLister.listComponents(getResourceURL("asset", bomFile, "json"));
    }

    private URL getResourceURL(String directory, String filename, String extension) {
        return getClass().getResource(String.format("/%s/%s.%s", directory, filename, extension));
    }
}
