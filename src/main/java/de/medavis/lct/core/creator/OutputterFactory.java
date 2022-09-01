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

import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class OutputterFactory {

    private static final Map<Format, Outputter> outputters = ImmutableMap.<Format, Outputter>builder()
            .put(Format.PDF, new PDFOutputter())
            .put(Format.FREEMARKER, new FreemarkerOutputter())
            .build();

    private OutputterFactory() {
    }

    public static Outputter getOutputter(Format format) {
        if (outputters.containsKey(format)) {
            return outputters.get(format);
        } else {
            throw new IllegalArgumentException("Format not supported yet: " + format);
        }
    }
}
