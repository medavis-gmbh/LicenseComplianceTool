/*-
 * #%L
 * License Compliance Tool - Implementation Core
 * %%
 * Copyright (C) 2022 - 2024 medavis GmbH
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
package de.medavis.lct.core.urlchecker;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Implementation of {@link HttpUrlChecker} that accesses the URL over the Internet.
 */
public class OnlineHttpUrlChecker implements HttpUrlChecker {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                                                                  .followRedirects(HttpClient.Redirect.NORMAL)
                                                                  .connectTimeout(Duration.of(1, SECONDS))
                                                                  .build();

    @Override
    public boolean isUrlAvailable(String urlString) {
        boolean result = false;
        String reason;
        try {
            var req = HttpRequest.newBuilder()
                                 .uri(new URI(urlString))
                                 .GET()
                                 .build();
            var resp = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            result = resp.statusCode() == 200;
            reason = "Status code is " + resp.statusCode();
        } catch(Exception e) {
            if(e instanceof InterruptedException) {
                // Thread was interrupted, re-interrupt it
                Thread.currentThread().interrupt();
            }
            result = false;
            reason = "Exception: " + e.getMessage();
        }
        if (!result) {
            log.debug("URL {} is not available. Reason: {}", urlString, reason);
        }
        return result;

    }

}
