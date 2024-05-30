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
package de.medavis.lct.core.patcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public abstract class AbstractRestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRestClient.class);
    private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

    /**
     * Create a default HTTP GET request.
     *
     * @param uri URI of the servers endpoint
     * @param headers Additional headers
     * @return  Returns the created HTTP GET request
     */
    @NotNull
    protected HttpRequest createDefaultGET(@NotNull URI uri, @NotNull String... headers) {
        HttpRequest.Builder builder = HttpRequest
                .newBuilder(uri)
                .timeout(Duration.ofSeconds(60));

        if (headers.length != 0) {
            builder = builder.headers(headers);
        }

        return builder
                .GET()
                .build();
    }

    /**
     * Execute a HTTP request and return the HTTP response body as string.
     *
     * @param request The HTTP request
     * @return Returns the HTTP response body as string
     * @throws IOException Thrown if an I/ O error occurs when sending or receiving
     * @throws InterruptedException Thrown if the operation is interrupted
     */
    @NotNull
    protected String executeRequest(@NotNull HttpRequest request) throws IOException, InterruptedException {
        try {
            LOGGER.debug("Executing HTTP {} to {}", request.method(), request.uri());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (!List.of(200, 302).contains(response.statusCode())) {
                throw new LicensePatcherException("Unexpected HTTP status code " + response.statusCode() + ": Body=" + response.body());
            }
            return response.body();
        } catch (Exception ex) {
            LOGGER.error("Error on url request '{}' occurred.", request.uri());
            throw ex;
        }
    }

    /**
     * Execute a HTTP request and return the JSON result as a Java object.
     *
     * @param request The HTTP request
     * @param valueTypeRef Encapsulated Java object type
     * @return JSON mapped Java object
     * @param <T> Java object type to be returned
     * @throws IOException Thrown if an I/ O error occurs when sending or receiving
     * @throws InterruptedException Thrown if the operation is interrupted
     */
    @NotNull
    protected <T> T executeRequest(@NotNull HttpRequest request, TypeReference<T> valueTypeRef) throws IOException, InterruptedException {
        try {
            String content = executeRequest(request);

            LOGGER.trace("HTTP response body={}", content);

            ObjectMapper objectMapper = Json5MapperFactory.create();
            return objectMapper.readValue(content, valueTypeRef);
        } catch (Exception ex) {
            LOGGER.error("Error on url request '{}' occurred.", request.uri());
            throw ex;
        }
    }

}
