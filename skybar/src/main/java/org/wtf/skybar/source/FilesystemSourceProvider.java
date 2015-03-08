package org.wtf.skybar.source;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;

/**
 * Looks up source files in the filesystem
 */
public final class FilesystemSourceProvider implements SourceProvider {

    private final Path root;

    public FilesystemSourceProvider(Path root) {
        this.root = root;
    }

    @Override
    public String getSource(@Nonnull String sourcePath) throws IOException {

        File file = root.resolve(sourcePath).toFile();
        if (file.canRead() && file.isFile()) {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        }

        return null;
    }
}
