package de.medavis.lct.cli;

import de.medavis.lct.core.UserLogger;

class ConsoleUserLogger implements UserLogger {

    @Override
    public void info(String format, Object... args) {
        System.out.printf(format, args);
    }

    @Override
    public void error(String format, Object... args) {
        System.err.printf(format, args);
    }
}
