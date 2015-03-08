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

    private final SkybarInstrumentation skybarInstrumentation;

    public SkybarMethodVisitor(int access, String name, String desc, MethodVisitor mv, SkybarInstrumentation skybarInstrumentation) {
        super(ASM5, mv, access, name, desc);
        this.skybarInstrumentation = skybarInstrumentation;
    }

    @Override
    protected void onLineNumber(int lineNumber) {
        skybarInstrumentation.onLineNumber(lineNumber);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        skybarInstrumentation.visitMaxs(maxStack, maxLocals);
    }
}
