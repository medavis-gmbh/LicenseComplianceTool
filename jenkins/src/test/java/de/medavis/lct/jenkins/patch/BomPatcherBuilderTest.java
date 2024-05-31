package de.medavis.lct.jenkins.patch;

import com.google.common.io.Resources;
import com.sun.net.httpserver.HttpServer;
import hudson.FilePath;
import hudson.model.FreeStyleProject;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.assertj.core.api.SoftAssertions;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@WithJenkins
class BomPatcherBuilderTest {

    private static final String INPUT_FILE = "input.bom";
    private static final String OUTPUT_FILE = "output.bom";

    private static final String OUTPUT_PATH = "output/licenses";
    private static final Map<String, String> FAKE_LICENSES = Map.of(
            "EPL-1.0", "This is EPL-1.0",
            "LGPL-2.1", "This is LGPL-2.1",
            "MIT", "This is MIT");
    private static final String OUTPUT_EXT = ".txt";

    private String baseUrl;
    private HttpServer httpServer;

    @BeforeEach
    void configureWebserver() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/", exchange -> {
            var path = exchange.getRequestURI().getPath();
            var licenseMatch = FAKE_LICENSES.entrySet().stream()
                    .filter(license -> path.substring(1).equals(license.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst();
            var status = licenseMatch.isPresent() ? 200 : 404;
            String body = IOUtils.resourceToString("/de/medavis/lct/jenkins/patch/DefaultLicenseRules.json", StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
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
    @Disabled
    void testConfigRoundtrip(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        final BomPatcherBuilder builder = new BomPatcherBuilder(INPUT_FILE, OUTPUT_FILE);
        //project.setLi
        project.getBuildersList().add(builder);
        project = jenkins.configRoundtrip(project);

        jenkins.assertEqualDataBoundBeans(builder, project.getBuildersList().get(0));
    }

    @Test
    @Disabled
    void testScriptedPipelineBuild(JenkinsRule jenkins, SoftAssertions softly) throws Exception {
        executePipelineAndVerifyResult(jenkins, softly, "scriptedPipeline.groovy");
    }

    @Test
    @Disabled
    void testDeclarativePipelineBuild(JenkinsRule jenkins, SoftAssertions softly) throws Exception {
        executePipelineAndVerifyResult(jenkins, softly, "declarativePipeline.groovy");
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
