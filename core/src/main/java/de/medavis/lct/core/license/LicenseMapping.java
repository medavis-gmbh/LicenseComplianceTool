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
package de.medavis.lct.core.license;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

final class LicenseMapping {

    private final String alias;
    private final String canonicalName;

    @JsonCreator
    LicenseMapping(@JsonProperty("alias") String alias, @JsonProperty("canonicalName") String canonicalName) {
        this.alias = alias;
        this.canonicalName = canonicalName;
    }

    public String alias() {
        return alias;
    }

    public String canonicalName() {
        return canonicalName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        LicenseMapping that = (LicenseMapping) obj;
        return Objects.equals(this.alias, that.alias) &&
               Objects.equals(this.canonicalName, that.canonicalName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alias, canonicalName);
    }

    @Override
    public String toString() {
        return "LicenseMapping[" +
               "alias=" + alias + ", " +
               "canonicalName=" + canonicalName + ']';
    }
}
