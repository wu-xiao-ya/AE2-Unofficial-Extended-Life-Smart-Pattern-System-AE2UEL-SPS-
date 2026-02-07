package com.lwx1145.techstart;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;
import java.util.ArrayList;
import java.util.List;

/**
 * 虚拟样板展开器 - 将通配符样板自动扩展为多个虚拟样板ItemStack
 * 实现了自定义的"Mixin功能"来处理通配符样板的展开
 * 不依赖外部Mixin框架，而是直接在代码中处理
 */
public class VirtualPatternExpander {
    
    /**
     * 预定义常见金属种类（示例列表，实际扩展基于矿物辞典）
     */
    public static final String[] METAL_TYPES = {
        "Iron", "Gold", "Copper", "Tin", "Silver", "Lead", "Nickel", "Platinum", "Aluminum",
        "Zinc", "Cobalt", "Tungsten", "Bronze", "Brass", "Electrum", "Invar", "Constantan",
        "Steel", "Mithril"
    };
    
    /**
     * 将通配符样板展开为多个虚拟样板ItemStack
     * 每个ItemStack对应一个特定的金属配方
     * 实际数量基于矿物辞典中注册的金属种类
     * 
     * @param wildcardPattern 通配符样板 (如 ingot* -> plate*)
     * @return 虚拟样板ItemStack的列表（根据矿辞动态生成）
     */
    public static List<ItemStack> expandWildcardPatternToVirtual(ItemStack wildcardPattern) {
        List<ItemStack> virtualPatterns = new ArrayList<>();
        
        if (!wildcardPattern.hasTagCompound()) {
            return virtualPatterns;
        }
        
        NBTTagCompound nbt = wildcardPattern.getTagCompound();
        String inputOre = nbt.getString("inputOre");
        String outputOre = nbt.getString("outputOre");
        
        // 如果不是通配符，不需要展开
        if (!inputOre.contains("*") || !outputOre.contains("*")) {
            return virtualPatterns;
        }
        
        // 获取所有有效的配方对
        List<String[]> allRecipes = getAllValidRecipePairs(inputOre, outputOre);
        
        // 为每个配方对创建一个虚拟样板
        for (String[] recipe : allRecipes) {
            String specificInput = recipe[0];
            String specificOutput = recipe[1];
            String displayName = recipe[2];
            
            ItemStack virtualPattern = wildcardPattern.copy();
            virtualPattern.setCount(1);
            
            // 确保有NBT标签
            if (!virtualPattern.hasTagCompound()) {
                virtualPattern.setTagCompound(new NBTTagCompound());
            }
            
            NBTTagCompound virtualNBT = virtualPattern.getTagCompound();
            
            // 标记为虚拟样板
            virtualNBT.setBoolean("VirtualInputOreName", true);
            
            // 存储具体的矿物辞典条目（不是通配符）
            virtualNBT.setString("inputOre", specificInput);
            virtualNBT.setString("outputOre", specificOutput);
            virtualNBT.setString("encodedItemName", displayName);
            
            virtualPatterns.add(virtualPattern);
        }
        
        return virtualPatterns;
    }
    
    /**
     * 获取所有有效的配方对
     * 根据通配符模式（如 ingot* -> plate*）返回所有有效的配方对
     * 
     * @param inputPattern 输入模式（如 ingot*）
     * @param outputPattern 输出模式（如 plate*）
     * @return 包含所有有效配方对的列表，每个元素为 [具体输入, 具体输出, 显示名称]
     */
    private static List<String[]> getAllValidRecipePairs(String inputPattern, String outputPattern) {
        List<String[]> recipes = new ArrayList<>();
        
        for (String metal : METAL_TYPES) {
            String inputOre = inputPattern.replace("*", metal);
            String outputOre = outputPattern.replace("*", metal);
            
            // 检查这两个矿物辞典条目是否都存在
            if (OreDictionary.doesOreNameExist(inputOre) && OreDictionary.doesOreNameExist(outputOre)) {
                List<ItemStack> inputs = OreDictionary.getOres(inputOre);
                List<ItemStack> outputs = OreDictionary.getOres(outputOre);
                
                if (!inputs.isEmpty() && !outputs.isEmpty()) {
                    // 创建显示名称
                    String displayName = metal + " " + outputPattern.replace("*", "");
                    recipes.add(new String[]{inputOre, outputOre, displayName});
                }
            }
        }
        
        return recipes;
    }
    
    /**
     * 将通配符样板转换为虚拟样板（单个）
     * 返回的是一个已标记为虚拟的样板，可以直接用于ICraftingPatternDetails
     * 
     * @param wildcardPattern 通配符样板
     * @param metalType 金属类型（如 "Iron"）
     * @return 对应的虚拟样板ItemStack
     */
    public static ItemStack createVirtualPatternForMetal(ItemStack wildcardPattern, String metalType) {
        if (!wildcardPattern.hasTagCompound()) {
            return ItemStack.EMPTY;
        }
        
        NBTTagCompound nbt = wildcardPattern.getTagCompound();
        String inputOre = nbt.getString("inputOre");
        String outputOre = nbt.getString("outputOre");
        
        String specificInput = inputOre.replace("*", metalType);
        String specificOutput = outputOre.replace("*", metalType);
        
        // 检查矿物辞典条目是否存在
        if (!OreDictionary.doesOreNameExist(specificInput) || !OreDictionary.doesOreNameExist(specificOutput)) {
            return ItemStack.EMPTY;
        }
        
        ItemStack virtualPattern = wildcardPattern.copy();
        virtualPattern.setCount(1);
        
        if (!virtualPattern.hasTagCompound()) {
            virtualPattern.setTagCompound(new NBTTagCompound());
        }
        
        NBTTagCompound virtualNBT = virtualPattern.getTagCompound();
        virtualNBT.setBoolean("VirtualInputOreName", true);
        virtualNBT.setString("inputOre", specificInput);
        virtualNBT.setString("outputOre", specificOutput);
        virtualNBT.setString("encodedItemName", metalType + " " + outputOre.replace("*", ""));
        
        return virtualPattern;
    }
}
