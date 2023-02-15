package de.medavis.lct.cli;

import java.net.URL;
import java.util.Optional;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import de.medavis.lct.core.Configuration;

import static de.medavis.lct.cli.StringToUrlConverter.convert;

@Command
class ConfigurationOptions implements Configuration {

    @Option(names = {"--componentMetadata", "-cmd"})
    private String componentMetadataUrl;
    @Option(names = {"licenses", "-l"})
    private String licensesUrl;
    @Option(names = {"--licenseMapping", "-lm"})
    private String licenseMappingsUrl;

    public Optional<URL> getComponentMetadataUrl() {
        return convert(componentMetadataUrl);
    }

    public Optional<URL> getLicensesUrl() {
        return convert(licensesUrl);
    }

    public Optional<URL> getLicenseMappingsUrl() {
        return convert(licenseMappingsUrl);
    }

}
