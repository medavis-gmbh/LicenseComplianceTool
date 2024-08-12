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

import hudson.Extension;
import hudson.model.PersistentDescriptor;
import hudson.util.FormValidation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medavis.lct.core.Configuration;

@Extension
public class GlobalConfiguration extends jenkins.model.GlobalConfiguration implements PersistentDescriptor {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalConfiguration.class);

    public static Configuration getInstance() {
        final var configurationProfiles = jenkins.model.GlobalConfiguration.all().getInstance(GlobalConfiguration.class).getProfiles();
        return configurationProfiles.isEmpty() ? new ConfigurationProfile("empty") : configurationProfiles.get(0);
    }

    private List<ConfigurationProfile> profiles = new ArrayList<>();

    public GlobalConfiguration() {
        load();
    }

    public List<ConfigurationProfile> getProfiles() {
        return profiles;
    }

//    @DataBoundSetter
    public void setProfiles(final List<ConfigurationProfile> profiles) {
        this.profiles = profiles;
        save();
    }

    @Override public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        if (Jenkins.get().hasPermission(getRequiredGlobalConfigPagePermission())) {
            setProfiles(Collections.emptyList()); // allow last library to be deleted
            return super.configure(req, json);
        } else {
            return true;
        }
    }

    public FormValidation doCheckName(@QueryParameter String name) {
        return FormValidation.validateRequired(name);
    }

    public FormValidation doCheckNewName(@QueryParameter String name) {
        return FormValidation.validateRequired(name);
    }

}