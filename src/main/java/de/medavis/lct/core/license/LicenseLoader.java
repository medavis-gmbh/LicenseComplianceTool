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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LicenseLoader {

    private static final Logger log = LoggerFactory.getLogger(LicenseLoader.class);

    public Map<String, License> load(URL licenseUrl) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, License> result = objectMapper.<List<License>>readValue(licenseUrl,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, License.class))
                    .stream()
                    .collect(Collectors.toMap(License::getName, Function.identity()));
            log.info("Imported {} licenses from {}", result.size(), licenseUrl);
            return result;

        } catch (Exception e) {
            throw new IllegalStateException("Failure while processing licenses from " + licenseUrl, e);
        }
    }

}
