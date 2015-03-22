package org.wtf.skybar.transform;

import net.openhft.koloboke.collect.map.IntIntMap;
import net.openhft.koloboke.collect.map.hash.HashIntIntMaps;
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
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import static org.objectweb.asm.Type.*;

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

    private IntIntMap lineNumberLocals = HashIntIntMaps.newMutableMap();
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
            if(!lineNumberLocals.containsKey(node.line)) {
                int lineNumberLocal = localVariablesSorter.newLocal(Type.INT_TYPE);
                lineNumberLocals.put(node.line, lineNumberLocal);
                superVisitInsn(Opcodes.ICONST_0);
                mv.visitVarInsn(Opcodes.ISTORE, lineNumberLocal);
            }
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
           PerLineMethodVisitor.reportSingleLineExecuted(lineNumber, sourceFile, version, mv);
        }
    }

    @Override
    protected void onMethodExit(int opcode) {
        if(opcode != ATHROW) {
            reportLinesExecuted("visitLine_methodExit");
        }
    }

    private void reportLinesExecuted(String methodName) {
        lineNumberLocals.forEach((int line, int local) -> {
            if (useInvokeDynamic()) {

                // The invokedynamic byte code points to a bootstrap method used by the JVM to look up the call site method at the first executions.
                // Subsequent calls are direct and optimized

                // Need a descriptor for the bootstrap method (return type + parameter types)
                String descriptor = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class)
                        .toMethodDescriptorString();

                // and a handle
                Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, getInternalName(SkybarRegistry.class), "bootstrapMulti",
                        descriptor);

                // Load the local variable holding the execution count for the line
                mv.visitVarInsn(ILOAD, local);
                // LongAdder expects a long
                mv.visitInsn(I2L);
                // Pass sourceFile and lineNumber as the "extra" arguments to the bootstrap method
                mv.visitInvokeDynamicInsn(methodName, "(J)V", bootstrap, sourceFile, line);
            } else {
                // Slower, but compatible with Java <= 1.6
                // We output the byte code equivalent to:
                //    SkybarRegistry.registry.getAdderForLine(sourceFile, lineNumber).add(numExecutionsOfLineX)


                // Get the SkyBarRegistry instance onto the stack
                mv.visitFieldInsn(GETSTATIC, getInternalName(SkybarRegistry.class), "registry", getDescriptor(SkybarRegistry.class));
                // Add source file and line number
                mv.visitLdcInsn(sourceFile);
                mv.visitLdcInsn(line);
                // Get the LongAdder
                mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(SkybarRegistry.class), "getAdderForLine", getMethodDescriptor(getType(LongAdder.class), getType(String.class), Type.INT_TYPE), false);
                // Load the execution count for the line
                mv.visitVarInsn(ILOAD, local);
                // LongAdder expects a long
                mv.visitInsn(I2L);
                // Invoke the add method
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
        super.visitMaxs(maxStack + 3, maxLocals + lineNumberLocals.size());
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
        reportLinesExecuted("visitLine_catchHandler");
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
