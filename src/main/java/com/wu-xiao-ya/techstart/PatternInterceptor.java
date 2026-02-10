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

        // Block ItemTest patterns from being added to the interface crafting list.
        return true;
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
