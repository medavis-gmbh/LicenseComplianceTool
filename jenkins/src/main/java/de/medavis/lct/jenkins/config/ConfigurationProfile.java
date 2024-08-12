package de.medavis.lct.jenkins.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Messages;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medavis.lct.core.Configuration;
import de.medavis.lct.jenkins.util.UrlValidator;

public class ConfigurationProfile extends AbstractDescribableImpl<ConfigurationProfile> implements Configuration {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationProfile.class);

    private final String name;
    private final boolean defaultProfile;
    private String componentMetadata;
    private String licenses;
    private String licenseMappings;

    @DataBoundConstructor
    public ConfigurationProfile(final String name, final boolean defaultProfile) {
        this.name = name;
        this.defaultProfile = defaultProfile;
    }

    public String getName() {
        return name;
    }

    public boolean isDefaultProfile() {
        return defaultProfile;
    }

    public String getComponentMetadata() {
        return componentMetadata;
    }

    @DataBoundSetter
    public void setComponentMetadata(String componentMetadata) {
        this.componentMetadata = componentMetadata;
    }

    public String getLicenses() {
        return licenses;
    }

    @DataBoundSetter
    public void setLicenses(String licenses) {
        this.licenses = licenses;
    }

    public String getLicenseMappings() {
        return licenseMappings;
    }

    @DataBoundSetter
    public void setLicenseMappings(String licenseMappings) {
        this.licenseMappings = licenseMappings;
    }

    @Override
    public Optional<URL> getComponentMetadataUrl() {
        return toURL(componentMetadata);
    }

    @Override
    public Optional<URL> getLicensesUrl() {
        return toURL(licenses);
    }

    @Override
    public Optional<URL> getLicenseMappingsUrl() {
        return toURL(licenseMappings);
    }

    private Optional<URL> toURL(String url) {
        if(Strings.isNullOrEmpty(url)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new URL(url));
        } catch(MalformedURLException e) {
            LOG.warn("{} is not a valid URL - use empty value as fallback.", url);
            return Optional.empty();
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ConfigurationProfile> {

        public FormValidation doCheckName(@QueryParameter String value) {
            if(Util.fixEmptyAndTrim(value) == null) {
                return FormValidation.error(Messages.FormValidation_ValidateRequired());
            }
            return getSavedProfiles().map(ConfigurationProfile::getName).filter(value::equals).count() > 1 ?
                    FormValidation.error(de.medavis.lct.jenkins.config.Messages.ConfigurationProfile_error_duplicateName()) : FormValidation.ok();
        }

        private Stream<ConfigurationProfile> getSavedProfiles() {
            var profiles = jenkins.model.GlobalConfiguration.all().getInstance(GlobalConfiguration.class).getProfiles();
            return profiles.stream();
        }

        public FormValidation doCheckDefaultProfile(@QueryParameter boolean value) {
            if (value) {
                return getSavedProfiles().filter(ConfigurationProfile::isDefaultProfile).count() > 1 ?
                        FormValidation.error(de.medavis.lct.jenkins.config.Messages.ConfigurationProfile_error_duplicateName()) : FormValidation.ok();
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckComponentMetadata(@QueryParameter String value) {
            return UrlValidator.validate(value);
        }

        public FormValidation doCheckLicenses(@QueryParameter String value) {
            return UrlValidator.validate(value);
        }

        public FormValidation doCheckLicenseMappings(@QueryParameter String value) {
            return UrlValidator.validate(value);
        }

    }
}
