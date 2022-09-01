package de.medavis.lct.core.creator;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import de.medavis.lct.core.list.ComponentData;

public class FreemarkerOutputter implements Outputter {

    private static final String DEFAULT_TEMPLATE = "DefaultComponentManifest.ftlh";

    private final Configuration configuration;

    public FreemarkerOutputter() {
        configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        configuration.setClassForTemplateLoading(getClass(), "");
        configuration.setDefaultEncoding("UTF-8");
    }

    @Override
    public void output(List<ComponentData> data, Path outputFile) throws IOException {
        Template template = configuration.getTemplate(DEFAULT_TEMPLATE);
        try (FileWriter writer = new FileWriter(outputFile.toFile())) {
            try {
                template.process(Collections.singletonMap("components", data), writer);
            } catch (TemplateException e) {
                throw new IllegalStateException(e);
            }
        }

    }
}
