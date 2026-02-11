package com.lwx1145.techstart;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;
import net.minecraftforge.fml.common.Loader;

public class ContainerPatternEditor extends Container {

    private static final int INPUT_SLOTS = 9;
    private static final int OUTPUT_SLOTS = 9;
    private static final int TOTAL_PATTERN_SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;
    private static final int DEFAULT_FLUID_AMOUNT = 1000;
    private static final int DEFAULT_GAS_AMOUNT = 1000;
    private static final String TAG_ITEM_MARKER = "TechStartItemMarker";
    private static final String TAG_ITEM_AMOUNT = "TechStartItemAmount";

    private static Method cachedFakeFluidCheck;
    private static Method cachedFakeFluidDisplay;
    private static Method cachedFakeItemGetStack;
    private static boolean fakeFluidMethodsReady = false;

    private static Method cachedFakeGasCheck;
    private static Method cachedFakeGasDisplay;
    private static boolean fakeGasMethodsReady = false;

    private static Class<?> cachedGasItemClass;
    private static Method cachedGasItemGetGas;
    private static Method cachedGasStackGetGas;
    private static Method cachedGasGetName;
    private static Method cachedGasStackGetAmount;
    private static java.lang.reflect.Field cachedGasStackAmountField;
    private static Class<?> cachedDummyGasItemClass;
    private static Method cachedDummyGasSetGas;
    private static Method cachedDummyGasGetGas;
    private static Method cachedGasRegistryGetGas;
    private static java.lang.reflect.Constructor<?> cachedGasStackCtor;
    private static boolean gasMethodsReady = false;

    private final EntityPlayer player;
    private final ItemStack patternStack;
    private final PatternInputInventory inputInventory = new PatternInputInventory(INPUT_SLOTS);
    private final PatternOutputInventory outputInventory = new PatternOutputInventory(OUTPUT_SLOTS);
    private int selectedRecipeType = 0; // ????????

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

