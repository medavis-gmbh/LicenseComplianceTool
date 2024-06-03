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

import com.google.common.base.Strings;
import hudson.Extension;
import hudson.model.PersistentDescriptor;
import hudson.util.FormValidation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medavis.lct.core.Configuration;
import de.medavis.lct.jenkins.util.UrlValidator;

@Extension
public class ManifestGlobalConfiguration extends GlobalConfiguration implements PersistentDescriptor, Configuration {

    private static final Logger LOG = LoggerFactory.getLogger(ManifestGlobalConfiguration.class);
    private String componentMetadata;
    private String licenses;
    private String licenseMappings;

    private String licensePatchingRules;
    private String spdxLicenses;
    private boolean resolveExpressions;

    public static ManifestGlobalConfiguration getInstance() {
        return GlobalConfiguration.all().getInstance(ManifestGlobalConfiguration.class);
    }

    public String getComponentMetadata() {
        return componentMetadata;
    }


    @Override
    public Optional<URL> getComponentMetadataUrl() {
        return toURL(componentMetadata);
    }

    @DataBoundSetter
    public void setComponentMetadata(String componentMetadata) {
        this.componentMetadata = componentMetadata;
        save();
    }

    public FormValidation doCheckComponentMetadata(@QueryParameter String value) {
        return UrlValidator.validate(value);
    }

    public String getLicenses() {
        return licenses;
    }

    @Override
    public Optional<URL> getLicensesUrl() {
        return toURL(licenses);
    }

    @DataBoundSetter
    public void setLicenses(String licenses) {
        this.licenses = licenses;
        save();
    }

    public FormValidation doCheckLicenses(@QueryParameter String value) {
        return UrlValidator.validate(value);
    }

    public String getLicenseMappings() {
        return licenseMappings;
    }

    @Override
    public Optional<URL> getLicenseMappingsUrl() {
        return toURL(licenseMappings);
    }

    @DataBoundSetter
    public void setLicenseMappings(String licenseMappings) {
        this.licenseMappings = licenseMappings;
        save();
    }

    public String getLicensePatchingRules() {
        return licensePatchingRules;
    }

    @Override
    public Optional<URL> getLicensePatchingRulesUrl() {
        return toURL(licensePatchingRules);
    }

    @DataBoundSetter
    public void setLicensePatchingRules(String licensePatchingRules) {
        this.licensePatchingRules = licensePatchingRules;
        save();
    }

    public String getSpdxLicenses() {
        return spdxLicenses;
    }

    @Override
    public Optional<URL> getSpdxLicensesUrl() {
        return toURL(spdxLicenses);
    }

    @DataBoundSetter
    public void setSpdxLicenses(String spdxLicenses) {
        this.spdxLicenses = spdxLicenses;
        save();
    }

    @Override
    public boolean isResolveExpressions() {
        return resolveExpressions;
    }

    @DataBoundSetter
    public void setResolveExpressions(boolean resolveExpressions) {
        this.resolveExpressions = resolveExpressions;
        save();
    }

    private Optional<URL> toURL(String url) {
        if (Strings.isNullOrEmpty(url)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new URL(url));
        } catch (MalformedURLException e) {
            LOG.warn("{} is not a valid URL - use empty value as fallback.", url);
            return Optional.empty();
        }
    }

    public FormValidation doCheckLicenseMappings(@QueryParameter String value) {
        return UrlValidator.validate(value);
    }

}