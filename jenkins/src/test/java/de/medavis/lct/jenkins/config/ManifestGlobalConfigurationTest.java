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
package de.medavis.lct.jenkins.config;

import java.net.URL;
import java.util.Optional;
import java.util.Set;

import org.htmlunit.html.HtmlCheckBoxInput;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlTextInput;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

// Still using JUnit 4 annotations since there seems to be no JUnit 5 equivalent for JenkinsSessionRule
public class ManifestGlobalConfigurationTest {

    private static final String COMPONENT_METADATA_URL = "https://component.metadata.url";
    private static final String LICENSES_URL = "https://licenses.url";
    private static final String LICENSES_MAPPING_URL = "https://licenses.mapping.url";

    private static final String LICENSE_PATCHING_RULES_URL = "https://licenses.patching.rules.url";
    private static final String SPDX_LICENSES_URL = "https://spdx.licenses.url";
    private static final String SKIP_GROUP_NAMES = "de.medavis,com.anywhere";
    private static final boolean RESOLVE_EXPRESSION = true;

    @Rule
    public JenkinsSessionRule jenkinsSession = new JenkinsSessionRule();

    @Test
    public void persistSettingDuringReload() throws Throwable {
        jenkinsSession.then(jenkins -> {
            HtmlForm config = jenkins.createWebClient().goTo("configure").getFormByName("config");
            setInputValue(config, "_.componentMetadata", COMPONENT_METADATA_URL);
            setInputValue(config, "_.licenses", LICENSES_URL);
            setInputValue(config, "_.licenseMappings", LICENSES_MAPPING_URL);
            setInputValue(config, "_.licensePatchingRules", LICENSE_PATCHING_RULES_URL);
            setInputValue(config, "_.spdxLicenses", SPDX_LICENSES_URL);
            setInputValue(config, "_.skipGroupNames", SKIP_GROUP_NAMES);
            setInputValue(config, "_.resolveExpressions", RESOLVE_EXPRESSION);
            jenkins.submit(config);
            verifyStoredConfig("After submit");
        });
        jenkinsSession.then(jenkins -> verifyStoredConfig("After restart"));
    }

    private void setInputValue(HtmlForm config, String name, String value) {
        HtmlTextInput textbox = config.getInputByName(name);
        textbox.setText(value);
    }

    private void setInputValue(HtmlForm config, String name, boolean value) {
        HtmlCheckBoxInput textbox = config.getInputByName(name);
        textbox.setChecked(value);
    }

    private void verifyStoredConfig(String stage) {
        assertThat(ManifestGlobalConfiguration.getInstance()).as(stage).satisfies(storedConfig -> assertSoftly(softly -> {
            softly.assertThat(storedConfig.getComponentMetadata()).isEqualTo(COMPONENT_METADATA_URL);
            softly.assertThat(storedConfig.getComponentMetadataUrl()).map(URL::toString).hasValue(COMPONENT_METADATA_URL);
            softly.assertThat(storedConfig.getLicenses()).isEqualTo(LICENSES_URL);
            softly.assertThat(storedConfig.getLicensesUrl()).map(URL::toString).hasValue(LICENSES_URL);
            softly.assertThat(storedConfig.getLicenseMappings()).isEqualTo(LICENSES_MAPPING_URL);
            softly.assertThat(storedConfig.getLicenseMappingsUrl()).map(URL::toString).hasValue(LICENSES_MAPPING_URL);
            softly.assertThat(storedConfig.getLicensePatchingRules()).isEqualTo(LICENSE_PATCHING_RULES_URL);
            softly.assertThat(storedConfig.getLicensePatchingRulesUrl()).map(URL::toString).hasValue(LICENSE_PATCHING_RULES_URL);
            softly.assertThat(storedConfig.getSpdxLicenses()).isEqualTo(SPDX_LICENSES_URL);
            softly.assertThat(storedConfig.getSpdxLicensesUrl()).map(URL::toString).hasValue(SPDX_LICENSES_URL);
            softly.assertThat(storedConfig.getSkipGroupNames()).isEqualTo(SKIP_GROUP_NAMES);
            softly.assertThat(storedConfig.getSkipGroupNameSet()).map(Set::size).isEqualTo(Optional.of(2));
            softly.assertThat(storedConfig.isResolveExpressions()).isTrue();
        }));
    }

    @Test
    public void handleEmptyConfig() throws Throwable {
        jenkinsSession.then(jenkins -> {
            HtmlForm config = jenkins.createWebClient().goTo("configure").getFormByName("config");
            jenkins.submit(config);

            assertThat(ManifestGlobalConfiguration.getInstance()).satisfies(storedConfig -> assertSoftly(softly -> {
                softly.assertThat(storedConfig.getComponentMetadata()).isNullOrEmpty();
                softly.assertThat(storedConfig.getComponentMetadataUrl()).isNotPresent();
                softly.assertThat(storedConfig.getLicenses()).isNullOrEmpty();
                softly.assertThat(storedConfig.getLicensesUrl()).isNotPresent();
                softly.assertThat(storedConfig.getLicenseMappings()).isNullOrEmpty();
                softly.assertThat(storedConfig.getLicenseMappingsUrl()).isNotPresent();
                softly.assertThat(storedConfig.getLicensePatchingRules()).isNullOrEmpty();
                softly.assertThat(storedConfig.getLicensePatchingRulesUrl()).isNotPresent();
                softly.assertThat(storedConfig.getSpdxLicenses()).isNullOrEmpty();
                softly.assertThat(storedConfig.getSpdxLicensesUrl()).isNotPresent();
                softly.assertThat(storedConfig.getSkipGroupNames()).isNullOrEmpty();
                softly.assertThat(storedConfig.getSkipGroupNameSet()).map(s -> String.join(",")).isEqualTo(Optional.of(""));
                softly.assertThat(storedConfig.isResolveExpressions()).isFalse();
            }));
        });
    }

}
