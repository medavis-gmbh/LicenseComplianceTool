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
package de.medavis.license.comic.core.asset;

import java.util.Objects;
import java.util.Set;

public final class Asset {

    private final String name;
    private final String version;
    private final Set<Component> components;

    public Asset(String name, String version, Set<Component> components) {
        this.name = name;
        this.version = version;
        this.components = components;
    }

    public String name() {
        return name;
    }

    public String version() {
        return version;
    }

    public Set<Component> components() {
        return components;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        Asset that = (Asset) obj;
        return Objects.equals(this.name, that.name) &&
               Objects.equals(this.version, that.version) &&
               Objects.equals(this.components, that.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, components);
    }

    @Override
    public String toString() {
        return "Asset[" +
               "name=" + name + ", " +
               "version=" + version + ", " +
               "components=" + components + ']';
    }


}
