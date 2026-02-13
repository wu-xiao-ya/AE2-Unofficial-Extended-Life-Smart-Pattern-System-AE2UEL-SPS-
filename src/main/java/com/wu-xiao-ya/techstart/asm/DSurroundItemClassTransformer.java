package com.lwx1145.techstart.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * EN: Temporary fallback comment after encoding recovery.
 * ZH: 编码修复后使用的临时兜底注释。
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 */
public class DSurroundItemClassTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!"org.orecruncher.dsurround.registry.item.ItemClass".equals(transformedName)) {
            return basicClass;
        }
        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);
            boolean injected = false;
            for (MethodNode method : classNode.methods) {
                // EN: Original comment text was corrupted by encoding.
                // ZH: 原注释因编码问题已损坏。
                System.out.println("[DSurroundItemClassTransformer] method: " + method.name + " desc: " + method.desc + " access: " + method.access);
                // EN: Original comment text was corrupted by encoding.
                // ZH: 原注释因编码问题已损坏。
                if ((method.access & Opcodes.ACC_STATIC) != 0
                        && method.name.equals("effectiveArmorStack")
                        && method.desc.equals("(Lnet/minecraft/entity/EntityLivingBase;)Lnet/minecraft/item/ItemStack;")) {
                    injectDefensiveHeadEntityLivingBase(method);
                    System.out.println("[DSurroundItemClassTransformer] Injected defensive head to method: " + method.name);
                    injected = true;
                }
            }
            if (!injected) {
                System.err.println("[DSurroundItemClassTransformer] 鏈壘鍒板尮閰嶇殑 effectiveArmorStack(EntityLivingBase) 闈欐€佹柟娉曡繘琛屾敞鍏ワ紒");
            }
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            return classWriter.toByteArray();
        } catch (Throwable t) {
            System.err.println("[DSurroundItemClassTransformer] ASM娉ㄥ叆澶辫触: " + t.getMessage());
            t.printStackTrace();
            return basicClass;
        }
    }

    // EN: Original comment text was corrupted by encoding.
    // ZH: 原注释因编码问题已损坏。
    private void injectDefensiveHeadEntityLivingBase(MethodNode method) {
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        LabelNode startTry = new LabelNode();
        LabelNode endTry = new LabelNode();
        LabelNode catchHandler = new LabelNode();

        InsnList insns = method.instructions;
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        insns.insert(startTry);
        // EN: Temporary fallback comment after encoding recovery.
        // ZH: 编码修复后使用的临时兜底注释。
        insns.add(endTry);
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        insns.add(catchHandler);
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        insns.add(new InsnNode(Opcodes.ACONST_NULL));
        insns.add(new InsnNode(Opcodes.ARETURN));

        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        method.tryCatchBlocks.clear();
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        method.tryCatchBlocks.add(new TryCatchBlockNode(startTry, endTry, catchHandler, "java/lang/Throwable"));
    }
}
