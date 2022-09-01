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
package de.medavis.lct.core.creator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import de.medavis.lct.core.UserLogger;
import de.medavis.lct.core.list.ComponentData;
import de.medavis.lct.core.list.ComponentLister;

public class ManifestCreator {

    private final ComponentLister componentLister;

    public ManifestCreator(ComponentLister componentLister) {
        this.componentLister = componentLister;
    }

    public void create(UserLogger logger, Path inputPath, Path outputPath, Format format) throws IOException {
        logger.info("Exporting component manifest in format %s from '%s' to '%s'.%n", format, inputPath, outputPath);
        List<ComponentData> components = componentLister.listComponents(inputPath.toUri().toURL());
        OutputterFactory.getOutputter(format).output(components, outputPath);
        logger.info("Exported %d components.%n", components.size());
    }

}
