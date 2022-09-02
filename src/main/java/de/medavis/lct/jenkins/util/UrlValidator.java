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
package de.medavis.lct.jenkins.util;

import com.google.common.base.Strings;
import hudson.util.FormValidation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class UrlValidator {

    public static final String[] WEB = new String[]{"http", "https"};
    public static final String[] FILE = new String[]{"file"};
    public static final String[] WEB_AND_FILE = new String[]{"http", "https", "file"};

    public static FormValidation validate(String value) {
        return validate(value, WEB_AND_FILE);
    }

    public static FormValidation validate(String value, String... allowedProtocols) {
        if (Strings.isNullOrEmpty(value)) {
            return FormValidation.ok();
        }

        final String errorMessage = de.medavis.lct.jenkins.util.Messages.error_invalidUrl(Arrays.toString(allowedProtocols));
        try {
            URL asUrl = new URL(value);
            final List<String> http = Arrays.asList(allowedProtocols);
            if (http.stream().noneMatch(asUrl.getProtocol()::equalsIgnoreCase)) {
                return FormValidation.error(errorMessage);
            } else {
                return FormValidation.ok();
            }
        } catch (MalformedURLException e) {
            return FormValidation.error(errorMessage);
        }
    }

}
