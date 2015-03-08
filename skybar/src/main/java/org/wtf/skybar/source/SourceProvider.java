package org.wtf.skybar.source;

import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface SourceProvider {

    /**
     * @param sourcePath source path to look for
     * @return null if not found, or the contents
     * @throws IOException on IO errors
     */
    @Nullable
    String getSource(@Nonnull String sourcePath) throws IOException;
}
