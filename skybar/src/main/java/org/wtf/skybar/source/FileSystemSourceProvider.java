package org.wtf.skybar.source;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 *
 */
public class FileSystemSourceProvider implements SourceProvider {

    private final File directory;

    public FileSystemSourceProvider(File directory) {
        this.directory = directory;
    }

    @Override
    public Source lookup(String path, ClassLoader classLoader) {
        File file = new File(directory, path);
        if(file.exists() && file.isFile()) {
            return new FileSystemFile(file);
        }
        return null;
    }

    private class FileSystemFile implements Source {
        private final File file;

        public FileSystemFile(File file) {
            this.file = file;
        }

        @Override
        public void write(OutputStream outputStream) {
            try {
                Files.copy(file.toPath(), outputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
