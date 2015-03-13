package org.wtf.skybar.transform;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Set;

class SkybarClassVisitor extends ClassVisitor implements Opcodes {
    private String className;
    private String sourceFile;
    private int version;

    public SkybarClassVisitor(ClassVisitor writer) {
        super(ASM5, writer);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
        this.version = version;
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
        this.sourceFile = className.substring(0, className.lastIndexOf("/") + 1) + source;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if(sourceFile != null) {
            return new FirstPass(ASM5, access, name, desc, signature, exceptions, mv);
        } else {
            return mv;
        }
    }

    class FirstPass extends MethodNode {
        private final MethodVisitor mv;

        public FirstPass(int api, int access, String name, String desc, String signature, String[] exceptions, MethodVisitor mv) {
            super(api, access, name, desc, signature, exceptions);
            this.mv = mv;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            if (hasLoops() && !isConstructor(name)) {
                LocalVariablesSorter localVariablesSorter = new LocalVariablesSorter(access, desc, mv);
                accept(new TryCatchMethodVisitor(className, version, access, desc, sourceFile, mv, instructions, localVariablesSorter));
            } else {
                accept(new PerLineMethodVisitor(version, sourceFile, mv));
            }

        }

        private boolean isConstructor(String name) {
            return "<init>".equals(name);
        }

        private boolean hasLoops() {
            Set<Label> visitedLabels = new HashSet<>();

            for (int i = 0; i < instructions.size(); i++) {
                AbstractInsnNode ins = instructions.get(i);
                if(ins instanceof LabelNode) {
                    visitedLabels.add(LabelNode.class.cast(ins).getLabel());
                }
                if (ins instanceof JumpInsnNode) {
                    if(visitedLabels.contains(JumpInsnNode.class.cast(ins).label.getLabel())) {
                        return true;
                    }
                }
            }

            return false;
        }
    }
}

