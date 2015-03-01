package org.wtf.skybar.transform;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.regex.Pattern;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkybarTransformer implements ClassFileTransformer {
    private static final Logger logger = LoggerFactory.getLogger(SkybarTransformer.class);

    private final Pattern pattern;

    public SkybarTransformer(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {

        if (shouldInstrument(className, bytes)) {
            logger.debug("Instrumenting " + className);
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(reader, 0);
            reader.accept(new SkybarClassVisitor(writer), ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        }
        return bytes;
    }

    private boolean shouldInstrument(String className, byte[] bytes) {
        if (className == null) {
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
        return pattern.matcher(className).matches() && !className.contains("$$");
    }
}
