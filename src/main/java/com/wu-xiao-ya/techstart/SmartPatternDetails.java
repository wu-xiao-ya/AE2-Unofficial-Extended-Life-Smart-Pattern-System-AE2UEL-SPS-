package com.lwx1145.techstart;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.World;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * 智能样板详情实现 - 为AE2UEL提供样板信息
 * 根据矿物辞典条目生成对应的合成配方
 * 支持单个配方对，每个样板对应一个特定的输入输出配方
 */
public class SmartPatternDetails implements ICraftingPatternDetails {

    private static java.lang.reflect.Method cachedGasRegistryGetGas;
    private static java.lang.reflect.Constructor<?> cachedGasStackCtor;
    private static java.lang.reflect.Method cachedFakeGasPack;
    private static boolean gasHelperReady = false;

    private final ItemStack patternStack;
    private final List<String> inputOreNames;
    private final List<String> outputOreNames;
    private final List<Integer> inputCounts;
    private final List<Integer> outputCounts;
    // 流体相关字段
    private final List<String> inputFluids;
    private final List<String> outputFluids;
    private final List<Integer> inputFluidAmounts;
    private final List<Integer> outputFluidAmounts;
    // 气体相关字段
    private final List<String> inputGases;
    private final List<String> outputGases;
    private final List<Integer> inputGasAmounts;
    private final List<Integer> outputGasAmounts;
    private final List<ItemStack> inputGasItems;
    private final List<ItemStack> outputGasItems;
    private final String inputOreName;
    private final String outputOreName;
    private final String displayName;
    private int priority = 100; // 默认优先级
    private final boolean isVirtual; // 是否是虚拟样板（单个材料）
    private final int inputCount; // 输入数量（兼容字段）
    private final int outputCount; // 输出数量（兼容字段）

    public SmartPatternDetails(ItemStack patternStack) {
        this.patternStack = patternStack;
        this.inputOreNames = ItemTest.getInputOreNamesStatic(patternStack);
        this.outputOreNames = ItemTest.getOutputOreNamesStatic(patternStack);
        this.inputCounts = ItemTest.getInputCountsStatic(patternStack);
        this.outputCounts = ItemTest.getOutputCountsStatic(patternStack);
        this.inputFluids = ItemTest.getInputFluidsStatic(patternStack);
        this.outputFluids = ItemTest.getOutputFluidsStatic(patternStack);
        this.inputFluidAmounts = ItemTest.getInputFluidAmountsStatic(patternStack);
        this.outputFluidAmounts = ItemTest.getOutputFluidAmountsStatic(patternStack);
        this.inputGases = ItemTest.getInputGasesStatic(patternStack);
        this.outputGases = ItemTest.getOutputGasesStatic(patternStack);
        this.inputGasAmounts = ItemTest.getInputGasAmountsStatic(patternStack);
        this.outputGasAmounts = ItemTest.getOutputGasAmountsStatic(patternStack);
        this.inputGasItems = ItemTest.getInputGasItemsStatic(patternStack);
        this.outputGasItems = ItemTest.getOutputGasItemsStatic(patternStack);
        this.inputOreName = this.inputOreNames.isEmpty() ? "" : this.inputOreNames.get(0);
        this.outputOreName = this.outputOreNames.isEmpty() ? "" : this.outputOreNames.get(0);
        this.displayName = ItemTest.getEncodedItemNameStatic(patternStack);
        this.inputCount = ItemTest.getInputCountStatic(patternStack);
        this.outputCount = ItemTest.getOutputCountStatic(patternStack);
        this.isVirtual = patternStack.hasTagCompound() &&
            (patternStack.getTagCompound().hasKey("VirtualInputOreNames") ||
             patternStack.getTagCompound().hasKey("VirtualInputOreName"));
    }

