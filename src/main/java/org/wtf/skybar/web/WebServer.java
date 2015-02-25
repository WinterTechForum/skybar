package org.wtf.skybar.web;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.wtf.skybar.registry.SkybarRegistry;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;


public class WebServer {
    public static final String SOURCE_PATH_SYS_PROPERTY = "skybar.source.path";
    public static final String SOURCE_PATH_ENV_VAR_NAME = "SKYBAR_SOURCE_PATH";

    public void start(int port) {
        try {
            Server server = new Server();
            ServerConnector connector = new ServerConnector(server);
            connector.setPort(port);
            server.addConnector(connector);
            ContextHandlerCollection handlers = new ContextHandlerCollection();

            ServletContextHandler handler = new ServletContextHandler();
            handler.setContextPath("/");
            handler.setBaseResource(getBaseResource());
            handler.addServlet(DefaultServlet.class, "/");
            handler.setWelcomeFiles(new String[]{"index.html"});
            handler.addServlet(new ServletHolder(new CoverageServlet(SkybarRegistry.registry)), "/coverage.json");
            handlers.addHandler(handler);

            // Add servlet to deliver Java source files
            ContextHandler sourceContext = new ContextHandler();
            sourceContext.setContextPath("/source");
            SourceLister sourceLister = new SourceLister(getSourcePathString());
            sourceLister.setDirectoriesListed(true);
            sourceContext.setHandler(sourceLister);
            handlers.addHandler(sourceContext);

            // Add a WebSocketServlet for pushing touched classes live
            ContextHandler wsContext = new ContextHandler();
            wsContext.setContextPath("/time");
            WebSocketHandler wsHandler = new WebSocketHandler() {
                @Override
                public void configure(WebSocketServletFactory wssf) {
                    wssf.register(TimeWebSocket.class);
                }
            };
            wsContext.setHandler(wsHandler);
            handlers.addHandler(wsContext);

            server.setHandler(handlers);
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getSourcePathString() throws IOException {
        String sourcePath = System.getProperty(SOURCE_PATH_SYS_PROPERTY);
        if (sourcePath == null) {
            sourcePath = System.getenv(SOURCE_PATH_ENV_VAR_NAME);
        }
        if (sourcePath == null) {
            File srcDir = new File("src/main/java");
            sourcePath = srcDir.getCanonicalPath();
        }
        return sourcePath;
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
