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

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import de.medavis.lct.core.downloader.LicenseDownloader;
import de.medavis.lct.core.downloader.DownloadHandler;
import de.medavis.lct.util.InputStreamContentArgumentMatcher;

import static de.medavis.lct.util.WorkspaceResolver.getWorkspacePath;

@ExtendWith(MockitoExtension.class)
@WithJenkins
class LicenseDownloadBuilderTest {

    private static final String INPUT_PATH = "input.bom";
    private static final String OUTPUT_PATH = "output/licenses";
    private static final String FAKE_SBOM = "Normally, this would be a CycloneDX SBOM.";
    private static final Map<String, String> FAKE_LICENSES = ImmutableMap.of(
            "license1", "This is LICENSE1",
            "license2", "This is LICENSE2");
    private static final String OUTPUT_EXT = ".txt";

    @Mock(strictness = Strictness.LENIENT)
    private LicenseDownloader licenseDownloaderMock;

    @BeforeEach
    public void setUp() throws IOException {
        LicenseDownloadBuilderFactory.setLicenseDownloaderFactory(configuration -> licenseDownloaderMock);
        doAnswer(invocation -> {
            DownloadHandler handler = invocation.getArgument(2, DownloadHandler.class);
            FAKE_LICENSES.forEach((name, content) -> {
                try {
                    handler.handle(name+ OUTPUT_EXT, content.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return null;
        }).when(licenseDownloaderMock).download(any(), argThat(new InputStreamContentArgumentMatcher(FAKE_SBOM)), any());
    }

    @Test
    void testConfigRoundtripDefaultFormat(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new LicenseDownloadBuilder(INPUT_PATH, OUTPUT_PATH));
        project = jenkins.configRoundtrip(project);

        LicenseDownloadBuilder expected = new LicenseDownloadBuilder(INPUT_PATH, OUTPUT_PATH);
        jenkins.assertEqualDataBoundBeans(expected, project.getBuildersList().get(0));
    }

    @Test
    void testConfigRoundtripCustomFormat(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        final LicenseDownloadBuilder licenseDownloadBuilder = new LicenseDownloadBuilder(INPUT_PATH, OUTPUT_PATH);
        project.getBuildersList().add(licenseDownloadBuilder);
        project = jenkins.configRoundtrip(project);

        LicenseDownloadBuilder expected = new LicenseDownloadBuilder(INPUT_PATH, OUTPUT_PATH);
        jenkins.assertEqualDataBoundBeans(expected, project.getBuildersList().get(0));
    }

    @Test
    void testScriptedPipelineBuild(JenkinsRule jenkins) throws Exception {
        String agentLabel = "any";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript = Resources.toString(getClass().getResource("scriptedPipeline.groovy"), Charset.defaultCharset());
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        WorkflowRun run = jenkins.buildAndAssertSuccess(job);

        assertThat(getWorkspacePath(run).resolve(OUTPUT_PATH)).isNotEmptyDirectory()
                .satisfies(outputDir -> FAKE_LICENSES.forEach((name, content) -> assertThat(outputDir.resolve(name + OUTPUT_EXT)).hasContent(content)));
    }

    @Test
    void testDeclarativePipelineBuild(JenkinsRule jenkins) throws Exception {
        String agentLabel = "any";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-declarative-pipeline");
        String pipelineScript = Resources.toString(getClass().getResource("declarativePipeline.groovy"), Charset.defaultCharset());
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        WorkflowRun run = jenkins.buildAndAssertSuccess(job);

        assertThat(getWorkspacePath(run).resolve(OUTPUT_PATH)).isNotEmptyDirectory()
                .satisfies(outputDir -> FAKE_LICENSES.forEach((name, content) -> assertThat(outputDir.resolve(name + OUTPUT_EXT)).hasContent(content)));
    }

}
