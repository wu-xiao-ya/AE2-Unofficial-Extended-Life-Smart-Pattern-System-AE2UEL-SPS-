package com.lwx1145.sampleintegration.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;

/**
 * MMCE compatibility shim for runtimes where ItemStack.EMPTY is remapped.
 */
public class MMCEItemStackEmptyCompatTransformer implements IClassTransformer {

    private static final String ITEMSTACK_OWNER = "net/minecraft/item/ItemStack";
    private static final String ITEMSTACK_DESC = "Lnet/minecraft/item/ItemStack;";
    private static final String HOOK_OWNER = "com/lwx1145/sampleintegration/compat/ItemStackCompat";
    private static final String HOOK_NAME = "empty";
    private static final String HOOK_DESC = "()Lnet/minecraft/item/ItemStack;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || transformedName == null) {
            return basicClass;
        }
        if (!transformedName.startsWith("hellfirepvp.modularmachinery.")
            && !transformedName.startsWith("github.kasuminova.mmce.")) {
            return basicClass;
        }

        try {
            ClassNode classNode = new ClassNode();
            new ClassReader(basicClass).accept(classNode, 0);

            int replaced = 0;
            for (MethodNode method : classNode.methods) {
                ListIterator<AbstractInsnNode> it = method.instructions.iterator();
                while (it.hasNext()) {
                    AbstractInsnNode insn = it.next();
                    if (!(insn instanceof FieldInsnNode)) {
                        continue;
                    }
                    FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                    if (fieldInsn.getOpcode() == Opcodes.GETSTATIC
                        && ITEMSTACK_OWNER.equals(fieldInsn.owner)
                        && "EMPTY".equals(fieldInsn.name)
                        && ITEMSTACK_DESC.equals(fieldInsn.desc)) {
                        MethodInsnNode replacement = new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            HOOK_OWNER,
                            HOOK_NAME,
                            HOOK_DESC,
                            false
                        );
                        method.instructions.set(fieldInsn, replacement);
                        replaced++;
                    }
                }
            }

            if (replaced == 0) {
                return basicClass;
            }

            // Keep original stack map frames to avoid subtype merge degradation on Java 8 verifier.
            // This transformer only replaces GETSTATIC with INVOKESTATIC of identical stack effect.
            ClassWriter classWriter = new ClassWriter(0);
            classNode.accept(classWriter);
            System.out.println("[MMCEItemStackEmptyCompatTransformer] Patched " + transformedName
                + " (" + replaced + " ItemStack.EMPTY access)");
            return classWriter.toByteArray();
        } catch (Throwable t) {
            System.err.println("[MMCEItemStackEmptyCompatTransformer] Transform failed for " + transformedName + ": " + t.getMessage());
            t.printStackTrace();
            return basicClass;
        }
    }
}
