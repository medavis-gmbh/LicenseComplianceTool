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

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import jenkins.tasks.SimpleBuildStep;
import jenkins.util.BuildListenerAdapter;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import de.medavis.lct.core.creator.Format;
import de.medavis.lct.core.creator.ManifestCreator;
import de.medavis.lct.core.creator.ManifestCreatorFactory;
import de.medavis.lct.jenkins.config.ManifestGlobalConfiguration;
import de.medavis.lct.jenkins.util.JenkinsLogger;

import static de.medavis.lct.core.creator.Format.PDF;

public class CreateManifestBuilder extends Builder implements SimpleBuildStep {

    public static final String ARCHIVE_FILE_NAME = "componentManifest";

    private final String inputPath;
    private final String outputPath;

    private Format format = DescriptorImpl.defaultFormat;
    private ManifestCreator manifestCreator;

    @DataBoundConstructor
    public CreateManifestBuilder(@NonNull String inputPath, @NonNull String outputPath) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.manifestCreator = ManifestCreatorFactory.getInstance(ManifestGlobalConfiguration.getInstance());
    }

    public String getInputPath() {
        return inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public Format getFormat() {
        return format;
    }

    @DataBoundSetter
    public void setFormat(Format format) {
        this.format = format;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, @NonNull Launcher launcher, @NonNull TaskListener listener)
            throws AbortException, InterruptedException {
        try {
            Path inputPathAbsolute = Paths.get(workspace.child(inputPath).toURI());
            Path outputPathAbsolute = Paths.get(workspace.child(outputPath).toURI());
            manifestCreator.create(new JenkinsLogger(listener), inputPathAbsolute, outputPathAbsolute, format);
            archiveOutput(run, workspace, launcher, listener);
        } catch (IOException e) {
            throw new AbortException("Could not create component manifest: " + e.getMessage());
        }
    }

    private void archiveOutput(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        String archiveFilename = ARCHIVE_FILE_NAME + "." + format.getExtension();
        Map<String, String> file = Collections.singletonMap(archiveFilename, outputPath);
        run.pickArtifactManager().archive(workspace, launcher, new BuildListenerAdapter(listener), file);
    }

    @Symbol("componentManifest")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public static final Format defaultFormat = PDF;

        @POST
        public FormValidation doCheckInputPath(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        @POST
        public FormValidation doCheckOutputPath(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        public ListBoxModel doFillFormatItems() {
            ListBoxModel result = new ListBoxModel();
            Arrays.stream(Format.values()).forEach(f -> result.add(f.name()));
            return result;
        }

        @POST
        public FormValidation doCheckFormat(@QueryParameter String value) {
            Optional<Format> format = Format.fromString(value);
            if (!format.isPresent()) {
                return FormValidation.error(Messages.CreateManifestBuilder_DescriptorImpl_error_invalidFormat());
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.CreateManifestBuilder_DescriptorImpl_displayName();
        }

    }

}
