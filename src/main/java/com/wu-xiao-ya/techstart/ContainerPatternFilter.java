package com.lwx1145.techstart;


import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ContainerPatternFilter extends Container {

    private final EntityPlayer player;
    private final ItemStack patternStack;

    public ContainerPatternFilter(EntityPlayer player) {
        this.player = player;
        ItemStack mainHandStack = player.getHeldItemMainhand();
        if (!mainHandStack.isEmpty() && mainHandStack.getItem() instanceof ItemTest) {
            this.patternStack = mainHandStack;
        } else {
            ItemStack offHandStack = player.getHeldItemOffhand();
            if (!offHandStack.isEmpty() && offHandStack.getItem() instanceof ItemTest) {
                this.patternStack = offHandStack;
            } else {
                this.patternStack = findPatternInInventory(player);
            }
        }

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(player.inventory, j + i * 9 + 9, 8 + j * 18, 125 + i * 18));
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlotToContainer(new Slot(player.inventory, k, 8 + k * 18, 185));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    public int getFilterMode() {
        return ItemTest.getFilterModeStatic(patternStack);
    }

    public List<String> getFilterEntries() {
        return ItemTest.getFilterEntriesStatic(patternStack);
    }

    public void applyFilterMode(int mode) {
        ItemTest.setFilterModeStatic(patternStack, mode);
        markDirty();
    }

    public void toggleFilterEntry(String entry) {
        ItemTest.toggleFilterEntryStatic(patternStack, entry);
        markDirty();
    }

    public void clearFilterEntries() {
        ItemTest.clearFilterEntriesStatic(patternStack);
        markDirty();
    }

    private void markDirty() {
        this.player.inventory.markDirty();
    }

    public List<String[]> getAvailableRecipeTypes() {
        List<String[]> recipeTypes = new ArrayList<>();
        String inputOreName = ItemTest.getInputOreNameStatic(patternStack);
        String outputOreName = ItemTest.getOutputOreNameStatic(patternStack);
        if (inputOreName == null || inputOreName.isEmpty() || outputOreName == null || outputOreName.isEmpty()) {
            return recipeTypes;
        }

        List<String[]> derived = deriveRecipes(inputOreName, outputOreName);
        for (String[] recipe : derived) {
            if (recipe.length >= 2) {
                String displayName = recipe[0] + " -> " + recipe[1];
                if (recipe.length > 2 && recipe[2] != null && !recipe[2].isEmpty()) {
                    displayName = recipe[2];
                }
                recipeTypes.add(new String[]{recipe[0], recipe[1], displayName});
            }
        }
        return recipeTypes;
    }

    private List<String[]> deriveRecipes(String inputOreName, String outputOreName) {
        List<String[]> derived = new ArrayList<>();
        String inputBaseType = normalizeBaseType(inputOreName);
        String outputBaseType = normalizeBaseType(outputOreName);
        if (inputBaseType == null || outputBaseType == null) {
            return derived;
        }

        Set<String> inputMaterials = new LinkedHashSet<>();
        Set<String> outputMaterials = new LinkedHashSet<>();
        for (String oreName : OreDictionary.getOreNames()) {
            if (oreName.startsWith(inputBaseType)) {
                inputMaterials.add(oreName.substring(inputBaseType.length()));
            }
            if (oreName.startsWith(outputBaseType)) {
                outputMaterials.add(oreName.substring(outputBaseType.length()));
            }
        }
        inputMaterials.retainAll(outputMaterials);

        for (String material : inputMaterials) {
            if (material == null || material.isEmpty()) {
                continue;
            }
            String derivedInputOre = inputBaseType + material;
            String derivedOutputOre = outputBaseType + material;
            if (!OreDictionary.doesOreNameExist(derivedInputOre) || !OreDictionary.doesOreNameExist(derivedOutputOre)) {
                continue;
            }
            List<ItemStack> inputItems = OreDictionary.getOres(derivedInputOre);
            List<ItemStack> outputItems = OreDictionary.getOres(derivedOutputOre);
            if (!inputItems.isEmpty() && !outputItems.isEmpty()) {
                String displayName = inputItems.get(0).getDisplayName() + " -> " + outputItems.get(0).getDisplayName();
                derived.add(new String[]{derivedInputOre, derivedOutputOre, displayName});
            }
        }
        return derived;
    }

    private String normalizeBaseType(String oreName) {
        if (oreName == null || oreName.isEmpty()) {
            return null;
        }
        String name = oreName.endsWith("*") ? oreName.substring(0, oreName.length() - 1) : oreName;
        return extractBaseType(name);
    }

    private String extractBaseType(String oreName) {
        if (oreName == null) return null;
        return OreDictRecipeCache.findPrefix(oreName);
    }

    private ItemStack findPatternInInventory(EntityPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemTest) {
                return stack;
            }
        }
        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemTest) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
