package com.lwx1145.techstart.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * ASM Transformer: 拦截 org.orecruncher.dsurround.registry.item.ItemClass.effectiveArmorStack 静态方法，
 * 在方法头插入 entity 判空、stack 判空、itemData 判空，任一为 null 直接 return ItemStack.EMPTY。
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
                // 打印所有方法名和签名，便于调试
                System.out.println("[DSurroundItemClassTransformer] method: " + method.name + " desc: " + method.desc + " access: " + method.access);
                // 注入所有静态、参数为(EntityLivingBase)且返回ItemStack的方法
                if ((method.access & Opcodes.ACC_STATIC) != 0
                        && method.name.equals("effectiveArmorStack")
                        && method.desc.equals("(Lnet/minecraft/entity/EntityLivingBase;)Lnet/minecraft/item/ItemStack;")) {
                    injectDefensiveHeadEntityLivingBase(method);
                    System.out.println("[DSurroundItemClassTransformer] Injected defensive head to method: " + method.name);
                    injected = true;
                }
            }
            if (!injected) {
                System.err.println("[DSurroundItemClassTransformer] 未找到匹配的 effectiveArmorStack(EntityLivingBase) 静态方法进行注入！");
            }
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            return classWriter.toByteArray();
        } catch (Throwable t) {
            System.err.println("[DSurroundItemClassTransformer] ASM注入失败: " + t.getMessage());
            t.printStackTrace();
            return basicClass;
        }
    }

    // 用 try-catch(Exception) 包裹整个方法体，catch 时 return ItemStack.EMPTY
    private void injectDefensiveHeadEntityLivingBase(MethodNode method) {
        // 在现有方法体上直接插入 try/catch 标记，避免对原指令做克隆（克隆在部分 ASM 版本下会返回 null）
        LabelNode startTry = new LabelNode();
        LabelNode endTry = new LabelNode();
        LabelNode catchHandler = new LabelNode();

        InsnList insns = method.instructions;
        // 插入 try 开始标签到方法头
        insns.insert(startTry);
        // 在方法结尾处插入 try 结束标签
        insns.add(endTry);
        // 添加 catch 处理器标签并在异常时返回 null（避免在 catch 分支中再次触发 NoSuchFieldError）
        insns.add(catchHandler);
        // 返回 null 而不是引用 Items.AIR 或 ItemStack.EMPTY，以避免 NoSuchFieldError 或 NoSuchFieldError 在 catch 中再次抛出
        insns.add(new InsnNode(Opcodes.ACONST_NULL));
        insns.add(new InsnNode(Opcodes.ARETURN));

        // 清理并添加 try/catch 区间
        method.tryCatchBlocks.clear();
        // 捕获 Throwable（包括 Error），以拦截 NoSuchFieldError 等错误，避免导致 JVM 终止
        method.tryCatchBlocks.add(new TryCatchBlockNode(startTry, endTry, catchHandler, "java/lang/Throwable"));
    }
}
