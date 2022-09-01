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

import com.google.common.io.Resources;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.Run.Artifact;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.Mock.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import de.medavis.lct.core.creator.Format;
import de.medavis.lct.core.creator.ManifestCreator;
import de.medavis.lct.core.creator.ManifestCreatorFactory;
import de.medavis.lct.jenkins.create.CreateManifestBuilder.DescriptorImpl;

import static de.medavis.lct.util.WorkspaceResolver.getPathRelativeToWorkspace;

@ExtendWith(MockitoExtension.class)
@WithJenkins
class CreateManifestBuilderTest {

    private static final String INPUT_PATH = "input.bom";
    private static final String OUTPUT_FILE_EXTENSION = ".pdf";
    private static final String OUTPUT_PATH = "output." + OUTPUT_FILE_EXTENSION;

    @Mock(strictness = Strictness.LENIENT)
    private ManifestCreator manifestCreatorMock;

    @BeforeEach
    public void setUp() throws IOException {
        ManifestCreatorFactory.setInstance(manifestCreatorMock);
        doAnswer(invocation -> {
            Path outputPath = invocation.getArgument(2, Path.class);
            outputPath.toFile().createNewFile();
            return null;
        }).when(manifestCreatorMock).create(any(), any(), any(), any());
    }

    @Test
    void testConfigRoundtripDefaultFormat(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new CreateManifestBuilder(INPUT_PATH, OUTPUT_PATH));
        project = jenkins.configRoundtrip(project);

        CreateManifestBuilder expected = new CreateManifestBuilder(INPUT_PATH, OUTPUT_PATH);
        expected.setFormat(Format.PDF);
        jenkins.assertEqualDataBoundBeans(expected, project.getBuildersList().get(0));
    }

    @Test
    void testConfigRoundtripCustomFormat(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        final CreateManifestBuilder createManifestBuilder = new CreateManifestBuilder(INPUT_PATH, OUTPUT_PATH);
        createManifestBuilder.setFormat(Format.PDF);
        project.getBuildersList().add(createManifestBuilder);
        project = jenkins.configRoundtrip(project);

        CreateManifestBuilder expected = new CreateManifestBuilder(INPUT_PATH, OUTPUT_PATH);
        expected.setFormat(Format.PDF);
        jenkins.assertEqualDataBoundBeans(expected, project.getBuildersList().get(0));
    }

    @Test
    void testFreeStyleBuild(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new CreateManifestBuilder(INPUT_PATH, OUTPUT_PATH));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        verify(manifestCreatorMock).create(any(), eq(getPathRelativeToWorkspace(INPUT_PATH, build)),
                eq(getPathRelativeToWorkspace(OUTPUT_PATH, build)), eq(DescriptorImpl.defaultFormat));
        assertThat(build.getArtifacts()).extracting(Artifact::getFileName)
                .containsExactly(CreateManifestBuilder.ARCHIVE_FILE_NAME + OUTPUT_FILE_EXTENSION);
    }

    @Test
    void testScriptedPipelineBuild(JenkinsRule jenkins) throws Exception {
        String agentLabel = "any";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript = Resources.toString(getClass().getResource("scriptedPipeline.groovy"), Charset.defaultCharset());
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        WorkflowRun run = jenkins.buildAndAssertSuccess(job);

        verify(manifestCreatorMock).create(any(), eq(getPathRelativeToWorkspace(INPUT_PATH, run)),
                eq(getPathRelativeToWorkspace(OUTPUT_PATH, run)), eq(DescriptorImpl.defaultFormat));
        assertThat(run.getArtifacts()).extracting(Artifact::getFileName)
                .containsExactly(CreateManifestBuilder.ARCHIVE_FILE_NAME + OUTPUT_FILE_EXTENSION);
    }

    @Test
    void testDeclarativePipelineBuild(JenkinsRule jenkins) throws Exception {
        String agentLabel = "any";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-declarative-pipeline");
        String pipelineScript = Resources.toString(getClass().getResource("declarativePipeline.groovy"), Charset.defaultCharset());
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        WorkflowRun run = jenkins.buildAndAssertSuccess(job);

        verify(manifestCreatorMock).create(any(), eq(getPathRelativeToWorkspace(INPUT_PATH, run)),
                eq(getPathRelativeToWorkspace(OUTPUT_PATH, run)), eq(DescriptorImpl.defaultFormat));
        assertThat(run.getArtifacts()).extracting(Artifact::getFileName)
                .containsExactly(CreateManifestBuilder.ARCHIVE_FILE_NAME + OUTPUT_FILE_EXTENSION);
    }

}
