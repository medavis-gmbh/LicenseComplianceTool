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
package de.medavis.lct.jenkins.download;

import hudson.FilePath;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class JenkinsLicenseFileHandlerTest {

    private static final String OUTPUT_PATH = "licenses";
    private static final String LICENSE_NAME = "FKL-1.0";
    private static final String EXTENSION = ".txt";
    private static final String LICENSE_CONTENT = "Fake License 1.0";
    private static final byte[] LICENSE_CONTENT_BYTES = "Fake License 1.0".getBytes(StandardCharsets.UTF_8);

    @TempDir
    private Path workspacePath;

    @Test
    void shouldSaveLicenseFirstAndThenCacheItForSubsequentUse() throws IOException {
        var sut = new JenkinsLicenseFileHandler(new FilePath(workspacePath.toFile()), OUTPUT_PATH);

        sut.save(LICENSE_NAME, EXTENSION, LICENSE_CONTENT_BYTES);

        final Path targetLicenseFile = workspacePath.resolve(OUTPUT_PATH).resolve(LICENSE_NAME + EXTENSION);
        assertThat(targetLicenseFile).exists().hasContent(LICENSE_CONTENT);
        assertThat(sut.isCached(LICENSE_NAME)).isTrue();

        targetLicenseFile.toFile().delete();
        sut.copyFromCache(LICENSE_NAME);

        assertThat(targetLicenseFile).exists().hasContent(LICENSE_CONTENT);
    }
}
