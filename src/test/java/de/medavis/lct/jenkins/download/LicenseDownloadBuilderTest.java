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

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import hudson.FilePath;
import hudson.model.FreeStyleProject;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Map;
import org.apache.http.entity.ContentType;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.Mock.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import de.medavis.lct.core.downloader.LicensesDownloader;

@ExtendWith(MockitoExtension.class)
@WithJenkins
@WireMockTest
@ExtendWith(SoftAssertionsExtension.class)
class LicenseDownloadBuilderTest {

    private static final String INPUT_PATH = "input.bom";
    private static final String OUTPUT_PATH = "output/licenses";
    private static final Map<String, String> FAKE_LICENSES = ImmutableMap.of(
            "EPL-1.0", "This is EPL-1.0",
            "LGPL-2.1", "This is LGPL-2.1",
            "MIT", "This is MIT");
    private static final String OUTPUT_EXT = ".txt";

    @Mock(strictness = Strictness.LENIENT)
    private LicensesDownloader licenseDownloaderMock;
    private String baseUrl;

    @BeforeEach
    void configureWireMock(WireMockRuntimeInfo wiremock) {
        baseUrl = wiremock.getHttpBaseUrl();
        FAKE_LICENSES.forEach((licenseName, licenseText) ->
                stubFor(get("/" + licenseName).willReturn(okForContentType(ContentType.TEXT_PLAIN.getMimeType(), licenseText))));
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
    void testScriptedPipelineBuild(JenkinsRule jenkins, SoftAssertions softly) throws Exception {
        executePipelineAndVerifyResult(jenkins, softly, "scriptedPipeline.groovy");
    }

    @Test
    void testDeclarativePipelineBuild(JenkinsRule jenkins, SoftAssertions softly) throws Exception {
        executePipelineAndVerifyResult(jenkins, softly, "declarativePipeline.groovy");
    }

    private void executePipelineAndVerifyResult(JenkinsRule jenkins, SoftAssertions softly, String pipelineFile) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-pipeline");
        String pipelineScript = Resources.toString(getClass().getResource(pipelineFile), Charset.defaultCharset());
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        final FilePath workspace = jenkins.jenkins.getWorkspaceFor(job);
        workspace.child("input.json").write(getModifiedInputBom(), StandardCharsets.UTF_8.name());

        jenkins.buildAndAssertSuccess(job);

        assertThat(Paths.get(workspace.toURI()).resolve(OUTPUT_PATH))
                .isNotEmptyDirectory()
                .satisfies(outputDir -> FAKE_LICENSES.forEach((name, content) -> softly.assertThat(outputDir.resolve(name + OUTPUT_EXT)).hasContent(content)));
    }

    private String getModifiedInputBom() throws IOException {
        return Resources.asCharSource(getClass().getResource("test-bom.json"), StandardCharsets.UTF_8)
                .read()
                .replaceAll("%BASEURL%", baseUrl);
    }

}
