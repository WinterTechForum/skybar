package org.wtf.skybar.transform;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.wtf.skybar.registry.SkybarRegistry;
import org.wtf.skybar.testcases.OneLiner;
import org.wtf.skybar.testcases.StaticOneLiner;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

import static org.hamcrest.CoreMatchers.is;
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

    private ConcurrentMap<String, Map<Integer, LongAdder>> getVisits() {
        try {
            Field visits = SkybarRegistry.class.getDeclaredField("visits");
            visits.setAccessible(true);
            return (ConcurrentMap<String, Map<Integer, LongAdder>>) visits.get(SkybarRegistry.registry);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

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
            reader.accept(new SkybarClassVisitor(writer), ClassReader.EXPAND_FRAMES);

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