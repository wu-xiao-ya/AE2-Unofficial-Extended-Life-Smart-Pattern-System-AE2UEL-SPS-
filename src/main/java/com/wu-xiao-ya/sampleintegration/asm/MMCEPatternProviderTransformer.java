package com.lwx1145.sampleintegration.asm;

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

public class MMCEPatternProviderTransformer implements IClassTransformer {

    private static final String TARGET_CLASS = "github.kasuminova.mmce.common.tile.MEPatternProvider";
    private static final String PROVIDE_METHOD = "provideCrafting";
    private static final String PROVIDE_DESC = "(Lappeng/api/networking/crafting/ICraftingProviderHelper;)V";
    private static final String REFRESH_METHOD = "refreshPattern";
    private static final String REFRESH_DESC = "(I)V";
    private static final String PUSH_METHOD = "pushPattern";
    private static final String PUSH_DESC = "(Lappeng/api/networking/crafting/ICraftingPatternDetails;Lnet/minecraft/inventory/InventoryCrafting;)Z";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !TARGET_CLASS.equals(transformedName)) {
            return basicClass;
        }

        try {
            ClassNode classNode = new ClassNode();
            new ClassReader(basicClass).accept(classNode, 0);

            boolean provideInjected = false;
            boolean refreshInjected = false;
            boolean pushInjected = false;

            for (MethodNode method : classNode.methods) {
                if (PROVIDE_METHOD.equals(method.name) && PROVIDE_DESC.equals(method.desc)) {
                    injectProvideHook(method);
                    provideInjected = true;
                } else if (REFRESH_METHOD.equals(method.name) && REFRESH_DESC.equals(method.desc)) {
                    injectRefreshHook(method);
                    refreshInjected = true;
                } else if (PUSH_METHOD.equals(method.name) && PUSH_DESC.equals(method.desc)) {
                    injectPushHook(method);
                    pushInjected = true;
                }
            }

            if (!provideInjected || !refreshInjected || !pushInjected) {
                System.err.println("[MMCEPatternProviderTransformer] Missing method(s): provide=" + provideInjected + ", refresh=" + refreshInjected + ", push=" + pushInjected);
                return basicClass;
            }

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
                @Override
                protected String getCommonSuperClass(final String type1, final String type2) {
                    return "java/lang/Object";
                }
            };
            classNode.accept(cw);
            System.out.println("[MMCEPatternProviderTransformer] Injected provideCrafting + refreshPattern + pushPattern");
            return cw.toByteArray();
        } catch (Throwable t) {
            System.err.println("[MMCEPatternProviderTransformer] Transform failed: " + t.getMessage());
            t.printStackTrace();
            return basicClass;
        }
    }

    private void injectRefreshHook(MethodNode method) {
        InsnList insn = new InsnList();
        LabelNode continueLabel = new LabelNode();

        insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insn.add(new VarInsnNode(Opcodes.ILOAD, 1));
        insn.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "com/lwx1145/sampleintegration/PatternInterceptor",
            "interceptMMCERefreshPattern",
            "(Ljava/lang/Object;I)Z",
            false
        ));
        insn.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
        insn.add(new InsnNode(Opcodes.RETURN));
        insn.add(continueLabel);

        method.instructions.insert(insn);
    }

    private void injectProvideHook(MethodNode method) {
        InsnList insn = new InsnList();
        LabelNode continueLabel = new LabelNode();

        insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insn.add(new VarInsnNode(Opcodes.ALOAD, 1));
        insn.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "com/lwx1145/sampleintegration/PatternInterceptor",
            "interceptMMCEProvideCrafting",
            "(Ljava/lang/Object;Ljava/lang/Object;)Z",
            false
        ));
        insn.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
        insn.add(new InsnNode(Opcodes.RETURN));
        insn.add(continueLabel);

        method.instructions.insert(insn);
    }

    private void injectPushHook(MethodNode method) {
        InsnList insn = new InsnList();
        LabelNode continueLabel = new LabelNode();
        LabelNode handledTrue = new LabelNode();

        insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insn.add(new VarInsnNode(Opcodes.ALOAD, 1));
        insn.add(new VarInsnNode(Opcodes.ALOAD, 2));
        insn.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "com/lwx1145/sampleintegration/PatternInterceptor",
            "interceptMMCEPushPattern",
            "(Ljava/lang/Object;Lappeng/api/networking/crafting/ICraftingPatternDetails;Lnet/minecraft/inventory/InventoryCrafting;)I",
            false
        ));
        insn.add(new InsnNode(Opcodes.DUP));
        insn.add(new InsnNode(Opcodes.ICONST_M1));
        insn.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, continueLabel));

        insn.add(new InsnNode(Opcodes.DUP));
        insn.add(new InsnNode(Opcodes.ICONST_1));
        insn.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, handledTrue));

        // handled but rejected => return false
        insn.add(new InsnNode(Opcodes.POP));
        insn.add(new InsnNode(Opcodes.ICONST_0));
        insn.add(new InsnNode(Opcodes.IRETURN));

        // handled and accepted => return true
        insn.add(handledTrue);
        insn.add(new InsnNode(Opcodes.POP));
        insn.add(new InsnNode(Opcodes.ICONST_1));
        insn.add(new InsnNode(Opcodes.IRETURN));

        // not handled => continue original pushPattern
        insn.add(continueLabel);
        insn.add(new InsnNode(Opcodes.POP));

        method.instructions.insert(insn);
    }
}

