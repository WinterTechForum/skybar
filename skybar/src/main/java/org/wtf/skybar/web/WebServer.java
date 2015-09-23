package org.wtf.skybar.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.jetty.server.AbstractConnectionFactory;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.wtf.skybar.registry.SkybarRegistry;
import org.wtf.skybar.time.RegistryUpdateListeners;

public class WebServer {
    private Server server;
    private final SkybarRegistry registry;
    private final int port;
    private final Resource[] sourcePaths;

    /**
     * @param registry   registry to use for listeners
     * @param port       port to listen on, or 0 to use an available port
     * @param sourcePaths where to load source from
     */
    public WebServer(SkybarRegistry registry, int port, Resource... sourcePaths) {
        this.registry = registry;
        this.port = port;
        this.sourcePaths = sourcePaths;
    }

    /**
     * @return port which server started on. Will be different than configured port when port 0 is used, which is in
     * tests.
     */
    public int start() {
        try {
            QueuedThreadPool pool = new QueuedThreadPool();
            pool.setDaemon(true);
            server = new Server(pool);

            Scheduler scheduler = new ScheduledExecutorScheduler(null, true);
            HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory();

            ServerConnector connector =
                    new ServerConnector(server, null, scheduler, null, -1, -1, httpConnectionFactory);
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
            SourceLister sourceLister = new SourceLister(sourcePaths);
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
            RegistryUpdateListeners timer = new RegistryUpdateListeners(registry);
            server.addLifeCycleListener(timer);
            timer.start();

            server.setHandler(handlers);
            server.start();

            return connector.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ResourceCollection getBaseResource() throws IOException {
        List<Resource> bases = new ArrayList<>();

        Optional<File> skybarHome = findSkybarHome();
        if(skybarHome.isPresent()) {
            bases.add(Resource.newResource(skybarHome.get()));
        } else {
            bases.add(Resource.newClassPathResource("/org/wtf/skybar/web/"));
        }

        Collections.list(getClass().getClassLoader().getResources("META-INF/resources/"))
                .forEach(url -> bases.add(Resource.newResource(url)));

        return new ResourceCollection(bases.toArray(new Resource[bases.size()]));
    }

    private Optional<File> findSkybarHome() {
        String home = System.getProperty("skybar.home");
        if(home != null) {
            File source = new File(home, "skybar/src/main/resources/org/wtf/skybar/web");
            if (source.exists()) {
                try {
                    return Optional.of(source.getCanonicalFile());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return Optional.empty();
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
