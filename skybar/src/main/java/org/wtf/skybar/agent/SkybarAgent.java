package org.wtf.skybar.agent;

import java.lang.instrument.Instrumentation;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.wtf.skybar.registry.SkybarRegistry;
import org.wtf.skybar.source.FilesystemSourceProvider;
import org.wtf.skybar.source.SourceProvider;
import org.wtf.skybar.transform.SkybarTransformer;
import org.wtf.skybar.web.WebServer;

public class SkybarAgent {

    public static void premain(String options, Instrumentation instrumentation) throws Exception {

        String prefix = System.getProperty("skybar.includedPackage");
        if (prefix == null) {
            System.err.println("skybar.includedPackage system property not defined.");
            System.exit(-1);
        }
        instrumentation.addTransformer(new SkybarTransformer(prefix), false);
        int port = Integer.parseInt(System.getProperty("skybar.serverPort", "4321"));
        List<SourceProvider> sourceProviders = new ArrayList<>();
        sourceProviders.add(new FilesystemSourceProvider(Paths.get("src/main/java")));
        new WebServer(SkybarRegistry.registry, port, sourceProviders).start();
        System.out.println("Skybar started on port " + port + " against package:" + prefix);
    }
}
