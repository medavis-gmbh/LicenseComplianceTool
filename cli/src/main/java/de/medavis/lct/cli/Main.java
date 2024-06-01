/*-
 * #%L
 * License Compliance Tool - Command Line Interface
 * %%
 * Copyright (C) 2022 - 2023 medavis GmbH
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
package de.medavis.lct.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

@Command
class Main {

    int run(String[] args) {
        System.setProperty("org.jboss.logging.provider", "slf4j");

        final CommandLine commandLine = new CommandLine(this);
        commandLine.addSubcommand(new HelpCommand());
        commandLine.addSubcommand(new CreateManifest());
        commandLine.addSubcommand(new DownloadLicenses());
        commandLine.addSubcommand(new PatchBOM());

        return commandLine.execute(args);
    }

    public static void main(String[] args) {
        Main main = new Main();
        System.exit(main.run(args));
    }

}
