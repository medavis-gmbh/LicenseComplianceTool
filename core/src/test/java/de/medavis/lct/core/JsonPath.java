/*-
 * #%L
 * License Compliance Tool - Implementation Core
 * %%
 * Copyright (C) 2022 - 2024 medavis GmbH
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

import com.fasterxml.jackson.databind.JsonNode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonPath {

    private static final String REGEX = "(?<name>\\D[a-zA-Z0-9]*)(\\[(?<index>\\d*)\\])?";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private JsonPath() {}

    @Nullable
    public static JsonNode path(@NotNull JsonNode firstNode, @NotNull String path) {

        List<String> items = List.of(path.split("\\."));

        JsonNode node = firstNode;

        for (String item : items) {
            Matcher matcher = PATTERN.matcher(item);
            while(matcher.find()) {
                String name = matcher.group("name");
                node = node.path(name);

                String sIndex = matcher.group("index");
                if (sIndex != null) {
                    int index = Integer.parseInt(matcher.group("index"));
                    node = node.path(index);
                    if (node == null) {
                        return null;
                    }
                }
            }
        }

        return node;
    }
}
