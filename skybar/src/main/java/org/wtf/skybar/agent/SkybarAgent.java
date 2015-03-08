package org.wtf.skybar.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wtf.skybar.registry.SkybarRegistry;
import org.wtf.skybar.transform.SkybarTransformer;
import org.wtf.skybar.web.WebServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class SkybarAgent {

    private static final Logger logger = LoggerFactory.getLogger(SkybarAgent.class);

    public static void premain(String options, Instrumentation instrumentation) throws Exception {

        SkybarConfig config = getSkybarConfig();

        Pattern classNameRegex = config.getClassNameRegex();
        if (classNameRegex == null) {
            System.err.println("skybar.instrumentation.classRegex property not defined.");
            System.exit(-1);
        }

        instrumentation.addTransformer(new SkybarTransformer(classNameRegex), false);
        int port = config.getWebUiPort();
        new WebServer(SkybarRegistry.registry, port, getSourcePathString(config)).start();
        logger.info("Skybar started on port " + port + " against classes matching " + classNameRegex);
    }

    private static SkybarConfig getSkybarConfig() throws IOException {
        String configFile = System.getProperty("skybar.config");
        if (configFile == null) {
            configFile = System.getenv("SKYBAR_CONFIG");
        }
        Properties fileProps = new Properties();
        if (configFile != null) {
            try (InputStreamReader reader =
                     new InputStreamReader(new FileInputStream(new File(configFile)), StandardCharsets.UTF_8)) {
                fileProps.load(reader);
            }
        }

        return new SkybarConfig(toMap(fileProps), toMap(System.getProperties()), System.getenv());
    }

    private static String getSourcePathString(SkybarConfig config) throws IOException {
        String sourceLookupPath = config.getSourceLookupPath();
        if (sourceLookupPath == null) {
            return new File("src/main/java").getCanonicalPath();
        }
        return sourceLookupPath;
    }

    private static Map<String, String> toMap(Properties props) {
        HashMap<String, String> map = new HashMap<>();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            map.put((String) entry.getKey(), (String) entry.getValue());
        }

        return map;
    }
}
