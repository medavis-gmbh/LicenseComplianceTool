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
package de.medavis.lct.core.license;


import java.net.URL;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

class LicenseLoaderTest {

    private final LicenseLoader underTest = new LicenseLoader();

    @Test
    void shouldLoadLicenseWithUrl() {
        final URL licenseUrl = getClass().getResource("/license/license-lgpl.json");

        final Map<String, License> actual = underTest.load(licenseUrl);

        assertThat(actual).containsExactly(entry("LGPL", License.fromConfig("LGPL", "https://my.lgpl.link", "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt")));
    }

    @Test
    void shouldLoadLicenseWithoutUrl() {
        final URL licenseUrl = getClass().getResource("/license/license-minimal.json");

        final Map<String, License> actual = underTest.load(licenseUrl);

        assertThat(actual).containsExactly(entry("LGPL", License.fromConfig("LGPL", null, null)));
    }
}
