package org.wtf.skybar.source;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public interface SourceProvider {

    /**
     * @param classLoader classloader used to load the class
     * @param className   class to look for
     * @return null if not found, or the contents
     * @throws IOException on IO errors
     */
    @Nullable
    String getSource(ClassLoader classLoader, @Nonnull String className) throws IOException;
}
