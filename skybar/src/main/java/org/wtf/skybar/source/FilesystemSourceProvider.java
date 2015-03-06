package org.wtf.skybar.source;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class FilesystemSourceProvider implements SourceProvider {

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    private static final String[] SUFFIXES = new String[]{"java", "groovy", "scala", "kt"};

    private final Path root;

    public FilesystemSourceProvider(Path root) {
        this.root = root;
    }

    @Override
    public String getSource(@Nonnull String className) throws IOException {
        Path sourcePath = root;

        // try to figure out top level class
        List<String> chunks = Arrays.asList(className.split(FILE_SEPARATOR));
        List<String> parentChunks = chunks.subList(0, chunks.size() - 1);

        for (String chunk : parentChunks) {
            sourcePath = sourcePath.resolve(chunk);
        }

        String sourceFileBase = guessTopLevelClassName(chunks.get(chunks.size() - 1));
        for (String suffix : SUFFIXES) {
            String candidate = sourceFileBase + "." + suffix;
            File file = sourcePath.resolve(candidate).toFile();
            if (file.canRead()) {
                return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            }
        }

        return null;
    }

    private String guessTopLevelClassName(String className) {

        int index = className.indexOf("$");
        if (index != -1) {
            return className.substring(0, index);
        }

        return className;
    }
}
