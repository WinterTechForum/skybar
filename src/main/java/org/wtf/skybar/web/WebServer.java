package org.wtf.skybar.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.wtf.skybar.registry.SkybarRegistry;
import org.wtf.skybar.time.RegistryUpdateListeners;


public class WebServer {
    private Server server;
    private final int port;
    private final String sourcePath;

    public WebServer(int port, String sourcePath) {
        this.port = port;
        this.sourcePath = sourcePath;
    }

    public void start() {
        try {
            server = new Server();
            ServerConnector connector = new ServerConnector(server);
            connector.setPort(port);
            server.addConnector(connector);
            ContextHandlerCollection handlers = new ContextHandlerCollection();

            ServletContextHandler handler = new ServletContextHandler();
            handler.setContextPath("/");
            handler.setBaseResource(getBaseResource());
            handler.addServlet(DefaultServlet.class, "/");
            handler.setWelcomeFiles(new String[]{"index.html"});
            handlers.addHandler(handler);

            // Add servlet to deliver Java source files
            ContextHandler sourceContext = new ContextHandler();
            sourceContext.setContextPath("/source");
            SourceLister sourceLister = new SourceLister(sourcePath);
            sourceLister.setDirectoriesListed(true);
            sourceContext.setHandler(sourceLister);
            handlers.addHandler(sourceContext);

            // Add a WebSocketServlet for pushing touched classes live
            ContextHandler wsCoverageContext = new ContextHandler();
            wsCoverageContext.setContextPath("/livecoverage");
            WebSocketHandler wsCoverageHandler = new WebSocketHandler() {
                @Override
                public void configure(WebSocketServletFactory wssf) {
                    wssf.register(CoverageWebSocket.class);
                }
            };
            wsCoverageContext.setHandler(wsCoverageHandler);
            handlers.addHandler(wsCoverageContext);

            // Make sure the Registry calls all its DeltaListeners every period (200ms?)
            RegistryUpdateListeners timer = new RegistryUpdateListeners(SkybarRegistry.registry);
            server.addLifeCycleListener(timer);
            timer.start();
            
            server.setHandler(handlers);
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

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
