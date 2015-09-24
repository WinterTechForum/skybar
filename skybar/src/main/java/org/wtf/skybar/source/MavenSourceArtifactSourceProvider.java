package org.wtf.skybar.source;

import org.eclipse.jetty.util.IO;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Source provider which parses the jar file of the class, tries to discover Maven G:A:V and use
 * that to locate a source jar in the local Maven repository.
 */
public class MavenSourceArtifactSourceProvider implements SourceProvider {

    private final ConcurrentMap<String, SourceJar> sourceJars = new ConcurrentHashMap<>();
    private final File localMavenRepo;

    public MavenSourceArtifactSourceProvider() {
        localMavenRepo = new File(System.getProperty("user.home"), ".m2/repository");
    }

    @Override
    public Source lookup(String path, ClassLoader classLoader) {
        String classFile = path.substring(0, path.lastIndexOf(".")) + ".class";
        URL classResource = classLoader.getResource(classFile);
        if (classResource == null || !"jar".equals(classResource.getProtocol())) {
            return null;
        }

        String jarFile = getJarFile(classResource);
        SourceJar sourceJar = lookupSourceJar(jarFile, classResource);

        if (sourceJar.isFound()) {
            return new JarSource(path, sourceJar.getFile());
        }
        return null;
    }

    /**
     * Returns the path to the Jar file a .class file is packaged in
     * @param classResource the URL of the .class file
     * @return the path to the Jar file
     */
    private String getJarFile(URL classResource) {
        try {
            String file = URLDecoder.decode(classResource.getFile(), Charset.defaultCharset().displayName());
            return file.substring("file:".length(), file.indexOf("!"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Looks up a SourceJar, possibly from the cache
     * @param jarPath the path to the jar file
     * @param classResource  the URL of the .class file
     * @return the SourceJar
     */
    private SourceJar lookupSourceJar(String jarPath, URL classResource) {
        SourceJar cached = sourceJars.get(jarPath);
        return cached == null ? findSourceJar(jarPath, classResource) : cached;
    }

    /**
     * Locates a SourceJar, adding it to the cache if not already present
     * @param jarPath the path to the jar file
     * @param classResource the URL to the .class file
     * @return the SourceJar
     */
    private SourceJar findSourceJar(String jarPath, URL classResource) {
        Optional<Gav> gav = parsePom(jarPath, classResource);
        if (gav.isPresent()) {
            File sourceJarFile = localMavenRepo.toPath()
                    .resolve(gav.get().getGroupId().replace('.', '/'))
                    .resolve(gav.get().getArtifactId())
                    .resolve(gav.get().getVersion())
                    .resolve(gav.get().getArtifactId() + "-" + gav.get().getVersion() + "-sources.jar").toFile();
            if (sourceJarFile.exists()) {
                SourceJar sourceJar = new SourceJar(sourceJarFile);
                this.sourceJars.putIfAbsent(jarPath, sourceJar);
                return sourceJar;
            }

        }
        return SourceJar.NOT_FOUND;
    }

    /**
     * Scans the jar file looking for META-INF/maven/{artifactId}/pom.properties
     * @param jarPath the path to the jar file
     * @param classResource the URL to the .class file
     * @return an optional Gav if found
     */
    private Optional<Gav> parsePom(String jarPath, URL classResource) {

        File file = new File(jarPath);

        try {
            URLConnection urlConnection = classResource.openConnection();
            if (urlConnection instanceof JarURLConnection) {
                JarFile jarFile = ((JarURLConnection) urlConnection).getJarFile();

                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String prefix = "META-INF/maven/";
                    String propSuffix = "/pom.properties";

                    if (entry.getName().startsWith(prefix)
                            && entry.getName().endsWith(propSuffix)) {
                        Properties props = new Properties();
                        InputStream inputStream = jarFile.getInputStream(entry);
                        props.load(inputStream);
                        inputStream.close();
                        String groupId = props.getProperty("groupId");
                        String artifactId = props.getProperty("artifactId");
                        String version = props.getProperty("version");

                        if (file.getName().startsWith(artifactId + "-" + version)) {
                            return Optional.of(new Gav(groupId, artifactId, version));
                        }
                    }
                }
            }

            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class SourceJar {
        private final File file;
        public static final SourceJar NOT_FOUND = new SourceJar();

        private SourceJar() {
            this.file = null;
        }

        public SourceJar(File file) {
            this.file = file;
        }

        public boolean isFound() {
            return file != null;
        }

        public File getFile() {
            return file;
        }
    }

    private class Gav {
        private final String groupId;
        private final String artifactId;
        private final String version;

        public Gav(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getVersion() {
            return version;
        }
    }

    private static class JarSource implements Source {

        private final String path;
        private final File file;

        public JarSource(String path, File file) {

            this.path = path;
            this.file = file;
        }

        @Override
        public void write(OutputStream outputStream) {
            try (JarFile jarFile = new JarFile(file);
                 InputStream is = jarFile.getInputStream(jarFile.getEntry(path))) {
                IO.copy(is, outputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
