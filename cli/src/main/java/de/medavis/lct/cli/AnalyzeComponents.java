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
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import de.medavis.lct.core.asset.AssetLoader;
import de.medavis.lct.core.license.LicenseLoader;
import de.medavis.lct.core.license.LicenseMappingLoader;
import de.medavis.lct.core.list.ComponentLister;
import de.medavis.lct.core.metadata.ComponentMetaDataLoader;

import static de.medavis.lct.cli.AnalyzeComponents.AnalyzeMode.MISSING_URL;

@Command(name = "analyze-components", description = "Analyze components based on specified criteria")
class AnalyzeComponents implements Callable<Void> {

    enum AnalyzeMode {
        MISSING_URL
    }

    @Option(names = {"--in", "-i"}, required = true)
    private File inputFile;
    @Option(names = {"--out", "-o"}, required = true)
    private File outputFile;
    @Option(names = {"--mode", "-m"}, required = true)
    private AnalyzeMode mode;

    @Mixin
    private ConfigurationOptions configurationOptions;

    @Override
    public Void call() throws Exception {
        if(mode == MISSING_URL) {
            analyzeMissingUrl();
        }
        return null;
    }

    private void analyzeMissingUrl() throws IOException {
        var componentLister = new ComponentLister(new AssetLoader(true), new ComponentMetaDataLoader(), new LicenseLoader(), new LicenseMappingLoader(),
                configurationOptions);
        try(var bomInputStream = new FileInputStream(inputFile)) {
            var componentsWithoutUrl = componentLister.listComponents(bomInputStream).stream()
                                                      .filter(component -> component.getUrl() == null)
                                                      .collect(Collectors.toList());
            if(componentsWithoutUrl.isEmpty()) {
                System.out.println("No component without URL detected.");
            } else {
                System.out.printf("Detected %d components without URL:%n", componentsWithoutUrl.size());
                componentsWithoutUrl.forEach(component -> System.out.println(component.getName()));
            }
        }
    }

}
