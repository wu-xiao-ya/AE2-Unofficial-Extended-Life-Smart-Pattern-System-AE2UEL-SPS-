package com.lwx1145.sampleintegration.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
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

                System.out.println("[DSurroundItemClassTransformer] method: " + method.name + " desc: " + method.desc + " access: " + method.access);

                if ((method.access & Opcodes.ACC_STATIC) != 0
                        && method.name.equals("effectiveArmorStack")
                        && method.desc.equals("(Lnet/minecraft/entity/EntityLivingBase;)Lnet/minecraft/item/ItemStack;")) {
                    injectDefensiveHeadEntityLivingBase(method);
                    System.out.println("[DSurroundItemClassTransformer] Injected defensive head to method: " + method.name);
                    injected = true;
                }
            }
            if (!injected) {
                System.err.println("[DSurroundItemClassTransformer] 閺堫亝澹橀崚鏉垮爱闁板秶娈?effectiveArmorStack(EntityLivingBase) 闂堟瑦鈧焦鏌熷▔鏇＄箻鐞涘本鏁為崗銉磼");
            }
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            return classWriter.toByteArray();
        } catch (Throwable t) {
            System.err.println("[DSurroundItemClassTransformer] ASM濞夈劌鍙嗘径杈Е: " + t.getMessage());
            t.printStackTrace();
            return basicClass;
        }
    }


    private void injectDefensiveHeadEntityLivingBase(MethodNode method) {

        LabelNode startTry = new LabelNode();
        LabelNode endTry = new LabelNode();
        LabelNode catchHandler = new LabelNode();

        InsnList insns = method.instructions;

        insns.insert(startTry);

        insns.add(endTry);

        insns.add(catchHandler);

        insns.add(new InsnNode(Opcodes.ACONST_NULL));
        insns.add(new InsnNode(Opcodes.ARETURN));


        method.tryCatchBlocks.clear();

        method.tryCatchBlocks.add(new TryCatchBlockNode(startTry, endTry, catchHandler, "java/lang/Throwable"));
    }
}

