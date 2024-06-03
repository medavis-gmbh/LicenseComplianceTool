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
package de.medavis.lct.jenkins.download;

import java.util.function.Function;

import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.asset.AssetLoader;
import de.medavis.lct.core.downloader.LicenseFileDownloader;
import de.medavis.lct.core.downloader.LicensesDownloader;
import de.medavis.lct.core.license.LicenseLoader;
import de.medavis.lct.core.license.LicenseMappingLoader;
import de.medavis.lct.core.list.ComponentLister;
import de.medavis.lct.core.metadata.ComponentMetaDataLoader;

// TODO Try to use dependency injection (maybe using ExtensionFinder, GuiceFinder?)
class LicenseDownloadBuilderFactory {

    private static Function<Configuration, LicensesDownloader> licensesDownloaderFactory = configuration -> new LicensesDownloader(
            new ComponentLister(
                    new AssetLoader(),
                    new ComponentMetaDataLoader(),
                    new LicenseLoader(),
                    new LicenseMappingLoader(),
                    configuration),
            new LicenseFileDownloader()
    );

    private LicenseDownloadBuilderFactory() {
    }

    public static LicensesDownloader getLicensesDownloader(Configuration configuration) {
        return licensesDownloaderFactory.apply(configuration);
    }

    /**
     * Should only be used for tests
     */
    static void setLicensesDownloaderFactory(Function<Configuration, LicensesDownloader> licensesDownloaderFactory) {
        LicenseDownloadBuilderFactory.licensesDownloaderFactory = licensesDownloaderFactory;
    }

}
