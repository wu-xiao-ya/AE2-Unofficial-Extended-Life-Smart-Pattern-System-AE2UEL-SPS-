package com.lwx1145.techstart.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * ASM类转换器 - 手动实现Mixin功能
 * 拦截appeng.helpers.DualityInterface.addToCraftingList()方法
 * 在方法开始时插入我们的拦截逻辑
 */
public class DualityInterfaceTransformer implements IClassTransformer {
    
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        // 只处理DualityInterface类
        if (!"appeng.helpers.DualityInterface".equals(transformedName)) {
            return basicClass;
        }
        
        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);
            
            // 查找addToCraftingList方法
            for (MethodNode method : classNode.methods) {
                if ("addToCraftingList".equals(method.name) && "(Lnet/minecraft/item/ItemStack;)V".equals(method.desc)) {
                    injectInterceptor(method);
                }
            }
            
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            
            return classWriter.toByteArray();
            
        } catch (Exception e) {
            System.err.println("[DualityInterfaceTransformer] 转换失败: " + e.getMessage());
            e.printStackTrace();
            return basicClass;
        }
    }
    
    /**
     * 在addToCraftingList方法开始时注入拦截器调用
     * 注入的代码等效于：
     * 
     * if (PatternInterceptor.interceptAndExpand(this, stack)) {
     *     return;
     * }
     */
    private void injectInterceptor(MethodNode method) {
        InsnList insnList = new InsnList();
        
        // 加载this (DualityInterface实例)
        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
        
        // 加载stack参数 (ItemStack)
        insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
        
        // 调用PatternInterceptor.interceptAndExpand(this, stack)
        insnList.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "com/lwx1145/techstart/PatternInterceptor",
            "interceptAndExpand",
            "(Lappeng/helpers/DualityInterface;Lnet/minecraft/item/ItemStack;)Z",
            false
        ));
        
        // 检查返回值，如果为true则直接返回
        LabelNode continueLabel = new LabelNode();
        insnList.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel)); // 如果返回false，跳转到continueLabel
        insnList.add(new InsnNode(Opcodes.RETURN)); // 返回
        insnList.add(continueLabel);
        
        // 在方法开始处插入
        method.instructions.insert(insnList);
    }
}
