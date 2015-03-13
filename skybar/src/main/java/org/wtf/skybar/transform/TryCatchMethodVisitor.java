package org.wtf.skybar.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LineNumberNode;
import org.wtf.skybar.registry.SkybarRegistry;
import org.wtf.skybar.transform.util.WorkingLineNumberVisitor;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Inserts instrumentation to update the SkybarRegistry on each method exit. Line visits are tracked as increments of local variables.
 */
class TryCatchMethodVisitor extends WorkingLineNumberVisitor {

    private final String className;
    private final int version;
    private final int access;
    private final String desc;
    private final String sourceFile;
    private final MethodVisitor mv;
    private final InsnList instructions;
    private final LocalVariablesSorter localVariablesSorter;

    private Map<Integer, Integer> lineNumberLocals = new HashMap<>();
    private Label tryStart;
    private Label tryEnd;
    private Label catchHandler;
    private boolean entered;


    public TryCatchMethodVisitor(String className, int version, int access, String desc, String sourceFile, MethodVisitor mv, InsnList instructions, LocalVariablesSorter localVariablesSorter) {
        super(ASM5, localVariablesSorter);
        this.className = className;
        this.version = version;
        this.access = access;
        this.desc = desc;
        this.sourceFile = sourceFile;
        this.mv = mv;
        this.instructions = instructions;
        this.localVariablesSorter = localVariablesSorter;
    }

    @Override
    protected void onMethodEnter() {

        getNodesOfType(LineNumberNode.class).forEach(node -> {
            int lineNumberLocal = localVariablesSorter.newLocal(Type.INT_TYPE);
            lineNumberLocals.put(node.line, lineNumberLocal);
            superVisitInsn(Opcodes.ICONST_0);
            mv.visitVarInsn(Opcodes.ISTORE, lineNumberLocal);
        });
        tryStart = new Label();
        tryEnd = new Label();
        catchHandler = new Label();
        mv.visitLabel(tryStart);
        entered = true;
    }


    @Override
    protected void onLineNumber(int lineNumber) {
        SkybarRegistry.registry.registerLine(sourceFile, lineNumber);
        if(entered) {
            mv.visitIincInsn(lineNumberLocals.get(lineNumber), 1);
        } else {
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
    }


    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        super.visitFrame(type, nLocal, local, nStack, stack);
    }

    @Override
    protected void onMethodExit(int opcode) {
        reportLinesExecuted();
    }

    private void reportLinesExecuted() {
        getNodesOfType(LineNumberNode.class).forEach(node -> {
            if (useInvokeDynamic()) {
                MethodType mt = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class);

                Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, Type.getInternalName(SkybarRegistry.class), "bootstrapMulti",
                        mt.toMethodDescriptorString());
                mv.visitVarInsn(ILOAD, lineNumberLocals.get(node.line));
                mv.visitInsn(I2L);
                mv.visitInvokeDynamicInsn("visitLine", "(J)V", bootstrap, sourceFile, node.line);
            } else {
                mv.visitFieldInsn(GETSTATIC, "org/wtf/skybar/registry/SkybarRegistry", "registry", "Lorg/wtf/skybar/registry/SkybarRegistry;");
                mv.visitLdcInsn(sourceFile);
                mv.visitLdcInsn(node.line);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/wtf/skybar/registry/SkybarRegistry", "getAdderForLine", "(Ljava/lang/String;I)Ljava/util/concurrent/atomic/LongAdder;", false);
                mv.visitVarInsn(ILOAD, lineNumberLocals.get(node.line));
                mv.visitInsn(I2L);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/LongAdder", "add", "(J)V", false);

            }
        });
    }

    private boolean useInvokeDynamic() {
        return version != Opcodes.V1_1 && version >= Opcodes.V1_7;
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        instrumentCatchHandler();
        super.visitMaxs(maxStack + 3, maxLocals + getNodesOfType(LineNumberNode.class).size());
    }

    private void instrumentCatchHandler() {
        mv.visitTryCatchBlock(tryStart, tryEnd, catchHandler, null);
        mv.visitLabel(tryEnd);
        mv.visitLabel(catchHandler);
        List<Object> locals = new ArrayList<>();
        if((Opcodes.ACC_STATIC & access) == 0) {
            locals.add(getLocalObjectFor(Type.getObjectType(className)));
        }
        Type[] argumentTypes = Type.getArgumentTypes(desc);
        for (Type argumentType : argumentTypes) {
            Object t = getLocalObjectFor(argumentType);
            locals.add(t);
        }
        super.visitFrame(Opcodes.F_NEW, locals.size(), locals.toArray(new Object[locals.size()]), 1, new Object[]{"java/lang/Throwable"});
        reportLinesExecuted();
        mv.visitInsn(ATHROW);
    }

    private Object getLocalObjectFor(Type argumentType) {
        Object t;
        switch (argumentType.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                t = Opcodes.INTEGER;
                break;
            case Type.FLOAT:
                t = Opcodes.FLOAT;
                break;
            case Type.LONG:
                t = Opcodes.LONG;
                break;
            case Type.DOUBLE:
                t = Opcodes.DOUBLE;
                break;
            case Type.ARRAY:
                t = argumentType.getDescriptor();
                break;
            // case Type.OBJECT:
            default:
                t = argumentType.getInternalName();
                break;
        }
        return t;
    }

    private <T extends AbstractInsnNode> List<T> getNodesOfType(Class<T> type) {
        List<T> nodes = new ArrayList<>();

        for(int i = 0; i < instructions.size(); i++) {
            if (instructions.get(i) instanceof LineNumberNode) {
                nodes.add(type.cast(instructions.get(i)));
            }
        }
        return nodes;
    }
}
