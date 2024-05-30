/*-
 * #%L
 * License Compliance Tool - Command Line Interface
 * %%
 * Copyright (C) 2022 - 2024 medavis GmbH
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

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import de.medavis.lct.core.patcher.BomPatcher;

import java.net.URI;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(name = "patch-sbom", description = "Patch SBOM with licenses mapping rules")
public class PatchSBOM implements Callable<Void> {

    @Option(names = {"--in", "-i"}, required = true)
    private Path inputFile;
    @Option(names = {"--out", "-o"}, required = true)
    private Path outputFile;
    @Option(names = {"--skipGroupNames", "-sgn"}, description = "Comma separated list of group names which will skipped during BOM patching")
    private Set<String> skipGroupNames;
    @Option(names = {"--spdxLicenseListUri", "-sllu"}, description = "URI where to download official supported SPDX licenses. If not set, then local copy will be used")
    private URI spdxLicenseListUri;
    @Option(names = {"--resolveExpressions", "-re"}, description = "When set, then license expression will be resolved and mapped into licenses")
    private boolean resolveExpressions;
    @Mixin
    private ConfigurationOptions configurationOptions;

    @Override
    public Void call() throws Exception {
        BomPatcher patcher = new BomPatcher(
                configurationOptions,
                spdxLicenseListUri,
                skipGroupNames,
                resolveExpressions
        );
        patcher.patch(inputFile, outputFile);

        return null;
    }

}
