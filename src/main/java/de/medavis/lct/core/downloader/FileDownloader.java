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

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.UnsupportedCharsetException;
import org.apache.http.Header;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClients;

import static org.apache.http.entity.ContentType.TEXT_HTML;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

class FileDownloader {

    private final HttpClient httpclient = HttpClients.createDefault();

    void downloadToFile(String url, String targetName, TargetHandler targetHandler) throws IOException {
        httpclient.execute(new HttpGet(url), response -> {
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("Download not successful: Status " + statusCode);
            }

            String extension = determineExtension(response.getEntity().getContentType());
            try (final InputStream input = response.getEntity().getContent()) {
                targetHandler.handle(targetName, extension, ByteStreams.toByteArray(input));
            }
            return null;
        });
    }


    private String determineExtension(Header contentTypeHeader) {
        String result = "";
        if (contentTypeHeader != null) {
            String contentType = parseContentType(contentTypeHeader);
            if (TEXT_HTML.getMimeType().equals(contentType)) {
                result = ".html";
            } else if (TEXT_PLAIN.getMimeType().equals(contentType)) {
                result = ".txt";
            }
        }
        return result;
    }

    private String parseContentType(Header contentTypeHeader) {
        try {
            return ContentType.parse(contentTypeHeader.getValue()).getMimeType();
        } catch (ParseException | UnsupportedCharsetException e) {
            // Ignore error and assume unknown content type
            return null;
        }
    }

}
