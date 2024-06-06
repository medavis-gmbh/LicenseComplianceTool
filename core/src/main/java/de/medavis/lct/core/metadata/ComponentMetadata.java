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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;

public final class ComponentMetadata {

    private final String groupMatch;
    private final String nameMatch;
    private final String purlMatch;
    private final boolean ignore;
    private final String mappedName;
    private final String url;
    private final String comment;
    private final Set<String> licenses;
    private final Set<String> attributionNotices;

    @JsonCreator
    public ComponentMetadata(
            @JsonProperty("groupMatch") String groupMatch,
            @JsonProperty("nameMatch") String nameMatch,
            @JsonProperty("purlMatch") String purlMatch,
            @JsonProperty("ignore") boolean ignore,
            @JsonProperty("mappedName") String mappedName,
            @JsonProperty("url") String url,
            @JsonProperty("comment") String comment,
            @JsonProperty("licenses")
            Set<String> licenses,
            @JsonProperty("attributionNotices")
            Set<String> attributionNotices) {
        this.groupMatch = groupMatch;
        this.nameMatch = nameMatch;
        this.purlMatch = purlMatch;

        this.ignore = ignore;
        this.mappedName = mappedName;
        this.url = url;
        this.comment = comment;
        this.licenses = licenses;
        this.attributionNotices = attributionNotices;
    }

    /**
     * @param group Group to match or null. If null then it will be ignored in the match. According to CycloneDX Spec this is not a mandatory field
     * @param name Name to match. According to CycloneDX Spec this is a mandatory field
     * @param purl Package URL or null. If null then it will be ignored in the match. According to CycloneDX Spec this is not a mandatory field
     *
     * @return Returns true if we have a match
     */
    public boolean matches(@Nullable String group, @NotNull String name, @Nullable String purl) {
        boolean matchesGroup = StringUtils.isNotBlank(groupMatch) && StringUtils.isNotBlank(group) && Pattern.matches(groupMatch, group);
        boolean matchesName = StringUtils.isNotBlank(nameMatch) && Pattern.matches(nameMatch, name);
        boolean matchesPurl = StringUtils.isNotBlank(purlMatch) && StringUtils.isNotBlank(purl) && Pattern.matches(purlMatch, purl);
        return matchesPurl
                || StringUtils.isBlank(nameMatch) && matchesGroup
                || StringUtils.isBlank(groupMatch) && matchesName
                || matchesGroup && matchesName;
    }

    @Nullable
    public String groupMatch() {
        return groupMatch;
    }

    @Nullable
    public String nameMatch() {
        return nameMatch;
    }

    @Nullable
    public String purlMatch() {
        return purlMatch;
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

    @JsonProperty("attributionNotices")
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    @JsonDeserialize(as = LinkedHashSet.class)
    public Set<String> attributionNotices() {
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
        ComponentMetadata that = (ComponentMetadata) o;
        return ignore == that.ignore
                && Objects.equals(groupMatch, that.groupMatch)
                && Objects.equals(nameMatch, that.nameMatch)
                && Objects.equals(purlMatch, that.purlMatch)
                && Objects.equals(mappedName, that.mappedName)
                && Objects.equals(url, that.url)
                && Objects.equals(comment, that.comment)
                && Objects.equals(licenses, that.licenses)
                && Objects.equals(attributionNotices, that.attributionNotices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupMatch, nameMatch, purlMatch, ignore, mappedName, url, comment, licenses, attributionNotices);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ComponentMetadata.class.getSimpleName() + "[", "]")
                .add("groupMatch='" + groupMatch + "'")
                .add("nameMatch='" + nameMatch + "'")
                .add("purlMatch='" + purlMatch + "'")
                .add("ignore=" + ignore)
                .add("mappedName='" + mappedName + "'")
                .add("url='" + url + "'")
                .add("comment='" + comment + "'")
                .add("licenses=" + licenses)
                .add("attributionNotices=" + attributionNotices)
                .toString();
    }

}
