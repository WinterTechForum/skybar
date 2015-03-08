package org.wtf.skybar.transform;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class SkybarClassVisitor extends ClassVisitor implements Opcodes {
    private String className;
    private String sourceFile;

    public SkybarClassVisitor(ClassVisitor writer) {
        super(ASM5, writer);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
        this.sourceFile = className.substring(0, className.lastIndexOf("/") + 1) + source;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new SkybarMethodVisitor(sourceFile, super.visitMethod(access, name, desc, signature, exceptions));
    }
}
