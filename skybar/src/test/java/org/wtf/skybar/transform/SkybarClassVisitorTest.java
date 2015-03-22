package org.wtf.skybar.transform;

import com.palominolabs.http.url.PercentDecoder;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.openhft.koloboke.collect.map.IntLongMap;
import net.openhft.koloboke.collect.map.hash.HashIntLongMaps;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.wtf.skybar.registry.SkybarRegistry;
import org.wtf.skybar.transform.testcases.Conditional;
import org.wtf.skybar.transform.testcases.ConstructorOneLiner;
import org.wtf.skybar.transform.testcases.ExceptionCatch;
import org.wtf.skybar.transform.testcases.ForLoop;
import org.wtf.skybar.transform.testcases.ForLoopWithException;
import org.wtf.skybar.transform.testcases.InstanceInitializerOneLiner;
import org.wtf.skybar.transform.testcases.MultStatementsOnSameLine;
import org.wtf.skybar.transform.testcases.OneLiner;
import org.wtf.skybar.transform.testcases.StaticInitializerOneLiner;
import org.wtf.skybar.transform.testcases.StaticOneLiner;
import org.wtf.skybar.transform.testcases.TryWithResources;
import org.wtf.skybar.transform.testcases.WhileLoop;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class SkybarClassVisitorTest {

    @Test
    public void shouldCountOneliner() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException,
        InstantiationException {
        // Given
        Class<?> clazz = instrumentClass(OneLiner.class);

        // When
        clazz.getMethod("oneLiner")
            .invoke(clazz.getConstructor().newInstance());

        // Then
        assertCorrectSourceCount(clazz);
    }

    @Test
    public void shouldCountStaticOneliner() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        // Given
        Class<?> clazz = instrumentClass(StaticOneLiner.class);

        // When
        invokeStaticMethod(clazz, "staticOneLiner");

        // Then
        assertCorrectSourceCount(clazz);
    }

    @Test
    public void shouldCountStaticInitOneLiner() {
        Class<?> clazz = instrumentClass(StaticInitializerOneLiner.class);

        assertCorrectSourceCount(clazz);
    }

    @Test
    public void shouldCountInstanceInitOneLiner() throws NoSuchMethodException, IllegalAccessException,
        InvocationTargetException, InstantiationException {
        Class<?> clazz = instrumentClass(InstanceInitializerOneLiner.class);
        clazz.getConstructor().newInstance();

        assertCorrectSourceCount(clazz);
    }

    @Test
    public void shouldCountCtorInitOneLiner() throws NoSuchMethodException, IllegalAccessException,
        InvocationTargetException, InstantiationException {
        Class<?> clazz = instrumentClass(ConstructorOneLiner.class);
        clazz.getConstructor().newInstance();

        assertCorrectSourceCount(clazz);
    }

    @Test
    public void shouldCountConditional() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        Class<?> clazz = instrumentClass(Conditional.class);

        invokeStaticMethod(clazz, "foo");

        assertCorrectSourceCount(clazz);
    }

    @Test
    public void shouldCountTryCatch() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        Class<?> clazz = instrumentClass(ExceptionCatch.class);

        invokeStaticMethod(clazz, "foo");

        assertCorrectSourceCount(clazz);
    }

    @Test
    public void shouldCountTryWithResources() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        Class<?> clazz = instrumentClass(TryWithResources.class);

        invokeStaticMethod(clazz, "foo");

        assertCorrectSourceCount(clazz);
    }

    @Test
    public void shouldCountForLoop() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        Class<?> clazz = instrumentClass(ForLoop.class);

        invokeStaticMethod(clazz, "foo");

        assertCorrectSourceCount(clazz);
    }

    @Test
    public void shouldCountWhileLoop() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        Class<?> clazz = instrumentClass(WhileLoop.class);

        invokeStaticMethod(clazz, "foo");

        assertCorrectSourceCount(clazz);
    }

    @Test
    public void shouldCountMultipleStatementsOnSameLine() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        Class<?> clazz = instrumentClass(MultStatementsOnSameLine.class);

        invokeStaticMethod(clazz, "foo");

        assertCorrectSourceCount(clazz);
    }

    @Test
    public void shouldCountForLoopWithException() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        Class<?> clazz = instrumentClass(ForLoopWithException.class);

        invokeStaticMethod(clazz, "foo");

        assertCorrectSourceCount(clazz);
    }

    private static void invokeStaticMethod(Class<?> clazz, String methodName) throws IllegalAccessException,
        InvocationTargetException,
        NoSuchMethodException {
        clazz.getMethod(methodName)
            .invoke(null);
    }

    private static IntLongMap linesOf(Class<?> clazz) {
        SkybarRegistry.DeltaListener deltaListener = (x) -> {
        };
        SkybarRegistry registry = SkybarRegistry.registry;
        registry.updateListeners(new HashMap<>());
        Map<String, IntLongMap> snapshot = registry.getCurrentSnapshot(deltaListener);

        registry.unregisterListener(deltaListener);

        return snapshot.get(sourceName(clazz));
    }

    private static String sourceName(Class<?> clazz) {
        return clazz.getPackage().getName().replace('.', '/') + "/" + clazz.getSimpleName() + ".java";
    }

    private void assertCorrectSourceCount(Class<?> clazz) {
        IntLongMap expected = HashIntLongMaps.newMutableMap();
        parseExpectedLines(clazz, expected);
        assertThat(linesOf(clazz), equalTo(expected));
    }

    private void parseExpectedLines(Class<?> clazz, IntLongMap expected) {
        File source = sourceOf(clazz);
        try {
            List<String> lines = Files.readAllLines(source.toPath(), Charset.defaultCharset());
            for (int i = 1; i <= lines.size(); i++) {
                String l = lines.get(i-1);
                int commentStart = l.lastIndexOf("//");
                if(commentStart != -1) {

                    String comment = l.substring(commentStart+2).trim();
                    try {
                        expected.put(i, Long.parseLong(comment));
                    } catch(NumberFormatException e) {
                        System.err.print("Error parsing expected line number on line " + (i) + " of " + source.getAbsolutePath());
                    }
                }


            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File sourceOf(Class<?> clazz) {
        String path  = "src/test/java/" + clazz.getName().replace('.', '/') +".java";

        File root;
        try {
            root = new File(new PercentDecoder(UTF_8.newDecoder()).decode(clazz.getResource("/").getFile()));
        } catch (CharacterCodingException e) {
            throw new RuntimeException("Couldn't decode", e);
        }
        File source = new File(root, path);
        while(root.getParentFile() != null && !source.exists()) {
            root = root.getParentFile();
            source = new File(root, path);
        }
        return source;
    }

    private static Class<?> instrumentClass(Class<?> clazz) {
        try {

            return Class.forName(clazz.getName(), true, new ClassLoader() {

                @Override
                public Class<?> loadClass(String name) throws ClassNotFoundException {
                    try {
                        return findClass(name);
                    } catch (ClassNotFoundException e) {
                        return super.loadClass(name);
                    }
                }

                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    if (name.equals(clazz.getName())) {
                        try {
                            ClassReader reader =
                                new ClassReader(clazz.getResourceAsStream(clazz.getSimpleName() + ".class"));
                            System.out.println("Unchanged bytecode: ");
                            reader
                                .accept(new TraceClassVisitor(new PrintWriter(System.out)), ClassReader.EXPAND_FRAMES);
                            ClassWriter writer = new ClassWriter(reader, 0);
                            ClassVisitor visitor = new TraceClassVisitor(writer, new PrintWriter(System.out));
                            reader.accept(new SkybarClassVisitor(visitor), ClassReader.EXPAND_FRAMES);

                            byte[] bytes = writer.toByteArray();
                            return defineClass(name, bytes, 0, bytes.length);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    throw new ClassNotFoundException(name);
                }
            });
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
