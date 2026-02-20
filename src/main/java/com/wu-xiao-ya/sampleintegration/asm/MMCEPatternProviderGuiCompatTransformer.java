package com.lwx1145.sampleintegration.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;

/**
 * Replaces PatternProviderIngredientList lambda factories with stable hook factories.
 */
public class MMCEPatternProviderGuiCompatTransformer implements IClassTransformer {

    private static final String TARGET_CLASS = "github.kasuminova.mmce.client.gui.widget.impl.patternprovider.PatternProviderIngredientList";
    private static final String HOOK_OWNER = "com/lwx1145/sampleintegration/compat/MMCEGuiCompatHooks";
    private static final String HOOK_DESC = "()Ljava/util/function/Function;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !TARGET_CLASS.equals(transformedName)) {
            return basicClass;
        }

        try {
            ClassNode classNode = new ClassNode();
            new ClassReader(basicClass).accept(classNode, 0);

            int replaced = 0;
            for (MethodNode method : classNode.methods) {
                if ("setStackList".equals(method.name)) {
                    replaced += replaceInSetStackList(method);
                } else if ("addGasSlots".equals(method.name)) {
                    replaced += replaceInAddGasSlots(method);
                }
            }

            if (replaced == 0) {
                return basicClass;
            }

            // Preserve original frames; lambda indy is replaced by a static factory with same stack behavior.
            ClassWriter cw = new ClassWriter(0);
            classNode.accept(cw);
            System.out.println("[MMCEPatternProviderGuiCompatTransformer] Patched " + TARGET_CLASS
                + " (" + replaced + " lambda supplier)");
            return cw.toByteArray();
        } catch (Throwable t) {
            System.err.println("[MMCEPatternProviderGuiCompatTransformer] Transform failed: " + t.getMessage());
            t.printStackTrace();
            return basicClass;
        }
    }

    private int replaceInSetStackList(MethodNode method) {
        int lambdaIndex = 0;
        int replaced = 0;
        ListIterator<AbstractInsnNode> it = method.instructions.iterator();
        while (it.hasNext()) {
            AbstractInsnNode insn = it.next();
            if (!(insn instanceof InvokeDynamicInsnNode)) {
                continue;
            }
            InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
            if (!"apply".equals(indy.name) || !HOOK_DESC.equals(indy.desc)) {
                continue;
            }
            String hookName;
            if (lambdaIndex == 0) {
                hookName = "itemSlotFunction";
            } else if (lambdaIndex == 1) {
                hookName = "fluidSlotFunction";
            } else {
                lambdaIndex++;
                continue;
            }
            MethodInsnNode replacement = new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HOOK_OWNER,
                hookName,
                HOOK_DESC,
                false
            );
            method.instructions.set(indy, replacement);
            replaced++;
            lambdaIndex++;
        }
        return replaced;
    }

    private int replaceInAddGasSlots(MethodNode method) {
        ListIterator<AbstractInsnNode> it = method.instructions.iterator();
        while (it.hasNext()) {
            AbstractInsnNode insn = it.next();
            if (!(insn instanceof InvokeDynamicInsnNode)) {
                continue;
            }
            InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
            if (!"apply".equals(indy.name) || !HOOK_DESC.equals(indy.desc)) {
                continue;
            }
            MethodInsnNode replacement = new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HOOK_OWNER,
                "gasSlotFunction",
                HOOK_DESC,
                false
            );
            method.instructions.set(indy, replacement);
            return 1;
        }
        return 0;
    }
}
