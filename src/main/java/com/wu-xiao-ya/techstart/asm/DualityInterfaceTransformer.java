package com.lwx1145.techstart.asm;


import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * ASM绫昏浆鎹㈠櫒 - 鎵嬪姩瀹炵幇Mixin鍔熻兘
 * 鎷︽埅appeng.helpers.DualityInterface.addToCraftingList()鏂规硶
 * 鍦ㄦ柟娉曞紑濮嬫椂鎻掑叆鎴戜滑鐨勬嫤鎴€昏緫
 */
public class DualityInterfaceTransformer implements IClassTransformer {
    
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        // 鍙鐞咲ualityInterface绫?
        if (!"appeng.helpers.DualityInterface".equals(transformedName)) {
            return basicClass;
        }
        
        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);
            
            // 鏌ユ壘addToCraftingList鏂规硶
            for (MethodNode method : classNode.methods) {
                if ("addToCraftingList".equals(method.name) && "(Lnet/minecraft/item/ItemStack;)V".equals(method.desc)) {
                    injectInterceptor(method);
                }
            }
            
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            
            return classWriter.toByteArray();
            
        } catch (Exception e) {
            System.err.println("[DualityInterfaceTransformer] 杞崲澶辫触: " + e.getMessage());
            e.printStackTrace();
            return basicClass;
        }
    }
    
    /**
     * 鍦╝ddToCraftingList鏂规硶寮€濮嬫椂娉ㄥ叆鎷︽埅鍣ㄨ皟鐢?
     * 娉ㄥ叆鐨勪唬鐮佺瓑鏁堜簬锛?
     * 
     * if (PatternInterceptor.interceptAndExpand(this, stack)) {
     *     return;
     * }
     */
    private void injectInterceptor(MethodNode method) {
        InsnList insnList = new InsnList();
        
        // 鍔犺浇this (DualityInterface瀹炰緥)
        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
        
        // 鍔犺浇stack鍙傛暟 (ItemStack)
        insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
        
        // 璋冪敤PatternInterceptor.interceptAndExpand(this, stack)
        insnList.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "com/lwx1145/techstart/PatternInterceptor",
            "interceptAndExpand",
            "(Lappeng/helpers/DualityInterface;Lnet/minecraft/item/ItemStack;)Z",
            false
        ));
        
        // 妫€鏌ヨ繑鍥炲€硷紝濡傛灉涓簍rue鍒欑洿鎺ヨ繑鍥?
        LabelNode continueLabel = new LabelNode();
        insnList.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel)); // 濡傛灉杩斿洖false锛岃烦杞埌continueLabel
        insnList.add(new InsnNode(Opcodes.RETURN)); // 杩斿洖
        insnList.add(continueLabel);
        
        // 鍦ㄦ柟娉曞紑濮嬪鎻掑叆
        method.instructions.insert(insnList);
    }
}
