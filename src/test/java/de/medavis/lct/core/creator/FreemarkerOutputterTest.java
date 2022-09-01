package de.medavis.lct.core.creator;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

import de.medavis.lct.core.license.License;
import de.medavis.lct.core.list.ComponentData;

class FreemarkerOutputterTest {

    private final FreemarkerOutputter underTest = new FreemarkerOutputter();

    @Test
    void shouldPutComponentsIntoTables(@TempDir Path outputPath) throws IOException {
        List<ComponentData> components = Arrays.asList(
                component("ComponentA", "1.0.0", "https://component-a.com",
                        license("LIC-A", "https://license-a.com"))
        );
        final Path outputFile = outputPath.resolve("output.html");

        underTest.output(components, outputFile);

        // TODO Check HTML
        assertThat(outputFile).exists();
    }

    private ComponentData component(String name, String version, String url, License... licenses) {
        return new ComponentData(name, version, url, ImmutableSet.copyOf(licenses));
    }

    private License license(String name, String url) {
        return new License(name, url);
    }
}