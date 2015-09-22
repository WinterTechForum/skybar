package org.wtf.skybar.agent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SkybarConfig {

    private final Map<String, String> fileProps;
    private final Map<String, String> systemProps;
    private final Map<String, String> env;

    SkybarConfig(Map<String, String> fileProps, Map<String, String> systemProps, Map<String, String> env) {
        this.fileProps = fileProps;
        this.systemProps = systemProps;
        this.env = env;
    }

    /**
     * @return port to listen on, or 0 to choose a random port
     */
    int getWebUiPort() {
        return Integer.parseInt(getConfigValue("skybar.port", "7070"));
    }

    /**
     * This regex will be used with {@link Matcher#matches()} to determine if a class should be instrumented. This will
     * be used to match against the "binary name" of the class, e.g. "com/foo/bar/SomeClass".
     *
     * @return regex to use against class names
     */
    @Nullable
    Pattern getIncludeRegex() {
        return getRegexPattern("skybar.includeRegex");
    }

    /**
     * This regex will be used with {@link Matcher#matches()} to determine if a class should be excluded. This will
     * be used to match against the "binary name" of the class, e.g. "com/foo/bar/SomeClass".
     *
     * @return regex to use against class names
     */
    @Nullable
    Pattern getExcludeRegex() {
        return getRegexPattern("skybar.excludeRegex");
    }

    private Pattern getRegexPattern(String propName) {
        String configValue = getConfigValue(propName, null);

        return configValue == null ? null : Pattern.compile(configValue);
    }

    /**
     * This list of class name prefixes will be used to determine if a class should be included. This will
     * be used to match against the "binary name" of the class, e.g. "com/foo/bar/SomeClass".
     *
     * @return class name prefixes to use against class names
     */
    @Nullable
    String[] getExcludes() {
        String configValue = getConfigValue("skybar.exclude", null);

        return configValue == null ? null : parseList(configValue);
    }
    /**
     * This list of class name prefixes will be used to determine if a class should be excluded. This will
     * be used to match against the "binary name" of the class, e.g. "com/foo/bar/SomeClass".
     *
     * @return class name prefixes to use against class names
     */
    @Nullable
    String[] getIncludes() {
        String configValue = getConfigValue("skybar.include", null);

        return configValue == null ? null : parseList(configValue);
    }

    /**
     * Parse a comma-separated String into an array of Strings
     * @param configValue
     * @return
     */
    private String[] parseList(String configValue) {
        List<String> values = new ArrayList<>();
        for(String part : configValue.split(",")) {
            values.add(part.trim());
        }

        return values.toArray(new String[values.size()]);
    }

    /**
     * @return filesystem path, or null to use 'src/main/java' resolved from the current directory
     */
    @Nullable
    String getSourceLookupPath() {
        return getConfigValue("skybar.source.fsPath", null);
    }

    @Nullable
    private String getConfigValue(String propName, @Nullable String defaultValue) {
        if (systemProps.containsKey(propName)) {
            return systemProps.get(propName);
        }

        if (env.containsKey(transformToEnvVar(propName))) {
            return env.get(transformToEnvVar(propName));
        }

        if (fileProps.containsKey(propName)) {
            return fileProps.get(propName);
        }

        return defaultValue;
    }


    private String transformToEnvVar(String propName) {
        return propName.replace('.', '_').toUpperCase(Locale.US);
    }

    public boolean isIncludeConfigured() {
        return getIncludes() != null || getIncludeRegex() != null;
    }
}


