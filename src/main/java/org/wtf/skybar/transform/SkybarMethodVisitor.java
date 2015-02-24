package org.wtf.skybar.transform;

/**
 *
 */

import org.objectweb.asm.MethodVisitor;
import org.wtf.skybar.transform.util.WorkingLineNumberVisitor;

class SkybarMethodVisitor extends WorkingLineNumberVisitor {

    private final String sourceFile;

    public SkybarMethodVisitor(String sourceFile, MethodVisitor mv) {
        super(ASM5, mv);
        this.sourceFile = sourceFile;
    }

    @Override
    protected void onLineNumber(int lineNumber) {
        // TODO: This just outputs the count to System.out.
        // Instead, inject a call to SkybarRegistry.registry.visitLine()
        // Hint: HelloWorldAsm in tests will output the ASM code which produces HelloWorld's byte code
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn("Running line " + lineNumber + " of source file " + sourceFile);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack+3, maxLocals);
    }
}
