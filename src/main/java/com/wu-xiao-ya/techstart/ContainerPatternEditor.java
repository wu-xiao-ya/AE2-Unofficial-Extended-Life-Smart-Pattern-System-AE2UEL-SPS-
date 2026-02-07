// ???????
package com.lwx1145.techstart;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

public class ContainerPatternEditor extends Container {

    private final EntityPlayer player;
    private final ItemStack patternStack;
    private final PatternInputInventory inputInventory = new PatternInputInventory();
    private final PatternOutputInventory outputInventory = new PatternOutputInventory();
    private int selectedRecipeType = 0; // ?????????

    public ContainerPatternEditor(EntityPlayer player) {
        this.player = player;
        
        // 优先从玩家主手获取pattern（从方块GUI打开时）
        ItemStack mainHandStack = player.getHeldItemMainhand();
        if (!mainHandStack.isEmpty() && mainHandStack.getItem() instanceof ItemTest) {
            this.patternStack = mainHandStack;
        } else {
            // 否则从玩家副手或背包查找
            ItemStack offHandStack = player.getHeldItemOffhand();
            if (!offHandStack.isEmpty() && offHandStack.getItem() instanceof ItemTest) {
                this.patternStack = offHandStack;
            } else {
                this.patternStack = findPatternInInventory(player);
            }
        }

        // ????????????????
        if (this.patternStack.isEmpty()) {
            return;
        }

        // ?????? (????????)
        this.addSlotToContainer(new SlotPatternInput(this, 0, 44, 35));

        // ?????? (????????)
        this.addSlotToContainer(new SlotPatternOutput(this, 0, 116, 35));

        // ?????? (3?x9?)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(player.inventory, j + i * 9 + 9, 8 + j * 18, 125 + i * 18));
            }
        }

        // ???????
        for (int k = 0; k < 9; ++k) {
            this.addSlotToContainer(new Slot(player.inventory, k, 8 + k * 18, 185));
        }
    }

    /**
     * ?????????????
     */
    private ItemStack findPatternInInventory(EntityPlayer player) {
        // ?????
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemTest) {
                return stack;
            }
        }
        // ????
        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemTest) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * ????????
     */
    public ItemStack getPatternStack() {
        return patternStack;
    }

    /**
     * ??????
     */
    public ItemStack getInputStack() {
        return inputInventory.getStackInSlot(0);
    }

    /**
     * ??????
     */
    public ItemStack getOutputStack() {
        return outputInventory.getStackInSlot(0);
    }

    /**
     * ????????
     */
    public String getOreName(ItemStack stack) {
        if (stack.isEmpty()) return null;
        int[] oreIds = OreDictionary.getOreIDs(stack);
        if (oreIds.length > 0) {
            return OreDictionary.getOreName(oreIds[0]);
        }
        return null;
    }

    /**
     * ????????
     */
    public IInventory getDerivedInventory() {
        return new DerivedRecipeInventory();
    }

    /**
     * ???????????
     */
    public int getSelectedRecipeType() {
        return selectedRecipeType;
    }

    /**
     * ???????????
     */
    public void setSelectedRecipeType(int index) {
        this.selectedRecipeType = index;
        // ????????????????
        encodeSelectedPattern();
    }

    /**
     * 设置输入数量
     */
    public void setInputCount(int count) {
        if (patternStack.isEmpty() || !(patternStack.getItem() instanceof ItemTest)) {
            return;
        }
        
        ItemTest patternItem = (ItemTest) patternStack.getItem();
        String inputOre = patternItem.getInputOreName(patternStack);
        String outputOre = patternItem.getOutputOreName(patternStack);
        String displayName = patternItem.getEncodedItemName(patternStack);
        int outputCount = patternItem.getOutputCount(patternStack);
        
        if (inputOre != null && outputOre != null) {
            patternItem.setEncodedItem(patternStack, inputOre, outputOre, displayName, count, outputCount);
        }
    }

    /**
     * 设置输出数量
     */
    public void setOutputCount(int count) {
        if (patternStack.isEmpty() || !(patternStack.getItem() instanceof ItemTest)) {
            return;
        }
        
        ItemTest patternItem = (ItemTest) patternStack.getItem();
        String inputOre = patternItem.getInputOreName(patternStack);
        String outputOre = patternItem.getOutputOreName(patternStack);
        String displayName = patternItem.getEncodedItemName(patternStack);
        int inputCount = patternItem.getInputCount(patternStack);
        
        if (inputOre != null && outputOre != null) {
            patternItem.setEncodedItem(patternStack, inputOre, outputOre, displayName, inputCount, count);
        }
    }

    /**
     * ???????????
     */
    public List<String[]> getAvailableRecipeTypes() {
        List<String[]> recipeTypes = new ArrayList<>();

        ItemStack inputStack = getInputStack();
        ItemStack outputStack = getOutputStack();

        if (inputStack.isEmpty() || outputStack.isEmpty()) {
            return recipeTypes;
        }

        // ????????
        List<ItemStack[]> derivedRecipes = deriveRecipes(inputStack, outputStack);

        for (int i = 0; i < derivedRecipes.size(); i++) {
            ItemStack[] recipe = derivedRecipes.get(i);
            if (recipe.length >= 2) {
                String inputName = recipe[0].getDisplayName();
                String outputName = recipe[1].getDisplayName();
                String displayName = inputName + " ? " + outputName;
                recipeTypes.add(new String[]{getOreName(recipe[0]), getOreName(recipe[1]), displayName});
            }
        }

        return recipeTypes;
    }

    /**
     * ??????????
     */
    private void encodeSelectedPattern() {
        ItemStack patternStack = getPatternStack();
        if (patternStack.isEmpty() || !(patternStack.getItem() instanceof ItemTest)) {
            return;
        }

        List<String[]> availableTypes = getAvailableRecipeTypes();
        if (selectedRecipeType < 0 || selectedRecipeType >= availableTypes.size()) {
            return;
        }

        String[] selectedRecipe = availableTypes.get(selectedRecipeType);
        String inputOreName = toWildcardPattern(selectedRecipe[0]);
        String outputOreName = toWildcardPattern(selectedRecipe[1]);
        String displayName = selectedRecipe[2];

        ItemTest patternItem = (ItemTest) patternStack.getItem();
        // 保留当前的数量设置
        int inputCount = patternItem.getInputCount(patternStack);
        int outputCount = patternItem.getOutputCount(patternStack);
        patternItem.setEncodedItem(patternStack, inputOreName, outputOreName, displayName, inputCount, outputCount);
    }

    /**
     * ??????????????????
     */
    private String toWildcardPattern(String oreName) {
        if (oreName.startsWith("ingot")) {
            return "ingot*";
        } else if (oreName.startsWith("block")) {
            return "block*";
        } else if (oreName.startsWith("nugget")) {
            return "nugget*";
        } else if (oreName.startsWith("plate")) {
            return "plate*";
        } else if (oreName.startsWith("dust")) {
            return "dust*";
        } else if (oreName.startsWith("rod")) {
            return "rod*";
        } else if (oreName.startsWith("gear")) {
            return "gear*";
        } else if (oreName.startsWith("wire")) {
            return "wire*";
        }
        return oreName; // ??????????????
    }

    /**
     * 根据输入输出物品推导其他配方
     */
    private List<ItemStack[]> deriveRecipes(ItemStack inputStack, ItemStack outputStack) {
        List<ItemStack[]> derivedRecipes = new ArrayList<>();

        // 获取输入和输出的矿物辞典名称
        String inputOreName = getOreName(inputStack);
        String outputOreName = getOreName(outputStack);

        if (inputOreName == null || outputOreName == null) {
            return derivedRecipes;
        }

        // 提取基础类型 (如 ingot, plate, dust 等)
        String inputBaseType = extractBaseType(inputOreName);
        String outputBaseType = extractBaseType(outputOreName);

        if (inputBaseType == null || outputBaseType == null) {
            return derivedRecipes;
        }

        // 提取材料名称 (如 Copper, Iron, Gold 等)
        String inputMaterial = extractMaterial(inputOreName);
        String outputMaterial = extractMaterial(outputOreName);

        if (inputMaterial == null || outputMaterial == null) {
            return derivedRecipes;
        }

        // 只为相同材料的输入输出生成配方
        if (inputMaterial.equals(outputMaterial)) {
            String derivedInputOre = inputBaseType + inputMaterial;
            String derivedOutputOre = outputBaseType + outputMaterial;

            // 检查这些矿物辞典条目是否存在
            if (OreDictionary.doesOreNameExist(derivedInputOre) &&
                OreDictionary.doesOreNameExist(derivedOutputOre)) {

                List<ItemStack> inputItems = OreDictionary.getOres(derivedInputOre);
                List<ItemStack> outputItems = OreDictionary.getOres(derivedOutputOre);

                if (!inputItems.isEmpty() && !outputItems.isEmpty()) {
                    derivedRecipes.add(new ItemStack[]{inputItems.get(0), outputItems.get(0)});
                }
            }
        }

        return derivedRecipes;
    }

    /**
     * 提取基础类型
     */
    private String extractBaseType(String oreName) {
        if (oreName.startsWith("ingot")) return "ingot";
        if (oreName.startsWith("block")) return "block";
        if (oreName.startsWith("nugget")) return "nugget";
        if (oreName.startsWith("plate")) return "plate";
        if (oreName.startsWith("dust")) return "dust";
        if (oreName.startsWith("rod")) return "rod";
        if (oreName.startsWith("gear")) return "gear";
        if (oreName.startsWith("wire")) return "wire";
        return null;
    }

    /**
     * 提取材料名称
     */
    private String extractMaterial(String oreName) {
        // 移除基础类型前缀，剩下的就是材料名称
        if (oreName.startsWith("ingot")) return oreName.substring(5);
        if (oreName.startsWith("block")) return oreName.substring(5);
        if (oreName.startsWith("nugget")) return oreName.substring(6);
        if (oreName.startsWith("plate")) return oreName.substring(5);
        if (oreName.startsWith("dust")) return oreName.substring(4);
        if (oreName.startsWith("rod")) return oreName.substring(3);
        if (oreName.startsWith("gear")) return oreName.substring(4);
        if (oreName.startsWith("wire")) return oreName.substring(4);
        return null;
    }

    /**
     * 获取常见材料列表
     */
    private List<String> getCommonMaterials() {
        List<String> materials = new ArrayList<>();
        materials.add("Iron");
        materials.add("Gold");
        materials.add("Copper");
        materials.add("Tin");
        materials.add("Bronze");
        materials.add("Silver");
        materials.add("Lead");
        materials.add("Aluminum");
        return materials;
    }

    /**
     * ??????
     */
    public void updateDerivedRecipes() {
        // ????????
    }

    /**
     * ??????
     */
    public void encodeSmartPattern() {
        ItemStack inputStack = getInputStack();
        ItemStack outputStack = getOutputStack();

        if (inputStack.isEmpty() || outputStack.isEmpty()) {
            return;
        }

        String inputOreName = toWildcardPattern(getOreName(inputStack));
        String outputOreName = toWildcardPattern(getOreName(outputStack));
        String displayName = inputStack.getDisplayName() + " ? " + outputStack.getDisplayName();

        ItemStack patternStack = getPatternStack();
        if (!patternStack.isEmpty() && patternStack.getItem() instanceof ItemTest) {
            ItemTest patternItem = (ItemTest) patternStack.getItem();
            patternItem.setEncodedItem(patternStack, inputOreName, outputOreName, displayName);
        }
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        
        // 当容器关闭时，返还输入和输出槽位中的物品
        if (!playerIn.world.isRemote) {
            // 确保两个槽位的物品都会被处理，即使其中一个出错也不影响另一个
            try {
                ItemStack inputStack = inputInventory.removeStackFromSlot(0);
                if (!inputStack.isEmpty()) {
                    if (!playerIn.inventory.addItemStackToInventory(inputStack)) {
                        playerIn.dropItem(inputStack, false);
                    }
                }
            } catch (Exception e) {
                // 输入返还失败，继续处理输出
            }
            
            try {
                ItemStack outputStack = outputInventory.removeStackFromSlot(0);
                if (!outputStack.isEmpty()) {
                    if (!playerIn.inventory.addItemStackToInventory(outputStack)) {
                        playerIn.dropItem(outputStack, false);
                    }
                }
            } catch (Exception e) {
                // 输出返还失败，至少已经尝试过了
            }
            
            // 标记容器已改变，强制同步
            try {
                inputInventory.markDirty();
                outputInventory.markDirty();
            } catch (Exception e) {
                // 标记同步失败，但物品已经处理了
            }
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, net.minecraft.inventory.ClickType clickTypeIn, EntityPlayer player) {
        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index < 2) {
                // 从容器槽位(0-1)移到玩家背包(2-37)
                if (!this.mergeItemStack(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 38) {
                // 从玩家背包(2-37)移到容器槽位(0-1)
                if (isOreDictItem(itemstack1)) {
                    // 合并到输入/输出槽位
                    if (!this.mergeItemStack(itemstack1, 0, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }

        return itemstack;
    }

    /**
     * ????????????
     */
    private boolean isOreDictItem(ItemStack stack) {
        int[] oreIds = OreDictionary.getOreIDs(stack);
        return oreIds.length > 0;
    }

    /**
     * ?????? - ?????
     */
    private class SlotPatternInput extends Slot {
        private final ContainerPatternEditor container;

        public SlotPatternInput(ContainerPatternEditor container, int index, int xPosition, int yPosition) {
            super(container.inputInventory, index, xPosition, yPosition);
            this.container = container;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            // 允许任何矿物辞典物品
            if (stack.isEmpty()) return false;
            int[] oreIds = OreDictionary.getOreIDs(stack);
            return oreIds.length > 0;
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }

        @Override
        public boolean canTakeStack(EntityPlayer playerIn) {
            return true;
        }

        @Override
        public ItemStack decrStackSize(int amount) {
            // 返回堆栈的一部分
            ItemStack itemstack = this.inventory.getStackInSlot(this.getSlotIndex());
            if (!itemstack.isEmpty()) {
                if (itemstack.getCount() <= amount) {
                    this.inventory.setInventorySlotContents(this.getSlotIndex(), ItemStack.EMPTY);
                    return itemstack;
                } else {
                    ItemStack itemstack1 = itemstack.splitStack(amount);
                    if (this.inventory.getInventoryStackLimit() == 1) {
                        this.inventory.setInventorySlotContents(this.getSlotIndex(), ItemStack.EMPTY);
                    }
                    return itemstack1;
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void putStack(ItemStack stack) {
            // 限制堆栈大小为1
            if (!stack.isEmpty()) {
                stack.setCount(Math.min(stack.getCount(), 1));
            }
            this.inventory.setInventorySlotContents(this.getSlotIndex(), stack);
            this.onSlotChanged();
        }

        @Override
        public void onSlotChanged() {
            super.onSlotChanged();
            // 重置配方选择
            container.setSelectedRecipeType(0);
            // 更新派生配方
            container.updateDerivedRecipes();
            container.encodeSmartPattern();
        }
    }

    /**
     * 输出槽位 - 输出物品
     */
    private class SlotPatternOutput extends Slot {
        private final ContainerPatternEditor container;

        public SlotPatternOutput(ContainerPatternEditor container, int index, int xPosition, int yPosition) {
            super(container.outputInventory, index, xPosition, yPosition);
            this.container = container;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            // 允许任何矿物辞典物品
            if (stack.isEmpty()) return false;
            int[] oreIds = OreDictionary.getOreIDs(stack);
            return oreIds.length > 0;
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }

        @Override
        public boolean canTakeStack(EntityPlayer playerIn) {
            return true;
        }

        @Override
        public ItemStack decrStackSize(int amount) {
            // 返回堆栈的一部分
            ItemStack itemstack = this.inventory.getStackInSlot(this.getSlotIndex());
            if (!itemstack.isEmpty()) {
                if (itemstack.getCount() <= amount) {
                    this.inventory.setInventorySlotContents(this.getSlotIndex(), ItemStack.EMPTY);
                    return itemstack;
                } else {
                    ItemStack itemstack1 = itemstack.splitStack(amount);
                    if (this.inventory.getInventoryStackLimit() == 1) {
                        this.inventory.setInventorySlotContents(this.getSlotIndex(), ItemStack.EMPTY);
                    }
                    return itemstack1;
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void putStack(ItemStack stack) {
            // 限制堆栈大小为1
            if (!stack.isEmpty()) {
                stack.setCount(Math.min(stack.getCount(), 1));
            }
            this.inventory.setInventorySlotContents(this.getSlotIndex(), stack);
            this.onSlotChanged();
        }

        @Override
        public void onSlotChanged() {
            super.onSlotChanged();
            // 重置配方选择
            container.setSelectedRecipeType(0);
            // 更新派生配方
            container.updateDerivedRecipes();
            container.encodeSmartPattern();
        }
    }

    /**
     * 派生配方槽位 - 只读 (不可放置物品)
     */
    private class SlotDerivedRecipe extends Slot {
        private final ContainerPatternEditor container;

        public SlotDerivedRecipe(ContainerPatternEditor container, int index, int xPosition, int yPosition) {
            super(container.getDerivedInventory(), index, xPosition, yPosition);
            this.container = container;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false; // 不允许放置物品
        }

        @Override
        public boolean canTakeStack(EntityPlayer playerIn) {
            return false; // 不允许取出物品
        }
    }

    /**
     * 输入物品槽位 - 只读 (不可放置物品)
     */
    private class PatternInputInventory implements IInventory {
        private ItemStack stack = ItemStack.EMPTY;

        @Override
        public int getSizeInventory() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return stack.isEmpty();
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            return index == 0 ? stack : ItemStack.EMPTY;
        }

        @Override
        public ItemStack decrStackSize(int index, int count) {
            if (index == 0 && !stack.isEmpty()) {
                ItemStack itemstack = stack.splitStack(count);
                if (stack.isEmpty()) {
                    stack = ItemStack.EMPTY;
                }
                markDirty();
                return itemstack;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeStackFromSlot(int index) {
            if (index == 0) {
                ItemStack itemstack = stack;
                stack = ItemStack.EMPTY;
                markDirty();
                return itemstack;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
            if (index == 0) {
                this.stack = stack;
                markDirty();
            }
        }

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public void markDirty() {
            // ?????????????
            if (!stack.isEmpty() && ContainerPatternEditor.this.patternStack != null) {
                ItemTest patternItem = (ItemTest) ContainerPatternEditor.this.patternStack.getItem();
                patternItem.setEncodedItem(ContainerPatternEditor.this.patternStack,
                    OreDictionary.getOreName(OreDictionary.getOreIDs(stack)[0]),
                    stack.getDisplayName());
            }
        }

        @Override
        public boolean isUsableByPlayer(EntityPlayer player) {
            return true;
        }

        @Override
        public void openInventory(EntityPlayer player) {
        }

        @Override
        public void closeInventory(EntityPlayer player) {
        }

        @Override
        public boolean isItemValidForSlot(int index, ItemStack stack) {
            return true;
        }

        @Override
        public int getField(int id) {
            return 0;
        }

        @Override
        public void setField(int id, int value) {
        }

        @Override
        public int getFieldCount() {
            return 0;
        }

        @Override
        public void clear() {
            stack = ItemStack.EMPTY;
            markDirty();
        }

        @Override
        public String getName() {
            return "PatternInput";
        }

        @Override
        public boolean hasCustomName() {
            return false;
        }

        @Override
        public net.minecraft.util.text.ITextComponent getDisplayName() {
            return new net.minecraft.util.text.TextComponentString(getName());
        }
    }

    /**
     * ??????????
     */
    private class PatternOutputInventory implements IInventory {
        private ItemStack stack = ItemStack.EMPTY;

        @Override
        public int getSizeInventory() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return stack.isEmpty();
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            return index == 0 ? stack : ItemStack.EMPTY;
        }

        @Override
        public ItemStack decrStackSize(int index, int count) {
            if (index == 0 && !stack.isEmpty()) {
                ItemStack itemstack = stack.splitStack(count);
                if (stack.isEmpty()) {
                    stack = ItemStack.EMPTY;
                }
                markDirty();
                return itemstack;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeStackFromSlot(int index) {
            if (index == 0) {
                ItemStack itemstack = stack;
                stack = ItemStack.EMPTY;
                markDirty();
                return itemstack;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
            if (index == 0) {
                this.stack = stack;
                markDirty();
            }
        }

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public void markDirty() {
            // ?????????????
        }

        @Override
        public boolean isUsableByPlayer(EntityPlayer player) {
            return true;
        }

        @Override
        public void openInventory(EntityPlayer player) {
        }

        @Override
        public void closeInventory(EntityPlayer player) {
        }

        @Override
        public boolean isItemValidForSlot(int index, ItemStack stack) {
            return true;
        }

        @Override
        public int getField(int id) {
            return 0;
        }

        @Override
        public void setField(int id, int value) {
        }

        @Override
        public int getFieldCount() {
            return 0;
        }

        @Override
        public void clear() {
            stack = ItemStack.EMPTY;
            markDirty();
        }

        @Override
        public String getName() {
            return "PatternOutput";
        }

        @Override
        public boolean hasCustomName() {
            return false;
        }

        @Override
        public net.minecraft.util.text.ITextComponent getDisplayName() {
            return new net.minecraft.util.text.TextComponentString(getName());
        }
    }

    /**
     * ???????????
     */
    private class DerivedRecipeInventory implements IInventory {
        @Override
        public int getSizeInventory() {
            return 9;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            List<ItemStack[]> derivedRecipes = deriveRecipes(getInputStack(), getOutputStack());
            if (index < derivedRecipes.size()) {
                ItemStack[] recipe = derivedRecipes.get(index);
                return index == 0 ? recipe[0] : recipe[1];
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack decrStackSize(int index, int count) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeStackFromSlot(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
        }

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public void markDirty() {
        }

        @Override
        public boolean isUsableByPlayer(EntityPlayer player) {
            return false;
        }

        @Override
        public void openInventory(EntityPlayer player) {
        }

        @Override
        public void closeInventory(EntityPlayer player) {
        }

        @Override
        public boolean isItemValidForSlot(int index, ItemStack stack) {
            return false;
        }

        @Override
        public int getField(int id) {
            return 0;
        }

        @Override
        public void setField(int id, int value) {
        }

        @Override
        public int getFieldCount() {
            return 0;
        }

        @Override
        public void clear() {
        }

        @Override
        public String getName() {
            return "DerivedRecipes";
        }

        @Override
        public boolean hasCustomName() {
            return false;
        }

        @Override
        public net.minecraft.util.text.ITextComponent getDisplayName() {
            return new net.minecraft.util.text.TextComponentString(getName());
        }
    }
}


