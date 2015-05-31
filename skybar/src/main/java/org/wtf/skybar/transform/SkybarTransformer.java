package org.wtf.skybar.transform;

import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.regex.Pattern;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.objectweb.asm.util.TraceClassVisitor;

public class SkybarTransformer implements ClassFileTransformer {
    private static final Logger logger = LoggerFactory.getLogger(SkybarTransformer.class);

    private final Pattern pattern;

    public SkybarTransformer(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {

        if(shouldInstrument(className, loader, bytes))  {
            logger.debug("Instrumenting " + className);
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(reader, 0);
            ClassVisitor visitor = writer;
            if(shouldTrace()) {
                visitor = new TraceClassVisitor(writer, new PrintWriter(System.out));
                System.out.println("Unchanged bytecode: ");
                reader.accept(new TraceClassVisitor(new PrintWriter(System.out)), ClassReader.EXPAND_FRAMES);
            }
            try {
                reader.accept(new SkybarClassVisitor(visitor), ClassReader.EXPAND_FRAMES);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return writer.toByteArray();
        }
        return bytes;
    }

    private boolean shouldTrace() {
        return false;
    }

    private boolean shouldInstrument(String className, ClassLoader loader, byte[] bytes) {
        if(loader == null) {
            return false; // JDK classes
        }
        if(className == null) {
            return false; // Lambda weirdness?
        }
        if (className.startsWith("org/wtf/skybar")) {
            return false; // Can't instrument self
        }
        if (bytes == null) {
            return false; // Can't instrument with no byte code
        }
        // Ok, do we match our pattern?
        // TODO figure out what to do with classes we can't find source for
        return pattern.matcher(className).matches();
    }
}
