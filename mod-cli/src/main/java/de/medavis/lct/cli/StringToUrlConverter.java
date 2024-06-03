/*-
 * #%L
 * License Compliance Tool - Command Line Interface
 * %%
 * Copyright (C) 2022 - 2023 medavis GmbH
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
package de.medavis.lct.cli;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StringToUrlConverter {

    private static final Logger logger = LoggerFactory.getLogger(StringToUrlConverter.class);

    private StringToUrlConverter() {
    }

    static Optional<URL> convert(String urlString) {
        if (urlString == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new URL(urlString));
        } catch (MalformedURLException e) {
            logger.debug(urlString + " is not a valid URL, trying to interpret as file path", e);
            try {
                return Optional.of(Paths.get(urlString).toUri().toURL());
            } catch (MalformedURLException ex) {
                logger.debug(urlString + " is not a valid file path. Value will be ignored.", e);
                return Optional.empty();
            }
        }
    }

}
