package org.wtf.skybar.source;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Loads source from the classpath.
 */
final class ClasspathSourceProvider implements SourceProvider {
    @Nullable
    @Override
    public String getSource(ClassLoader classLoader, @Nonnull String className) throws IOException {
        String sourceFilePath = FilesystemSourceProvider.guessSourceFilePath(className);

        for (String suffix : FilesystemSourceProvider.SUFFIXES) {
            String candidate = sourceFilePath + "." + suffix;
            try (InputStream res = classLoader.getResourceAsStream(candidate)) {
                if (res == null) {
                    continue;
                }

                return getString(res);
            }
        }

        return null;
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
