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

import com.google.common.base.MoreObjects;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import de.medavis.lct.core.list.ComponentData;

public class FreemarkerOutputter {

    private static final String DEFAULT_TEMPLATE = "DefaultComponentManifest.ftlh";

    private final Configuration configuration;

    public FreemarkerOutputter() {
        configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        MultiTemplateLoader loader = new MultiTemplateLoader(new TemplateLoader[]{
                new ClassTemplateLoader(getClass(), ""),
                new ExternalUrlTemplateLoader()
        });
        configuration.setLocalizedLookup(false);
        configuration.setTemplateLoader(loader);
        configuration.setDefaultEncoding("UTF-8");
    }

    public void output(List<ComponentData> data, Path outputFile, String templateUrl) throws IOException {
        Template template = configuration.getTemplate(MoreObjects.firstNonNull(templateUrl, DEFAULT_TEMPLATE));
        try (FileWriter writer = new FileWriter(outputFile.toFile())) {
            try {
                template.process(Collections.singletonMap("components", data), writer);
            } catch (TemplateException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
