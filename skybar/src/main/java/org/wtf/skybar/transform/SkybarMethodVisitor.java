package org.wtf.skybar.transform;

import org.objectweb.asm.MethodVisitor;
import org.wtf.skybar.registry.SkybarRegistry;
import org.wtf.skybar.transform.util.WorkingLineNumberVisitor;

/**
 * Inserts instrumentation to update the SkybarRegistry on each line number.
 */
class SkybarMethodVisitor extends WorkingLineNumberVisitor {

    private final String sourceFile;

    public SkybarMethodVisitor(String sourceFile, MethodVisitor mv) {
        super(ASM5, mv);
        this.sourceFile = sourceFile;
    }

    @Override
    protected void onLineNumber(int lineNumber) {
        final long index = SkybarRegistry.registry.registerLine(sourceFile, lineNumber);

        mv.visitFieldInsn(GETSTATIC, "org/wtf/skybar/registry/SkybarRegistry", "registry",
                "Lorg/wtf/skybar/registry/SkybarRegistry;");
        mv.visitLdcInsn(index);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/wtf/skybar/registry/SkybarRegistry", "visitLine", "(J)V", false);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack + 3, maxLocals);
    }
}
