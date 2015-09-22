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

public class SkybarAgent {

    private static final Logger logger = LoggerFactory.getLogger(SkybarAgent.class);

    public static void premain(String options, Instrumentation instrumentation) throws Exception {

        SkybarConfig config = getSkybarConfig();

        if(! config.isIncludeConfigured()) {
            System.err.println("Skybar needs at least one include pattern to be configured.");
            System.err.println("Please define the skybar.include property");
            System.exit(-1);
        }

        SkybarTransformer transformer = new SkybarTransformer(config.getIncludes(),
                config.getExcludes(),
                config.getIncludeRegex(),
                config.getExcludeRegex());
        instrumentation.addTransformer(transformer, false);
        int configuredPort = config.getWebUiPort();
        int actualPort = new WebServer(SkybarRegistry.registry, configuredPort, getSourcePathString(config)).start();
        logger.info("Skybar started on port " + actualPort+ " against classes matching " + describeIncludes(config));
    }

    private static String describeIncludes(SkybarConfig config) {

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(toString(config.getIncludes()));
        sb.append("]");

        if(config.getExcludes() != null) {
            sb.append(" and not matching [");
            sb.append(toString(config.getExcludes()));
            sb.append("]");
        }

        return sb.toString();
    }

    private static String toString(String[] includes) {
        StringBuilder sb = new StringBuilder();
        if(includes != null) {
            for (int i = 0; i < includes.length; i++) {
                if(i > 0 ) {
                    sb.append(", ");
                }
                String include = includes[i];
                sb.append(include);
            }
        }
        return sb.toString();
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
