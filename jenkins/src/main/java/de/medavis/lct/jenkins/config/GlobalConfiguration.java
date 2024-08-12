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
package de.medavis.lct.jenkins.config;

import java.util.ArrayList;
import java.util.List;

import hudson.Extension;
import hudson.model.PersistentDescriptor;

import de.medavis.lct.core.Configuration;

@Extension
public class GlobalConfiguration extends jenkins.model.GlobalConfiguration implements PersistentDescriptor {

    public static Configuration getConfiguration() {
        final var configurationProfiles = jenkins.model.GlobalConfiguration.all().getInstance(GlobalConfiguration.class).getProfiles();
        if (!configurationProfiles.isEmpty()) {
            return configurationProfiles.stream()
                    .filter(ConfigurationProfile::isDefaultProfile)
                    .findFirst()
                    .orElseGet(() -> configurationProfiles.get(0));
        } else {
            return new NoConfiguration();
        }
    }

    private List<ConfigurationProfile> profiles = new ArrayList<>();

    public GlobalConfiguration() {
        load();
    }

    public List<ConfigurationProfile> getProfiles() {
        return profiles;
    }

    public void setProfiles(final List<ConfigurationProfile> profiles) {
        this.profiles = profiles;
        save();
    }

}