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
package de.medavis.lct.jenkins.factory;

import de.medavis.lct.core.license.LicenseLoader;
import de.medavis.lct.core.license.LicenseMappingLoader;
import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.asset.AssetLoader;
import de.medavis.lct.core.list.ComponentLister;
import de.medavis.lct.core.metadata.ComponentMetaDataLoader;
import de.medavis.lct.core.outputter.FreemarkerOutputter;

// TODO Try to use dependency injection (maybe using ExtensionFinder, GuiceFinder?)
public class ComponentFactory {

    private static ComponentLister componentLister;
    private static FreemarkerOutputter outputter;

    private ComponentFactory() {
    }

    public static ComponentLister getComponentLister(Configuration configuration) {
        if (componentLister == null) {
            componentLister = new ComponentLister(
                    new AssetLoader(),
                    new ComponentMetaDataLoader(),
                    new LicenseLoader(),
                    new LicenseMappingLoader(),
                    configuration);
        }
        return componentLister;
    }

    public static FreemarkerOutputter getOutputter() {
        if (outputter == null) {
            outputter = new FreemarkerOutputter();
        }
        return outputter;
    }

    /**
     * Should only be used for tests
     */
    public static void setComponentLister(ComponentLister componentLister) {
        ComponentFactory.componentLister = componentLister;
    }

    /**
     * Should only be used for tests
     */
    public static void setOutputter(FreemarkerOutputter outputter) {
        ComponentFactory.outputter = outputter;
    }

}
