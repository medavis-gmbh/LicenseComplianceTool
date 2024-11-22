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
package de.medavis.lct.core.downloader;

import de.medavis.lct.core.patcher.AbstractRestClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.UnsupportedCharsetException;

import org.apache.http.HttpHeaders;
import org.apache.http.ParseException;
import org.apache.http.entity.ContentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.apache.http.entity.ContentType.TEXT_HTML;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

public class LicenseFileDownloader extends AbstractRestClient {

    /**
     * Request the license text from external source.
     *
     * @return Returns result status of download
     */
    @NotNull
    Result downloadToFile(String url, String license, LicenseFileHandler licenseFileHandler) throws IOException {
        try {
            if (!licenseFileHandler.isCached(license)) {
                HttpRequest request = createDefaultGET(URI.create(url));
                HttpResponse<byte[]> response = executeRequestWithResponse(request, HttpResponse.BodyHandlers.ofByteArray());
                int statusCode = response.statusCode();
                if (statusCode < 200 || statusCode >= 300) {
                    throw new IOException("Download not successful: Status " + statusCode);
                }

                String contentType = determineExtension(response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(""));
                licenseFileHandler.save(license, contentType, response.body());

                return Result.DOWNLOADED;
            } else {
                licenseFileHandler.copyFromCache(license);
                return Result.FROM_CACHE;
            }
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
    }

    @NotNull
    private String determineExtension(@NotNull String contentTypeHeader) {
        String result = "";
        String contentType = parseContentType(contentTypeHeader);

        if (TEXT_HTML.getMimeType().equals(contentType)) {
            result = ".html";
        } else if (TEXT_PLAIN.getMimeType().equals(contentType)) {
            result = ".txt";
        }

        return result;
    }

    @Nullable
    private String parseContentType(String contentTypeHeader) {
        try {
            return ContentType.parse(contentTypeHeader).getMimeType();
        } catch (ParseException | UnsupportedCharsetException e) {
            // Ignore error and assume unknown content type
            return null;
        }
    }

    enum Result {
        DOWNLOADED,
        FROM_CACHE
    }

}
