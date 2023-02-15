package de.medavis.lct.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

@Command
class Main {

    public Main(String[] args) {
        System.setProperty("org.jboss.logging.provider", "slf4j");

        final CommandLine commandLine = new CommandLine(this);
        commandLine.addSubcommand(new HelpCommand());
        commandLine.addSubcommand(new CreateComponentManifest());
        System.exit(commandLine.execute(args));
    }

    public static void main(String[] args) {
        new Main(args);

    }

}
