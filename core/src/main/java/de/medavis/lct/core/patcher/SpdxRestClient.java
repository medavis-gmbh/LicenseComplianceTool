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

import de.medavis.lct.core.patcher.model.SpdxLicenses;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;

/**
 * SPDX REST client.
 */
public class SpdxRestClient extends AbstractRestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpdxRestClient.class);
    private final URI uri;

    public static final URI DEFAULT_URI = URI.create("https://github.com/spdx/license-list-data/raw/main/json/licenses.json");

    /**
     * Creates a new SPDX REST client.
     *
     * @param uri URI of the servers endpoint
     */
    public SpdxRestClient(@NotNull URI uri) {
        this.uri = uri;
    }

    /**
     * Request the SPDX licenses from the server.
     *
     * @return Returns the SPDX licenses but never null
     */
    @NotNull
    public SpdxLicenses fetchLicenses() {
        try {
            LOGGER.info("Fetching licenses");
            HttpRequest request = createDefaultGET(uri);

            return executeRequest(request, SpdxLicenses.class);
        } catch (IOException | InterruptedException ex) {
            throw new LicensePatcherException(ex.getMessage(), ex);
        }
    }

}
