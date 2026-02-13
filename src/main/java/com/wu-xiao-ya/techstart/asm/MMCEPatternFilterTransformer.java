package com.lwx1145.techstart.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class MMCEPatternFilterTransformer implements IClassTransformer {

    private static final String TARGET_CLASS = "github.kasuminova.mmce.common.util.PatternItemFilter";
    private static final String TARGET_METHOD = "allowInsert";
    private static final String TARGET_DESC = "(Lnet/minecraftforge/items/IItemHandler;ILnet/minecraft/item/ItemStack;)Z";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !TARGET_CLASS.equals(transformedName)) {
            return basicClass;
        }

        try {
            ClassNode classNode = new ClassNode();
            new ClassReader(basicClass).accept(classNode, 0);

            boolean injected = false;
            for (MethodNode method : classNode.methods) {
                if (TARGET_METHOD.equals(method.name) && TARGET_DESC.equals(method.desc)) {
                    injectAllowSmartPattern(method);
                    System.out.println("[MMCEPatternFilterTransformer] Injected into " + TARGET_CLASS + "#" + TARGET_METHOD);
                    injected = true;
                    break;
                }
            }

            if (!injected) {
                System.err.println("[MMCEPatternFilterTransformer] Method not found: " + TARGET_METHOD + TARGET_DESC);
                return basicClass;
            }

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
                @Override
                protected String getCommonSuperClass(final String type1, final String type2) {
                    return "java/lang/Object";
                }
            };
            classNode.accept(cw);
            return cw.toByteArray();
        } catch (Throwable t) {
            System.err.println("[MMCEPatternFilterTransformer] Transform failed: " + t.getMessage());
            t.printStackTrace();
            return basicClass;
        }
    }

    private void injectAllowSmartPattern(MethodNode method) {
        InsnList insn = new InsnList();
        LabelNode continueLabel = new LabelNode();

        insn.add(new VarInsnNode(Opcodes.ALOAD, 3));
        insn.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "com/lwx1145/techstart/PatternInterceptor",
            "allowMMCEPatternInsert",
            "(Lnet/minecraft/item/ItemStack;)Z",
            false
        ));
        insn.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
        insn.add(new InsnNode(Opcodes.ICONST_1));
        insn.add(new InsnNode(Opcodes.IRETURN));
        insn.add(continueLabel);

        method.instructions.insert(insn);
    }
}
