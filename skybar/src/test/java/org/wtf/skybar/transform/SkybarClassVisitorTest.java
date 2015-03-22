package org.wtf.skybar.transform;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
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
        Map<Integer, Long> lines = linesOf(clazz);

        assertThat(lines.size(), is(2));
        assertThat(lines.get(6).longValue(), is(1l));
        assertThat(lines.get(9).longValue(), is(1l));
    }

    @Test
    public void shouldCountStaticOneliner() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        // Given
        Class<?> clazz = instrumentClass(StaticOneLiner.class);

        // When
        invokeStaticMethod(clazz, "staticOneLiner");

        // Then
        Map<Integer, Long> lines = linesOf(clazz);

        assertThat(lines.size(), is(2));
        assertThat(lines.get(6).longValue(), is(0l));
        assertThat(lines.get(9).longValue(), is(1l));
    }

    @Test
    public void shouldCountStaticInitOneLiner() {
        Class<?> clazz = instrumentClass(StaticInitializerOneLiner.class);

        assertCounts(clazz, map(p(7, 1), p(8, 1)));
    }

    @Test
    public void shouldCountInstanceInitOneLiner() throws NoSuchMethodException, IllegalAccessException,
        InvocationTargetException, InstantiationException {
        Class<?> clazz = instrumentClass(InstanceInitializerOneLiner.class);
        clazz.getConstructor().newInstance();

        assertCounts(clazz, linesWithSingleCounts(7));
    }

    @Test
    public void shouldCountCtorInitOneLiner() throws NoSuchMethodException, IllegalAccessException,
        InvocationTargetException, InstantiationException {
        Class<?> clazz = instrumentClass(ConstructorOneLiner.class);
        clazz.getConstructor().newInstance();

        assertCounts(clazz, linesWithSingleCounts(7));
    }

    @Test
    public void shouldCountConditional() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        Class<?> clazz = instrumentClass(Conditional.class);

        invokeStaticMethod(clazz, "foo");

        assertCounts(clazz, linesWithSingleCounts(5, 6, 10));
    }

    @Test
    public void shouldCountTryCatch() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        Class<?> clazz = instrumentClass(ExceptionCatch.class);

        invokeStaticMethod(clazz, "foo");

        assertCounts(clazz, linesWithSingleCounts(5, 7, 9, 10, 13));
    }

    @Test
    public void shouldCountTryWithResources() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        Class<?> clazz = instrumentClass(TryWithResources.class);

        invokeStaticMethod(clazz, "foo");

        assertCounts(clazz, linesWithSingleCounts(9, 10, 11, 14));
    }

    @Test
    public void shouldCountForLoop() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        Class<?> clazz = instrumentClass(ForLoop.class);

        invokeStaticMethod(clazz, "foo");

        assertCounts(clazz, map(p(7, 1), p(8, 3), p(9, 3), p(12, 1)));
    }

    @Test
    public void shouldCountWhileLoop() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        Class<?> clazz = instrumentClass(WhileLoop.class);

        invokeStaticMethod(clazz, "foo");

        assertCounts(clazz, map(p(7, 1), p(8, 4), p(9, 3), p(12, 1)));
    }

    @Test
    public void shouldCountMultipleStatementsOnSameLine() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        Class<?> clazz = instrumentClass(MultStatementsOnSameLine.class);

        invokeStaticMethod(clazz, "foo");

        assertCounts(clazz, map(p(7, 1), p(8, 1), p(10, 1)));
    }

    @Test
    public void shouldCountForLoopWithException() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        Class<?> clazz = instrumentClass(ForLoopWithException.class);

        invokeStaticMethod(clazz, "foo");

        assertCounts(clazz, map(p(7, 1), p(9, 3), p(10, 3), p(11, 1), p(13, 2), p(15, 1), p(16, 1)));
    }

    private void assertCounts(Class<?> clazz, IntLongMap counts) {
        assertThat(nonZeroLinesOf(clazz), equalTo(counts));
    }

    private static void invokeStaticMethod(Class<?> clazz, String methodName) throws IllegalAccessException,
        InvocationTargetException,
        NoSuchMethodException {
        clazz.getMethod(methodName)
            .invoke(null);
    }

    private static IntLongMap map(Pair... pairs) {
        return HashIntLongMaps.newImmutableMap((c) -> {
            for (Pair pair : pairs) {
                c.accept(pair.line, pair.count);
            }
        }, pairs.length);
    }

    private static Pair p(int line, long count) {
        return new Pair(line, count);
    }

    private static IntLongMap linesWithSingleCounts(int... lineNums) {
        return HashIntLongMaps.newImmutableMap((c) -> {
            for (int lineNum : lineNums) {
                c.accept(lineNum, 1);
            }
        }, lineNums.length);
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

    private static IntLongMap nonZeroLinesOf(Class<?> clazz) {
        IntLongMap map = linesOf(clazz);
        map.removeIf((line, count) -> count == 0);
        return map;
    }

    private static String sourceName(Class<?> clazz) {
        return clazz.getPackage().getName().replace('.', '/') + "/" + clazz.getSimpleName() + ".java";
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
                            ClassReader reader = new ClassReader(clazz.getResourceAsStream(clazz.getSimpleName() + ".class"));
                            System.out.println("Unchanged bytecode: ");
                            reader.accept(new TraceClassVisitor(new PrintWriter(System.out)), ClassReader.EXPAND_FRAMES);
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

    static class Pair {
        final int line;
        final long count;

        public Pair(int line, long count) {
            this.line = line;
            this.count = count;
        }
    }
}
