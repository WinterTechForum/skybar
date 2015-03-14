package org.wtf.skybar.transform;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.wtf.skybar.registry.SkybarRegistry;
import org.wtf.skybar.testcases.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

public class SkybarClassVisitorTest {

    @Test
    public void shouldCountOneliner() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        // Given
        Class<?> clazz = instrumentClass(OneLiner.class);

        // When
        clazz.getMethod("oneLiner")
                .invoke(clazz.newInstance());

        // Then
        Map<Integer, LongAdder> lines = linesOf(clazz);

        assertThat(lines.size(), is(2));
        assertThat(lines.get(6).longValue(), is(1l));
        assertThat(lines.get(9).longValue(), is(1l));
    }

    @Test
    public void shouldCountStaticOneliner() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Given
        Class<?> clazz = instrumentClass(StaticOneLiner.class);

        // When
        clazz.getMethod("staticOneLiner")
                .invoke(null);

        // Then
        Map<Integer, LongAdder> lines = linesOf(clazz);

        assertThat(lines.size(), is(2));
        assertThat(lines.get(6).longValue(), is(0l));
        assertThat(lines.get(9).longValue(), is(1l));
    }

    @Test
    public void shouldCountLoopMethod() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        // Given
        Class<?> clazz = instrumentClass(Loop.class);

        // When
        clazz.getMethod("loop")
                .invoke(clazz.newInstance());

        // Then
        Map<Integer, LongAdder> lines = linesOf(clazz);

        assertThat(lines.size(), is(5));
        assertThat(lines.get(6).longValue(), is(1l));
        assertThat(lines.get(9).longValue(), is(1l));
        assertThat(lines.get(10).longValue(), is(11l));
        assertThat(lines.get(11).longValue(), is(10l));
        assertThat(lines.get(13).longValue(), is(1l));
    }

    @Test
    public void shouldCountExceptional() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        // Given
        Class<?> clazz = instrumentClass(Exceptional.class);

        // When
        try {
            clazz.getMethod("exceptional")
                    .invoke(clazz.newInstance());
        } catch (InvocationTargetException e) {
            assertThat(e.getCause(), instanceOf(ArithmeticException.class));
        }

        // Then
        Map<Integer, LongAdder> lines = linesOf(clazz);

        assertThat(lines.size(), is(5));
        assertThat(lines.get(6).longValue(), is(1l));
        assertThat(lines.get(9).longValue(), is(1l));
        assertThat(lines.get(10).longValue(), is(1l));
        assertThat(lines.get(11).longValue(), is(0l));
        assertThat(lines.get(12).longValue(), is(0l));
    }


    @Test
    public void shouldCountConstructorLoops() throws IllegalAccessException, InstantiationException {
        // Given
        Class<?> clazz = instrumentClass(Constructors.class);

        clazz.newInstance();

        // Then
        Map<Integer, LongAdder> lines = linesOf(clazz);

        assertThat(lines.size(), is(4));
        assertThat(lines.get(10).longValue(), is(1l));
        assertThat(lines.get(11).longValue(), is(4l));
        assertThat(lines.get(12).longValue(), is(3l));
        assertThat(lines.get(14).longValue(), is(1l));
    }

    @Test
    public void shouldCountCatching() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        // Given
        Class<?> clazz = instrumentClass(Catching.class);

        // When
        clazz.getMethod("catching")
                .invoke(clazz.newInstance());


        // Then
        Map<Integer, LongAdder> lines = linesOf(clazz);

        assertThat(lines.size(), is(9));
        assertThat(lines.get(6).longValue(), is(1l));
        assertThat(lines.get(9).longValue(), is(1l));
        assertThat(lines.get(11).longValue(), is(1l));
        assertThat(lines.get(12).longValue(), is(0l));
        assertThat(lines.get(13).longValue(), is(1l));
        assertThat(lines.get(14).longValue(), is(1l));
        assertThat(lines.get(15).longValue(), is(0l));
        assertThat(lines.get(16).longValue(), is(1l));
    }

    private ConcurrentMap<String, Map<Integer, LongAdder>> getVisits() {
        try {
            Field visits = SkybarRegistry.class.getDeclaredField("visits");
            visits.setAccessible(true);
            return (ConcurrentMap<String, Map<Integer, LongAdder>>) visits.get(SkybarRegistry.registry);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    private Map<Integer, LongAdder> linesOf(Class<?> clazz) {
        return getVisits().get(sourceName(clazz));
    }

    private String sourceName(Class clazz) {
        return clazz.getPackage().getName().replace('.', '/') + "/" + clazz.getSimpleName() + ".java";
    }

    private Class instrumentClass(Class<?> clazz) {
        try {
            ClassReader reader = new ClassReader(clazz.getResourceAsStream(clazz.getSimpleName() +".class"));
            ClassWriter writer = new ClassWriter(reader, 0);
            ClassVisitor trace = new TraceClassVisitor(writer, new PrintWriter(System.out));
            System.out.println("Unchanged: ======>");
            reader.accept(new TraceClassVisitor(new PrintWriter(System.out)), ClassReader.EXPAND_FRAMES);
            System.out.println("Instrumented: ======>");
            reader.accept(new SkybarClassVisitor(trace), ClassReader.EXPAND_FRAMES);

            return new ClassLoader() {

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
                    if(name.equals(clazz.getName()))  {
                        byte[] bytes = writer.toByteArray();
                        return defineClass(name, bytes, 0, bytes.length);
                    }
                    throw new ClassNotFoundException(name);
                }
            }.loadClass(clazz.getName());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}