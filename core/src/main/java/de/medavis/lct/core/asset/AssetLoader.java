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
package de.medavis.lct.core.asset;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.ExternalReference.Type;
import org.cyclonedx.parsers.BomParserFactory;

import de.medavis.lct.core.license.License;
import de.medavis.lct.core.urlchecker.OnlineHttpUrlChecker;
import de.medavis.lct.core.urlchecker.HttpUrlChecker;

public class AssetLoader {

    private final HttpUrlChecker urlChecker;

    public AssetLoader() {
        this(false);
    }

    public AssetLoader(final boolean checkUrlAvailability) {
        this.urlChecker = checkUrlAvailability ? new OnlineHttpUrlChecker() : url -> true;
    }

    public Asset loadFromBom(InputStream bomStream) {
        Bom assetBom = parseBom(bomStream);
        final String group = assetBom.getMetadata().getComponent().getGroup();
        final String name = assetBom.getMetadata().getComponent().getName();
        String assetName = Strings.isNullOrEmpty(group) ? name : Joiner.on(".").join(group, name);
        String assetVersion = assetBom.getMetadata().getComponent().getVersion();
        Set<Component> components = assetBom.getComponents() == null
                ? Collections.emptySet()
                : assetBom.getComponents().parallelStream()
                        // FIXME Find out what the scope exactly means and why some components are added that are not in the BOM
//                        .filter(component -> component.getScope() != null)
                        .map(this::bomComponentToEntity)
                        .collect(Collectors.toSet());
        return new Asset(assetName, assetVersion, components);
    }

    private Bom parseBom(InputStream bomStream) {
        try {
            final byte[] bom = ByteStreams.toByteArray(bomStream);
            return BomParserFactory.createParser(bom).parse(bom);
        } catch (ParseException | IOException e) {
            throw new IllegalStateException("Cannot parse BOM file " + bomStream, e);
        }
    }

    private Component bomComponentToEntity(org.cyclonedx.model.Component component) {
        String group = component.getGroup();
        String name = component.getName();
        String version = component.getVersion();
        String url = getWebsite(component.getExternalReferences());
        Set<License> licenses = getLicenseStream(component)
                .map(this::extractLicense)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return new Component(group, name, version, url, licenses);
    }

    private License extractLicense(org.cyclonedx.model.License license) {
        String name = license.getId();
        if (name == null) {
            name = license.getName();
        }
        if (name == null) {
            // If neither id nor name are set, we cannot use the license
            return null;
        }

        return License.dynamic(name, license.getUrl(), null);
    }

    private String getWebsite(List<ExternalReference> externalReferences) {
        return getUrl(externalReferences, Type.VCS)
                .orElseGet(() -> getUrl(externalReferences, Type.WEBSITE)
                        .orElse(null));
    }

    private Optional<String> getUrl(List<ExternalReference> externalReferences, Type type) {
        return externalReferences != null ?
                externalReferences.stream()
                        .filter(ref -> ref.getType() == type)
                        .map(ExternalReference::getUrl)
                        .filter(urlChecker::isUrlAvailable)
                        .findFirst()
                : Optional.empty();
    }

    private Stream<org.cyclonedx.model.License> getLicenseStream(org.cyclonedx.model.Component component) {
        return component.getLicenseChoice() != null && component.getLicenseChoice().getLicenses() != null
                ? component.getLicenseChoice().getLicenses().stream()
                : Stream.empty();
    }

}
