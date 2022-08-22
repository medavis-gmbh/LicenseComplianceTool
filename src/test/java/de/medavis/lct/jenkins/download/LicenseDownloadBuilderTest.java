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

import com.google.common.io.Resources;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import java.nio.charset.Charset;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import de.medavis.lct.core.downloader.LicenseDownloader;
import de.medavis.lct.core.downloader.LicenseDownloaderFactory;

import static de.medavis.lct.util.WorkspaceResolver.getPathRelativeToWorkspace;

@ExtendWith(MockitoExtension.class)
@WithJenkins
class LicenseDownloadBuilderTest {

    private static final String INPUT_PATH = "input.bom";
    private static final String OUTPUT_PATH = "output/licenses";

    @Mock(strictness = Strictness.LENIENT)
    private LicenseDownloader licenseDownloaderMock;

    @BeforeEach
    public void setUp() {
        LicenseDownloaderFactory.setInstance(licenseDownloaderMock);
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
    void testFreeStyleBuild(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new LicenseDownloadBuilder(INPUT_PATH, OUTPUT_PATH));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        verify(licenseDownloaderMock).download(any(), eq(getPathRelativeToWorkspace(INPUT_PATH, build)), eq(getPathRelativeToWorkspace(OUTPUT_PATH, build)));
    }

    @Test
    void testScriptedPipelineBuild(JenkinsRule jenkins) throws Exception {
        String agentLabel = "any";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript = Resources.toString(getClass().getResource("scriptedPipeline.groovy"), Charset.defaultCharset());
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        WorkflowRun run = jenkins.buildAndAssertSuccess(job);

        verify(licenseDownloaderMock).download(any(), eq(getPathRelativeToWorkspace(INPUT_PATH, run)), eq(getPathRelativeToWorkspace(OUTPUT_PATH, run)));
    }

    @Test
    void testDeclarativePipelineBuild(JenkinsRule jenkins) throws Exception {
        String agentLabel = "any";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-declarative-pipeline");
        String pipelineScript = Resources.toString(getClass().getResource("declarativePipeline.groovy"), Charset.defaultCharset());
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        WorkflowRun run = jenkins.buildAndAssertSuccess(job);

        verify(licenseDownloaderMock).download(any(), eq(getPathRelativeToWorkspace(INPUT_PATH, run)), eq(getPathRelativeToWorkspace(OUTPUT_PATH, run)));
    }

}
