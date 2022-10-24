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
package de.medavis.lct.util;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class InputStreamContentArgumentMatcher extends BaseMatcher<InputStream> {

    private final String expected;

    public InputStreamContentArgumentMatcher(String expected) {
        this.expected = expected;
    }

    @Override
    public boolean matches(Object item) {
        if (!(item instanceof InputStream)) {
            return false;
        }
        InputStream inputStream = (InputStream) item;
        try {
            String actual = CharStreams.toString(new InputStreamReader(inputStream));
            return actual.equals(expected);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Input stream matches: ").appendValue(expected);
    }
}
