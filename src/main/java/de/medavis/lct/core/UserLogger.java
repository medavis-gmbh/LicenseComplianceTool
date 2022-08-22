package de.medavis.lct.core;

/**
 * A logger that prints output which is meant for an end user.
 * Use {@link org.slf4j.Logger} to log information which is not meant for end users.
 */
public interface UserLogger {

    void info(String format, Object... args);
    void error(String format, Object... args);

}
