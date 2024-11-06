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
import com.sun.net.httpserver.HttpServer;
import hudson.FilePath;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import org.apache.http.entity.ContentType;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.Mock.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

import de.medavis.lct.core.downloader.LicensesDownloader;

@ExtendWith(MockitoExtension.class)
@WithJenkins
@ExtendWith(SoftAssertionsExtension.class)
class LicenseDownloadBuilderTest {

    private static final String INPUT_PATH = "input.bom";
    private static final String OUTPUT_PATH = "output/licenses";
    private static final String CONFIGURATION_PROFILE = "default";
    private static final Map<String, String> FAKE_LICENSES = ImmutableMap.of(
            "EPL-1.0", "This is EPL-1.0",
            "LGPL-2.1", "This is LGPL-2.1",
            "MIT", "This is MIT");
    private static final String OUTPUT_EXT = ".txt";

    @Mock(strictness = Strictness.LENIENT)
    private LicensesDownloader licenseDownloaderMock;
    private String baseUrl;
    private HttpServer httpServer;

    @BeforeEach
    void configureWebserver() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/", exchange -> {
            var path = exchange.getRequestURI().getPath();
            var licenseMatch = FAKE_LICENSES.entrySet().stream()
                    .filter(license -> path.substring(1).equals(license.getKey()))
                    .map(Entry::getValue)
                    .findFirst();
            var status = licenseMatch.isPresent() ? 200 : 404;
            var body = licenseMatch.orElse("");
            exchange.getResponseHeaders().add("Content-Type", ContentType.TEXT_PLAIN.getMimeType());
            exchange.sendResponseHeaders(status, body.length());
            try (var response = exchange.getResponseBody()) {
                exchange.getResponseBody().write(body.getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().flush();
            }

        });
        httpServer.setExecutor(Executors.newSingleThreadExecutor());
        httpServer.start();

        baseUrl = String.format("http://%s:%d", httpServer.getAddress().getHostName(), httpServer.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        httpServer.stop(1);
    }

    @Test
    void testConfigRoundtrip(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        final LicenseDownloadBuilder builder = new LicenseDownloadBuilder(INPUT_PATH, OUTPUT_PATH);
        builder.setFailOnDynamicLicense(true);
        builder.setConfigurationProfile(CONFIGURATION_PROFILE);
        project.getBuildersList().add(builder);
        project = jenkins.configRoundtrip(project);

        jenkins.assertEqualDataBoundBeans(builder, project.getBuildersList().get(0));
    }

    @Test
    void testScriptedPipelineBuild(JenkinsRule jenkins, SoftAssertions softly) throws Exception {
        executePipelineAndVerifyResult(jenkins, softly, "scriptedPipeline.groovy");
    }

    @Test
    void testDeclarativePipelineBuild(JenkinsRule jenkins, SoftAssertions softly) throws Exception {
        executePipelineAndVerifyResult(jenkins, softly, "declarativePipeline.groovy");
    }

    @Test
    void testDeclarativePipelineBuildFailingOnDynamicLicenses(JenkinsRule jenkins, SoftAssertions softly) throws Exception {
        WorkflowJob job = createJob(jenkins, "declarativePipelineFailOnDynamicLicense.groovy");
        jenkins.jenkins.getWorkspaceFor(job).child("input.json").write(getModifiedInputBom(), StandardCharsets.UTF_8.name());

        var run = jenkins.buildAndAssertStatus(Result.FAILURE, job);
        assertThat(run.getLog())
                .contains("failOnDynamicLicense")
                .contains(FAKE_LICENSES.keySet());
    }

    private void executePipelineAndVerifyResult(JenkinsRule jenkins, SoftAssertions softly, String pipelineFile) throws Exception {
        WorkflowJob job = createJob(jenkins, pipelineFile);
        final FilePath workspace = jenkins.jenkins.getWorkspaceFor(job);
        workspace.child("input.json").write(getModifiedInputBom(), StandardCharsets.UTF_8.name());

        jenkins.buildAndAssertSuccess(job);

        assertThat(Paths.get(workspace.toURI()).resolve(OUTPUT_PATH))
                .isNotEmptyDirectory()
                .satisfies(outputDir -> FAKE_LICENSES.forEach((name, content) -> softly.assertThat(outputDir.resolve(name + OUTPUT_EXT)).hasContent(content)));
    }

    private WorkflowJob createJob(JenkinsRule jenkins, String name) throws IOException {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-pipeline");
        String pipelineScript = Resources.toString(getClass().getResource(name), Charset.defaultCharset());
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        return job;
    }

    private String getModifiedInputBom() throws IOException {
        return Resources.asCharSource(getClass().getResource("test-bom.json"), StandardCharsets.UTF_8)
                .read()
                .replaceAll("%BASEURL%", baseUrl);
    }

}
