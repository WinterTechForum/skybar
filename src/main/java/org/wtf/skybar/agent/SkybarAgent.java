package org.wtf.skybar.agent;

import org.wtf.skybar.transform.SkybarTransformer;
import org.wtf.skybar.web.WebServer;

import java.lang.instrument.Instrumentation;

/**
 *
 */
public class SkybarAgent {

    public static void premain(String options, Instrumentation instrumentation) throws Exception {

        String prefix = System.getProperty("skybar.includedPackage");
        if (prefix == null) {
            System.out.println("skybar.includedPackage system property not defined.");
            System.exit(-1);
        }
        instrumentation.addTransformer(new SkybarTransformer(prefix), false);
        int port = Integer.parseInt(System.getProperty("skybar.serverPort", "4321"));
        new WebServer().start(port);
        System.out.println("SKYBAR  started on port " + port + " against package:" + prefix);
    }
}
