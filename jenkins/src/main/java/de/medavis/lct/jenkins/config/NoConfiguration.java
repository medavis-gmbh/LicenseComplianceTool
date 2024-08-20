package de.medavis.lct.jenkins.config;

import java.net.URL;
import java.util.Optional;

import de.medavis.lct.core.Configuration;

class NoConfiguration implements Configuration {

    @Override
    public Optional<URL> getComponentMetadataUrl() {
        return Optional.empty();
    }

    @Override
    public Optional<URL> getLicensesUrl() {
        return Optional.empty();
    }

    @Override
    public Optional<URL> getLicenseMappingsUrl() {
        return Optional.empty();
    }
}
