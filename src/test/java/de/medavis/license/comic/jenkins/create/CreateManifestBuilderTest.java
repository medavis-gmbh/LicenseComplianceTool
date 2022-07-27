/*-
 * #%L
 * CoMiC - Component Manifest Creator
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
package de.medavis.license.comic.jenkins.create;

import com.google.common.io.Resources;
import hudson.model.AbstractBuild;
import hudson.model.Label;
import hudson.model.Run.Artifact;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.StreamSupport;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.actions.WorkspaceActionImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import de.medavis.license.comic.core.creator.Format;
import de.medavis.license.comic.core.creator.ManifestCreator;
import de.medavis.license.comic.core.creator.ManifestCreatorFactory;
import de.medavis.license.comic.jenkins.create.CreateManifestBuilder.DescriptorImpl;

public class CreateManifestBuilderTest {

    private static final String INPUT_PATH = "input.bom";
    private static final String OUTPUT_PATH = "output.pdf";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private ManifestCreator manifestCreatorMock;

    @Before
    public void setUp() throws MalformedURLException {
        ManifestCreatorFactory.setInstance(manifestCreatorMock);
        doAnswer(invocation -> {
            var outputPath = invocation.getArgument(2, Path.class);
            outputPath.toFile().createNewFile();
            return null;
        }).when(manifestCreatorMock).create(any(), any(), any(), any());
    }

    @Test
    public void testConfigRoundtripDefaultFormat() throws Exception {
        var project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new CreateManifestBuilder(INPUT_PATH, OUTPUT_PATH));
        project = jenkins.configRoundtrip(project);

        var expected = new CreateManifestBuilder(INPUT_PATH, OUTPUT_PATH);
        expected.setFormat(Format.PDF);
        jenkins.assertEqualDataBoundBeans(expected, project.getBuildersList().get(0));
    }

    @Test
    public void testConfigRoundtripCustomFormat() throws Exception {
        var project = jenkins.createFreeStyleProject();
        final var createManifestBuilder = new CreateManifestBuilder(INPUT_PATH, OUTPUT_PATH);
        createManifestBuilder.setFormat(Format.HTML);
        project.getBuildersList().add(createManifestBuilder);
        project = jenkins.configRoundtrip(project);

        var expected = new CreateManifestBuilder(INPUT_PATH, OUTPUT_PATH);
        expected.setFormat(Format.HTML);
        jenkins.assertEqualDataBoundBeans(expected, project.getBuildersList().get(0));
    }

    @Test
    public void testFreeStyleBuild() throws Exception {
        var project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new CreateManifestBuilder(INPUT_PATH, OUTPUT_PATH));

        var build = jenkins.buildAndAssertSuccess(project);

        verify(manifestCreatorMock).create(any(), eq(pathRelativeToWorkspace(INPUT_PATH, build)),
                eq(pathRelativeToWorkspace(OUTPUT_PATH, build)), eq(DescriptorImpl.defaultFormat));
        assertThat(build.getArtifacts()).extracting(Artifact::getFileName)
                .containsExactly(CreateManifestBuilder.ARCHIVE_FILE_NAME + "." + DescriptorImpl.defaultFormat.getExtension());
    }

    @Test
    public void testScriptedPipelineBuild() throws Exception {
        String agentLabel = "any";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript = Resources.toString(getClass().getResource("scriptedPipeline.groovy"), Charset.defaultCharset());
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        var run = jenkins.buildAndAssertSuccess(job);

        verify(manifestCreatorMock).create(any(), eq(pathRelativeToWorkspace(INPUT_PATH, run)),
                eq(pathRelativeToWorkspace(OUTPUT_PATH, run)), eq(DescriptorImpl.defaultFormat));
        assertThat(run.getArtifacts()).extracting(Artifact::getFileName)
                .containsExactly(CreateManifestBuilder.ARCHIVE_FILE_NAME + "." + DescriptorImpl.defaultFormat.getExtension());
    }

    @Test
    public void testDeclarativePipelineBuild() throws Exception {
        String agentLabel = "any";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-declarative-pipeline");
        String pipelineScript = Resources.toString(getClass().getResource("declarativePipeline.groovy"), Charset.defaultCharset());
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        var run = jenkins.buildAndAssertSuccess(job);

        verify(manifestCreatorMock).create(any(), eq(pathRelativeToWorkspace(INPUT_PATH, run)),
                eq(pathRelativeToWorkspace(OUTPUT_PATH, run)), eq(DescriptorImpl.defaultFormat));
        assertThat(run.getArtifacts()).extracting(Artifact::getFileName)
                .containsExactly(CreateManifestBuilder.ARCHIVE_FILE_NAME + "." + DescriptorImpl.defaultFormat.getExtension());
    }

    private Path pathRelativeToWorkspace(String path, AbstractBuild build) throws IOException, InterruptedException {
        return Paths.get(build.getWorkspace().child(path).toURI());
    }

    private Path pathRelativeToWorkspace(String path, WorkflowRun build) {
        return StreamSupport.stream(new FlowGraphWalker(build.getExecution()).spliterator(), false)
                .filter(StepStartNode.class::isInstance)
                .flatMap(flowNode -> flowNode.getActions().stream())
                .filter(WorkspaceActionImpl.class::isInstance)
                .map(WorkspaceActionImpl.class::cast)
                .map(WorkspaceActionImpl::getPath)
                .map(Paths::get)
                .map(workspace -> workspace.resolve(path))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Could not determine workspace location."));
    }
}
