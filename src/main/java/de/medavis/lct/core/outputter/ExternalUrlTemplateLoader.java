package de.medavis.lct.core.outputter;

import freemarker.cache.URLTemplateLoader;
import java.net.MalformedURLException;
import java.net.URL;

public class ExternalUrlTemplateLoader extends URLTemplateLoader {

    @Override
    protected URL getURL(String name) {
        try {
            return new URL(name);
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
