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
package de.medavis.lct.core.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class ComponentMetadata {

    private final String groupMatch;
    private final String nameMatch;
    private final boolean ignore;
    private final String mappedName;
    private final String url;
    private final String comment;
    private final Set<String> licenses;

    @JsonCreator
    public ComponentMetadata(
            @JsonProperty("groupMatch") String groupMatch,
            @JsonProperty("nameMatch") String nameMatch,
            @JsonProperty("ignore") boolean ignore,
            @JsonProperty("mappedName") String mappedName,
            @JsonProperty("url") String url,
            @JsonProperty("comment") String comment,
            @JsonProperty("licenses")
            @JsonSetter(nulls = Nulls.AS_EMPTY)
            @JsonDeserialize(as = LinkedHashSet.class)
            Set<String> licenses
    ) {
        this.groupMatch = groupMatch;
        this.nameMatch = nameMatch;
        this.ignore = ignore;
        this.mappedName = mappedName;
        this.url = url;
        this.comment = comment;
        this.licenses = licenses;
    }

    public boolean matches(String group, String name) {
        boolean matchesGroup = Strings.isNullOrEmpty(groupMatch) || Pattern.matches(groupMatch, group);
        boolean matchesName = Strings.isNullOrEmpty(nameMatch) || Pattern.matches(nameMatch, name);
        return matchesGroup && matchesName;
    }

    public String groupMatch() {
        return groupMatch;
    }

    public String nameMatch() {
        return nameMatch;
    }

    public boolean ignore() {
        return ignore;
    }

    public String mappedName() {
        return mappedName;
    }

    public String url() {
        return url;
    }

    public String comment() {
        return comment;
    }

    @JsonProperty("licenses")
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    @JsonDeserialize(as = LinkedHashSet.class)
    public Set<String> licenses() {
        return licenses;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        ComponentMetadata that = (ComponentMetadata) obj;
        return Objects.equals(this.groupMatch, that.groupMatch) &&
               Objects.equals(this.nameMatch, that.nameMatch) &&
               this.ignore == that.ignore &&
               Objects.equals(this.mappedName, that.mappedName) &&
               Objects.equals(this.url, that.url) &&
               Objects.equals(this.comment, that.comment) &&
               Objects.equals(this.licenses, that.licenses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupMatch, nameMatch, ignore, mappedName, url, comment, licenses);
    }

    @Override
    public String toString() {
        return "ComponentMetadata[" +
               "groupMatch=" + groupMatch + ", " +
               "nameMatch=" + nameMatch + ", " +
               "ignore=" + ignore + ", " +
               "mappedName=" + mappedName + ", " +
               "url=" + url + ", " +
               "comment=" + comment + ", " +
               "licenses=" + licenses + ']';
    }


}
