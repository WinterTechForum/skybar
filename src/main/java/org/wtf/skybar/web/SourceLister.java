package org.wtf.skybar.web;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;

/**
 * Use Jetty ResourceHandler to search a path-style set of directories for a resource.
 */
public class SourceLister extends ResourceHandler {
    private static final Logger LOG = Log.getLogger(ResourceHandler.class);
    private final List<Resource> searchPaths;

    /**
     * String with "path.separator" delimiters to search for source files.
     *
     * @param searchPaths delimited string.
     */
    public SourceLister(String searchPaths) {
        String[] split = searchPaths.split(System.getProperty("path.separator"));
        this.searchPaths = new java.util.ArrayList<>(split.length);
        for (String str: split) {
            File dir = new File(str);
            if (!dir.isDirectory()) {
                throw new IllegalArgumentException("Invalid search path, not a directory: "+
                        dir.getAbsolutePath());
            }
            this.searchPaths.add(Resource.newResource(dir));
        }
        assert(this.searchPaths.size() > 0);
    }

    /**
     * Iterate through all our search paths to try to find the requested resource. 
     * @param path URI path starting with "/".
     * @return found Resource or null if not found.
     * @throws MalformedURLException if invalid URL passed in.
     */
    @Override
    public Resource getResource(String path) throws MalformedURLException {
        Resource result = null;
        for (Resource base: this.searchPaths) {
            this.setBaseResource(base);
            result = super.getResource(path);
            if (result != null) {
                break;
            }
        }
        return result;
    }
}
