package de.medavis.lct.core.creator;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlParagraph;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

import de.medavis.lct.core.license.License;
import de.medavis.lct.core.list.ComponentData;

class FreemarkerOutputterTest {

    private final FreemarkerOutputter underTest = new FreemarkerOutputter();

    @Test
    void fullAttributes(@TempDir Path outputPath) throws IOException {
        final ComponentData componentA = component("ComponentA", "1.0.0", "https://component-a.com",
                license("LIC-A", "https://license-a.com"));
        List<ComponentData> components = Collections.singletonList(componentA
        );
        final Path outputFile = outputPath.resolve("output.html");

        underTest.output(components, outputFile);

        assertThat(outputFile).exists();

        try (WebClient webClient = new WebClient()) {
            HtmlPage manifest = webClient.getPage(outputFile.toUri().toURL());
            HtmlTable table = manifest.getHtmlElementById("components");
            assertThat(table.getBodies()).hasSize(1)
                    .allSatisfy(tableBody -> {
                        assertThat(tableBody.getRows())
                                .hasSameSizeAs(components)
                                .satisfiesExactly(row -> verifyComponentRow(componentA, row));
                    });
        }
    }

    private ComponentData component(String name, String version, String url, License... licenses) {
        return new ComponentData(name, version, url, ImmutableSet.copyOf(licenses));
    }

    private License license(String name, String url) {
        return new License(name, url);
    }

    private void verifyComponentRow(ComponentData component, HtmlTableRow row) {
        assertThat(row.getCells()).hasSize(3);
        assertThat(row.getCell(0)).satisfies(isAnchor(component.getName(), component.getUrl()));
        assertThat(row.getCell(1).getTextContent()).isEqualToNormalizingWhitespace(component.getVersion());
        assertThat(row.getCell(2)).satisfies(cell -> {
            assertThat(cell.getChildElements()).hasSameSizeAs(component.getLicenses());
            List<DomElement> children = StreamSupport.stream(cell.getChildElements().spliterator(), false).collect(Collectors.toList());
            int i = 0;
            for (License license : component.getLicenses()) {
                assertThat(children.get(i))
                        .isInstanceOf(HtmlParagraph.class)
                        .satisfies(isAnchor(license.getName(), license.getUrl()));
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
}