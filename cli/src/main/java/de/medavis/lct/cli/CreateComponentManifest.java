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

    @Option(names = "--in, -i")
    private File inputPath;
    @Option(names = "--out, -o")
    private File outputFile;
    @Option(names = "--template, -t")
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
