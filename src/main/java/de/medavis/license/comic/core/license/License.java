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

public final class License {

    private final String name;
    private final String url;
    private final String downloadUrl;

    @JsonCreator
    public License(@JsonProperty("name") String name, @JsonProperty("url") String url, @JsonProperty("downloadUrl") String downloadUrl) {
        this.name = name;
        this.url = url;
        this.downloadUrl = downloadUrl;
    }

    public License(String name, String url) {
        this(name, url, null);
    }

    public String name() {
        return name;
    }

    public String url() {
        return url;
    }

    public String downloadUrl() {
        return downloadUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        License that = (License) obj;
        return Objects.equals(this.name, that.name) &&
               Objects.equals(this.url, that.url) &&
               Objects.equals(this.downloadUrl, that.downloadUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url, downloadUrl);
    }

    @Override
    public String toString() {
        return "License[" +
               "name=" + name + ", " +
               "url=" + url + ", " +
               "downloadUrl=" + downloadUrl + ']';
    }

}
