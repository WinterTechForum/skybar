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
    private final int version;

    public SkybarMethodVisitor(String sourceFile, int version, MethodVisitor mv) {
        super(ASM5, mv);
        this.sourceFile = sourceFile;
        this.version = version;
    }

    @Override
    protected void onLineNumber(int lineNumber) {
        SkybarRegistry.registry.registerLine(sourceFile, lineNumber);

        if(useInvokeDynamic()) {
            MethodType mt = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class);

            Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, Type.getInternalName(SkybarRegistry.class), "bootstrap",
                    mt.toMethodDescriptorString());
            mv.visitInvokeDynamicInsn("visitLine", "()V", bootstrap, sourceFile, lineNumber);
        } else {
            mv.visitFieldInsn(GETSTATIC, "org/wtf/skybar/registry/SkybarRegistry", "registry", "Lorg/wtf/skybar/registry/SkybarRegistry;");
            mv.visitLdcInsn(sourceFile);
            mv.visitLdcInsn(lineNumber);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/wtf/skybar/registry/SkybarRegistry", "getAdderForLine", "(Ljava/lang/String;I)Ljava/util/concurrent/atomic/LongAdder;", false);
            mv.visitInsn(LCONST_1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/LongAdder", "add", "(J)V", false);

        }
    }

    private boolean useInvokeDynamic() {
        return version != Opcodes.V1_1 && version >= Opcodes.V1_7;
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack + 3, maxLocals);
    }
}
