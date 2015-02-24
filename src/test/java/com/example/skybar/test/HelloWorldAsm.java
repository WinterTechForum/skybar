package com.example.skybar.test;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

/**
 *
 */
public class HelloWorldAsm {

    public static void main(String[] args) throws IOException {
        Class clazz = HelloWorld.class;
        InputStream inputStream = clazz.getResourceAsStream(clazz.getSimpleName() + ".class");

        ClassReader reader = new ClassReader(inputStream);

        ClassVisitor visitor = new TraceClassVisitor(null, new ASMifier(), new PrintWriter(System.out));
        reader.accept(visitor, 0);
    }
}
