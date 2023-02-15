package de.medavis.lct.cli;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StringToUrlConverter {

    private static final Logger logger = LoggerFactory.getLogger(StringToUrlConverter.class);

    private StringToUrlConverter() {
    }

    static Optional<URL> convert(String urlString) {
        try {
            return Optional.of(new URL(urlString));
        } catch (MalformedURLException e) {
            logger.debug(urlString + " is not a valid URL, trying to interpret as file path", e);
            try {
                return Optional.of(Paths.get(urlString).toUri().toURL());
            } catch (MalformedURLException ex) {
                logger.debug(urlString + " is not a valid file path. Value will be ignored.", e);
                return Optional.empty();
            }
        }
    }

}
