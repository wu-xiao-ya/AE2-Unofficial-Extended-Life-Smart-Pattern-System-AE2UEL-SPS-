package com.lwx1145.techstart;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

/**
 * 通配符样板包装器 - 当AE2需要获取样板信息时，自动展开为虚拟样板
 * 这样可以让单一样板对象在AE2内部自动变成19个虚拟样板
 * 
 * 原理：
 * 1. ItemTest返回这个包装器而不是SmartPatternDetails
 * 2. 包装器持有一个虚拟样板列表
 * 3. 当AE2调用方法时，包装器返回第一个虚拟样板的信息
 * 4. 当AE2尝试执行合成时，各个虚拟样板各司其职
 */
public class WildcardPatternWrapper implements ICraftingPatternDetails {
    
    private final List<SmartPatternDetails> virtualPatterns;
    private final SmartPatternDetails primaryPattern; // 用于显示和基础操作的主样板
    private final ItemStack patternStack;

    public WildcardPatternWrapper(ItemStack patternStack) {
        this.patternStack = patternStack;
        
        // 创建临时样板以获取配方列表
        SmartPatternDetails tempPattern = new SmartPatternDetails(patternStack);
        
        // 展开为虚拟样板
        this.virtualPatterns = tempPattern.expandToVirtualPatterns();
        
        // 使用第一个虚拟样板作为主样板用于显示
        this.primaryPattern = (this.virtualPatterns.isEmpty()) ? tempPattern : this.virtualPatterns.get(0);
    }

    @Override
    public ItemStack getPattern() {
        return primaryPattern.getPattern();
    }

    @Override
    public boolean isValidItemForSlot(int slot, ItemStack itemStack, World world) {
        // 检查是否任何虚拟样板都能接受这个物品
        for (SmartPatternDetails vPattern : virtualPatterns) {
            if (vPattern.isValidItemForSlot(slot, itemStack, world)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCraftable() {
        return primaryPattern.isCraftable();
    }

    @Override
    public IAEItemStack[] getInputs() {
        // 只返回主样板的输入
        return primaryPattern.getInputs();
    }

    @Override
    public IAEItemStack[] getCondensedInputs() {
        return primaryPattern.getCondensedInputs();
    }

    @Override
    public IAEItemStack[] getCondensedOutputs() {
        return primaryPattern.getCondensedOutputs();
    }

    @Override
    public IAEItemStack getPrimaryOutput() {
        return primaryPattern.getPrimaryOutput();
    }

    @Override
    public IAEItemStack[] getOutputs() {
        // 只返回主样板的输出（否则AE2会混淆）
        return primaryPattern.getOutputs();
    }

    @Override
    public boolean canSubstitute() {
        // 允许替代 - 这样用户可以选择其他材料
        return true;
    }

    @Override
    public List<IAEItemStack> getSubstituteInputs(int slot) {
        // 返回所有虚拟样板的输入作为替代品
        List<IAEItemStack> substitutes = new ArrayList<>();
        for (int i = 0; i < virtualPatterns.size(); i++) {
            SmartPatternDetails vPattern = virtualPatterns.get(i);
            IAEItemStack[] inputs = vPattern.getInputs();
            for (IAEItemStack input : inputs) {
                if (input != null) {
                    substitutes.add(input);
                }
            }
        }
        
        return substitutes;
    }

    @Override
    public ItemStack getOutput(InventoryCrafting inventory, World world) {
        // 根据实际输入，找到匹配的虚拟样板，返回其输出
        if (inventory.getSizeInventory() > 0) {
            ItemStack input = inventory.getStackInSlot(0);
            if (!input.isEmpty()) {
                // 检查每个虚拟样板是否能接受这个输入
                for (int i = 0; i < virtualPatterns.size(); i++) {
                    SmartPatternDetails vPattern = virtualPatterns.get(i);
                    if (vPattern.isValidItemForSlot(0, input, world)) {
                        // 找到匹配的虚拟样板，返回其输出
                        ItemStack result = vPattern.getOutput(inventory, world);
                        if (!result.isEmpty()) {
                            return result;
                        }
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getPriority() {
        return primaryPattern.getPriority();
    }

    @Override
    public void setPriority(int priority) {
        primaryPattern.setPriority(priority);
    }
}
