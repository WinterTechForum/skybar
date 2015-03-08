package org.wtf.skybar.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Loads source from the classpath.
 */
final class ClasspathSourceProvider implements SourceProvider {
    @Nullable
    @Override
    public String getSource(@Nonnull String sourcePath) throws IOException {

        // technically this can be null but only for classes loaded by the bootstrap classloader,
        // and this isn't one of those.
        // At some point we might care to know which classloader loaded the original class, but for
        // now just load the source with whatever we get here (which is probably the system classloader).
        ClassLoader cl = getClass().getClassLoader();

        try (InputStream res = cl.getResourceAsStream(sourcePath)) {
            if (res == null) {
                return null;
            }

            return getString(res);
        }
    }

    private String getString(InputStream res) throws IOException {
        try (Reader r = new BufferedReader(new InputStreamReader(res, StandardCharsets.UTF_8))) {
            StringBuilder buf = new StringBuilder();
            int ch;
            while ((ch = r.read()) != -1) {
                buf.append((char) ch);
            }

            return buf.toString();
        }
    }
}
