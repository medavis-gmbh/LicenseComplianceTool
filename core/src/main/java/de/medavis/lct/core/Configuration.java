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
package de.medavis.lct.core;

import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.Set;

public interface Configuration {

    Optional<URL> getComponentMetadataUrl();

    Optional<URL> getLicensesUrl();

    Optional<URL> getLicenseMappingsUrl();

    /**
     * Used by the {@link de.medavis.lct.core.patcher.BomPatcher}.
     * When true, then license expression will be resolved and mapped into licenses
     */
    boolean isResolveExpression = false;

    /**
     * Used by the {@link de.medavis.lct.core.patcher.BomPatcher}.
     *
     * @return Returns a optional comma separated list of group names which will skipped during BOM patching.
     */
    Optional<Set<String>> getSkipGroupNames();

    /**
     * Used by the {@link de.medavis.lct.core.patcher.BomPatcher}.
     *
     * @return Returns the optional SpdxLicenseManager URI. If not set, then local copy will be used
     */
    Optional<URI> getSpdxLicenseListUri();

}