    /**
     * 虚拟样板构造函数 - 用于展开通配符样板为具体材料
     * 不缓存具体物品，而是缓存矿物辞典名称，在需要时动态获取
     */
    public SmartPatternDetails(ItemStack patternStack, String specificInputOre, String specificOutputOre, String specificDisplayName) {
        this.patternStack = patternStack;
        this.inputOreNames = new ArrayList<>();
        this.outputOreNames = new ArrayList<>();
        this.inputCounts = ItemTest.getInputCountsStatic(patternStack);
        this.outputCounts = ItemTest.getOutputCountsStatic(patternStack);
        // 虚拟样板从原样板复制流体
        this.inputFluids = ItemTest.getInputFluidsStatic(patternStack);
        this.outputFluids = ItemTest.getOutputFluidsStatic(patternStack);
        this.inputFluidAmounts = ItemTest.getInputFluidAmountsStatic(patternStack);
        this.outputFluidAmounts = ItemTest.getOutputFluidAmountsStatic(patternStack);
        // 虚拟样板从原样板复制气体
        this.inputGases = ItemTest.getInputGasesStatic(patternStack);
        this.outputGases = ItemTest.getOutputGasesStatic(patternStack);
        this.inputGasAmounts = ItemTest.getInputGasAmountsStatic(patternStack);
        this.outputGasAmounts = ItemTest.getOutputGasAmountsStatic(patternStack);
        this.inputGasItems = ItemTest.getInputGasItemsStatic(patternStack);
        this.outputGasItems = ItemTest.getOutputGasItemsStatic(patternStack);
        this.inputOreNames.add(specificInputOre);
        this.outputOreNames.add(specificOutputOre);
        this.inputOreName = specificInputOre;
        this.outputOreName = specificOutputOre;
        this.displayName = specificDisplayName;
        this.inputCount = ItemTest.getInputCountStatic(patternStack);
        this.outputCount = ItemTest.getOutputCountStatic(patternStack);
        this.isVirtual = true;
    }

    @Override
    public ItemStack getPattern() {
        return patternStack.copy();
    }

