package de.medavis.lct.core.downloader;

import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.asset.AssetLoader;
import de.medavis.lct.core.license.LicenseLoader;
import de.medavis.lct.core.license.LicenseMappingLoader;
import de.medavis.lct.core.list.ComponentLister;
import de.medavis.lct.core.metadata.ComponentMetaDataLoader;

public class LicenseDownloaderFactory {

    private static LicenseDownloader instance;

    private LicenseDownloaderFactory() {
    }

    public static LicenseDownloader getInstance(Configuration configuration) {
        if (instance == null) {
            instance = new LicenseDownloader(new ComponentLister(
                    new AssetLoader(),
                    new ComponentMetaDataLoader(),
                    new LicenseLoader(),
                    new LicenseMappingLoader(),
                    configuration),
                    configuration);
        }
        return instance;
    }

    /**
     * Should only be used for tests
     */
    public static void setInstance(LicenseDownloader instance) {
        LicenseDownloaderFactory.instance = instance;
    }

}
