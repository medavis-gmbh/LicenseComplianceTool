package de.medavis.lct.jenkins.patch;

import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.asset.AssetLoader;
import de.medavis.lct.core.license.LicenseLoader;
import de.medavis.lct.core.license.LicenseMappingLoader;
import de.medavis.lct.core.metadata.ComponentMetaDataLoader;
import de.medavis.lct.core.patcher.BomPatcher;

import java.util.function.Function;

// TODO Try to use dependency injection (maybe using ExtensionFinder, GuiceFinder?)
public class BomPatcherBuilderFactory {

    private static Function<Configuration, BomPatcher> bomPatcherFactory = configuration -> new BomPatcher(
            new AssetLoader(),
            new ComponentMetaDataLoader(),
            new LicenseLoader(),
            new LicenseMappingLoader(),
            configuration
    );

    private BomPatcherBuilderFactory() {}

    public static BomPatcher getBomPatcher(Configuration configuration) {
        return bomPatcherFactory.apply(configuration);
    }

    /**
     * Should only be used for tests.
     */
    static void setLicensesDownloaderFactory(Function<Configuration, BomPatcher> bomPatcherFactory) {
        BomPatcherBuilderFactory.bomPatcherFactory = bomPatcherFactory;
    }

}
