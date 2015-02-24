package org.wtf.skybar.agent;

import org.wtf.skybar.transform.SkybarTransformer;
import org.wtf.skybar.web.WebServer;

import java.lang.instrument.Instrumentation;

/**
 *
 */
public class SkybarAgent {

    public static void premain(String options, Instrumentation instrumentation) throws Exception {

        // TODO: Make this configurable (a CLI option / SKYBAR_OPTS?)
        String prefix = "com/skybar";
        instrumentation.addTransformer(new SkybarTransformer(prefix), false);
        int port = 4321;
        new WebServer().start(port);
        System.out.println("SKYBAR  started on port " + port);
    }
}
