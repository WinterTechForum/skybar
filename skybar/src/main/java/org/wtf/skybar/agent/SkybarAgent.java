package org.wtf.skybar.agent;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;
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
import java.util.*;

public class SkybarAgent {

    private static final org.eclipse.jetty.util.log.Logger LOG = Log.getLogger(SkybarAgent.class);
    private static final Logger logger = LoggerFactory.getLogger(SkybarAgent.class);

    public static void premain(String options, Instrumentation instrumentation) throws Exception {

        perhapsWait();

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
        int actualPort = new WebServer(SkybarRegistry.registry, configuredPort, getSourceResources(config)).start();
        logger.info("Skybar started on port " + actualPort+ " against classes matching " + describeIncludes(config));
    }

    /**
     * Conditionally wait a few secs at startup to allow a debugger to connect. Useful for development of Skybar itself.
     */
    private static void perhapsWait() {

        String waitTime = System.getProperty("skybar.waitsecs");
        if(waitTime != null) {
            try {
                Thread.sleep(Long.parseLong(waitTime)*1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
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
        if(configFile == null) {
            File defaultFile = new File("skybar.config");
            if(defaultFile.exists()) {
                configFile = defaultFile.getAbsolutePath() ;
            }
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

    private static Resource[] getSourceResources(SkybarConfig config) throws IOException {
        List<Resource> resources = new ArrayList<>();

        String sourceLookupPath = config.getSourceLookupPath();
        if (sourceLookupPath != null) {
            String[] split = sourceLookupPath.split(System.getProperty("path.separator"));
            for (String str: split) {
                File dir = new File(str).getCanonicalFile();
                if (!dir.isDirectory()) {
                    throw new IOException("Invalid search path, not a directory: "+
                            dir.getAbsolutePath());
                }
                resources.add(Resource.newResource(dir));
                LOG.info("Skybar source path added: "+dir.getAbsolutePath());
            }
        }
        File projectSource = new File("src/main/java");
        if(projectSource.exists()) {
            resources.add(Resource.newResource(projectSource));
        }
        return resources.toArray(new Resource[resources.size()]);
    }

    private static Map<String, String> toMap(Properties props) {
        HashMap<String, String> map = new HashMap<>();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            map.put((String) entry.getKey(), (String) entry.getValue());
        }

        return map;
    }
}
