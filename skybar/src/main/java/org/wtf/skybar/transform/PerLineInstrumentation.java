package org.wtf.skybar.transform;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.wtf.skybar.registry.SkybarRegistry;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 *
 */
public class PerLineInstrumentation implements SkybarInstrumentation, Opcodes {

    private final MethodVisitor mv;
    private final String sourceFile;
    private final int version;

    public PerLineInstrumentation(MethodVisitor methodVisitor, String sourceFile, int version) {
        this.mv = methodVisitor;
        this.sourceFile = sourceFile;
        this.version = version;
    }

    @Override
    public void onLineNumber(int lineNumber) {
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

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(maxStack + 3, maxLocals);
    }

    private boolean useInvokeDynamic() {
        return version != Opcodes.V1_1 && version >= Opcodes.V1_7;
    }
}
