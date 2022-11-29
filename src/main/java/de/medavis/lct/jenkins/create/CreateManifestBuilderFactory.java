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
package de.medavis.lct.jenkins.create;

import java.util.function.Function;
import java.util.function.Supplier;

import de.medavis.lct.core.license.LicenseLoader;
import de.medavis.lct.core.license.LicenseMappingLoader;
import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.asset.AssetLoader;
import de.medavis.lct.core.list.ComponentLister;
import de.medavis.lct.core.metadata.ComponentMetaDataLoader;
import de.medavis.lct.core.outputter.FreemarkerOutputter;

// TODO Try to use dependency injection (maybe using ExtensionFinder, GuiceFinder?)
class CreateManifestBuilderFactory {

    private static Function<Configuration, ComponentLister> componentListerFactory = configuration -> new ComponentLister(
            new AssetLoader(),
            new ComponentMetaDataLoader(),
            new LicenseLoader(),
            new LicenseMappingLoader(),
            configuration);
    private static Supplier<FreemarkerOutputter> outputterFactory = FreemarkerOutputter::new;

    private CreateManifestBuilderFactory() {
    }

    public static ComponentLister getComponentLister(Configuration configuration) {
        return componentListerFactory.apply(configuration);
    }

    public static FreemarkerOutputter getOutputterFactory() {
        return outputterFactory.get();
    }

    /**
     * Should only be used for tests
     */
    static void setComponentListerFactory(Function<Configuration, ComponentLister> componentListerFactory) {
        CreateManifestBuilderFactory.componentListerFactory = componentListerFactory;
    }

    /**
     * Should only be used for tests
     */
    static void setOutputterFactory(Supplier<FreemarkerOutputter> outputterFactory) {
        CreateManifestBuilderFactory.outputterFactory = outputterFactory;
    }

}
