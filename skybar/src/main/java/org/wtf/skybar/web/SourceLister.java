package org.wtf.skybar.web;

import java.io.IOException;
import java.net.MalformedURLException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.wtf.skybar.registry.SkybarRegistry;
import org.wtf.skybar.source.Source;
import org.wtf.skybar.source.SourceProvider;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Use Jetty ResourceHandler to search a path-style set of directories for a resource.
 */
public class SourceLister extends AbstractHandler {
    private static final Logger LOG = Log.getLogger(SourceLister.class);
    private final SourceProvider[] sourceProviders;
    private final SkybarRegistry registry;

    /**
     * String with "path.separator" delimiters to search for source files.
     *
     * @param sourceProviders delimited string.
     * @throws java.io.IOException if given an invalid path/directory.
     */
    public SourceLister(SkybarRegistry registry, SourceProvider... sourceProviders) throws IOException {
        this.registry = registry;
        this.sourceProviders = sourceProviders;
    }

    /**
     * Iterate through all our search paths to try to find the requested resource. 
     * @param path URI path starting with "/".
     * @return found Resource or null if not found.
     * @throws MalformedURLException if invalid URL passed in.
     */
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String path = target.substring(1);
        ClassLoader loader = registry.getClassLoader(path);
        if(loader != null) {
            Source source = getSource(path, loader);
            if(source != null) {
                response.setContentType("text/plain");
                source.write(response.getOutputStream());
                baseRequest.setHandled(true);
            }
        }

    }

    Source getSource(String path, ClassLoader classLoader) {
        for (SourceProvider sourceProvider : sourceProviders) {
            Source source = sourceProvider.lookup(path, classLoader);
            if(source != null) {
                return  source;
            }
        }
        return null;
    }
}
