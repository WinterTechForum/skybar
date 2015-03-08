package org.wtf.skybar.transform.util;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 *
 */

/**
 * Calls onLineNumber before the first instruction following a LINENUMBER, but after any FRAME instructions
 * Adding code straight after LINE, but before frame usually causes problems with verifiers.
 * Also make sure onLineNumber is called AFTER any NEW operator, since the preceding LABEL is used to identify the uninitialized
 */
public class WorkingLineNumberVisitor extends AdviceAdapter implements Opcodes{

    public WorkingLineNumberVisitor(int api, MethodVisitor mv, int access, String name, String desc) {
        super(api, mv, access, name, desc);
    }

    private int lineNumber;

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        this.lineNumber = line;
    }

    @Override
    public void visitInsn(int opcode) {
        checkLineNumber();
        super.visitInsn(opcode);
    }

    private void checkLineNumber() {
        if(lineNumber != -1) {
            onLineNumber(lineNumber);
            lineNumber = -1;
        }
    }

    protected void onLineNumber(int lineNumber) {};

    @Override
    public void visitIntInsn(int opcode, int operand) {
        checkLineNumber();
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        checkLineNumber();
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if(opcode == NEW) {
            super.visitTypeInsn(opcode, type);
            checkLineNumber();
        } else {
            checkLineNumber();
            super.visitTypeInsn(opcode, type);
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        checkLineNumber();
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        checkLineNumber();
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        checkLineNumber();
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        checkLineNumber();
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object cst) {
        checkLineNumber();
        super.visitLdcInsn(cst);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        checkLineNumber();
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        checkLineNumber();
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        checkLineNumber();
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        checkLineNumber();
        super.visitMultiANewArrayInsn(desc, dims);
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        checkLineNumber();
        return super.visitInsnAnnotation(typeRef, typePath, desc, visible);
    }
}
