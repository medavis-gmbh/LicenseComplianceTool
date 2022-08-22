package de.medavis.lct.jenkins;

import hudson.model.TaskListener;

import de.medavis.lct.core.UserLogger;

public class JenkinsLogger implements UserLogger {

    private final TaskListener listener;

    public JenkinsLogger(TaskListener listener) {
        this.listener = listener;
    }

    @Override
    public void info(String format, Object... args) {
        listener.getLogger().printf(format, args);
    }

    @Override
    public void error(String format, Object... args) {
        listener.error(format, args);
    }
}
