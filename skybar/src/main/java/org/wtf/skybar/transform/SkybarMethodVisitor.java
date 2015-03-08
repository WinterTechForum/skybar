package org.wtf.skybar.transform;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.wtf.skybar.registry.SkybarRegistry;
import org.wtf.skybar.transform.util.WorkingLineNumberVisitor;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

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
        SkybarRegistry.registry.registerLine(sourceFile, lineNumber);

        MethodType mt = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class);

        Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, Type.getInternalName(SkybarRegistry.class), "bootstrap",
                mt.toMethodDescriptorString());
        mv.visitInvokeDynamicInsn("visitLine", "()V", bootstrap, sourceFile, lineNumber);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack + 3, maxLocals);
    }
}
