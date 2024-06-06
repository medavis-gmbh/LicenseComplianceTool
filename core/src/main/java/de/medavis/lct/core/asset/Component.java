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

import java.util.Objects;
import java.util.Set;

import de.medavis.lct.core.license.License;

public final class Component {

    private final String group;
    private final String name;
    private final String version;
    private final String url;
    private final String purl;
    private final Set<License> licenses;

    public Component(String group, String name, String version, String url, String purl, Set<License> licenses) {
        this.group = group;
        this.name = name;
        this.version = version;
        this.url = url;
        this.purl = purl;
        this.licenses = licenses;
    }

    public String group() {
        return group;
    }

    public String name() {
        return name;
    }

    public String version() {
        return version;
    }

    public String url() {
        return url;
    }

    public String purl() {
        return purl;
    }

    public Set<License> licenses() {
        return licenses;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        Component that = (Component) obj;
        return Objects.equals(this.group, that.group) &&
               Objects.equals(this.name, that.name) &&
               Objects.equals(this.version, that.version) &&
               Objects.equals(this.url, that.url) &&
               Objects.equals(this.purl, that.purl) &&
               Objects.equals(this.licenses, that.licenses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, name, version, url, purl, licenses);
    }

    @Override
    public String toString() {
        return "Component[" +
               "group=" + group + ", " +
               "name=" + name + ", " +
               "version=" + version + ", " +
               "url=" + url + ", " +
               "purl=" + purl + ", " +
               "licenses=" + licenses + ']';
    }


}
