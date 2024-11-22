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
package de.medavis.lct.core.outputter;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlParagraph;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.medavis.lct.core.license.License;
import de.medavis.lct.core.list.ComponentData;

class FreemarkerOutputterTest {

    private final FreemarkerOutputter underTest = new FreemarkerOutputter();

    private void output(List<ComponentData> components, Path outputFile, String templateUrl) throws IOException {
        try (Writer outputStream = Files.newBufferedWriter(outputFile)) {
            underTest.output(components, outputStream, templateUrl);
        }
    }

    @Nested
    class ComponentTable {

        @Test
        void fullAttributes(@TempDir Path outputPath) throws IOException {
            final ComponentData componentA = createComponent("ComponentA","1.0.0", "https://component-a.com", null,
                    createLicenses("LIC-A", "https://license-a.com"), Collections.singleton("Copyright (c) 2020"));
            final ComponentData componentB = createComponent("ComponentB", "2.0.0", "https://component-b.com", null,
                    createLicenses("LIC-B", "https://license-b.com"), ImmutableSet.of("Copyright (c) 2015", "Resistance is futile"));

            createAndVerifyOutput(outputPath, componentA, componentB);
        }

        @Test
        void missingComponentUrl(@TempDir Path outputPath) throws IOException {
            final ComponentData component = createComponent("ComponentA", "1.0.0", null, null,
                    createLicenses("LIC-A", "https://license-a.com"), Collections.singleton("Copyright (c) 2020"));

            createAndVerifyOutput(outputPath, component);
        }

        @Test
        void missingVersionUrl(@TempDir Path outputPath) throws IOException {
            final ComponentData component = createComponent("ComponentA", null, "https://component-a.com", null,
                    createLicenses("LIC-A", "https://license-a.com"), Collections.singleton("Copyright (c) 2020"));

            createAndVerifyOutput(outputPath, component);
        }

        @Test
        void missingLicenseUrl(@TempDir Path outputPath) throws IOException {
            final ComponentData component = createComponent("ComponentA", "1.0.0", null, null,
                    createLicenses("LIC-A", null), Collections.singleton("Copyright (c) 2020"));

            createAndVerifyOutput(outputPath, component);
        }

        @Test
        void noLicenses(@TempDir Path outputPath) throws IOException {
            final ComponentData component = createComponent("ComponentA", "1.0.0", "https://component-a.com", null, Collections.emptySet(),
                    Collections.singleton("Copyright (c) 2020"));

            createAndVerifyOutput(outputPath, component);
        }

        @Test
        void noAttributionNotices(@TempDir Path outputPath) throws IOException {
            final ComponentData component = createComponent("ComponentA", "1.0.0", "https://component-a.com", null, createLicenses("LIC-A", "https://license-a.com"),
                    Collections.emptySet());

            createAndVerifyOutput(outputPath, component);
        }

        private ComponentData createComponent(String name, String version, String url, String purl, Set<License> licenses, Set<String> attributionNotices) {
            return new ComponentData(name, version, url, purl, licenses, attributionNotices);
        }

        private Set<License> createLicenses(String name, String url) {
            return Collections.singleton(License.dynamic(name, url));
        }

        private void createAndVerifyOutput(Path outputPath, ComponentData... components) throws IOException {
            final Path outputFile = outputPath.resolve("output.html");
            output(Arrays.asList(components), outputFile, null);
            assertThat(outputFile).exists();
            try (WebClient webClient = new WebClient()) {
                HtmlPage manifest = webClient.getPage(outputFile.toUri().toURL());
                HtmlTable table = manifest.getHtmlElementById("components");
                assertThat(table.getBodies()).hasSize(1)
                        .satisfiesExactly(tableBody -> {
                            assertThat(tableBody.getRows()).hasSameSizeAs(components);
                            for (int i = 0; i < tableBody.getRows().size(); i++) {
                                ComponentData component = components[i];
                                assertThat(tableBody.getRows().get(i)).satisfies(row -> verifyComponentRow(component, row));
                            }
                        });
            }
        }

