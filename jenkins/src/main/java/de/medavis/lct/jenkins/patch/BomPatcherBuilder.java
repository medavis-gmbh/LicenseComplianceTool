package de.medavis.lct.jenkins.patch;

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
import jenkins.tasks.SimpleBuildStep;

import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.patcher.BomPatcher;
import de.medavis.lct.jenkins.config.LCTGlobalConfiguration;
import de.medavis.lct.jenkins.util.JenkinsLogger;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.io.IOException;

public class BomPatcherBuilder extends Builder implements SimpleBuildStep {

    private final String inputFile;
    private final String outputFile;
    private String configurationProfile;

    @DataBoundConstructor
    public BomPatcherBuilder(@NonNull String inputFile, @NonNull String outputFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    @NonNull
    public String getInputFile() {
        return inputFile;
    }

    @NonNull
    public String getOutputFile() {
        return outputFile;
    }

    public String getConfigurationProfile() {
        return configurationProfile;
    }

    @DataBoundSetter
    public void setConfigurationProfile(final String configurationProfile) {
        this.configurationProfile = configurationProfile;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, @NonNull Launcher launcher, @NonNull TaskListener listener)
            throws AbortException, InterruptedException {
        try {
            final JenkinsLogger logger = new JenkinsLogger(listener);
            logger.info("Patching BOM from %s into %s.%n", inputFile, outputFile);

            Configuration configuration = LCTGlobalConfiguration.getConfigurationByProfile(configurationProfile);
            BomPatcher bomPatcher = BomPatcherBuilderFactory.getBomPatcher(configuration);
            bomPatcher.patch(/*logger,*/ workspace.child(inputFile).read(), workspace.child(outputFile).write());
        } catch (IOException e) {
            throw new AbortException("Could not patch licenses: " + e.getMessage());
        }
    }

    @Symbol("patchBOM")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.BomPatcherBuilder_DescriptorImpl_displayName();
        }

        @POST
        public FormValidation doCheckInputFile(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        @POST
        public FormValidation doCheckOutputFile(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

    }
}
