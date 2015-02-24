package org.wtf.skybar.web;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.wtf.skybar.registry.SkybarRegistry;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class WebServer {

    public void start(int port) {
        try {
            Server server = new Server();
            ServerConnector connector = new ServerConnector(server);
            connector.setPort(port);
            server.addConnector(connector);

            ServletContextHandler handler = new ServletContextHandler();
            handler.setBaseResource(getBaseResource());
            handler.addServlet(DefaultServlet.class, "/");

            handler.addServlet(new ServletHolder(new CoverageServlet(SkybarRegistry.registry)), "/coverage.json");

            // TODO: Add servlet to deliver Java source files
            // TODO: Add a WebSocketServlet for pushing touched classes live



            server.setHandler(handler);

            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ResourceCollection getBaseResource() throws IOException {
        List<Resource> bases = new ArrayList<>();

        File source = new File("src/main/resources/org/wtf/skybar/web");
        if(source.exists()) {
            bases.add(Resource.newResource(source));
        } else {
            bases.add(Resource.newClassPathResource("/org/wtf/skybar/web/"));
        }


        Collections.list(getClass().getClassLoader().getResources("META-INF/resources/"))
                .forEach(url -> bases.add(Resource.newResource(url)));

        return new ResourceCollection(bases.toArray(new Resource[bases.size()]));
    }
}