        // ?????? (9?????)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = row * 3 + col;
                int xPos = 26 + col * 18;
                int yPos = 35 + row * 18;
                this.addSlotToContainer(new SlotPatternInput(this, slotIndex, xPos, yPos));
            }
        }

        // ?????? (9?????)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = row * 3 + col;
                int xPos = 98 + col * 18;
                int yPos = 35 + row * 18;
                this.addSlotToContainer(new SlotPatternOutput(this, slotIndex, xPos, yPos));
            }
        }

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

    public int getFilterMode() {
        return ItemTest.getFilterModeStatic(patternStack);
    }

    public List<String> getFilterEntries() {
        return ItemTest.getFilterEntriesStatic(patternStack);
    }

    public void applyFilterMode(int mode) {
        ItemTest.setFilterModeStatic(patternStack, mode);
        this.player.inventory.markDirty();
    }

    public void toggleFilterEntry(String entry) {
        ItemTest.toggleFilterEntryStatic(patternStack, entry);
        this.player.inventory.markDirty();
    }

    public void clearFilterEntries() {
        ItemTest.clearFilterEntriesStatic(patternStack);
        this.player.inventory.markDirty();
    }

    private void markDirty() {
        this.player.inventory.markDirty();
    }

    /**
     * ??????
     */
    public ItemStack getInputStack() {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = inputInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getInputStack(int index) {
        return inputInventory.getStackInSlot(index);
    }

    /**
     * ??????
     */
    public ItemStack getOutputStack() {
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack stack = outputInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getOutputStack(int index) {
        return outputInventory.getStackInSlot(index);
    }

    /**
     * 获取矿物辞典名称
     */
    public String getOreName(ItemStack stack) {
        if (stack.isEmpty()) return null;
        int[] oreIds = OreDictionary.getOreIDs(stack);
        if (oreIds.length > 0) {
            return OreDictionary.getOreName(oreIds[0]);
        }
        return null;
    }

    public IInventory getDerivedInventory() {
        return new DerivedRecipeInventory();
    }

    public int getSelectedRecipeType() {
        return selectedRecipeType;
    }

    public void setSelectedRecipeType(int index) {
        this.selectedRecipeType = index;
        encodeSelectedPattern();
        markDirty();
    }

    public List<String[]> getAvailableRecipeTypes() {
        List<String[]> recipeTypes = new ArrayList<>();

        ItemStack inputStack = getInputStack();
        ItemStack outputStack = getOutputStack();

        if (inputStack.isEmpty() || outputStack.isEmpty()) {
            return recipeTypes;
        }

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
        
        // 提取流体信息
        List<String> inputFluids = new ArrayList<>();
        List<Integer> inputFluidAmounts = new ArrayList<>();
        List<String> outputFluids = new ArrayList<>();
        List<Integer> outputFluidAmounts = new ArrayList<>();
        List<String> inputGases = new ArrayList<>();
        List<Integer> inputGasAmounts = new ArrayList<>();
        List<String> outputGases = new ArrayList<>();
        List<Integer> outputGasAmounts = new ArrayList<>();
        List<ItemStack> inputGasItems = new ArrayList<>();
        List<ItemStack> outputGasItems = new ArrayList<>();
        
        // 从输入槽位提取流体
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = getInputStack(i);
            if (!stack.isEmpty()) {
                FluidInfo fluidInfo = extractFluidFromStack(stack);
                if (fluidInfo != null && fluidInfo.fluidName != null && !fluidInfo.fluidName.isEmpty()) {
                    inputFluids.add(fluidInfo.fluidName);
                    inputFluidAmounts.add(fluidInfo.amount);
                    continue;
                }
                GasInfo gasInfo = extractGasFromStack(stack);
                if (gasInfo != null && gasInfo.gasName != null && !gasInfo.gasName.isEmpty()) {
                    inputGases.add(gasInfo.gasName);
                    inputGasAmounts.add(gasInfo.amount);
                }
            }
        }
        
        // 从输出槽位提取流体
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack stack = getOutputStack(i);
            if (!stack.isEmpty()) {
                FluidInfo fluidInfo = extractFluidFromStack(stack);
                if (fluidInfo != null && fluidInfo.fluidName != null && !fluidInfo.fluidName.isEmpty()) {
                    outputFluids.add(fluidInfo.fluidName);
                    outputFluidAmounts.add(fluidInfo.amount);
                    continue;
                }
                GasInfo gasInfo = extractGasFromStack(stack);
                if (gasInfo != null && gasInfo.gasName != null && !gasInfo.gasName.isEmpty()) {
                    outputGases.add(gasInfo.gasName);
                    outputGasAmounts.add(gasInfo.amount);
                }
            }
        }
        
        // 如果有流体，使用新的setEncodedItemWithFluids方法
        if (!inputFluids.isEmpty() || !outputFluids.isEmpty() || !inputGases.isEmpty() || !outputGases.isEmpty()) {
            List<String> inputOres = new ArrayList<>();
            List<Integer> inputCounts = new ArrayList<>();
            inputOres.add(inputOreName);
            inputCounts.add(inputCount);
            
            List<String> outputOres = new ArrayList<>();
            List<Integer> outputCounts = new ArrayList<>();
            outputOres.add(outputOreName);
            outputCounts.add(outputCount);
            
                patternItem.setEncodedItemWithFluidsAndGases(patternStack, inputOres, inputCounts, outputOres, outputCounts,
                    inputFluids, inputFluidAmounts, outputFluids, outputFluidAmounts,
                    inputGases, inputGasAmounts, outputGases, outputGasAmounts,
                    inputGasItems, outputGasItems, displayName);
        } else {
            // 无流体，使用原有的setEncodedItem方法
            patternItem.setEncodedItem(patternStack, inputOreName, outputOreName, displayName, inputCount, outputCount);
        }
    }

    public void savePattern() {
        encodeSelectedPattern();
    }

    /**
     * 从物品堆栈中提取流体信息
     * 支持原版桶、热力便携罐等所有Forge流体容器（1.12.2版本）
     */
    private FluidInfo extractFluidFromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        if (!isFakeFluidItem(stack)) {
            return null;
        }

        // 优先处理假流体标记
        FluidStack fake = getFakeFluidStack(stack);
        if (fake != null && fake.getFluid() != null) {
            int amount = resolveMarkerAmount(stack, fake.amount);
            if (amount > 0) {
                return new FluidInfo(fake.getFluid().getName(), amount);
            }
        }

        // 如果都无法获取，返回null
        return null;
    }

    private FluidInfo extractFluidFromContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        try {
            FluidStack fluidStack = FluidUtil.getFluidContained(stack);
            if (fluidStack != null && fluidStack.amount > 0 && fluidStack.getFluid() != null) {
                return new FluidInfo(fluidStack.getFluid().getName(), fluidStack.amount);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private GasInfo extractGasFromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("TechStartGasName")) {
            String gasName = stack.getTagCompound().getString("TechStartGasName");
            int amount = stack.getTagCompound().getInteger("TechStartGasAmount");
            if (gasName != null && !gasName.isEmpty()) {
                return new GasInfo(gasName, Math.max(1, amount));
            }
        }
        initFakeGasMethods();
        if (cachedFakeGasCheck != null) {
            try {
                Object isFake = cachedFakeGasCheck.invoke(null, stack);
                if (isFake instanceof Boolean && (Boolean) isFake) {
                    Object gasStack = getFakeGasStack(stack);
                    if (gasStack != null) {
                        String gasName = resolveGasName(gasStack);
                        int amount = resolveGasAmount(gasStack);
                        if (gasName != null && !gasName.isEmpty() && amount > 0) {
                            return new GasInfo(gasName, amount);
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore and continue.
            }
        }
        initGasMethods();
        if (cachedDummyGasItemClass != null && cachedDummyGasItemClass.isInstance(stack.getItem()) && cachedDummyGasGetGas != null) {
            try {
                Object gasStack = cachedDummyGasGetGas.invoke(stack.getItem(), stack);
                if (gasStack != null) {
                    String gasName = resolveGasName(gasStack);
                    int amount = resolveGasAmount(gasStack);
                    if (gasName != null && !gasName.isEmpty() && amount > 0) {
                        return new GasInfo(gasName, amount);
                    }
                }
            } catch (Exception e) {
                // Ignore and continue.
            }
        }
        return null;
    }

    private GasInfo extractGasFromContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        initGasMethods();
        if (cachedGasItemClass == null || cachedGasItemGetGas == null) {
            return null;
        }
        Object item = stack.getItem();
        if (!cachedGasItemClass.isInstance(item)) {
            return null;
        }
        try {
            Object gasStack = cachedGasItemGetGas.invoke(item, stack);
            if (gasStack == null) {
                return null;
            }
            String gasName = resolveGasName(gasStack);
            int amount = resolveGasAmount(gasStack);
            if (gasName == null || gasName.isEmpty() || amount <= 0) {
                return null;
            }
            return new GasInfo(gasName, amount);
        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack createGasMarkerStack(ItemStack source, GasInfo gasInfo) {
        if (source == null || source.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (gasInfo == null || gasInfo.gasName == null || gasInfo.gasName.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack marker = createFakeGasDisplayStack(gasInfo.gasName, gasInfo.amount);
        if (marker.isEmpty()) {
            marker = source.copy();
            marker.setCount(1);
        }
        NBTTagCompound tag = marker.hasTagCompound() ? marker.getTagCompound() : new NBTTagCompound();
        tag.setBoolean("TechStartGasMarker", true);
        tag.setString("TechStartGasName", gasInfo.gasName);
        tag.setInteger("TechStartGasAmount", Math.max(1, gasInfo.amount));
        marker.setTagCompound(tag);
        return marker;
    }

    private ItemStack createFakeGasDisplayStack(String gasName, int amount) {
        initFakeGasMethods();
        if (cachedFakeGasDisplay == null || cachedGasRegistryGetGas == null || cachedGasStackCtor == null) {
            return ItemStack.EMPTY;
        }
        try {
            Object gas = cachedGasRegistryGetGas.invoke(null, gasName);
            if (gas == null) {
                return ItemStack.EMPTY;
            }
            Object gasStack = cachedGasStackCtor.newInstance(gas, Math.max(1, amount));
            Object result = cachedFakeGasDisplay.invoke(null, gasStack);
            if (result instanceof ItemStack) {
                return (ItemStack) result;
            }
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack createDummyGasStack(String gasName, int amount) {
        initGasMethods();
        if (cachedDummyGasItemClass == null || cachedDummyGasSetGas == null || cachedGasRegistryGetGas == null || cachedGasStackCtor == null) {
            return ItemStack.EMPTY;
        }
        net.minecraft.item.Item dummyItem = net.minecraft.item.Item.getByNameOrId("mekeng:dummy_gas");
        if (dummyItem == null) {
            return ItemStack.EMPTY;
        }
        try {
            Object gas = cachedGasRegistryGetGas.invoke(null, gasName);
            if (gas == null) {
                return ItemStack.EMPTY;
            }
            Object gasStack = cachedGasStackCtor.newInstance(gas, Math.max(1, amount));
            ItemStack stack = new ItemStack(dummyItem);
            cachedDummyGasSetGas.invoke(dummyItem, stack, gasStack);
            return stack;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private ItemStack stripGasMarkerTag(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return stack;
        }
        NBTTagCompound tag = stack.getTagCompound();
        tag.removeTag("TechStartGasMarker");
        tag.removeTag("TechStartGasName");
        tag.removeTag("TechStartGasAmount");
        if (tag.isEmpty()) {
            stack.setTagCompound(null);
        } else {
            stack.setTagCompound(tag);
        }
        return stack;
    }

    private String resolveGasName(Object gasStack) {
        if (cachedGasStackGetGas == null || cachedGasGetName == null) {
            return null;
        }
        try {
            Object gas = cachedGasStackGetGas.invoke(gasStack);
            if (gas == null) {
                return null;
            }
            Object name = cachedGasGetName.invoke(gas);
            return name == null ? null : name.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private int resolveGasAmount(Object gasStack) {
        if (cachedGasStackGetAmount != null) {
            try {
                Object value = cachedGasStackGetAmount.invoke(gasStack);
                if (value instanceof Integer) {
                    return (Integer) value;
                }
            } catch (Exception e) {
                // Fallback to field read.
            }
        }
        if (cachedGasStackAmountField != null) {
            try {
                return cachedGasStackAmountField.getInt(gasStack);
            } catch (Exception e) {
                return 0;
            }
        }
        return DEFAULT_GAS_AMOUNT;
    }

    private int resolveMarkerAmount(ItemStack stack, int fallback) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("TechStartFluidAmount")) {
            int value = stack.getTagCompound().getInteger("TechStartFluidAmount");
            return Math.max(1, value);
        }
        return Math.max(1, fallback);
    }

    private int getItemMarkerAmount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 1;
        }
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(TAG_ITEM_AMOUNT)) {
            int value = stack.getTagCompound().getInteger(TAG_ITEM_AMOUNT);
            return Math.max(1, value);
        }
        return Math.max(1, stack.getCount());
    }

    private ItemStack createItemMarkerStack(ItemStack source, int amount) {
        if (source == null || source.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack marker = source.copy();
        marker.setCount(1);
        NBTTagCompound tag = marker.hasTagCompound() ? marker.getTagCompound() : new NBTTagCompound();
        tag.setBoolean(TAG_ITEM_MARKER, true);
        tag.setInteger(TAG_ITEM_AMOUNT, Math.max(1, amount));
        marker.setTagCompound(tag);
        return marker;
    }

    private static void initGasMethods() {
        if (gasMethodsReady) {
            return;
        }
        gasMethodsReady = true;
        // 如果 Mekanism 未安装，则不尝试反射加载其类，防止 NoClassDefFoundError
        if (!Loader.isModLoaded("mekanism")) {
            cachedGasItemClass = null;
            cachedGasItemGetGas = null;
            cachedGasStackGetGas = null;
            cachedGasStackGetAmount = null;
            cachedGasStackAmountField = null;
            cachedGasStackCtor = null;
            cachedGasGetName = null;
            cachedDummyGasItemClass = null;
            cachedDummyGasSetGas = null;
            cachedDummyGasGetGas = null;
            cachedGasRegistryGetGas = null;
            return;
        }
        try {
            cachedGasItemClass = Class.forName("mekanism.api.gas.IGasItem");
            cachedGasItemGetGas = cachedGasItemClass.getMethod("getGas", ItemStack.class);
        } catch (Throwable e) {
            cachedGasItemClass = null;
            cachedGasItemGetGas = null;
        }
        try {
            Class<?> gasStackClass = Class.forName("mekanism.api.gas.GasStack");
            cachedGasStackGetGas = gasStackClass.getMethod("getGas");
            try {
                cachedGasStackGetAmount = gasStackClass.getMethod("getAmount");
            } catch (Exception e) {
                cachedGasStackGetAmount = null;
            }
            try {
                cachedGasStackAmountField = gasStackClass.getDeclaredField("amount");
                cachedGasStackAmountField.setAccessible(true);
            } catch (Exception e) {
                cachedGasStackAmountField = null;
            }
            try {
                cachedGasStackCtor = gasStackClass.getConstructor(Class.forName("mekanism.api.gas.Gas"), int.class);
            } catch (Exception e) {
                cachedGasStackCtor = null;
            }
        } catch (Throwable e) {
            cachedGasStackGetGas = null;
            cachedGasStackGetAmount = null;
            cachedGasStackAmountField = null;
            cachedGasStackCtor = null;
        }
        try {
            Class<?> gasClass = Class.forName("mekanism.api.gas.Gas");
            cachedGasGetName = gasClass.getMethod("getName");
        } catch (Throwable e) {
            cachedGasGetName = null;
        }
        try {
            cachedDummyGasItemClass = Class.forName("com.mekeng.github.common.item.ItemDummyGas");
            cachedDummyGasSetGas = cachedDummyGasItemClass.getMethod("setGasStack", ItemStack.class, Class.forName("mekanism.api.gas.GasStack"));
            cachedDummyGasGetGas = cachedDummyGasItemClass.getMethod("getGasStack", ItemStack.class);
        } catch (Throwable e) {
            cachedDummyGasItemClass = null;
            cachedDummyGasSetGas = null;
            cachedDummyGasGetGas = null;
        }
        try {
            Class<?> gasRegistryClass = Class.forName("mekanism.api.gas.GasRegistry");
            cachedGasRegistryGetGas = gasRegistryClass.getMethod("getGas", String.class);
        } catch (Throwable e) {
            cachedGasRegistryGetGas = null;
        }
    }

    private static void initFakeFluidMethods() {
        if (fakeFluidMethodsReady) {
            return;
        }
        fakeFluidMethodsReady = true;
        try {
            Class<?> fakeFluids = Class.forName("com.glodblock.github.common.item.fake.FakeFluids");
            cachedFakeFluidCheck = fakeFluids.getMethod("isFluidFakeItem", ItemStack.class);
            cachedFakeFluidDisplay = fakeFluids.getMethod("displayFluid", FluidStack.class);
        } catch (Exception e) {
            cachedFakeFluidCheck = null;
            cachedFakeFluidDisplay = null;
        }
        try {
            Class<?> fakeItemRegister = Class.forName("com.glodblock.github.common.item.fake.FakeItemRegister");
            cachedFakeItemGetStack = fakeItemRegister.getMethod("getStack", ItemStack.class);
        } catch (Exception e) {
            cachedFakeItemGetStack = null;
        }
    }

    private static void initFakeGasMethods() {
        if (fakeGasMethodsReady) {
            return;
        }
        fakeGasMethodsReady = true;
        initFakeFluidMethods();
        // 如果 Mekanism 未安装，直接跳过对气体相关反射的加载，避免在缺失依赖时触发 ClassNotFound/NoClassDefFound 错误
        if (!Loader.isModLoaded("mekanism")) {
            cachedFakeGasCheck = null;
            cachedFakeGasDisplay = null;
            return;
        }
        try {
            Class.forName("mekanism.api.gas.GasStack");
            Class<?> fakeGases = Class.forName("com.glodblock.github.integration.mek.FakeGases");
            cachedFakeGasCheck = fakeGases.getMethod("isGasFakeItem", ItemStack.class);
            cachedFakeGasDisplay = fakeGases.getMethod("displayGas", Class.forName("mekanism.api.gas.GasStack"));
        } catch (Throwable e) {
            cachedFakeGasCheck = null;
            cachedFakeGasDisplay = null;
        }
    }

    private boolean isFakeFluidItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        initFakeFluidMethods();
        if (cachedFakeFluidCheck == null) {
            return false;
        }
        try {
            Object result = cachedFakeFluidCheck.invoke(null, stack);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }

    private FluidStack getFakeFluidStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        initFakeFluidMethods();
        if (cachedFakeItemGetStack == null) {
            return null;
        }
        try {
            Object result = cachedFakeItemGetStack.invoke(null, stack);
            if (result instanceof FluidStack) {
                return (FluidStack) result;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private Object getFakeGasStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        initFakeGasMethods();
        if (cachedFakeItemGetStack == null || cachedGasStackGetGas == null) {
            return null;
        }
        try {
            Object result = cachedFakeItemGetStack.invoke(null, stack);
            if (result != null && cachedGasStackGetGas.getDeclaringClass().isInstance(result)) {
                return result;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private boolean isGasItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        initGasMethods();
        if (cachedDummyGasItemClass != null && cachedDummyGasItemClass.isInstance(stack.getItem())) {
            return true;
        }
        if (cachedGasItemClass == null) {
            return false;
        }
        return cachedGasItemClass.isInstance(stack.getItem());
    }

    private boolean isGasMarkerStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        initFakeGasMethods();
        if (cachedFakeGasCheck != null) {
            try {
                Object result = cachedFakeGasCheck.invoke(null, stack);
                if (result instanceof Boolean && (Boolean) result) {
                    return true;
                }
            } catch (Exception e) {
                // Ignore and continue.
            }
        }
        if (cachedDummyGasItemClass != null && cachedDummyGasItemClass.isInstance(stack.getItem())) {
            return true;
        }
        if (!stack.hasTagCompound()) {
            return false;
        }
        NBTTagCompound tag = stack.getTagCompound();
        return tag.getBoolean("TechStartGasMarker") || tag.hasKey("TechStartGasName");
    }

    private ItemStack createFluidMarkerStack(String fluidName, int amount) {
        if (fluidName == null || fluidName.isEmpty()) {
            return ItemStack.EMPTY;
        }
        net.minecraftforge.fluids.Fluid fluid = FluidRegistry.getFluid(fluidName);
        if (fluid == null) {
            return ItemStack.EMPTY;
        }
        initFakeFluidMethods();
        if (cachedFakeFluidDisplay == null) {
            return ItemStack.EMPTY;
        }
        try {
            FluidStack fluidStack = new FluidStack(fluid, Math.max(1, amount));
            Object result = cachedFakeFluidDisplay.invoke(null, fluidStack);
            if (result instanceof ItemStack) {
                ItemStack marker = (ItemStack) result;
                NBTTagCompound tag = marker.hasTagCompound() ? marker.getTagCompound() : new NBTTagCompound();
                tag.setInteger("TechStartFluidAmount", Math.max(1, amount));
                marker.setTagCompound(tag);
                return marker;
            }
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }


    /**
     * 流体信息容器类
     */
    private static class FluidInfo {
        String fluidName;
        int amount;

        FluidInfo(String fluidName, int amount) {
            this.fluidName = fluidName;
            this.amount = amount;
        }
    }

    /**
     * 气体信息容器类
     */
    private static class GasInfo {
        String gasName;
        int amount;

        GasInfo(String gasName, int amount) {
            this.gasName = gasName;
            this.amount = amount;
        }
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

        // 收集所有同时存在输入/输出类型的材料，生成类比配方
        java.util.Set<String> inputMaterials = new java.util.LinkedHashSet<>();
        java.util.Set<String> outputMaterials = new java.util.LinkedHashSet<>();
        for (String oreName : OreDictionary.getOreNames()) {
            if (oreName.startsWith(inputBaseType)) {
                inputMaterials.add(oreName.substring(inputBaseType.length()));
            }
            if (oreName.startsWith(outputBaseType)) {
                outputMaterials.add(oreName.substring(outputBaseType.length()));
            }
        }
        inputMaterials.retainAll(outputMaterials);

        if (inputMaterials.isEmpty()) {
            return derivedRecipes;
        }

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
                derivedRecipes.add(new ItemStack[]{inputItems.get(0), outputItems.get(0)});
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
        List<ItemStack> inputStacks = new ArrayList<>();
        List<ItemStack> outputStacks = new ArrayList<>();

        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = inputInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                inputStacks.add(stack);
            }
        }

        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack stack = outputInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                outputStacks.add(stack);
            }
        }

        if (inputStacks.isEmpty() || outputStacks.isEmpty()) {
            return;
        }

        String material = null;
        List<String> inputOres = new ArrayList<>();
        List<Integer> inputCounts = new ArrayList<>();

        for (ItemStack stack : inputStacks) {
            String oreName = getOreName(stack);
            if (oreName == null) {
                return;
            }
            String baseType = extractBaseType(oreName);
            String stackMaterial = extractMaterial(oreName);
            if (baseType == null || stackMaterial == null) {
                return;
            }
            if (material == null) {
                material = stackMaterial;
            } else if (!material.equals(stackMaterial)) {
                return;
            }
            inputOres.add(baseType + "*");
            inputCounts.add(getItemMarkerAmount(stack));
        }

        List<String> outputOres = new ArrayList<>();
        List<Integer> outputCounts = new ArrayList<>();

        for (ItemStack stack : outputStacks) {
            String oreName = getOreName(stack);
            if (oreName == null) {
                return;
            }
            String baseType = extractBaseType(oreName);
            String stackMaterial = extractMaterial(oreName);
            if (baseType == null || stackMaterial == null) {
                return;
            }
            if (material == null) {
                material = stackMaterial;
            } else if (!material.equals(stackMaterial)) {
                return;
            }
            outputOres.add(baseType + "*");
            outputCounts.add(getItemMarkerAmount(stack));
        }

        String displayName = inputStacks.get(0).getDisplayName() + " → " + outputStacks.get(0).getDisplayName();

        ItemStack patternStack = getPatternStack();
        if (!patternStack.isEmpty() && patternStack.getItem() instanceof ItemTest) {
            ItemTest patternItem = (ItemTest) patternStack.getItem();
            patternItem.setEncodedItem(patternStack, inputOres, inputCounts, outputOres, outputCounts, displayName);
        }
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        
        // 在返还物品之前，尝试编码样板（包括流体）
        if (!playerIn.world.isRemote) {
            try {
                encodePatternOnClose();
            } catch (Exception e) {
                // 编码失败，继续返还物品
            }
        }
        
        // 当容器关闭时，返还输入和输出槽位中的物品
        if (!playerIn.world.isRemote) {
            // 确保所有槽位的物品都会被处理，即使其中一个出错也不影响其他
            for (int i = 0; i < INPUT_SLOTS; i++) {
                try {
                    ItemStack inputStack = inputInventory.removeStackFromSlot(i);
                    if (!inputStack.isEmpty()) {
                        if (isMarkerStack(inputStack)) {
                            continue;
                        }
                        if (!playerIn.inventory.addItemStackToInventory(inputStack)) {
                            playerIn.dropItem(inputStack, false);
                        }
                    }
                } catch (Exception e) {
                    // 输入返还失败，继续处理其他槽位
                }
            }

            for (int i = 0; i < OUTPUT_SLOTS; i++) {
                try {
                    ItemStack outputStack = outputInventory.removeStackFromSlot(i);
                    if (!outputStack.isEmpty()) {
                        if (isMarkerStack(outputStack)) {
                            continue;
                        }
                        if (!playerIn.inventory.addItemStackToInventory(outputStack)) {
                            playerIn.dropItem(outputStack, false);
                        }
                    }
                } catch (Exception e) {
                    // 输出返还失败，继续处理其他槽位
                }
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

    /**
     * 关闭GUI时编码样板（支持纯流体或混合模式）
     */
    private void encodePatternOnClose() {
        ItemStack patternStack = getPatternStack();
        if (patternStack.isEmpty() || !(patternStack.getItem() instanceof ItemTest)) {
            return;
        }

        ItemTest patternItem = (ItemTest) patternStack.getItem();
        
        // 收集所有输入物品的矿辞、流体和气体
        List<String> inputOres = new ArrayList<>();
        List<Integer> inputCounts = new ArrayList<>();
        List<String> inputFluids = new ArrayList<>();
        List<Integer> inputFluidAmounts = new ArrayList<>();
        List<String> inputGases = new ArrayList<>();
        List<Integer> inputGasAmounts = new ArrayList<>();
        List<ItemStack> inputGasItems = new ArrayList<>();

        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = getInputStack(i);
            if (!stack.isEmpty()) {
                FluidInfo fluidInfo = extractFluidFromStack(stack);
                if (fluidInfo != null && fluidInfo.fluidName != null && !fluidInfo.fluidName.isEmpty()) {
                    inputFluids.add(fluidInfo.fluidName);
                    inputFluidAmounts.add(fluidInfo.amount);
                    continue;
                }
                GasInfo gasInfo = extractGasFromStack(stack);
                if (gasInfo != null && gasInfo.gasName != null && !gasInfo.gasName.isEmpty()) {
                    inputGases.add(gasInfo.gasName);
                    inputGasAmounts.add(gasInfo.amount);
                    inputGasItems.add(stripGasMarkerTag(stack.copy()));
                    continue;
                }
                // 普通物品，获取矿辞
                String oreName = getOreName(stack);
                if (oreName != null && !oreName.isEmpty()) {
                    String wildcardOre = toWildcardPattern(oreName);
                    inputOres.add(wildcardOre);
                    inputCounts.add(getItemMarkerAmount(stack));
                }
            }
        }

        // 收集所有输出物品的矿辞、流体和气体
        List<String> outputOres = new ArrayList<>();
        List<Integer> outputCounts = new ArrayList<>();
        List<String> outputFluids = new ArrayList<>();
        List<Integer> outputFluidAmounts = new ArrayList<>();
        List<String> outputGases = new ArrayList<>();
        List<Integer> outputGasAmounts = new ArrayList<>();
        List<ItemStack> outputGasItems = new ArrayList<>();

        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack stack = getOutputStack(i);
            if (!stack.isEmpty()) {
                FluidInfo fluidInfo = extractFluidFromStack(stack);
                if (fluidInfo != null && fluidInfo.fluidName != null && !fluidInfo.fluidName.isEmpty()) {
                    outputFluids.add(fluidInfo.fluidName);
                    outputFluidAmounts.add(fluidInfo.amount);
                    continue;
                }
                GasInfo gasInfo = extractGasFromStack(stack);
                if (gasInfo != null && gasInfo.gasName != null && !gasInfo.gasName.isEmpty()) {
                    outputGases.add(gasInfo.gasName);
                    outputGasAmounts.add(gasInfo.amount);
                    outputGasItems.add(stripGasMarkerTag(stack.copy()));
                    continue;
                }
                // 普通物品，获取矿辞
                String oreName = getOreName(stack);
                if (oreName != null && !oreName.isEmpty()) {
                    String wildcardOre = toWildcardPattern(oreName);
                    outputOres.add(wildcardOre);
                    outputCounts.add(getItemMarkerAmount(stack));
                }
            }
        }

        // 只有当至少有一个输入或输出时才编码
        if (!inputOres.isEmpty() || !outputOres.isEmpty() || !inputFluids.isEmpty() || !outputFluids.isEmpty()
            || !inputGases.isEmpty() || !outputGases.isEmpty()) {
            String displayName = buildDisplayName(inputOres, inputFluids, inputGases, outputOres, outputFluids, outputGases);

            // 使用带流体/气体的编码方法
                patternItem.setEncodedItemWithFluidsAndGases(patternStack, inputOres, inputCounts, outputOres, outputCounts,
                    inputFluids, inputFluidAmounts, outputFluids, outputFluidAmounts,
                    inputGases, inputGasAmounts, outputGases, outputGasAmounts,
                    inputGasItems, outputGasItems, displayName);
        }
    }

    /**
     * 构建显示名称
     */
    private String buildDisplayName(List<String> inputOres, List<String> inputFluids, List<String> inputGases,
                                     List<String> outputOres, List<String> outputFluids, List<String> outputGases) {
        StringBuilder sb = new StringBuilder();
        
        if (!inputOres.isEmpty()) {
            sb.append(inputOres.get(0).replace("*", ""));
        }
        if (!inputFluids.isEmpty()) {
            if (sb.length() > 0) sb.append("+");
            sb.append(inputFluids.get(0));
        }
        if (!inputGases.isEmpty()) {
            if (sb.length() > 0) sb.append("+");
            sb.append(inputGases.get(0));
        }
        
        sb.append(" → ");
        
        if (!outputOres.isEmpty()) {
            sb.append(outputOres.get(0).replace("*", ""));
        }
        if (!outputFluids.isEmpty()) {
            if (!outputOres.isEmpty()) sb.append("+");
            sb.append(outputFluids.get(0));
        }
        if (!outputGases.isEmpty()) {
            if (!outputOres.isEmpty() || !outputFluids.isEmpty()) sb.append("+");
            sb.append(outputGases.get(0));
        }
        return sb.toString();
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, net.minecraft.inventory.ClickType clickTypeIn, EntityPlayer player) {
        if (slotId >= 0 && slotId < this.inventorySlots.size() && isPatternSlotId(slotId)) {
            Slot slot = this.inventorySlots.get(slotId);
            if (clickTypeIn != net.minecraft.inventory.ClickType.PICKUP) {
                return player.inventory.getItemStack();
            }
            if (clickTypeIn == net.minecraft.inventory.ClickType.PICKUP) {
                boolean handled = false;
                ItemStack slotStack = slot.getStack();
                if (!slotStack.isEmpty() && isMarkerStack(slotStack)) {
                    if (dragType == 0 || dragType == 1) {
                        slot.putStack(ItemStack.EMPTY);
                        slot.onSlotChanged();
                        handled = true;
                    }
                }
                if (!handled && (dragType == 0 || dragType == 1) && slotStack.isEmpty()) {
                    ItemStack carried = player.inventory.getItemStack();
                    if (!carried.isEmpty() && !isMarkerStack(carried)) {
                        FluidInfo carriedFluid = extractFluidFromContainer(carried);
                        if (carriedFluid != null && carriedFluid.fluidName != null && !carriedFluid.fluidName.isEmpty()) {
                            ItemStack marker = createFluidMarkerStack(carriedFluid.fluidName, DEFAULT_FLUID_AMOUNT);
                            if (!marker.isEmpty()) {
                                slot.putStack(marker);
                                slot.onSlotChanged();
                                handled = true;
                            }
                        } else {
                            GasInfo carriedGas = extractGasFromContainer(carried);
                            if (carriedGas != null && carriedGas.gasName != null && !carriedGas.gasName.isEmpty()) {
                                ItemStack marker = createGasMarkerStack(carried, carriedGas);
                                if (!marker.isEmpty()) {
                                    slot.putStack(marker);
                                    slot.onSlotChanged();
                                    handled = true;
                                }
                            }
                        }
                        if (!handled && isOreDictItem(carried)) {
                            ItemStack marker = createItemMarkerStack(carried, 1);
                            if (!marker.isEmpty()) {
                                slot.putStack(marker);
                                slot.onSlotChanged();
                                handled = true;
                            }
                        }
                    }
                }
                if (handled) {
                    markDirty();
                    return player.inventory.getItemStack();
                }
            }
        }
        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }

    @Override
    public boolean canDragIntoSlot(Slot slotIn) {
        if (slotIn != null && isPatternSlotId(slotIn.slotNumber)) {
            return false;
        }
        return super.canDragIntoSlot(slotIn);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            if (index < TOTAL_PATTERN_SLOTS && isMarkerStack(itemstack1)) {
                return ItemStack.EMPTY;
            }
            if (index >= TOTAL_PATTERN_SLOTS) {
                return ItemStack.EMPTY;
            }
            itemstack = itemstack1.copy();

            if (index < TOTAL_PATTERN_SLOTS) {
                // 从容器槽位(0-11)移到玩家背包(12-47)
                if (!this.mergeItemStack(itemstack1, TOTAL_PATTERN_SLOTS, TOTAL_PATTERN_SLOTS + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < TOTAL_PATTERN_SLOTS + 36) {
                // 从玩家背包(12-47)移到容器槽位(0-11)
                if (isOreDictItem(itemstack1)) {
                    // 合并到输入/输出槽位
                    if (!this.mergeItemStack(itemstack1, 0, TOTAL_PATTERN_SLOTS, false)) {
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
            if (stack.isEmpty()) return false;
            return isMarkerStack(stack);
        }

        @Override
        public int getSlotStackLimit() {
            return 64;
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
            if (!container.player.world.isRemote) {
                container.encodePatternOnClose();
                container.syncPatternToClient();
            }
        }
    }

    public boolean isPatternSlotId(int slotId) {
        return slotId >= 0 && slotId < TOTAL_PATTERN_SLOTS;
    }

    public boolean isInputSlotId(int slotId) {
        return slotId >= 0 && slotId < INPUT_SLOTS;
    }

    public boolean isOutputSlotId(int slotId) {
        return slotId >= INPUT_SLOTS && slotId < TOTAL_PATTERN_SLOTS;
    }

    public boolean isFluidMarkerStack(ItemStack stack) {
        return isFakeFluidItem(stack) || isGasMarkerStack(stack);
    }

    public boolean isMarkerStack(ItemStack stack) {
        return isFakeFluidItem(stack) || isGasMarkerStack(stack) || isItemMarkerStack(stack);
    }

    public boolean isItemMarkerStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return false;
        }
        return stack.getTagCompound().getBoolean(TAG_ITEM_MARKER);
    }

    public int getMarkerAmountForSlot(int slotId) {
        if (!isPatternSlotId(slotId)) {
            return 1;
        }
        Slot slot = this.inventorySlots.get(slotId);
        if (slot == null) {
            return 1;
        }
        ItemStack stack = slot.getStack();
        if (isItemMarkerStack(stack)) {
            return getItemMarkerAmount(stack);
        }
        return getFluidAmountForSlot(slotId);
    }

    public int getFluidAmountForSlot(int slotId) {
        if (!isPatternSlotId(slotId)) {
            return DEFAULT_FLUID_AMOUNT;
        }
        Slot slot = this.inventorySlots.get(slotId);
        if (slot == null) {
            return DEFAULT_FLUID_AMOUNT;
        }
        FluidInfo info = extractFluidFromStack(slot.getStack());
        if (info == null || info.amount <= 0) {
            GasInfo gasInfo = extractGasFromStack(slot.getStack());
            if (gasInfo == null || gasInfo.amount <= 0) {
                return DEFAULT_FLUID_AMOUNT;
            }
            return gasInfo.amount;
        }
        return info.amount;
    }

    public void applyMarkerAmountToSlot(int slotId, int amount) {
        if (!isPatternSlotId(slotId)) {
            return;
        }
        Slot slot = this.inventorySlots.get(slotId);
        if (slot == null) {
            return;
        }
        ItemStack current = slot.getStack();
        if (isItemMarkerStack(current)) {
            ItemStack marker = createItemMarkerStack(current, Math.max(1, amount));
            if (marker.isEmpty()) {
                return;
            }
            slot.putStack(marker);
            slot.onSlotChanged();
            if (!player.world.isRemote) {
                encodePatternOnClose();
                syncPatternToClient();
            }
            return;
        }
        if (isFakeFluidItem(current)) {
            FluidInfo info = extractFluidFromStack(current);
            if (info == null || info.fluidName == null || info.fluidName.isEmpty()) {
                return;
            }
            ItemStack marker = createFluidMarkerStack(info.fluidName, Math.max(1, amount));
            if (marker.isEmpty()) {
                return;
            }
            slot.putStack(marker);
            slot.onSlotChanged();
            if (!player.world.isRemote) {
                encodePatternOnClose();
                syncPatternToClient();
            }
            return;
        }
        if (isGasMarkerStack(current)) {
            GasInfo info = extractGasFromStack(current);
            if (info == null || info.gasName == null || info.gasName.isEmpty()) {
                return;
            }
            ItemStack marker = createGasMarkerStack(current, new GasInfo(info.gasName, Math.max(1, amount)));
            if (marker.isEmpty()) {
                return;
            }
            slot.putStack(marker);
            slot.onSlotChanged();
            if (!player.world.isRemote) {
                encodePatternOnClose();
                syncPatternToClient();
            }
        }
    }

    private void syncPatternToClient() {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP mp = (EntityPlayerMP) player;
        mp.inventory.markDirty();
        detectAndSendChanges();

        int hotbarSlot = mp.inventory.currentItem;
        int containerSlot = TOTAL_PATTERN_SLOTS + 27 + hotbarSlot;
        if (containerSlot >= 0 && containerSlot < this.inventorySlots.size()) {
            ItemStack stack = mp.inventory.getStackInSlot(hotbarSlot);
            mp.connection.sendPacket(new SPacketSetSlot(this.windowId, containerSlot, stack));
        }

        ItemStack offhand = mp.getHeldItemOffhand();
        if (!offhand.isEmpty() && offhand.getItem() instanceof ItemTest) {
            mp.connection.sendPacket(new SPacketSetSlot(0, 45, offhand));
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
            if (stack.isEmpty()) return false;
            return isMarkerStack(stack);
        }

        @Override
        public int getSlotStackLimit() {
            return 64;
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
            if (!container.player.world.isRemote) {
                container.encodePatternOnClose();
                container.syncPatternToClient();
            }
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
        private final NonNullList<ItemStack> stacks;

        private PatternInputInventory(int size) {
            this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
        }

        @Override
        public int getSizeInventory() {
            return stacks.size();
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack stack : stacks) {
                if (!stack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            if (index >= 0 && index < stacks.size()) {
                return stacks.get(index);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack decrStackSize(int index, int count) {
            if (index >= 0 && index < stacks.size()) {
                ItemStack stack = stacks.get(index);
                if (!stack.isEmpty()) {
                    ItemStack itemstack = stack.splitStack(count);
                    if (stack.isEmpty()) {
                        stacks.set(index, ItemStack.EMPTY);
                    }
                    markDirty();
                    return itemstack;
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeStackFromSlot(int index) {
            if (index >= 0 && index < stacks.size()) {
                ItemStack itemstack = stacks.get(index);
                stacks.set(index, ItemStack.EMPTY);
                markDirty();
                return itemstack;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
            if (index >= 0 && index < stacks.size()) {
                stacks.set(index, stack);
                markDirty();
            }
        }

        @Override
        public int getInventoryStackLimit() {
            return 64;
        }

        @Override
        public void markDirty() {
            // 编码逻辑由槽位变更触发
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
            for (int i = 0; i < stacks.size(); i++) {
                stacks.set(i, ItemStack.EMPTY);
            }
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
        private final NonNullList<ItemStack> stacks;

        private PatternOutputInventory(int size) {
            this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
        }

        @Override
        public int getSizeInventory() {
            return stacks.size();
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack stack : stacks) {
                if (!stack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            if (index >= 0 && index < stacks.size()) {
                return stacks.get(index);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack decrStackSize(int index, int count) {
            if (index >= 0 && index < stacks.size()) {
                ItemStack stack = stacks.get(index);
                if (!stack.isEmpty()) {
                    ItemStack itemstack = stack.splitStack(count);
                    if (stack.isEmpty()) {
                        stacks.set(index, ItemStack.EMPTY);
                    }
                    markDirty();
                    return itemstack;
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeStackFromSlot(int index) {
            if (index >= 0 && index < stacks.size()) {
                ItemStack itemstack = stacks.get(index);
                stacks.set(index, ItemStack.EMPTY);
                markDirty();
                return itemstack;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
            if (index >= 0 && index < stacks.size()) {
                stacks.set(index, stack);
                markDirty();
            }
        }

        @Override
        public int getInventoryStackLimit() {
            return 64;
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
            for (int i = 0; i < stacks.size(); i++) {
                stacks.set(i, ItemStack.EMPTY);
            }
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


