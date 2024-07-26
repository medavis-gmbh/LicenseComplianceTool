package de.medavis.lct.jenkins.patch;

import com.google.common.io.Resources;
import com.sun.net.httpserver.HttpServer;
import hudson.FilePath;
import hudson.model.FreeStyleProject;

import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.asset.AssetLoader;
import de.medavis.lct.core.license.LicenseLoader;
import de.medavis.lct.core.license.LicenseMappingLoader;
import de.medavis.lct.core.metadata.ComponentMetaDataLoader;
import de.medavis.lct.core.patcher.BomPatcher;

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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@WithJenkins
@ExtendWith(SoftAssertionsExtension.class)
class BomPatcherBuilderTest {

    private static final String INPUT_FILE = "input.bom";
    private static final String OUTPUT_FILE = "output.bom";

    private static final String PATH = "/test-component-metadata.json";

    private String baseUrl;
    private HttpServer httpServer;

    @BeforeEach
    void configureWebserver() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext(PATH, exchange -> {
            URL url = getClass().getResource("/de/medavis/lct/jenkins/patch/DefaultLicenseRules.json5");
            String body = Resources.toString(url, StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
            exchange.sendResponseHeaders(200, body.length());
            try (var response = exchange.getResponseBody()) {
                response.write(body.getBytes(StandardCharsets.UTF_8));
                response.flush();
            }
        });
        httpServer.setExecutor(Executors.newSingleThreadExecutor());
        httpServer.start();

        baseUrl = String.format("http://%s:%d", httpServer.getAddress().getHostName(), httpServer.getAddress().getPort());

        BomPatcherBuilderFactory.setLicensesDownloaderFactory(configuration -> new BomPatcher(
                new AssetLoader(),
                new ComponentMetaDataLoader(),
                new LicenseLoader(),
                new LicenseMappingLoader(),
                new Configuration() {

            @Override
            public Optional<URL> getLicenseMappingsUrl() {
                try {
                    URL url = new URL(baseUrl + PATH);
                    return Optional.of(url);
                } catch (MalformedURLException ex) {

                    return Optional.empty();
                }
            }

            @Override
            public boolean isResolveExpressions() {
                return true;
            }

        }));
    }

    @AfterEach
    void tearDown() {
        httpServer.stop(1);
    }

    @Test
    void testConfigRoundtrip(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        final BomPatcherBuilder builder = new BomPatcherBuilder(INPUT_FILE, OUTPUT_FILE);
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

    private void executePipelineAndVerifyResult(JenkinsRule jenkins, SoftAssertions softly, String pipelineFile) throws Exception {
        WorkflowJob job = createJob(jenkins, pipelineFile);
        final FilePath workspace = jenkins.jenkins.getWorkspaceFor(job);
        workspace.child("input.bom").write(getModifiedInputBom(), StandardCharsets.UTF_8.name());

        jenkins.buildAndAssertSuccess(job);

        assertThat(Paths.get(workspace.toURI()).resolve(OUTPUT_FILE))
                .exists();
    }

    private WorkflowJob createJob(JenkinsRule jenkins, String pipelineFile) throws IOException {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-pipeline");
        String pipelineScript = Resources.toString(getClass().getResource(pipelineFile), Charset.defaultCharset());
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        return job;
    }

    private String getModifiedInputBom() throws IOException {
        return Resources.asCharSource(getClass().getResource("test-bom.json"), StandardCharsets.UTF_8)
                .read()
                .replaceAll("%BASEURL%", baseUrl);
    }
}
