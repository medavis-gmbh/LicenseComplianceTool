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

import java.io.IOException;

import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlCheckBoxInput;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlTextInput;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

// Still using JUnit 4 annotations since there seems to be no JUnit 5 equivalent for JenkinsSessionRule
public class GlobalConfigurationTest {

    private static int uniqueCounter = 1;

    @Rule
    public JenkinsSessionRule jenkinsSession = new JenkinsSessionRule();

    @Test
    public void persistSettingDuringReload() throws Throwable {
        final ConfigurationProfile profile1 = createUniqueConfigurationProfile(false);
        final ConfigurationProfile profile2 = createUniqueConfigurationProfile(false);
        jenkinsSession.then(jenkins -> {
            HtmlForm config = jenkins.createWebClient().goTo("configure").getFormByName("config");
            final DomNode lctConfigSection = getLCTConfigSection(config);
            addProfile(lctConfigSection);
            fillConfig(lctConfigSection, 0, profile1);
            fillConfig(lctConfigSection, 1, profile2);

            jenkins.submit(config);

            verifyStoredConfig("After submit", profile1.getName(), profile1);
            verifyStoredConfig("After submit", profile2.getName(), profile2);
        });
        jenkinsSession.then(jenkins -> {
            verifyStoredConfig("After restart", profile1.getName(), profile1);
            verifyStoredConfig("After submit", profile2.getName(), profile2);
        });
    }

    @Test
    public void handleEmptyConfig() throws Throwable {
        jenkinsSession.then(jenkins -> {
            HtmlForm config = jenkins.createWebClient().goTo("configure").getFormByName("config");
            jenkins.submit(config);

            assertThat(GlobalConfiguration.getConfiguration()).satisfies(storedConfig -> assertSoftly(softly -> {
                softly.assertThat(storedConfig.getComponentMetadataUrl()).isNotPresent();
                softly.assertThat(storedConfig.getLicensesUrl()).isNotPresent();
                softly.assertThat(storedConfig.getLicenseMappingsUrl()).isNotPresent();
            }));
        });
    }

    @Test
    public void shouldTreatProfileFlaggedAsDefaultAsDefaultProfile() throws Throwable {
        final ConfigurationProfile profile1 = createUniqueConfigurationProfile(true);
        final ConfigurationProfile profile2 = createUniqueConfigurationProfile(false);
        jenkinsSession.then(jenkins -> {
            HtmlForm config = jenkins.createWebClient().goTo("configure").getFormByName("config");
            final DomNode lctConfigSection = getLCTConfigSection(config);
            addProfile(lctConfigSection);
            fillConfig(lctConfigSection, 0, profile1);
            fillConfig(lctConfigSection, 1, profile2);

            jenkins.submit(config);

            verifyStoredConfig("After submit", null, profile1);
        });
    }

    @Test
    public void shouldTreatFirstProfileAsDefaultIfNoneIsFlaggedAsDefault() throws Throwable {
        final ConfigurationProfile profile1 = createUniqueConfigurationProfile(false);
        final ConfigurationProfile profile2 = createUniqueConfigurationProfile(false);
        jenkinsSession.then(jenkins -> {
            HtmlForm config = jenkins.createWebClient().goTo("configure").getFormByName("config");
            final DomNode lctConfigSection = getLCTConfigSection(config);
            addProfile(lctConfigSection);
            fillConfig(lctConfigSection, 0, profile1);
            fillConfig(lctConfigSection, 1, profile2);

            jenkins.submit(config);

            verifyStoredConfig("After submit", null, profile1);
        });
    }

    private void addProfile(final DomNode lctConfigSection) throws IOException {
        HtmlButton addButton = lctConfigSection.querySelector(".repeatable-add");
        addButton.click();
    }

    private DomNode getLCTConfigSection(final HtmlForm config) {
        return config.querySelector("#lct-profiles");
    }

    private ConfigurationProfile createUniqueConfigurationProfile(final boolean isDefault) {
        var result = new ConfigurationProfile(uniqueString("name"), isDefault);
        result.setComponentMetadata(uniqueString("https://component.metadata"));
        result.setLicenses(uniqueString("https://licenses"));
        result.setLicenseMappings(uniqueString("https://license.mappings"));
        return result;
    }

    private String uniqueString(String prefix) {
        return prefix + uniqueCounter++;
    }

    private void fillConfig(final DomNode lctConfigForm, int index, ConfigurationProfile configurationProfile) {
        querySelectorNthMatch(lctConfigForm, "input[name='_.name']", index, HtmlTextInput.class).setText(configurationProfile.getName());
        querySelectorNthMatch(lctConfigForm, "input[name='_.defaultProfile']", index, HtmlCheckBoxInput.class).setChecked(configurationProfile.isDefaultProfile());
        querySelectorNthMatch(lctConfigForm, "input[name='_.componentMetadata']", index, HtmlTextInput.class).setText(configurationProfile.getComponentMetadata());
        querySelectorNthMatch(lctConfigForm, "input[name='_.licenses']", index, HtmlTextInput.class).setText(configurationProfile.getLicenses());
        querySelectorNthMatch(lctConfigForm, "input[name='_.licenseMappings']", index, HtmlTextInput.class).setText(configurationProfile.getLicenseMappings());
    }

    private static <N extends DomNode> N querySelectorNthMatch(final DomNode parent, String selectors, final int index, Class<N> elementClass) {
        return (N) parent.querySelectorAll(selectors).get(index);
    }

    private void verifyStoredConfig(String stage, final String profileName, final ConfigurationProfile expectedConfiguration) {
        assertThat(GlobalConfiguration.getConfigurationByProfile(profileName)).as(stage).satisfies(storedConfig -> assertSoftly(softly -> {
            softly.assertThat(storedConfig.getComponentMetadataUrl()).isEqualTo(expectedConfiguration.getComponentMetadataUrl());
            softly.assertThat(storedConfig.getLicensesUrl()).isEqualTo(expectedConfiguration.getLicensesUrl());
            softly.assertThat(storedConfig.getLicenseMappingsUrl()).isEqualTo(expectedConfiguration.getLicenseMappingsUrl());
        }));
    }

}
