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
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 * 
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 */
public class WildcardPatternWrapper implements ICraftingPatternDetails {
    
    private final List<SmartPatternDetails> virtualPatterns;
    private final SmartPatternDetails primaryPattern; // EN: Original comment text was corrupted by encoding.
    // ZH: 原注释因编码问题已损坏。
    private final ItemStack patternStack;

    public WildcardPatternWrapper(ItemStack patternStack) {
        this.patternStack = patternStack;
        
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        SmartPatternDetails tempPattern = new SmartPatternDetails(patternStack);
        
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        this.virtualPatterns = tempPattern.expandToVirtualPatterns();
        
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        this.primaryPattern = (this.virtualPatterns.isEmpty()) ? tempPattern : this.virtualPatterns.get(0);
    }

    @Override
    public ItemStack getPattern() {
        return primaryPattern.getPattern();
    }

    @Override
    public boolean isValidItemForSlot(int slot, ItemStack itemStack, World world) {
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
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
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
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
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        return primaryPattern.getOutputs();
    }

    @Override
    public boolean canSubstitute() {
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        return true;
    }

    @Override
    public List<IAEItemStack> getSubstituteInputs(int slot) {
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
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
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        if (inventory.getSizeInventory() > 0) {
            ItemStack input = inventory.getStackInSlot(0);
            if (!input.isEmpty()) {
                // EN: Original comment text was corrupted by encoding.
                // ZH: 原注释因编码问题已损坏。
                for (int i = 0; i < virtualPatterns.size(); i++) {
                    SmartPatternDetails vPattern = virtualPatterns.get(i);
                    if (vPattern.isValidItemForSlot(0, input, world)) {
                        // EN: Original comment text was corrupted by encoding.
                        // ZH: 原注释因编码问题已损坏。
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
