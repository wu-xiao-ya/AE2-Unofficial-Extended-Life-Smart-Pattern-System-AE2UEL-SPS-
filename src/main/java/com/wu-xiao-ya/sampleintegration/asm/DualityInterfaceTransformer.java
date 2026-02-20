package com.lwx1145.sampleintegration.asm;


import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 */
public class DualityInterfaceTransformer implements IClassTransformer {
    
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {

        if (!"appeng.helpers.DualityInterface".equals(transformedName)) {
            return basicClass;
        }
        
        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);
            

            for (MethodNode method : classNode.methods) {
                if ("addToCraftingList".equals(method.name) && "(Lnet/minecraft/item/ItemStack;)V".equals(method.desc)) {
                    injectInterceptor(method);
                }
            }
            
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            
            return classWriter.toByteArray();
            
        } catch (Exception e) {
            System.err.println("[DualityInterfaceTransformer] 閺夌儐鍓氬畷鍙夊緞鏉堫偉袝: " + e.getMessage());
            e.printStackTrace();
            return basicClass;
        }
    }
    
    /**
     * 
     * if (PatternInterceptor.interceptAndExpand(this, stack)) {
     *     return;
     * }
     */
    private void injectInterceptor(MethodNode method) {
        InsnList insnList = new InsnList();
        

        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
        

        insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
        

        insnList.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "com/lwx1145/sampleintegration/PatternInterceptor",
            "interceptAndExpand",
            "(Lappeng/helpers/DualityInterface;Lnet/minecraft/item/ItemStack;)Z",
            false
        ));
        

        LabelNode continueLabel = new LabelNode();
        insnList.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
        insnList.add(new InsnNode(Opcodes.RETURN));
        insnList.add(continueLabel);
        

        method.instructions.insert(insnList);
    }
}

