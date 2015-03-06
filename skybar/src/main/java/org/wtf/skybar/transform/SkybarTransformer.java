package org.wtf.skybar.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class SkybarTransformer implements ClassFileTransformer {
    private final String prefix;

    public SkybarTransformer(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {

        if (!shouldInstrument(className, bytes)) {
            return null;
        }

        System.out.println("Instrumenting " + className);
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new SkybarClassVisitor(writer), ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private boolean shouldInstrument(String className, byte[] bytes) {
        if(className == null) {
            return false; // Lambda weirdness?
        }
        if(className.startsWith("org/wtf/skybar")) {
            return false; // Can't instrument self
        }
        if(bytes == null) {
            return false; // Can't instrument with no byte code
        }
        // Ok, do we match our prefix?
        return className.startsWith(prefix);
    }
}
