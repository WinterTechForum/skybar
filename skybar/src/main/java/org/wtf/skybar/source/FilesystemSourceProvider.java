package org.wtf.skybar.source;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Looks up source files in the filesystem
 */
public final class FilesystemSourceProvider implements SourceProvider {

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    static final List<String> SUFFIXES = Collections.unmodifiableList(Arrays.asList("java", "groovy", "scala", "kt"));

    private final Path root;

    public FilesystemSourceProvider(Path root) {
        this.root = root;
    }

    @Override
    public String getSource(ClassLoader classLoader, @Nonnull String className) throws IOException {
        String fileBaseName = guessSourceFilePath(className);
        for (String suffix : SUFFIXES) {
            String candidate = fileBaseName + "." + suffix;
            File file = root.resolve(candidate).toFile();
            if (file.canRead() && file.isFile()) {
                return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            }
        }

        return null;
    }

    static String guessSourceFilePath(String className) {
        // try to figure out top level class
        List<String> chunks = Arrays.asList(className.split(FILE_SEPARATOR));

        String sourceFileBase = guessTopLevelClassName(chunks.get(chunks.size() - 1));
        chunks.set(chunks.size() - 1, sourceFileBase);

        return chunks.stream().collect(Collectors.joining("/"));
    }

    private static String guessTopLevelClassName(String classResourceName) {

        int index = classResourceName.indexOf("$");
        if (index != -1) {
            return classResourceName.substring(0, index);
        }

        return classResourceName;
    }
}
