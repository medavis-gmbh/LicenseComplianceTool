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

import java.util.Objects;
import java.util.Set;

import de.medavis.license.comic.core.license.License;

public final class ComponentData {

    private final String name;
    private final String version;
    private final String url;
    private final Set<License> licenses;

    public ComponentData(
            String name,
            String version,
            String url,
            Set<License> licenses) {
        this.name = name;
        this.version = version;
        this.url = url;
        this.licenses = licenses;
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

    public Set<License> licenses() {
        return licenses;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (ComponentData) obj;
        return Objects.equals(this.name, that.name) &&
               Objects.equals(this.version, that.version) &&
               Objects.equals(this.url, that.url) &&
               Objects.equals(this.licenses, that.licenses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, url, licenses);
    }

    @Override
    public String toString() {
        return "ComponentData[" +
               "name=" + name + ", " +
               "version=" + version + ", " +
               "url=" + url + ", " +
               "licenses=" + licenses + ']';
    }


}
