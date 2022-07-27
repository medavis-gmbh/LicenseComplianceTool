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
package de.medavis.license.comic.core.license;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

final class LicenseMapping {

    private final String alias;
    private final String canonicalNames;

    @JsonCreator
    LicenseMapping(@JsonProperty("alias") String alias, @JsonProperty("canonicalNames") String canonicalNames) {
        this.alias = alias;
        this.canonicalNames = canonicalNames;
    }

    public String alias() {
        return alias;
    }

    public String canonicalNames() {
        return canonicalNames;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (LicenseMapping) obj;
        return Objects.equals(this.alias, that.alias) &&
               Objects.equals(this.canonicalNames, that.canonicalNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alias, canonicalNames);
    }

    @Override
    public String toString() {
        return "LicenseMapping[" +
               "alias=" + alias + ", " +
               "canonicalNames=" + canonicalNames + ']';
    }
}
