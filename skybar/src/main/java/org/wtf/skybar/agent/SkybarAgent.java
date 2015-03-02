package org.wtf.skybar.agent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.skife.config.CommonsConfigSource;
import org.skife.config.ConfigurationObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wtf.skybar.registry.SkybarRegistry;
import org.wtf.skybar.transform.SkybarTransformer;
import org.wtf.skybar.web.WebServer;

public class SkybarAgent {

    private static final Logger logger = LoggerFactory.getLogger(SkybarAgent.class);

    public static void premain(String options, Instrumentation instrumentation) throws Exception {

        SkybarConfig config = getSkybarConfig();

        String classNameRegex = config.getClassNameRegex();
        if (classNameRegex == null) {
            System.err.println("skybar.instrumentation.classRegex property not defined.");
            System.exit(-1);
        }

        instrumentation.addTransformer(new SkybarTransformer(Pattern.compile(classNameRegex)), false);
        int port = config.getWebUiPort();
        new WebServer(SkybarRegistry.registry, port, getSourcePathString(config)).start();
        logger.info("Skybar started on port " + port + " against classes matching " + classNameRegex);
    }

    private static SkybarConfig getSkybarConfig() throws ConfigurationException {
        CompositeConfiguration configStack = new CompositeConfiguration();

        // check system properties first
        configStack.addConfiguration(new SystemConfiguration());

        String configFile = System.getProperty("skybar.configFile");
        if (configFile != null) {
            PropertiesConfiguration newPropertiesConfig = getNewPropertiesConfig();
            newPropertiesConfig.load(new File(configFile));
            configStack.addConfiguration(newPropertiesConfig);
        }

        return new ConfigurationObjectFactory(new CommonsConfigSource(configStack)).build(SkybarConfig.class);
    }

    private static String getSourcePathString(SkybarConfig config) throws IOException {
        String sourceLookupPath = config.getSourceLookupPath();
        if (sourceLookupPath == null) {
            return new File("src/main/java").getCanonicalPath();
        }
        return sourceLookupPath;
    }

    private static PropertiesConfiguration getNewPropertiesConfig() {
        PropertiesConfiguration pc = new PropertiesConfiguration();
        pc.setEncoding(StandardCharsets.UTF_8.name());
        pc.setDelimiterParsingDisabled(true);
        return pc;
    }
}
