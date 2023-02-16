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
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import de.medavis.lct.core.asset.AssetLoader;
import de.medavis.lct.core.downloader.LicenseFileDownloader;
import de.medavis.lct.core.downloader.LicensesDownloader;
import de.medavis.lct.core.license.LicenseLoader;
import de.medavis.lct.core.license.LicenseMappingLoader;
import de.medavis.lct.core.list.ComponentLister;
import de.medavis.lct.core.metadata.ComponentMetaDataLoader;

@Command(name = "download-licenses", description = "Download license texts")
class DownloadLicenses implements Callable<Void> {

    @Option(names = {"--in", "-i"}, required = true)
    private File inputFile;
    @Option(names = {"--out", "-o"}, required = true)
    private Path outputPath;
    @Mixin
    private ConfigurationOptions configurationOptions;

    @Override
    public Void call() throws Exception {
        var componentLister = new ComponentLister(new AssetLoader(), new ComponentMetaDataLoader(), new LicenseLoader(), new LicenseMappingLoader(),
                configurationOptions);
        LicensesDownloader licensesDownloader = new LicensesDownloader(componentLister, new LicenseFileDownloader());
        try (var bomInputStream = new FileInputStream(inputFile)) {
            licensesDownloader.download(new ConsoleUserLogger(), bomInputStream, new FilesystemLicenseFileHandler(outputPath));
        }
        return null;
    }
}
