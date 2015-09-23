package org.wtf.skybar.agent;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class SkybarConfigTest {

    @Test
    public void testCheckSystemProperties() {
        HashMap<String, String> systemProps = new HashMap<>();
        systemProps.put("skybar.port", "3");
        SkybarConfig config = new SkybarConfig(new HashMap<>(), systemProps, new HashMap<>());

        assertEquals(3, config.getWebUiPort());
    }

    @Test
    public void testCheckEnvVar() {
        HashMap<String, String> env = new HashMap<>();
        env.put("SKYBAR_PORT", "3");
        SkybarConfig config = new SkybarConfig(new HashMap<>(), new HashMap<>(), env);

        assertEquals(3, config.getWebUiPort());
    }

    @Test
    public void testCheckSystemPropertiesBeforeFile() {
        HashMap<String, String> systemProps = new HashMap<>();
        systemProps.put("skybar.port", "3");
        HashMap<String, String> fileProps = new HashMap<>();
        fileProps.put("port", "30000");
        SkybarConfig config = new SkybarConfig(fileProps, systemProps, new HashMap<>());

        assertEquals(3, config.getWebUiPort());
    }
}