    @Override
    public boolean isValidItemForSlot(int slot, ItemStack itemStack, World world) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        if (slot < 0 || slot >= inputOreNames.size()) {
            return false;
        }
        return matchesOrePattern(itemStack, inputOreNames.get(slot));
    }

    @Override
    public boolean isCraftable() {
        return true; // 智能样板总是可合成的
    }

    @Override
    public IAEItemStack[] getInputs() {
        List<ItemStack> inputs = buildStacksForOreList(inputOreNames, inputCounts, isVirtual);
        List<IAEItemStack> resultList = new ArrayList<>();
        for (ItemStack stack : inputs) {
            resultList.add(AEItemStack.fromItemStack(stack));
        }
        resultList.addAll(buildFluidAeStacks(inputFluids, inputFluidAmounts));
        resultList.addAll(buildGasItemAeStacks(inputGasItems, inputGases, inputGasAmounts));
        if (resultList.isEmpty()) {
            return new IAEItemStack[0];
        }
        return resultList.toArray(new IAEItemStack[0]);
    }

    @Override
    public IAEItemStack[] getCondensedInputs() {
        // 对于多输入样板，返回所有输入而不是只返回第一个
        // AE2UEL可能使用这个方法来提取需要的物品
        IAEItemStack[] inputs = getInputs();
        return inputs;
    }

    @Override
    public IAEItemStack[] getCondensedOutputs() {
        // 对于多输出样板，返回所有输出而不是只返回第一个
        IAEItemStack[] outputs = getOutputs();
        return outputs;
    }

    @Override
    public IAEItemStack getPrimaryOutput() {
        IAEItemStack[] outputs = getOutputs();
        return outputs.length > 0 ? outputs[0] : null;
    }

    @Override
    public IAEItemStack[] getOutputs() {
        List<ItemStack> outputs = buildStacksForOreList(outputOreNames, outputCounts, isVirtual);
        List<IAEItemStack> resultList = new ArrayList<>();
        for (ItemStack stack : outputs) {
            resultList.add(AEItemStack.fromItemStack(stack));
        }
        resultList.addAll(buildFluidAeStacks(outputFluids, outputFluidAmounts));
        resultList.addAll(buildGasItemAeStacks(outputGasItems, outputGases, outputGasAmounts));
        if (resultList.isEmpty()) {
            return new IAEItemStack[0];
        }
        return resultList.toArray(new IAEItemStack[0]);
    }

    private List<IAEItemStack> buildGasItemAeStacks(List<ItemStack> gasItems, List<String> gasNames, List<Integer> gasAmounts) {
        List<IAEItemStack> result = new ArrayList<>();
        if (gasNames == null || gasNames.isEmpty()) {
            return result;
        }
        for (int i = 0; i < gasNames.size(); i++) {
            String gasName = gasNames.get(i);
            if (gasName == null || gasName.isEmpty()) {
                continue;
            }
            int amount = (gasAmounts != null && i < gasAmounts.size()) ? gasAmounts.get(i) : 0;
            if (amount <= 0) {
                continue;
            }
            IAEItemStack aeStack = createGasAeStack(gasName, amount);
            if (aeStack != null) {
                result.add(aeStack);
            }
        }
        return result;
    }

    private static void initGasHelper() {
        if (gasHelperReady) {
            return;
        }
        gasHelperReady = true;
        try {
            Class<?> gasStackClass = Class.forName("mekanism.api.gas.GasStack");
            cachedGasStackCtor = gasStackClass.getConstructor(Class.forName("mekanism.api.gas.Gas"), int.class);
        } catch (Exception e) {
            cachedGasStackCtor = null;
        }
        try {
            Class<?> gasRegistryClass = Class.forName("mekanism.api.gas.GasRegistry");
            cachedGasRegistryGetGas = gasRegistryClass.getMethod("getGas", String.class);
        } catch (Exception e) {
            cachedGasRegistryGetGas = null;
        }
        try {
            Class<?> fakeGases = Class.forName("com.glodblock.github.integration.mek.FakeGases");
            cachedFakeGasPack = fakeGases.getMethod("packGas2AEDrops", Class.forName("mekanism.api.gas.GasStack"));
        } catch (Exception e) {
            cachedFakeGasPack = null;
        }
    }

    private static Object createGasStack(String gasName, int amount) {
        initGasHelper();
        if (cachedGasRegistryGetGas == null || cachedGasStackCtor == null) {
            return null;
        }
        try {
            Object gas = cachedGasRegistryGetGas.invoke(null, gasName);
            if (gas == null) {
                return null;
            }
            return cachedGasStackCtor.newInstance(gas, Math.max(1, amount));
        } catch (Exception e) {
            return null;
        }
    }

    private static IAEItemStack createGasAeStack(String gasName, int amount) {
        Object gasStack = createGasStack(gasName, amount);
        if (gasStack == null || cachedFakeGasPack == null) {
            return null;
        }
        try {
            Object packed = cachedFakeGasPack.invoke(null, gasStack);
            if (packed instanceof IAEItemStack) {
                IAEItemStack aeStack = (IAEItemStack) packed;
                aeStack.setStackSize(amount);
                return aeStack;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private List<IAEItemStack> buildFluidAeStacks(List<String> fluids, List<Integer> amounts) {
        List<IAEItemStack> result = new ArrayList<>();
        if (fluids == null || fluids.isEmpty()) {
            return result;
        }
        try {
            Class<?> fakeFluids = Class.forName("com.glodblock.github.common.item.fake.FakeFluids");
            java.lang.reflect.Method packMethod = fakeFluids.getMethod("packFluid2AEDrops", FluidStack.class);
            for (int i = 0; i < fluids.size(); i++) {
                String fluidName = fluids.get(i);
                if (fluidName == null || fluidName.isEmpty()) {
                    continue;
                }
                int amount = i < amounts.size() ? amounts.get(i) : 0;
                if (amount <= 0) {
                    continue;
                }
                net.minecraftforge.fluids.Fluid fluid = FluidRegistry.getFluid(fluidName);
                if (fluid == null) {
                    continue;
                }
                FluidStack fluidStack = new FluidStack(fluid, amount);
                Object packed = packMethod.invoke(null, fluidStack);
                if (packed instanceof IAEItemStack) {
                    result.add((IAEItemStack) packed);
                }
            }
        } catch (Exception e) {
            // AE2FC not present or reflection failed; ignore fluid entries.
        }
        return result;
    }

    @Override
    public boolean canSubstitute() {
        // 通配符样板不需要substitute机制（ASM Transformer已展开为19个虚拟样板）
        // 虚拟样板也不需要substitute
        return false;
    }

    @Override
    public List<IAEItemStack> getSubstituteInputs(int slot) {
        // 不提供替代输入
        return new ArrayList<>();
    }

    @Override
    public ItemStack getOutput(InventoryCrafting inventory, World world) {
        if (outputOreNames.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (isVirtual) {
            List<ItemStack> outputs = buildStacksForOreList(outputOreNames, outputCounts, true);
            return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0);
        }

        String material = deriveMaterialFromInventory(inventory);
        if (material == null) {
            return ItemStack.EMPTY;
        }

        String outputOre = replaceWildcard(outputOreNames.get(0), material);
        List<ItemStack> outputItems = OreDictionary.getOres(outputOre);
        if (!outputItems.isEmpty()) {
            ItemStack result = outputItems.get(0).copy();
            int count = outputCounts.isEmpty() ? 1 : outputCounts.get(0);
            result.setCount(count);
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getOreName() {
        if (inputOreNames.isEmpty() && outputOreNames.isEmpty()) {
            return "";
        }
        String inputName = inputOreNames.isEmpty() ? "" : inputOreNames.get(0);
        String outputName = outputOreNames.isEmpty() ? "" : outputOreNames.get(0);
        return inputName + "→" + outputName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public boolean isWildcardPattern() {
        for (String ore : inputOreNames) {
            if (ore.contains("*")) {
                return true;
            }
        }
        for (String ore : outputOreNames) {
            if (ore.contains("*")) {
                return true;
            }
        }
        return false;
    }

    public boolean isAllowedByFilter() {
        List<String> entries = ItemTest.getFilterEntriesStatic(patternStack);
        if (entries == null || entries.isEmpty()) {
            return true;
        }
        int mode = ItemTest.getFilterModeStatic(patternStack);
        String id = inputOreName + "->" + outputOreName;
        boolean contains = entries.contains(id);
        return mode == ItemTest.FILTER_MODE_BLACKLIST ? !contains : contains;
    }

    /**
     * 将通配符样板展开为多个虚拟样板（每个材料一个）
     * 仅用于通配符样板
     */
    public List<SmartPatternDetails> expandToVirtualPatterns() {
        List<SmartPatternDetails> virtualPatterns = new ArrayList<>();
        
        // 只有通配符样板需要展开
        if (!isWildcardPattern()) {
            virtualPatterns.add(this);
            return virtualPatterns;
        }

        List<String> materials = getWildcardMaterials();
        for (String material : materials) {
            List<String> specificInputs = new ArrayList<>();
            List<String> specificOutputs = new ArrayList<>();

            for (String ore : inputOreNames) {
                specificInputs.add(replaceWildcard(ore, material));
            }
            for (String ore : outputOreNames) {
                specificOutputs.add(replaceWildcard(ore, material));
            }

            ItemStack virtualStack = patternStack.copy();
            NBTTagCompound tag = new NBTTagCompound();
            if (virtualStack.hasTagCompound()) {
                tag = virtualStack.getTagCompound().copy();
            }

            NBTTagList inputOreList = new NBTTagList();
            for (String ore : specificInputs) {
                inputOreList.appendTag(new NBTTagString(ore));
            }
            NBTTagList outputOreList = new NBTTagList();
            for (String ore : specificOutputs) {
                outputOreList.appendTag(new NBTTagString(ore));
            }

            tag.setTag("VirtualInputOreNames", inputOreList);
            tag.setTag("VirtualOutputOreNames", outputOreList);
            tag.setString("VirtualDisplayName", material + " " + displayName);
            virtualStack.setTagCompound(tag);

            SmartPatternDetails virtualPattern = new SmartPatternDetails(virtualStack);
            virtualPatterns.add(virtualPattern);
        }
        
        return virtualPatterns;
    }

    /**
     * 获取当前类别下的所有配方
     */
    private List<String[]> getCategoryRecipes() {
        List<String[]> recipes = new ArrayList<>();

        // 如果是虚拟样板（具体材料），直接返回该材料的配方
        if (isVirtual) {
            if (!inputOreNames.isEmpty() && !outputOreNames.isEmpty()) {
                recipes.add(new String[]{inputOreNames.get(0), outputOreNames.get(0), displayName});
            }
            return recipes;
        }

        // 空值检查
        if (inputOreNames.isEmpty() || outputOreNames.isEmpty()) {
            return new ArrayList<>();
        }

        if (inputOreNames.size() != 1 || outputOreNames.size() != 1) {
            return getWildcardRecipes();
        }

        // 如果是通配符形式，使用通配符逻辑
        if (inputOreName.contains("*") || outputOreName.contains("*")) {
            return getWildcardRecipes();
        }

        // 对于具体材料，遍历所有矿物辞典条目找到匹配的配方
        String[] allOreNames = OreDictionary.getOreNames();
        for (String oreName : allOreNames) {
            // 简单的匹配逻辑：如果矿物辞典名称包含输入或输出名称的材料部分
            if (oreName.contains(extractMaterial(inputOreName)) ||
                oreName.contains(extractMaterial(outputOreName))) {

                // 尝试构造对应的配方
                String potentialInput = oreName;
                String potentialOutput = oreName.replace(extractMaterial(inputOreName), extractMaterial(outputOreName));

                // 验证配方有效性
                if (isValidRecipe(potentialInput, potentialOutput)) {
                    List<ItemStack> inputItems = OreDictionary.getOres(potentialInput);
                    List<ItemStack> outputItems = OreDictionary.getOres(potentialOutput);

                    if (!inputItems.isEmpty() && !outputItems.isEmpty()) {
                        String displayName = inputItems.get(0).getDisplayName() + " → " + outputItems.get(0).getDisplayName();
                        recipes.add(new String[]{potentialInput, potentialOutput, displayName});
                    }
                }
            }
        }

        return recipes;
    }

    /**
     * 处理通配符形式的配方
     */
    private List<String[]> getWildcardRecipes() {
        List<String[]> recipes = new ArrayList<>();
        if (inputOreNames.isEmpty() || outputOreNames.isEmpty()) {
            return recipes;
        }

        String inputWildcard = inputOreNames.get(0);
        String outputWildcard = outputOreNames.get(0);
        List<String> materials = getWildcardMaterials();
        for (String material : materials) {
            String inputOre = replaceWildcard(inputWildcard, material);
            String outputOre = replaceWildcard(outputWildcard, material);
            if (!OreDictionary.doesOreNameExist(inputOre) || !OreDictionary.doesOreNameExist(outputOre)) {
                continue;
            }
            List<ItemStack> inputItems = OreDictionary.getOres(inputOre);
            List<ItemStack> outputItems = OreDictionary.getOres(outputOre);
            if (!inputItems.isEmpty() && !outputItems.isEmpty()) {
                String displayName = inputItems.get(0).getDisplayName() + " → " + outputItems.get(0).getDisplayName();
                recipes.add(new String[]{inputOre, outputOre, displayName});
            }
        }
        return recipes;
    }

    private List<ItemStack> buildStacksForOreList(List<String> oreNames, List<Integer> counts, boolean virtual) {
        List<ItemStack> stacks = new ArrayList<>();
        if (oreNames.isEmpty()) {
            return stacks;
        }

        String material = null;
        if (!virtual && isWildcardPattern()) {
            List<String> materials = getWildcardMaterials();
            if (materials.isEmpty()) {
                return stacks;
            }
            material = materials.get(0);
        }

        for (int i = 0; i < oreNames.size(); i++) {
            String oreName = oreNames.get(i);
            if (material != null) {
                oreName = replaceWildcard(oreName, material);
            }
            List<ItemStack> items = OreDictionary.getOres(oreName);
            if (items.isEmpty()) {
                continue;
            }
            ItemStack stack = items.get(0).copy();
            int count = i < counts.size() ? counts.get(i) : 1;
            stack.setCount(count);
            stacks.add(stack);
        }

        return stacks;
    }

    private String replaceWildcard(String oreName, String material) {
        if (oreName.contains("*")) {
            return oreName.replace("*", material);
        }
        return oreName;
    }

    private List<String> getWildcardMaterials() {
        List<String> materials = new ArrayList<>();
        if (inputOreNames.isEmpty() || outputOreNames.isEmpty()) {
            return materials;
        }

        String inputWildcard = inputOreNames.get(0);
        String outputWildcard = outputOreNames.get(0);
        List<String[]> recipes = OreDictRecipeCache.getRecipes(inputWildcard, outputWildcard);
        for (String[] recipe : recipes) {
            String material = extractMaterial(recipe[0]);
            if (material == null) {
                continue;
            }
            if (isMaterialValidForAll(material)) {
                materials.add(material);
            }
        }

        return materials;
    }

    private boolean isMaterialValidForAll(String material) {
        for (String ore : inputOreNames) {
            String specific = replaceWildcard(ore, material);
            if (!OreDictionary.doesOreNameExist(specific) || OreDictionary.getOres(specific).isEmpty()) {
                return false;
            }
        }
        for (String ore : outputOreNames) {
            String specific = replaceWildcard(ore, material);
            if (!OreDictionary.doesOreNameExist(specific) || OreDictionary.getOres(specific).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String deriveMaterialFromInventory(InventoryCrafting inventory) {
        String material = null;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            int[] oreIDs = OreDictionary.getOreIDs(stack);
            for (int oreID : oreIDs) {
                String oreName = OreDictionary.getOreName(oreID);
                String match = matchMaterialForOre(oreName);
                if (match == null) {
                    continue;
                }
                if (material == null) {
                    material = match;
                } else if (!material.equals(match)) {
                    return null;
                }
            }
        }
        return material;
    }

    private String matchMaterialForOre(String oreName) {
        for (String pattern : inputOreNames) {
            if (pattern.contains("*")) {
                String base = pattern.replace("*", "");
                if (oreName.startsWith(base)) {
                    return extractMaterial(oreName);
                }
            } else if (pattern.equals(oreName)) {
                return extractMaterial(oreName);
            }
        }
        return null;
    }

    private boolean matchesAnyInputOre(ItemStack itemStack, List<String> oreNames) {
        int[] oreIDs = OreDictionary.getOreIDs(itemStack);
        for (int oreID : oreIDs) {
            String oreName = OreDictionary.getOreName(oreID);
            for (String pattern : oreNames) {
                if (pattern.contains("*")) {
                    String base = pattern.replace("*", "");
                    if (oreName.startsWith(base)) {
                        return true;
                    }
                } else if (pattern.equals(oreName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesOrePattern(ItemStack itemStack, String pattern) {
        int[] oreIDs = OreDictionary.getOreIDs(itemStack);
        for (int oreID : oreIDs) {
            String oreName = OreDictionary.getOreName(oreID);
            if (pattern.contains("*")) {
                String base = pattern.replace("*", "");
                if (oreName.startsWith(base)) {
                    return true;
                }
            } else if (pattern.equals(oreName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 提取材料名称 - 改进版，支持更复杂的矿物辞典名称
     */
    private String extractMaterial(String oreName) {
        // 定义常见前缀及其长度
        String[] prefixes = {"ingot", "block", "nugget", "plate", "dust", "rod", "gear", "wire"};
        for (String prefix : prefixes) {
            if (oreName.startsWith(prefix)) {
                String material = oreName.substring(prefix.length());
                // 验证提取的材料名称不为空，且不包含其他前缀（避免误判）
                if (!material.isEmpty() && !startsWithAnyPrefix(material, prefixes)) {
                    return material;
                }
            }
        }
        // 如果无法提取，返回 null（表示无效）
        return null;
    }

    /**
     * 辅助方法：检查字符串是否以任何前缀开头
     */
    private boolean startsWithAnyPrefix(String str, String[] prefixes) {
        for (String prefix : prefixes) {
            if (str.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证配方是否有效
     */
    private boolean isValidRecipe(String inputOre, String outputOre) {
        // 基本验证：输出矿物辞典必须存在
        if (OreDictionary.getOres(outputOre).isEmpty()) {
            return false;
        }

        // 验证材料一致性
        String inputMaterial = extractMaterial(inputOre);
        String outputMaterial = extractMaterial(outputOre);

        if (inputMaterial == null || outputMaterial == null) {
            return false; // 无法提取材料名称
        }

        if (!inputMaterial.equals(outputMaterial)) {
            return false; // 输入输出材料不匹配
        }

        // 验证物品实际存在
        List<ItemStack> inputItems = OreDictionary.getOres(inputOre);
        List<ItemStack> outputItems = OreDictionary.getOres(outputOre);

        if (inputItems.isEmpty() || outputItems.isEmpty()) {
            return false;
        }

        // 验证物品有效性
        for (ItemStack input : inputItems) {
            if (input.isEmpty()) return false;
        }
        for (ItemStack output : outputItems) {
            if (output.isEmpty()) return false;
        }

        return true;
    }

    /**
     * 流体相关的getter方法
     */
    public List<String> getInputFluids() {
        return inputFluids;
    }

    public List<String> getOutputFluids() {
        return outputFluids;
    }

    public List<Integer> getInputFluidAmounts() {
        return inputFluidAmounts;
    }

    public List<Integer> getOutputFluidAmounts() {
        return outputFluidAmounts;
    }

    public List<String> getInputGases() {
        return inputGases;
    }

    public List<String> getOutputGases() {
        return outputGases;
    }

    public List<Integer> getInputGasAmounts() {
        return inputGasAmounts;
    }

    public List<Integer> getOutputGasAmounts() {
        return outputGasAmounts;
    }


    /**
     * 检查样板是否包含流体
     */
    public boolean hasInputFluids() {
        return !inputFluids.isEmpty();
    }

    public boolean hasOutputFluids() {
        return !outputFluids.isEmpty();
    }

    public boolean hasInputGases() {
        return !inputGases.isEmpty();
    }

    public boolean hasOutputGases() {
        return !outputGases.isEmpty();
    }


    /**
     * 获取总的输入凭证信息（包含物品和流体）
     * 用于显示在GUI中
     */
    public List<String> getAllInputDescriptions() {
        List<String> descriptions = new ArrayList<>();
        for (int i = 0; i < inputOreNames.size(); i++) {
            int count = i < inputCounts.size() ? inputCounts.get(i) : 1;
            descriptions.add(inputOreNames.get(i) + " ×" + count);
        }
        for (int i = 0; i < inputFluids.size(); i++) {
            int amount = i < inputFluidAmounts.size() ? inputFluidAmounts.get(i) : 0;
            descriptions.add(inputFluids.get(i) + " ×" + amount + "mB");
        }
        for (int i = 0; i < inputGases.size(); i++) {
            int amount = i < inputGasAmounts.size() ? inputGasAmounts.get(i) : 0;
            descriptions.add(inputGases.get(i) + " ×" + amount + "mB");
        }
        return descriptions;
    }

    /**
     * 获取总的输出凭证信息（包含物品和流体）
     */
    public List<String> getAllOutputDescriptions() {
        List<String> descriptions = new ArrayList<>();
        for (int i = 0; i < outputOreNames.size(); i++) {
            int count = i < outputCounts.size() ? outputCounts.get(i) : 1;
            descriptions.add(outputOreNames.get(i) + " ×" + count);
        }
        for (int i = 0; i < outputFluids.size(); i++) {
            int amount = i < outputFluidAmounts.size() ? outputFluidAmounts.get(i) : 0;
            descriptions.add(outputFluids.get(i) + " ×" + amount + "mB");
        }
        for (int i = 0; i < outputGases.size(); i++) {
            int amount = i < outputGasAmounts.size() ? outputGasAmounts.get(i) : 0;
            descriptions.add(outputGases.get(i) + " ×" + amount + "mB");
        }
        return descriptions;
    }
}
