package com.lwx1145.techstart.asm;


import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 */
public class DualityInterfaceTransformer implements IClassTransformer {
    
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        if (!"appeng.helpers.DualityInterface".equals(transformedName)) {
            return basicClass;
        }
        
        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);
            
            // EN: Original comment text was corrupted by encoding.
            // ZH: 原注释因编码问题已损坏。
            for (MethodNode method : classNode.methods) {
                if ("addToCraftingList".equals(method.name) && "(Lnet/minecraft/item/ItemStack;)V".equals(method.desc)) {
                    injectInterceptor(method);
                }
            }
            
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            
            return classWriter.toByteArray();
            
        } catch (Exception e) {
            System.err.println("[DualityInterfaceTransformer] 鏉烆剚宕叉径杈Е: " + e.getMessage());
            e.printStackTrace();
            return basicClass;
        }
    }
    
    /**
     * EN: Original comment text was corrupted by encoding.
     * ZH: 原注释因编码问题已损坏。
     * EN: Original comment text was corrupted by encoding.
     * ZH: 原注释因编码问题已损坏。
     * 
     * if (PatternInterceptor.interceptAndExpand(this, stack)) {
     *     return;
     * }
     */
    private void injectInterceptor(MethodNode method) {
        InsnList insnList = new InsnList();
        
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
        
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
        
        // EN: Temporary fallback comment after encoding recovery.
        // ZH: 编码修复后使用的临时兜底注释。
        insnList.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "com/lwx1145/techstart/PatternInterceptor",
            "interceptAndExpand",
            "(Lappeng/helpers/DualityInterface;Lnet/minecraft/item/ItemStack;)Z",
            false
        ));
        
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        LabelNode continueLabel = new LabelNode();
        insnList.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel)); // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        insnList.add(new InsnNode(Opcodes.RETURN)); // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        insnList.add(continueLabel);
        
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        method.instructions.insert(insnList);
    }
}
