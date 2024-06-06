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
package de.medavis.lct.core.list;

import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import de.medavis.lct.core.license.License;

public final class ComponentData {

    private final String name;
    private final String version;
    private final String url;
    private final String purl;
    private final Set<License> licenses;
    private final Set<String> attributionNotices;

    public ComponentData(
            String name,
            String version,
            String url,
            String purl,
            Set<License> licenses,
            Set<String> attributionNotices) {
        this.name = name;
        this.version = version;
        this.url = url;
        this.purl = purl;
        this.licenses = licenses;
        this.attributionNotices = attributionNotices;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getUrl() {
        return url;
    }

    public String getPurl() {
        return purl;
    }

    public Set<License> getLicenses() {
        return licenses;
    }

    public Set<String> getAttributionNotices() {
        return attributionNotices;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ComponentData that = (ComponentData) o;
        return Objects.equals(name, that.name)
               && Objects.equals(version, that.version)
               && Objects.equals(url, that.url)
               && Objects.equals(purl, that.purl)
               && Objects.equals(licenses, that.licenses)
               && Objects.equals(attributionNotices, that.attributionNotices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, url, purl, licenses, attributionNotices);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ComponentData.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("version='" + version + "'")
                .add("url='" + url + "'")
                .add("purl='" + purl + "'")
                .add("licenses=" + licenses)
                .add("attributionNotices=" + attributionNotices)
                .toString();
    }


}
