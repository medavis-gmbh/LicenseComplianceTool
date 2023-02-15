/*-
 * #%L
 * License Compliance Tool - Command Line Interface
 * %%
 * Copyright (C) 2022 - 2023 medavis GmbH
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
package de.medavis.lct.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.net.URL;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import de.medavis.lct.core.asset.AssetLoader;
import de.medavis.lct.core.license.LicenseLoader;
import de.medavis.lct.core.license.LicenseMappingLoader;
import de.medavis.lct.core.list.ComponentLister;
import de.medavis.lct.core.metadata.ComponentMetaDataLoader;
import de.medavis.lct.core.outputter.FreemarkerOutputter;

@Command(name = "component-manifest", description = "Create component manifest")
class CreateComponentManifest implements Callable<Void> {

    @Option(names = {"--in", "-i"}, required = true)
    private File inputPath;
    @Option(names = {"--out", "-o"}, required = true)
    private File outputFile;
    @Option(names = {"--template", "-t"})
    private String template;
    @Mixin
    private ConfigurationOptions configurationOptions;

    @Override
    public Void call() throws Exception {
        var componentLister = new ComponentLister(new AssetLoader(), new ComponentMetaDataLoader(), new LicenseLoader(), new LicenseMappingLoader(),
                configurationOptions);
        try (var bomInputStream = new FileInputStream(inputPath); var outputWriter = new FileWriter(outputFile)) {
            var components = componentLister.listComponents(bomInputStream);
            new FreemarkerOutputter().output(components, outputWriter, getTemplateUrl());
        }
        return null;
    }

    private String getTemplateUrl() {
        return StringToUrlConverter.convert(template)
                .map(URL::toString)
                .orElse(null);
    }
}
