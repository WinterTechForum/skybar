package org.wtf.skybar.agent;

import java.util.regex.Matcher;
import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.DefaultNull;

interface SkybarConfig {

    /**
     * @return port to listen on, or 0 to choose a random port
     */
    @Config("skybar.webUi.port")
    @Default("54321")
    int getWebUiPort();

    /**
     * This regex will be used with {@link Matcher#matches()} to determine if a class should be instrumented. This will
     * be used to match against the "binary name" of the string, e.g. "com/foo/bar/SomeClass".
     *
     * @return regex to use against class names
     */
    @Config("skybar.instrumentation.classRegex")
    @DefaultNull
    String getClassNameRegex();

    /**
     * @return filesystem path, or null to use 'src/main/java' resolved from the current directory
     */
    @Config("skybar.source.fsPath")
    @DefaultNull
    String getSourceLookupPath();
}