        private void verifyComponentRow(ComponentData component, HtmlTableRow row) {
            assertThat(row.getCells()).hasSize(4);
            assertThat(row.getCell(0)).satisfies(component.getUrl() != null ? isAnchor(component.getName(), component.getUrl()) : isText(component.getName()));
            assertThat(row.getCell(1)).satisfies(component.getVersion() != null ? isText(component.getVersion()) : isEmpty());
            assertThat(row.getCell(2)).satisfies(cell -> {
                assertThat(cell.getChildElements()).hasSameSizeAs(component.getLicenses());
                List<DomElement> children = StreamSupport.stream(cell.getChildElements().spliterator(), false).collect(Collectors.toList());
                int i = 0;
                for (License license : component.getLicenses()) {
                    assertThat(children.get(i))
                            .isInstanceOf(HtmlParagraph.class)
                            .satisfies(license.getUrl() != null ? isAnchor(license.getName(), license.getUrl()) : isText(license.getName()));
                    i++;
                }
            });
            assertThat(row.getCell(3)).satisfies(cell -> {
                assertThat(cell.getChildElements()).hasSameSizeAs(component.getAttributionNotices());
                List<DomElement> children = StreamSupport.stream(cell.getChildElements().spliterator(), false).collect(Collectors.toList());
                int i = 0;
                for (String attributionNotice : component.getAttributionNotices()) {
                    assertThat(children.get(i))
                            .isInstanceOf(HtmlParagraph.class)
                            .satisfies(isText(attributionNotice));
                    i++;
                }
            });
        }

        private Consumer<? super DomElement> isAnchor(String name, String url) {
            return cell -> {
                assertThat(StreamSupport.stream(cell.getChildElements().spliterator(), false)
                        .filter(HtmlAnchor.class::isInstance)
                        .map(HtmlAnchor.class::cast)
                        .findFirst()).hasValueSatisfying(a -> {
                    assertThat(a).extracting(HtmlAnchor::getTextContent).isEqualTo(name);
                    assertThat(a).extracting(HtmlAnchor::getHrefAttribute).isEqualTo(url);
                });
            };
        }

        private Consumer<DomElement> isText(String text) {
            return cell -> assertThat(cell.getTextContent()).isEqualToNormalizingWhitespace(text);
        }

        private Consumer<DomElement> isEmpty() {
            return cell -> assertThat(cell.getTextContent()).isEmpty();
        }

    }

    @Nested
    @WireMockTest
    class Templates {

        private static final String TEST_TEMPLATE_NAME = "test.ftlh";
        private static final String TEST_TEMPLATE_CONTENT_PART = "Content part of template";

        @Test
        void fromFilesystem(@TempDir Path tempDir) throws IOException {
            Path template = tempDir.resolve("input.ftlh");
            FileUtils.copyURLToFile(getClass().getResource(TEST_TEMPLATE_NAME), template.toFile());
            final Path output = tempDir.resolve("output.html");

            output(Collections.emptyList(), output, template.toUri().toURL().toString());

            assertThat(output)
                    .exists()
                    .content().contains(TEST_TEMPLATE_CONTENT_PART);
        }

        @Test
        void fromHttp(@TempDir Path tempDir, WireMockRuntimeInfo wiremock) throws IOException {
            String templateRelativeUrl = "/template";
            stubFor(get(templateRelativeUrl).willReturn(ok(Resources.toString(getClass().getResource(TEST_TEMPLATE_NAME), StandardCharsets.UTF_8))));
            final Path output = tempDir.resolve("output.html");

            output(Collections.emptyList(), output, wiremock.getHttpBaseUrl() + templateRelativeUrl);

            assertThat(output)
                    .exists()
                    .content().contains(TEST_TEMPLATE_CONTENT_PART);
        }

        @Test
        void failOnInexistentTemplate(@TempDir Path tempDir, WireMockRuntimeInfo wiremock) {
            String templateRelativeUrl = "/template";
            stubFor(get(templateRelativeUrl).willReturn(notFound()));
            final Path output = tempDir.resolve("output.html");

            assertThatThrownBy(() -> output(Collections.emptyList(), output, wiremock.getHttpBaseUrl() + templateRelativeUrl))
                    .isInstanceOf(IOException.class);
        }
    }
}
