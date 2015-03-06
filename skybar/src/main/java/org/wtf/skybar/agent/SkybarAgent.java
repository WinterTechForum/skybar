package org.wtf.skybar.agent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import org.wtf.skybar.registry.SkybarRegistry;
import org.wtf.skybar.transform.SkybarTransformer;
import org.wtf.skybar.web.WebServer;

public class SkybarAgent {

    public static final String SOURCE_PATH_SYS_PROPERTY = "skybar.sourcePath";
    public static final String SOURCE_PATH_ENV_VAR_NAME = "SKYBAR_SOURCE_PATH";

    public static void premain(String options, Instrumentation instrumentation) throws Exception {

        String prefix = System.getProperty("skybar.includedPackage");
        if (prefix == null) {
            System.err.println("skybar.includedPackage system property not defined.");
            System.exit(-1);
        }
        instrumentation
            .addTransformer(new SkybarTransformer(prefix),
                false);
        int port = Integer.parseInt(System.getProperty("skybar.serverPort", "4321"));
        new WebServer(SkybarRegistry.registry, port, getSourcePathString()).start();
        System.out.println("Skybar started on port " + port + " against package:" + prefix);
    }

    private static String getSourcePathString() throws IOException {
        String sourcePath = System.getProperty(SOURCE_PATH_SYS_PROPERTY);
        if (sourcePath == null) {
            sourcePath = System.getenv(SOURCE_PATH_ENV_VAR_NAME);
        }
        if (sourcePath == null) {
            sourcePath = new File("src/main/java").getCanonicalPath();
        }
        return sourcePath;
    }
}
