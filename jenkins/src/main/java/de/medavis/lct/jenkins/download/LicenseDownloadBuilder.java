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
package de.medavis.lct.jenkins.download;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import java.io.IOException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import de.medavis.lct.jenkins.config.LCTGlobalConfiguration;
import de.medavis.lct.jenkins.util.JenkinsLogger;

public class LicenseDownloadBuilder extends Builder implements SimpleBuildStep {

    private final String inputPath;
    private final String outputPath;
    private boolean failOnDynamicLicense;
    private String configurationProfile;

    @DataBoundConstructor
    public LicenseDownloadBuilder(@NonNull String inputPath, @NonNull String outputPath) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    public String getInputPath() {
        return inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public boolean isFailOnDynamicLicense() {
        return failOnDynamicLicense;
    }

    public String getConfigurationProfile() {
        return configurationProfile;
    }

    @DataBoundSetter
    public void setFailOnDynamicLicense(boolean failOnDynamicLicense) {
        this.failOnDynamicLicense = failOnDynamicLicense;
    }

    @DataBoundSetter
    public void setConfigurationProfile(final String configurationProfile) {
        this.configurationProfile = configurationProfile;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, @NonNull Launcher launcher, @NonNull TaskListener listener)
            throws AbortException, InterruptedException {
         var licenseDownloader = LicenseDownloadBuilderFactory.getLicensesDownloader(LCTGlobalConfiguration.getConfigurationByProfile(configurationProfile));

        try {
            final JenkinsLogger logger = new JenkinsLogger(listener);
            logger.info("Downloading licenses from components in %s to %s.%n", inputPath, outputPath);
            licenseDownloader.download(logger, workspace.child(inputPath).read(), new JenkinsLicenseFileHandler(workspace, outputPath), failOnDynamicLicense);
        } catch (IOException e) {
            throw new AbortException("Could not download licenses: " + e.getMessage());
        }
    }

    @Symbol("downloadLicenses")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.LicenseDownloadBuilder_DescriptorImpl_displayName();
        }

        @POST
        public FormValidation doCheckConfigurationProfile(@QueryParameter String value) {
            if (Util.fixEmptyAndTrim(value) != null && LCTGlobalConfiguration.checkConfigurationProfile(value)) {
                return FormValidation.error(Messages.LicenseDownloadBuilder_DescriptorImpl_error_profileNotFound());
            }
            return FormValidation.ok(value);
        }

        @POST
        public FormValidation doCheckInputPath(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        @POST
        public FormValidation doCheckOutputPath(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

    }
}
