package com.lwx1145.techstart;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.helpers.DualityInterface;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * 样板拦截器 - 手动实现Mixin功能
 * 通过反射拦截DualityInterface的craftingList，将通配符样板展开为19个虚拟样板
 */
public class PatternInterceptor {
    
    private static Field craftingListField;
    
    static {
        try {
            // 获取DualityInterface的craftingList字段
            craftingListField = DualityInterface.class.getDeclaredField("craftingList");
            craftingListField.setAccessible(true);
        } catch (Exception e) {
            System.err.println("[PatternInterceptor] 无法获取craftingList字段: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 拦截并展开通配符样板
     * 这个方法应该在DualityInterface.addToCraftingList()被调用时触发
     * 
     * @param duality DualityInterface实例
     * @param stack 要添加的样板ItemStack
     * @return 是否成功拦截（如果是通配符样板则返回true）
     */
    @SuppressWarnings("unchecked")
    public static boolean interceptAndExpand(DualityInterface duality, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        if (!(stack.getItem() instanceof ItemTest)) {
            return false;
        }
        
        if (!ItemTest.hasEncodedItemStatic(stack)) {
            return false;
        }
        
        String inputOre = ItemTest.getInputOreNameStatic(stack);
        String outputOre = ItemTest.getOutputOreNameStatic(stack);
        
        // 检查是否是虚拟样板（已展开的）
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("VirtualInputOreName")) {
            return false;
        }
        
        // 检查是否是通配符样板
        if (!inputOre.contains("*") && !outputOre.contains("*")) {
            return false;
        }
        
        try {
            // 获取craftingList
            Set<ICraftingPatternDetails> craftingList = (Set<ICraftingPatternDetails>) craftingListField.get(duality);
            
            if (craftingList == null) {
                System.err.println("[PatternInterceptor] craftingList为null");
                return false;
            }
            
            // 创建主样板并展开为19个虚拟样板
            SmartPatternDetails mainPattern = new SmartPatternDetails(stack);
            List<SmartPatternDetails> virtualPatterns = mainPattern.expandToVirtualPatterns();
            
            // 将所有虚拟样板添加到craftingList
            for (SmartPatternDetails virtualPattern : virtualPatterns) {
                craftingList.add(virtualPattern);
            }
            
            // 返回true表示已拦截，不需要继续原始逻辑
            return true;
            
        } catch (Exception e) {
            System.err.println("[PatternInterceptor] 展开失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 清理通配符样板
     * 在provideCrafting时调用，移除通配符样板（只保留虚拟样板）
     * 
     * @param duality DualityInterface实例
     */
    @SuppressWarnings("unchecked")
    public static void cleanupWildcardPatterns(DualityInterface duality) {
        try {
            Set<ICraftingPatternDetails> craftingList = (Set<ICraftingPatternDetails>) craftingListField.get(duality);
            
            if (craftingList == null || craftingList.isEmpty()) {
                return;
            }
            
            // 移除通配符样板
            craftingList.removeIf(pattern -> {
                if (pattern instanceof SmartPatternDetails) {
                    SmartPatternDetails sp = (SmartPatternDetails) pattern;
                    if (sp.isWildcardPattern() && !sp.isVirtual()) {
                        return true;
                    }
                }
                return false;
            });
            
        } catch (Exception e) {
            System.err.println("[PatternInterceptor] 清理失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
