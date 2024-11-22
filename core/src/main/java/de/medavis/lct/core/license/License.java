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
import java.util.StringJoiner;

public final class License {

    private final String name;
    private final String url;
    private final String downloadUrl;
    private final boolean configured;

    @JsonCreator
    public static License fromConfig(@JsonProperty("name") String name, @JsonProperty("url") String url, @JsonProperty("downloadUrl") String downloadUrl) {
        return new License(name, url, downloadUrl, true);
    }

    public static License dynamic(String name, String url, String downloadUrl) {
        return new License(name, url, downloadUrl, false);
    }

    public static License dynamic(String name, String url) {
        return new License(name, url, null, false);
    }

    private License(String name, String url, String downloadUrl, boolean configured) {
        this.name = name;
        this.url = url;
        this.downloadUrl = downloadUrl;
        this.configured = configured;
    }

    /**
     * Can contain the name or ID of license.
     *
     * @return Returns Name or ID of the license
     */
    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public boolean isConfigured() {
        return configured;
    }

    public boolean isDynamic() {
        return !configured;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        License license = (License) o;
        return configured == license.configured && Objects.equals(name, license.name) && Objects.equals(url, license.url)
               && Objects.equals(downloadUrl, license.downloadUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url, downloadUrl, configured);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", License.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("url='" + url + "'")
                .add("downloadUrl='" + downloadUrl + "'")
                .add("configured=" + configured)
                .toString();
    }

}
