/*-
 * #%L
 * CoMiC - Component Manifest Creator
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
package de.medavis.license.comic.core.list;

import com.google.common.base.Strings;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.medavis.license.comic.core.Configuration;
import de.medavis.license.comic.core.asset.AssetLoader;
import de.medavis.license.comic.core.asset.Component;
import de.medavis.license.comic.core.license.License;
import de.medavis.license.comic.core.license.LicenseLoader;
import de.medavis.license.comic.core.license.LicenseMappingLoader;
import de.medavis.license.comic.core.metadata.ComponentMetaDataLoader;
import de.medavis.license.comic.core.metadata.ComponentMetadata;

public class ComponentLister {

    private final AssetLoader assetLoader;
    private final ComponentMetaDataLoader componentMetaDataLoader;
    private final LicenseLoader licenseLoader;
    private final LicenseMappingLoader licenseMappingLoader;
    private final Configuration configuration;

    public ComponentLister(AssetLoader assetLoader, ComponentMetaDataLoader componentMetaDataLoader, LicenseLoader licenseLoader,
            LicenseMappingLoader licenseMappingLoader, Configuration configuration) {
        this.assetLoader = assetLoader;
        this.componentMetaDataLoader = componentMetaDataLoader;
        this.licenseLoader = licenseLoader;
        this.licenseMappingLoader = licenseMappingLoader;
        this.configuration = configuration;
    }

    public List<ComponentData> listComponents(URL bomPath) {
        Collection<ComponentMetadata> componentMetadata = configuration.getComponentMetadataUrl().map(componentMetaDataLoader::load).orElse(Collections.emptyList());
        Map<String, License> licenses = configuration.getLicensesUrl().map(licenseLoader::load).orElse(Collections.emptyMap());
        Map<String, String> licenseMappings = configuration.getLicenseMappingsUrl().map(licenseMappingLoader::load).orElse(Collections.emptyMap());

        return assetLoader.loadFromBom(bomPath)
                .components()
                .stream()
                .filter(component -> !isIgnored(component, componentMetadata))
                .map(component -> enrichWithMetadata(component, componentMetadata, licenses, licenseMappings))
                .collect(Collectors.groupingBy(ComponentData::name))
                .entrySet()
                .stream()
                .map(componentByName -> {
                    // ComponentMetadata has to ensure that component with same name has same url and version
                    String url = componentByName.getValue().get(0).url();
                    String version = componentByName.getValue().get(0).version();
                    Set<License> allLicenses = componentByName.getValue().stream()
                            .flatMap(cd -> cd.licenses().stream())
                            .distinct()
                            .collect(Collectors.toSet());
                    return new ComponentData(componentByName.getKey(), url, version, allLicenses);
                })
                .sorted(Comparator.comparing(ComponentData::name, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private boolean isIgnored(Component component, Collection<ComponentMetadata> componentMetadata) {
        return componentMetadata.stream()
                .filter(cmd -> cmd.matches(component.group(), component.name()))
                .map(ComponentMetadata::ignore)
                .findFirst()
                .orElse(false);
    }

    private ComponentData enrichWithMetadata(Component component, Collection<ComponentMetadata> componentMetadata, Map<String, License> licenses,
            Map<String, String> licenseMappings) {
        Stream<License> actualLicenses = componentMetadata.stream()
                .filter(cmd -> cmd.matches(component.group(), component.name()))
                .filter(cmd -> !cmd.licenses().isEmpty())
                .findFirst()
                .map(cmd -> cmd.licenses().stream().map(licenseName -> new License(licenseName, null, null)))
                .orElse(component.licenses().stream());

        Set<License> convertedLicenses = actualLicenses
                .map(license -> {
                    String mappedLicenseName = licenseMappings.getOrDefault(license.name(), license.name());
                    return licenses.getOrDefault(mappedLicenseName, new License(mappedLicenseName, license.url(), license.downloadUrl()));
                })
                .collect(Collectors.toSet());

        return componentMetadata.stream()
                .filter(cmd -> cmd.matches(component.group(), component.name()))
                .findFirst()
                .map(cmd -> {
                    String exportName = !Strings.isNullOrEmpty(cmd.mappedName()) ? cmd.mappedName() : combineGroupAndName(component);
                    String url = !Strings.isNullOrEmpty(cmd.url()) ? cmd.url() : component.url();
                    return new ComponentData(exportName, url, component.version(), convertedLicenses);
                })
                .orElse(new ComponentData(combineGroupAndName(component), component.url(), component.version(), convertedLicenses));
    }

    private String combineGroupAndName(Component component) {
        return String.join(".", component.group(), component.name());
    }

}
