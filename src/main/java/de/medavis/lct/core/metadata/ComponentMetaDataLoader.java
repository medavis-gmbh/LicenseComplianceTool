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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentMetaDataLoader {

    private static final Logger logger = LoggerFactory.getLogger(ComponentMetaDataLoader.class);

    public Collection<ComponentMetadata> load(URL metadataUrl) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            List<ComponentMetadata> result = objectMapper.readValue(metadataUrl,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ComponentMetadata.class));
            logger.info("Imported {} component metadata entries from {}.", result.size(), metadataUrl);
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Failure while processing metadata from " + metadataUrl, e);
        }
    }

}
