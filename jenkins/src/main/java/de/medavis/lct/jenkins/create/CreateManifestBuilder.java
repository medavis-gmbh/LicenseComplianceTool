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
package de.medavis.lct.jenkins.create;

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
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jenkins.tasks.SimpleBuildStep;
import jenkins.util.BuildListenerAdapter;
import org.apache.commons.io.FilenameUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.list.ComponentData;
import de.medavis.lct.jenkins.config.LCTGlobalConfiguration;
import de.medavis.lct.jenkins.util.JenkinsLogger;
import de.medavis.lct.jenkins.util.UrlValidator;


public class CreateManifestBuilder extends Builder implements SimpleBuildStep {

    public static final String ARCHIVE_FILE_NAME = "componentManifest";

    private static final Logger log = LoggerFactory.getLogger(CreateManifestBuilder.class);

    private final String inputPath;
    private final String outputPath;
    private String templateUrl;
    private boolean ignoreUnavailableUrl;
    private String configurationProfile;

    @DataBoundConstructor
    public CreateManifestBuilder(@NonNull String inputPath, @NonNull String outputPath) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    public String getInputPath() {
        return inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getTemplateUrl() {
        return templateUrl;
    }

    public boolean isIgnoreUnavailableUrl() {
        return ignoreUnavailableUrl;
    }

    public String getConfigurationProfile() {
        return configurationProfile;
    }

    @DataBoundSetter
    public void setTemplateUrl(String templateUrl) {
        this.templateUrl = templateUrl;
    }

    @DataBoundSetter
    public void setIgnoreUnavailableUrl(final boolean ignoreUnavailableUrl) {
        this.ignoreUnavailableUrl = ignoreUnavailableUrl;
    }

    @DataBoundSetter
    public void setConfigurationProfile(final String configurationProfile) {
        this.configurationProfile = configurationProfile;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, @NonNull Launcher launcher, @NonNull TaskListener listener)
            throws AbortException, InterruptedException {
        final Configuration configuration = LCTGlobalConfiguration.getConfigurationByProfile(configurationProfile);
        var componentLister = CreateManifestBuilderFactory.getComponentLister(configuration, ignoreUnavailableUrl);
        var outputter = CreateManifestBuilderFactory.getOutputterFactory();

        try {
            final JenkinsLogger logger = new JenkinsLogger(listener);
            logger.info("Writing component manifest from '%s' to '%s'.%n", inputPath, outputPath);
            try (InputStream bomStream = workspace.child(inputPath).read()) {
                List<ComponentData> components = componentLister.listComponents(bomStream);
                try (Writer manifestWriter = new OutputStreamWriter(workspace.child(outputPath).write(), StandardCharsets.UTF_8)) {
                    outputter.output(components, manifestWriter, templateUrl);
                }
                archiveOutput(run, workspace, launcher, listener);
            }
        } catch (IOException e) {
            log.error("Could not create manifest.", e);
            throw new AbortException("Could not create component manifest: " + e.getMessage());
        }
    }

    private void archiveOutput(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        String archiveFilename = ARCHIVE_FILE_NAME + "." + FilenameUtils.getExtension(outputPath);
        Map<String, String> file = Collections.singletonMap(archiveFilename, outputPath);
        run.pickArtifactManager().archive(workspace, launcher, new BuildListenerAdapter(listener), file);
    }

    @Symbol("componentManifest")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @POST
        public FormValidation doCheckConfigurationProfile(@QueryParameter String value) {
            if (Util.fixEmptyAndTrim(value) != null && LCTGlobalConfiguration.checkConfigurationProfile(value)) {
                return FormValidation.error(Messages.CreateManifestBuilder_DescriptorImpl_error_profileNotFound());
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

        @POST
        public FormValidation doCheckTemplateUrl(@QueryParameter String value) {
            return UrlValidator.validate(value);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return de.medavis.lct.jenkins.create.Messages.CreateManifestBuilder_DescriptorImpl_displayName();
        }

    }

}
