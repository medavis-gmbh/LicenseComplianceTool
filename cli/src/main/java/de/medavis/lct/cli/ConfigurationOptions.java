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

import java.net.URL;
import java.util.Optional;
import java.util.Set;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import de.medavis.lct.core.Configuration;

import static de.medavis.lct.cli.StringToUrlConverter.convert;

@Command
class ConfigurationOptions implements Configuration {

    @Option(names = {"--componentMetadata", "-cmd"})
    private String componentMetadataUrl;
    @Option(names = {"--licenses", "-l"})
    private String licensesUrl;
    @Option(names = {"--licenseMapping", "-lm"})
    private String licenseMappingsUrl;

    @Option(names = {"--licensePatchingRulesUrl", "-lpru"}, description = "URL where to download the license patching rules")
    private String licensePatchingRulesUrl;
    @Option(names = {"--spdxLicenseListUrl", "-sllu"}, description = "URL where to download official supported SPDX licenses. If not set, then local copy will be used")
    private String spdxLicenseListUri;
    @Option(names = {"--skipGroupNames", "-sgn"}, description = "Comma separated list of group names which will skipped during BOM patching")
    private Set<String> skipGroupNames;
    @Option(names = {"--resolveExpressions", "-re"}, description = "When set, then license expression will be resolved and mapped into licenses")
    private boolean resolveExpressions;

    @Override
    public Optional<URL> getComponentMetadataUrl() {
        return convert(componentMetadataUrl);
    }

    @Override
    public Optional<URL> getLicensesUrl() {
        return convert(licensesUrl);
    }

    @Override
    public Optional<URL> getLicenseMappingsUrl() {
        return convert(licenseMappingsUrl);
    }

    @Override
    public Optional<URL> getLicensePatchingRulesUrl() {
        return convert(licensePatchingRulesUrl);
    }

    @Override
    public Optional<URL> getSpdxLicensesUrl() {
        return convert(spdxLicenseListUri);
    }

    @Override
    public Optional<Set<String>> getSkipGroupNameSet() {
        return Optional.ofNullable(skipGroupNames);
    }

    @Override
    public boolean isResolveExpressions() {
        return true;
    }

}
