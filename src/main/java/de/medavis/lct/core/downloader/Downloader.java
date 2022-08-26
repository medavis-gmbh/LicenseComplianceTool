package de.medavis.lct.core.downloader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClients;

import static org.apache.http.entity.ContentType.TEXT_HTML;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

class Downloader {

    private final HttpClient httpclient = HttpClients.createDefault();

    File downloadToFile(String sourceUrl, Path targetDirectory, String filename) throws IOException {
        return httpclient.execute(new HttpGet(sourceUrl), response -> handleDownload(response, targetDirectory, filename));
    }

    private File handleDownload(HttpResponse response, Path targetDirectory, String filename) throws IOException {
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("Download not successful: Status " + statusCode);
        }

        String extension = determineExtension(response.getEntity().getContentType());
        File outputFile = targetDirectory.resolve(filename + extension).toFile();
        FileUtils.copyToFile(response.getEntity().getContent(), outputFile);
        return outputFile;
    }

    private String determineExtension(Header contentTypeHeader) {
        String result = "";
        if (contentTypeHeader != null) {
            String contentType = parseContentType(contentTypeHeader);
            if (TEXT_HTML.getMimeType().equals(contentType)) {
                result = ".html";
            } else if (TEXT_PLAIN.getMimeType().equals(contentType)) {
                result = ".txt";
            }
        }
        return result;
    }

    private String parseContentType(Header contentTypeHeader) {
        try {
            return ContentType.parse(contentTypeHeader.getValue()).getMimeType();
        } catch (ParseException | UnsupportedCharsetException e) {
            // Ignore error and assume unknown content type
            return null;
        }
    }

}
