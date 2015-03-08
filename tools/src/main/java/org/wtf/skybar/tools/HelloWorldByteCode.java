package org.wtf.skybar.tools;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

/**
 *
 */
public class HelloWorldByteCode {

    public static void main(String[] args) throws IOException {
        Class clazz = HelloWorld.class;
        InputStream inputStream = clazz.getResourceAsStream(clazz.getSimpleName() + ".class");

        ClassReader reader = new ClassReader(inputStream);

        ClassVisitor visitor = new TraceClassVisitor(new PrintWriter(System.out));
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
    }

}
