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

import de.medavis.lct.core.patcher.BomPatcher;
import de.medavis.lct.jenkins.config.ManifestGlobalConfiguration;
import de.medavis.lct.jenkins.util.JenkinsLogger;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.io.IOException;

public class BomPatcherBuilder extends Builder implements SimpleBuildStep {

    private final String inputFile;
    private final String outputFile;
    private final transient BomPatcher bomPatcher;

    @DataBoundConstructor
    public BomPatcherBuilder(@NonNull String inputFile, @NonNull String outputFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;

        this.bomPatcher = BomPatcherBuilderFactory.getBomPatcher(ManifestGlobalConfiguration.getInstance());
    }

    public String getInputFile() {
        return inputFile;
    }

    public String getOutputFile() {
        return outputFile;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, @NonNull Launcher launcher, @NonNull TaskListener listener)
            throws AbortException, InterruptedException {
        try {
            final JenkinsLogger logger = new JenkinsLogger(listener);
            logger.info("Patching BOM from %s into %s.%n", inputFile, outputFile);
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
