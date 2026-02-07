package com.lwx1145.techstart;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.inventory.InventoryCrafting;
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

    private final ItemStack patternStack;
    private final String inputOreName;
    private final String outputOreName;
    private final String displayName;
    private int priority = 100; // 默认优先级
    private final boolean isVirtual; // 是否是虚拟样板（单个材料）
    private final ItemStack cachedInput; // 虚拟样板的具体输入物品
    private final ItemStack cachedOutput; // 虚拟样板的具体输出物品
    private final int inputCount; // 输入数量
    private final int outputCount; // 输出数量

    public SmartPatternDetails(ItemStack patternStack) {
        this.patternStack = patternStack;
        // 从NBT中读取输入和输出信息
        this.inputOreName = ItemTest.getInputOreNameStatic(patternStack);
        this.outputOreName = ItemTest.getOutputOreNameStatic(patternStack);
        this.displayName = ItemTest.getEncodedItemNameStatic(patternStack);
        this.inputCount = ItemTest.getInputCountStatic(patternStack);
        this.outputCount = ItemTest.getOutputCountStatic(patternStack);
        this.isVirtual = patternStack.hasTagCompound() && patternStack.getTagCompound().hasKey("VirtualInputOreName");
        
        // 初始化缓存
        if (this.isVirtual) {
            // 虚拟样板：使用明确指定的输入输出
            List<ItemStack> inputItems = OreDictionary.getOres(inputOreName);
            this.cachedInput = (!inputItems.isEmpty()) ? inputItems.get(0).copy() : ItemStack.EMPTY;
            if (!this.cachedInput.isEmpty()) {
                this.cachedInput.setCount(this.inputCount);
            }
            
            List<ItemStack> outputItems = OreDictionary.getOres(outputOreName);
            this.cachedOutput = (!outputItems.isEmpty()) ? outputItems.get(0).copy() : ItemStack.EMPTY;
            if (!this.cachedOutput.isEmpty()) {
                this.cachedOutput.setCount(this.outputCount);
            }
        } else {
            // 非虚拟（通配符）样板：使用配方中的第一个材料
            // 对于通配符，先直接从矿物辞典按 ingot* -> plate* 规则获取第一个
            List<String[]> recipes = getWildcardRecipes();
            if (!recipes.isEmpty()) {
                String[] firstRecipe = recipes.get(0);
                List<ItemStack> inputItems = OreDictionary.getOres(firstRecipe[0]);
                this.cachedInput = (!inputItems.isEmpty()) ? inputItems.get(0).copy() : ItemStack.EMPTY;
                if (!this.cachedInput.isEmpty()) {
                    this.cachedInput.setCount(this.inputCount);
                }
                
                List<ItemStack> outputItems = OreDictionary.getOres(firstRecipe[1]);
                this.cachedOutput = (!outputItems.isEmpty()) ? outputItems.get(0).copy() : ItemStack.EMPTY;
                if (!this.cachedOutput.isEmpty()) {
                    this.cachedOutput.setCount(this.outputCount);
                }
            } else {
                this.cachedInput = ItemStack.EMPTY;
                this.cachedOutput = ItemStack.EMPTY;
            }
        }
    }

    /**
     * 虚拟样板构造函数 - 用于展开通配符样板为具体材料
     * 不缓存具体物品，而是缓存矿物辞典名称，在需要时动态获取
     */
    public SmartPatternDetails(ItemStack patternStack, String specificInputOre, String specificOutputOre, String specificDisplayName) {
        this.patternStack = patternStack;
        this.inputOreName = specificInputOre;
        this.outputOreName = specificOutputOre;
        this.displayName = specificDisplayName;
        this.inputCount = ItemTest.getInputCountStatic(patternStack);
        this.outputCount = ItemTest.getOutputCountStatic(patternStack);
        this.isVirtual = true;
        
        // 不缓存物品，让getInputs()和getOutputs()动态从矿物辞典获取
        // 这样可以支持矿物辞典中的任何变体
        this.cachedInput = null;
        this.cachedOutput = null;
    }

    @Override
    public ItemStack getPattern() {
        return patternStack.copy();
    }

    @Override
    public boolean isValidItemForSlot(int slot, ItemStack itemStack, World world) {
        if (slot == 0) {
            if (itemStack == null || itemStack.isEmpty()) return false;

            // 虚拟样板：只验证是否匹配缓存的具体输入物品
            if (isVirtual) {
                if (cachedInput == null || cachedInput.isEmpty()) return false;
                int[] oreIDs = OreDictionary.getOreIDs(itemStack);
                for (int oreID : oreIDs) {
                    if (OreDictionary.getOreName(oreID).equals(inputOreName)) {
                        return true;
                    }
                }
                return false;
            }

            // 通配符样板：接受所有匹配类别的材料
            int[] oreIDs = OreDictionary.getOreIDs(itemStack);
            List<String[]> categoryRecipes = getCategoryRecipes();

            for (int oreID : oreIDs) {
                String oreName = OreDictionary.getOreName(oreID);
                for (String[] recipe : categoryRecipes) {
                    if (recipe[0].equals(oreName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isCraftable() {
        return true; // 智能样板总是可合成的
    }

    @Override
    public IAEItemStack[] getInputs() {
        if (isVirtual) {
            // 虚拟样板：动态从矿物辞典获取，支持矿物辞典的任何变体
            List<ItemStack> inputItems = OreDictionary.getOres(inputOreName);
            if (!inputItems.isEmpty()) {
                ItemStack input = inputItems.get(0).copy();
                input.setCount(this.inputCount);
                return new IAEItemStack[]{AEItemStack.fromItemStack(input)};
            }
            return new IAEItemStack[0];
        }
        
        // 通配符样板：只返回第一个（ASM Transformer会在addToCraftingList时展开为19个虚拟样板）
        List<String[]> categoryRecipes = getCategoryRecipes();
        if (!categoryRecipes.isEmpty()) {
            String[] firstRecipe = categoryRecipes.get(0);
            List<ItemStack> firstInputItems = OreDictionary.getOres(firstRecipe[0]);
            if (!firstInputItems.isEmpty()) {
                ItemStack input = firstInputItems.get(0).copy();
                input.setCount(this.inputCount);
                return new IAEItemStack[]{AEItemStack.fromItemStack(input)};
            }
        }
        return new IAEItemStack[0];
    }

    @Override
    public IAEItemStack[] getCondensedInputs() {
        if (isVirtual) {
            return getInputs();
        }
        // 通配符样板：只返回第一个用于显示（避免界面混乱）
        List<String[]> categoryRecipes = getCategoryRecipes();
        if (!categoryRecipes.isEmpty()) {
            String[] firstRecipe = categoryRecipes.get(0);
            List<ItemStack> firstInputItems = OreDictionary.getOres(firstRecipe[0]);
            if (!firstInputItems.isEmpty()) {
                ItemStack input = firstInputItems.get(0).copy();
                input.setCount(this.inputCount);
                return new IAEItemStack[]{AEItemStack.fromItemStack(input)};
            }
        }
        return new IAEItemStack[0];
    }

    @Override
    public IAEItemStack[] getCondensedOutputs() {
        if (isVirtual) {
            return getOutputs();
        }
        // 通配符样板：只返回第一个用于显示（避免界面混乱）
        List<String[]> categoryRecipes = getCategoryRecipes();
        if (!categoryRecipes.isEmpty()) {
            String[] firstRecipe = categoryRecipes.get(0);
            List<ItemStack> firstOutputItems = OreDictionary.getOres(firstRecipe[1]);
            if (!firstOutputItems.isEmpty()) {
                ItemStack output = firstOutputItems.get(0).copy();
                output.setCount(this.outputCount);
                return new IAEItemStack[]{AEItemStack.fromItemStack(output)};
            }
        }
        return new IAEItemStack[0];
    }

    @Override
    public IAEItemStack getPrimaryOutput() {
        IAEItemStack[] outputs = getOutputs();
        return outputs.length > 0 ? outputs[0] : null;
    }

    @Override
    public IAEItemStack[] getOutputs() {
        if (isVirtual) {
            // 虚拟样板：动态从矿物辞典获取，支持矿物辞典的任何变体
            List<ItemStack> outputItems = OreDictionary.getOres(outputOreName);
            if (!outputItems.isEmpty()) {
                ItemStack output = outputItems.get(0).copy();
                output.setCount(this.outputCount);
                return new IAEItemStack[]{AEItemStack.fromItemStack(output)};
            }
            return new IAEItemStack[0];
        }
        
        // 通配符样板：只返回第一个（ASM Transformer会在addToCraftingList时展开为19个虚拟样板）
        List<String[]> categoryRecipes = getCategoryRecipes();
        if (!categoryRecipes.isEmpty()) {
            String[] firstRecipe = categoryRecipes.get(0);
            List<ItemStack> firstOutputItems = OreDictionary.getOres(firstRecipe[1]);
            if (!firstOutputItems.isEmpty()) {
                ItemStack output = firstOutputItems.get(0).copy();
                output.setCount(this.outputCount);
                return new IAEItemStack[]{AEItemStack.fromItemStack(output)};
            }
        }
        return new IAEItemStack[0];
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
        if (isVirtual) {
            // 虚拟样板：优先从输入物品推导出材料名，再获取对应输出
            // 这样可以确保输出是正确对应的材料
            if (inventory.getSizeInventory() > 0) {
                ItemStack input = inventory.getStackInSlot(0);
                if (!input.isEmpty()) {
                    // 从输入物品推导材料
                    int[] oreIDs = OreDictionary.getOreIDs(input);
                    for (int oreID : oreIDs) {
                        String inputOre = OreDictionary.getOreName(oreID);
                        if (inputOre.equals(inputOreName)) {
                            // 找到匹配的输入，返回对应的输出
                            List<ItemStack> outputItems = OreDictionary.getOres(outputOreName);
                            if (!outputItems.isEmpty()) {
                                ItemStack result = outputItems.get(0).copy();
                                result.setCount(this.outputCount);
                                return result;
                            }
                        }
                    }
                }
            }
            
            // 如果没有输入库存，就直接从矿物辞典获取
            List<ItemStack> outputItems = OreDictionary.getOres(outputOreName);
            if (!outputItems.isEmpty()) {
                ItemStack result = outputItems.get(0).copy();
                result.setCount(this.outputCount);
                return result;
            }
            return ItemStack.EMPTY;
        }
        
        // 通配符样板：根据实际输入物品动态返回对应的输出
        if (inventory.getSizeInventory() > 0) {
            ItemStack input = inventory.getStackInSlot(0);
            if (!input.isEmpty()) {
                int[] oreIDs = OreDictionary.getOreIDs(input);
                for (int oreID : oreIDs) {
                    String inputOreName = OreDictionary.getOreName(oreID);
                    List<String[]> categoryRecipes = getCategoryRecipes();
                    for (String[] recipe : categoryRecipes) {
                        if (recipe[0].equals(inputOreName)) {
                            List<ItemStack> outputItems = OreDictionary.getOres(recipe[1]);
                            if (!outputItems.isEmpty()) {
                                ItemStack result = outputItems.get(0).copy();
                                result.setCount(this.outputCount);
                                return result;
                            }
                        }
                    }
                }
            }
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
        return inputOreName + "→" + outputOreName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public boolean isWildcardPattern() {
        return inputOreName.contains("*") || outputOreName.contains("*");
    }

    /**
     * 将通配符样板展开为多个虚拟样板（每个材料一个）
     * 仅用于通配符样板
     */
    public List<SmartPatternDetails> expandToVirtualPatterns() {
        List<SmartPatternDetails> virtualPatterns = new ArrayList<>();
        
        // 只有通配符样板需要展开
        if (!inputOreName.contains("*") && !outputOreName.contains("*")) {
            virtualPatterns.add(this);
            return virtualPatterns;
        }

        // 获取所有通配符配方
        List<String[]> recipes = getWildcardRecipes();
        
        // 为每个配方创建一个虚拟样板
        for (String[] recipe : recipes) {
            ItemStack virtualStack = patternStack.copy();
            // 创建全新的NBT标签，避免共享引用
            NBTTagCompound tag = new NBTTagCompound();
            // 复制原有NBT（如果有）
            if (virtualStack.hasTagCompound()) {
                tag = virtualStack.getTagCompound().copy();
            }
            tag.setString("VirtualInputOreName", recipe[0]);
            tag.setString("VirtualOutputOreName", recipe[1]);
            tag.setString("VirtualDisplayName", recipe[2]);
            // 保留数量信息
            if (patternStack.hasTagCompound()) {
                if (patternStack.getTagCompound().hasKey("InputCount")) {
                    tag.setInteger("InputCount", patternStack.getTagCompound().getInteger("InputCount"));
                }
                if (patternStack.getTagCompound().hasKey("OutputCount")) {
                    tag.setInteger("OutputCount", patternStack.getTagCompound().getInteger("OutputCount"));
                }
            }
            virtualStack.setTagCompound(tag);

            SmartPatternDetails virtualPattern = new SmartPatternDetails(
                virtualStack,
                recipe[0],  // 具体输入矿物名（如 ingotIron）
                recipe[1],  // 具体输出矿物名（如 plateIron）
                recipe[2]   // 显示名称
            );
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
            List<ItemStack> inputItems = OreDictionary.getOres(inputOreName);
            List<ItemStack> outputItems = OreDictionary.getOres(outputOreName);
            if (!inputItems.isEmpty() && !outputItems.isEmpty()) {
                recipes.add(new String[]{inputOreName, outputOreName, displayName});
            }
            return recipes;
        }

        // 空值检查
        if (inputOreName == null || outputOreName == null) {
            return new ArrayList<>();
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
        return OreDictRecipeCache.getRecipes(inputOreName, outputOreName);
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
}