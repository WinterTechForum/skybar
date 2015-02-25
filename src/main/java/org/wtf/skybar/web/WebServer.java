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
import java.util.Map;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.wtf.skybar.time.TimeGenerator;
import org.wtf.skybar.time.TimeListener;


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

            // Add a sample WebSocketServlet for pushing a simple {time:millis} value to client.
            ContextHandler wsTimeContext = new ContextHandler();
            wsTimeContext.setContextPath("/time");
            WebSocketHandler wsTimeHandler = new WebSocketHandler() {
                @Override
                public void configure(WebSocketServletFactory wssf) {
                    wssf.register(TimeWebSocket.class);
                }
            };
            wsTimeContext.setHandler(wsTimeHandler);
            handlers.addHandler(wsTimeContext);
            
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

            // Make sure the Registry calls all its DeltaListeners every second
            TimeGenerator.getInstance().register(new TimeListener() {
                private final Map<String,Map<Integer,Long>> tmpMap = new java.util.HashMap<>();
                @Override
                public void onTime(long timestamp) {
                    SkybarRegistry.registry.updateListeners(tmpMap);
                }
            });
            
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
