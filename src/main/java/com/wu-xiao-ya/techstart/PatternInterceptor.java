package com.lwx1145.techstart;


import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.helpers.DualityInterface;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * 鏍锋澘鎷︽埅鍣?- 鎵嬪姩瀹炵幇Mixin鍔熻兘
 * 閫氳繃鍙嶅皠鎷︽埅DualityInterface鐨刢raftingList锛屽皢閫氶厤绗︽牱鏉垮睍寮€涓?9涓櫄鎷熸牱鏉?
 */
public class PatternInterceptor {
    
    private static Field craftingListField;
    
    static {
        try {
            // 鑾峰彇DualityInterface鐨刢raftingList瀛楁
            craftingListField = DualityInterface.class.getDeclaredField("craftingList");
            craftingListField.setAccessible(true);
        } catch (Exception e) {
            System.err.println("[PatternInterceptor] 鏃犳硶鑾峰彇craftingList瀛楁: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 鎷︽埅骞跺睍寮€閫氶厤绗︽牱鏉?
     * 杩欎釜鏂规硶搴旇鍦―ualityInterface.addToCraftingList()琚皟鐢ㄦ椂瑙﹀彂
     * 
     * @param duality DualityInterface瀹炰緥
     * @param stack 瑕佹坊鍔犵殑鏍锋澘ItemStack
     * @return 鏄惁鎴愬姛鎷︽埅锛堝鏋滄槸閫氶厤绗︽牱鏉垮垯杩斿洖true锛?
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
     * 娓呯悊閫氶厤绗︽牱鏉?
     * 鍦╬rovideCrafting鏃惰皟鐢紝绉婚櫎閫氶厤绗︽牱鏉匡紙鍙繚鐣欒櫄鎷熸牱鏉匡級
     * 
     * @param duality DualityInterface瀹炰緥
     */
    @SuppressWarnings("unchecked")
    public static void cleanupWildcardPatterns(DualityInterface duality) {
        try {
            Set<ICraftingPatternDetails> craftingList = (Set<ICraftingPatternDetails>) craftingListField.get(duality);
            
            if (craftingList == null || craftingList.isEmpty()) {
                return;
            }
            
            // 绉婚櫎閫氶厤绗︽牱鏉?
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
            System.err.println("[PatternInterceptor] 娓呯悊澶辫触: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
