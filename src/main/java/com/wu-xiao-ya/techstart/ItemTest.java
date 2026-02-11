
// 物品类：智能样板物品
package com.lwx1145.techstart;

// 导入相关类
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraft.client.util.ITooltipFlag;
import java.util.ArrayList;
import java.util.List;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;

import java.util.List;

/**
 * 智能样板物品 - AE2UEL专用附属
 * 效仿AE2UEL样板功能，但大幅简化操作流程
 * 只需标记一种原料，通过矿物辞典自动生成相应的样板
 * 用于AE2UEL的分子装配室自动化生产
 */
public class ItemTest extends Item implements ICraftingPatternItem {
    private static final String TAG_INPUT_ORES = "InputOreNames";
    private static final String TAG_OUTPUT_ORES = "OutputOreNames";
    private static final String TAG_INPUT_COUNTS = "InputCounts";
    private static final String TAG_OUTPUT_COUNTS = "OutputCounts";
    // 流体相关标签
    private static final String TAG_INPUT_FLUIDS = "InputFluids";
    private static final String TAG_INPUT_FLUID_AMOUNTS = "InputFluidAmounts";
    private static final String TAG_OUTPUT_FLUIDS = "OutputFluids";
    private static final String TAG_OUTPUT_FLUID_AMOUNTS = "OutputFluidAmounts";
    // 气体相关标签
    private static final String TAG_INPUT_GASES = "InputGases";
    private static final String TAG_INPUT_GAS_AMOUNTS = "InputGasAmounts";
    private static final String TAG_OUTPUT_GASES = "OutputGases";
    private static final String TAG_OUTPUT_GAS_AMOUNTS = "OutputGasAmounts";
    private static final String TAG_INPUT_GAS_ITEMS = "InputGasItems";
    private static final String TAG_OUTPUT_GAS_ITEMS = "OutputGasItems";
    private static final String TAG_FILTER_MODE = "FilterMode";
    private static final String TAG_FILTER_ENTRIES = "FilterEntries";
    public static final int FILTER_MODE_WHITELIST = 0;
    public static final int FILTER_MODE_BLACKLIST = 1;
    /**
     * 构造方法：设置物品属性
     */
    public ItemTest() {
        setTranslationKey("sampleintegration.pattern_integrations");// 设置物品的翻译键（用于本地化和代码引用）
        setRegistryName("sampleintegration", "pattern_integrations");// 设置物品的注册名（用于资源定位和注册）
        setCreativeTab(CreativeTabs.TOOLS);// 设置物品在创造模式中的分类（TOOLS 工具）
        setMaxStackSize(1); // 不允许堆叠，每个样板都是独特的
    }

    /**
     * 右键使用物品的逻辑 - 现在仅用于清除NBT
     */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        // Shift+右键清除NBT
        if (player.isSneaking()) {
            clearEncodedItem(stack);
            if (!world.isRemote) {
                player.sendMessage(new TextComponentString("§a样板已清除"));
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        // 普通右键点击提示：放入方块编辑
        if (world.isRemote) {
            player.sendMessage(new TextComponentString("§e提示：将此物品放入模式展开器方块中进行编辑"));
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /**
     * 检查样板是否已标记原料
     */
    public boolean hasEncodedItem(ItemStack stack) {
        if (!stack.hasTagCompound()) return false;
        return stack.getTagCompound().hasKey("EncodedItem");
    }

    /**
     * 获取标记的原料名称
     */
    public String getEncodedItemName(ItemStack stack) {
        if (!hasEncodedItem(stack)) return "";
        return stack.getTagCompound().getString("EncodedItem");
    }

    /**
     * 设置编码的物品（输入和输出）
     */
    public void setEncodedItem(ItemStack stack, String inputOreName, String outputOreName, String displayName) {
        List<String> inputOres = new ArrayList<>();
        List<String> outputOres = new ArrayList<>();
        List<Integer> inputCounts = new ArrayList<>();
        List<Integer> outputCounts = new ArrayList<>();
        inputOres.add(inputOreName);
        outputOres.add(outputOreName);
        inputCounts.add(1);
        outputCounts.add(1);
        setEncodedItem(stack, inputOres, inputCounts, outputOres, outputCounts, displayName);
    }

    public void setEncodedItem(ItemStack stack, String inputOreName, String outputOreName, String displayName, int inputCount, int outputCount) {
        List<String> inputOres = new ArrayList<>();
        List<String> outputOres = new ArrayList<>();
        List<Integer> inputCounts = new ArrayList<>();
        List<Integer> outputCounts = new ArrayList<>();
        inputOres.add(inputOreName);
        outputOres.add(outputOreName);
        inputCounts.add(inputCount);
        outputCounts.add(outputCount);
        setEncodedItem(stack, inputOres, inputCounts, outputOres, outputCounts, displayName);
    }

    public void setEncodedItem(ItemStack stack, List<String> inputOreNames, List<Integer> inputCounts,
                               List<String> outputOreNames, List<Integer> outputCounts, String displayName) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound nbt = stack.getTagCompound();
        nbt.setString("EncodedItem", displayName);

        NBTTagList inputOreList = new NBTTagList();
        NBTTagList inputCountList = new NBTTagList();
        for (int i = 0; i < inputOreNames.size(); i++) {
            inputOreList.appendTag(new NBTTagString(inputOreNames.get(i)));
            int count = i < inputCounts.size() ? inputCounts.get(i) : 1;
            inputCountList.appendTag(new NBTTagInt(count));
        }

        NBTTagList outputOreList = new NBTTagList();
        NBTTagList outputCountList = new NBTTagList();
        for (int i = 0; i < outputOreNames.size(); i++) {
            outputOreList.appendTag(new NBTTagString(outputOreNames.get(i)));
            int count = i < outputCounts.size() ? outputCounts.get(i) : 1;
            outputCountList.appendTag(new NBTTagInt(count));
        }

        nbt.setTag(TAG_INPUT_ORES, inputOreList);
        nbt.setTag(TAG_INPUT_COUNTS, inputCountList);
        nbt.setTag(TAG_OUTPUT_ORES, outputOreList);
        nbt.setTag(TAG_OUTPUT_COUNTS, outputCountList);

        if (!inputOreNames.isEmpty()) {
            nbt.setString("InputOreName", inputOreNames.get(0));
            nbt.setInteger("InputCount", inputCounts.isEmpty() ? 1 : inputCounts.get(0));
        }
        if (!outputOreNames.isEmpty()) {
            nbt.setString("OutputOreName", outputOreNames.get(0));
            nbt.setInteger("OutputCount", outputCounts.isEmpty() ? 1 : outputCounts.get(0));
        }
    }

    /**
     * 设置编码的物品（向后兼容旧版本）
     */
    public void setEncodedItem(ItemStack stack, String oreName, String displayName) {
        setEncodedItem(stack, oreName, oreName, displayName);
    }

    /**
     * 清除编码的物品
     */
    public void clearEncodedItem(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag("EncodedItem");
            stack.getTagCompound().removeTag("OreName");
            stack.getTagCompound().removeTag("InputOreName");
            stack.getTagCompound().removeTag("OutputOreName");
            stack.getTagCompound().removeTag("InputCount");
            stack.getTagCompound().removeTag("OutputCount");
            stack.getTagCompound().removeTag(TAG_INPUT_ORES);
            stack.getTagCompound().removeTag(TAG_OUTPUT_ORES);
            stack.getTagCompound().removeTag(TAG_INPUT_COUNTS);
            stack.getTagCompound().removeTag(TAG_OUTPUT_COUNTS);
            stack.getTagCompound().removeTag(TAG_INPUT_FLUIDS);
            stack.getTagCompound().removeTag(TAG_INPUT_FLUID_AMOUNTS);
            stack.getTagCompound().removeTag(TAG_OUTPUT_FLUIDS);
            stack.getTagCompound().removeTag(TAG_OUTPUT_FLUID_AMOUNTS);
            stack.getTagCompound().removeTag(TAG_INPUT_GASES);
            stack.getTagCompound().removeTag(TAG_INPUT_GAS_AMOUNTS);
            stack.getTagCompound().removeTag(TAG_OUTPUT_GASES);
            stack.getTagCompound().removeTag(TAG_OUTPUT_GAS_AMOUNTS);
            stack.getTagCompound().removeTag(TAG_INPUT_GAS_ITEMS);
            stack.getTagCompound().removeTag(TAG_OUTPUT_GAS_ITEMS);
            stack.getTagCompound().removeTag(TAG_FILTER_MODE);
            stack.getTagCompound().removeTag(TAG_FILTER_ENTRIES);
        }
    }

    public int getFilterMode(ItemStack stack) {
        return getFilterModeStatic(stack);
    }

    public void setFilterMode(ItemStack stack, int mode) {
        setFilterModeStatic(stack, mode);
    }

    public List<String> getFilterEntries(ItemStack stack) {
        return getFilterEntriesStatic(stack);
    }

    public static int getFilterModeStatic(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return FILTER_MODE_BLACKLIST;
        }
        return stack.getTagCompound().getInteger(TAG_FILTER_MODE);
    }

    public static void setFilterModeStatic(ItemStack stack, int mode) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        int value = (mode == FILTER_MODE_BLACKLIST) ? FILTER_MODE_BLACKLIST : FILTER_MODE_WHITELIST;
        stack.getTagCompound().setInteger(TAG_FILTER_MODE, value);
    }

    public static List<String> getFilterEntriesStatic(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return new ArrayList<>();
        }
        return readStringList(stack.getTagCompound(), TAG_FILTER_ENTRIES, "");
    }

    public static void toggleFilterEntryStatic(ItemStack stack, String entry) {
        if (stack == null || stack.isEmpty() || entry == null || entry.isEmpty()) {
            return;
        }
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound tag = stack.getTagCompound();
        List<String> entries = readStringList(tag, TAG_FILTER_ENTRIES, "");
        if (entries.contains(entry)) {
            entries.remove(entry);
        } else {
            entries.add(entry);
        }
        NBTTagList list = new NBTTagList();
        for (String value : entries) {
            if (value != null && !value.isEmpty()) {
                list.appendTag(new NBTTagString(value));
            }
        }
        if (list.tagCount() > 0) {
            tag.setTag(TAG_FILTER_ENTRIES, list);
        } else {
            tag.removeTag(TAG_FILTER_ENTRIES);
        }
    }

    public static void clearFilterEntriesStatic(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return;
        }
        stack.getTagCompound().removeTag(TAG_FILTER_ENTRIES);
    }

    /**
     * 获取输入流体名称列表
     */
    public List<String> getInputFluids(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_INPUT_FLUIDS, "");
    }

    /**
     * 获取输入流体容量列表（毫桶）
     */
    public List<Integer> getInputFluidAmounts(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_INPUT_FLUID_AMOUNTS, "");
    }

    /**
     * 获取输出流体名称列表
     */
    public List<String> getOutputFluids(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_OUTPUT_FLUIDS, "");
    }

    /**
     * 获取输出流体容量列表（毫桶）
     */
    public List<Integer> getOutputFluidAmounts(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_OUTPUT_FLUID_AMOUNTS, "");
    }

    /**
     * 获取输入气体名称列表
     */
    public List<String> getInputGases(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_INPUT_GASES, "");
    }

    /**
     * 获取输入气体容量列表
     */
    public List<Integer> getInputGasAmounts(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_INPUT_GAS_AMOUNTS, "");
    }

    /**
     * 获取输出气体名称列表
     */
    public List<String> getOutputGases(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_OUTPUT_GASES, "");
    }

    /**
     * 获取输出气体容量列表
     */
    public List<Integer> getOutputGasAmounts(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_OUTPUT_GAS_AMOUNTS, "");
    }

    /**
     * 设置编码的物品（支持流体）
     */
    public void setEncodedItemWithFluids(ItemStack stack, List<String> inputOreNames, List<Integer> inputCounts,
                                         List<String> outputOreNames, List<Integer> outputCounts,
                                         List<String> inputFluids, List<Integer> inputFluidAmounts,
                                         List<String> outputFluids, List<Integer> outputFluidAmounts,
                                         String displayName) {
        setEncodedItemWithFluidsAndGases(stack, inputOreNames, inputCounts, outputOreNames, outputCounts,
            inputFluids, inputFluidAmounts, outputFluids, outputFluidAmounts,
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>(), displayName);
    }

    /**
     * 设置编码的物品（支持流体和气体）
     */
    public void setEncodedItemWithFluidsAndGases(ItemStack stack, List<String> inputOreNames, List<Integer> inputCounts,
                                                 List<String> outputOreNames, List<Integer> outputCounts,
                                                 List<String> inputFluids, List<Integer> inputFluidAmounts,
                                                 List<String> outputFluids, List<Integer> outputFluidAmounts,
                                                 List<String> inputGases, List<Integer> inputGasAmounts,
                                                 List<String> outputGases, List<Integer> outputGasAmounts,
                                                 List<ItemStack> inputGasItems, List<ItemStack> outputGasItems,
                                                 String displayName) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound nbt = stack.getTagCompound();
        nbt.setString("EncodedItem", displayName);

        // 编码矿物辞典
        NBTTagList inputOreList = new NBTTagList();
        NBTTagList inputCountList = new NBTTagList();
        for (int i = 0; i < inputOreNames.size(); i++) {
            inputOreList.appendTag(new NBTTagString(inputOreNames.get(i)));
            int count = i < inputCounts.size() ? inputCounts.get(i) : 1;
            inputCountList.appendTag(new NBTTagInt(count));
        }

        NBTTagList outputOreList = new NBTTagList();
        NBTTagList outputCountList = new NBTTagList();
        for (int i = 0; i < outputOreNames.size(); i++) {
            outputOreList.appendTag(new NBTTagString(outputOreNames.get(i)));
            int count = i < outputCounts.size() ? outputCounts.get(i) : 1;
            outputCountList.appendTag(new NBTTagInt(count));
        }

        nbt.setTag(TAG_INPUT_ORES, inputOreList);
        nbt.setTag(TAG_INPUT_COUNTS, inputCountList);
        nbt.setTag(TAG_OUTPUT_ORES, outputOreList);
        nbt.setTag(TAG_OUTPUT_COUNTS, outputCountList);

        // 编码流体
        NBTTagList inputFluidList = new NBTTagList();
        NBTTagList inputFluidAmountList = new NBTTagList();
        for (int i = 0; i < inputFluids.size(); i++) {
            inputFluidList.appendTag(new NBTTagString(inputFluids.get(i)));
            int amount = i < inputFluidAmounts.size() ? inputFluidAmounts.get(i) : 0;
            inputFluidAmountList.appendTag(new NBTTagInt(amount));
        }

        NBTTagList outputFluidList = new NBTTagList();
        NBTTagList outputFluidAmountList = new NBTTagList();
        for (int i = 0; i < outputFluids.size(); i++) {
            outputFluidList.appendTag(new NBTTagString(outputFluids.get(i)));
            int amount = i < outputFluidAmounts.size() ? outputFluidAmounts.get(i) : 0;
            outputFluidAmountList.appendTag(new NBTTagInt(amount));
        }

        if (!inputFluids.isEmpty()) {
            nbt.setTag(TAG_INPUT_FLUIDS, inputFluidList);
            nbt.setTag(TAG_INPUT_FLUID_AMOUNTS, inputFluidAmountList);
        }
        if (!outputFluids.isEmpty()) {
            nbt.setTag(TAG_OUTPUT_FLUIDS, outputFluidList);
            nbt.setTag(TAG_OUTPUT_FLUID_AMOUNTS, outputFluidAmountList);
        }

        // 编码气体
        NBTTagList inputGasList = new NBTTagList();
        NBTTagList inputGasAmountList = new NBTTagList();
        for (int i = 0; i < inputGases.size(); i++) {
            inputGasList.appendTag(new NBTTagString(inputGases.get(i)));
            int amount = i < inputGasAmounts.size() ? inputGasAmounts.get(i) : 0;
            inputGasAmountList.appendTag(new NBTTagInt(amount));
        }

        NBTTagList outputGasList = new NBTTagList();
        NBTTagList outputGasAmountList = new NBTTagList();
        for (int i = 0; i < outputGases.size(); i++) {
            outputGasList.appendTag(new NBTTagString(outputGases.get(i)));
            int amount = i < outputGasAmounts.size() ? outputGasAmounts.get(i) : 0;
            outputGasAmountList.appendTag(new NBTTagInt(amount));
        }

        if (!inputGases.isEmpty()) {
            nbt.setTag(TAG_INPUT_GASES, inputGasList);
            nbt.setTag(TAG_INPUT_GAS_AMOUNTS, inputGasAmountList);
        }
        if (!outputGases.isEmpty()) {
            nbt.setTag(TAG_OUTPUT_GASES, outputGasList);
            nbt.setTag(TAG_OUTPUT_GAS_AMOUNTS, outputGasAmountList);
        }

        if (inputGasItems != null && !inputGasItems.isEmpty()) {
            nbt.setTag(TAG_INPUT_GAS_ITEMS, writeItemStackList(inputGasItems));
        } else if (nbt.hasKey(TAG_INPUT_GAS_ITEMS)) {
            nbt.removeTag(TAG_INPUT_GAS_ITEMS);
        }
        if (outputGasItems != null && !outputGasItems.isEmpty()) {
            nbt.setTag(TAG_OUTPUT_GAS_ITEMS, writeItemStackList(outputGasItems));
        } else if (nbt.hasKey(TAG_OUTPUT_GAS_ITEMS)) {
            nbt.removeTag(TAG_OUTPUT_GAS_ITEMS);
        }

        // 保留兼容字段
        if (!inputOreNames.isEmpty()) {
            nbt.setString("InputOreName", inputOreNames.get(0));
            nbt.setInteger("InputCount", inputCounts.isEmpty() ? 1 : inputCounts.get(0));
        }
        if (!outputOreNames.isEmpty()) {
            nbt.setString("OutputOreName", outputOreNames.get(0));
            nbt.setInteger("OutputCount", outputCounts.isEmpty() ? 1 : outputCounts.get(0));
        }
    }

    /**
     * 静态方法：获取输入流体名称列表
     */
    public static List<String> getInputFluidsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_INPUT_FLUIDS, "");
    }

    /**
     * 静态方法：获取输入流体容量列表
     */
    public static List<Integer> getInputFluidAmountsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_INPUT_FLUID_AMOUNTS, "");
    }

    /**
     * 静态方法：获取输出流体名称列表
     */
    public static List<String> getOutputFluidsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_OUTPUT_FLUIDS, "");
    }

    /**
     * 静态方法：获取输出流体容量列表
     */
    public static List<Integer> getOutputFluidAmountsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_OUTPUT_FLUID_AMOUNTS, "");
    }

    /**
     * 静态方法：获取输入气体名称列表
     */
    public static List<String> getInputGasesStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_INPUT_GASES, "");
    }

    /**
     * 静态方法：获取输入气体容量列表
     */
    public static List<Integer> getInputGasAmountsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_INPUT_GAS_AMOUNTS, "");
    }

    /**
     * 静态方法：获取输出气体名称列表
     */
    public static List<String> getOutputGasesStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_OUTPUT_GASES, "");
    }

    /**
     * 静态方法：获取输出气体容量列表
     */
    public static List<Integer> getOutputGasAmountsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_OUTPUT_GAS_AMOUNTS, "");
    }

    public static List<ItemStack> getInputGasItemsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readItemStackList(stack.getTagCompound(), TAG_INPUT_GAS_ITEMS);
    }

    public static List<ItemStack> getOutputGasItemsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readItemStackList(stack.getTagCompound(), TAG_OUTPUT_GAS_ITEMS);
    }


    /**
     * 获取输入矿物辞典名称
     */
    public String getInputOreName(ItemStack stack) {
        List<String> names = getInputOreNames(stack);
        return names.isEmpty() ? "" : names.get(0);
    }

    public int getInputCount(ItemStack stack) {
        List<Integer> counts = getInputCounts(stack);
        return counts.isEmpty() ? 1 : counts.get(0);
    }

    public int getOutputCount(ItemStack stack) {
        List<Integer> counts = getOutputCounts(stack);
        return counts.isEmpty() ? 1 : counts.get(0);
    }

    /**
     * 获取输出矿物辞典名称
     */
    public String getOutputOreName(ItemStack stack) {
        List<String> names = getOutputOreNames(stack);
        return names.isEmpty() ? "" : names.get(0);
    }

    public List<String> getInputOreNames(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_INPUT_ORES, "InputOreName");
    }

    public List<String> getOutputOreNames(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readStringList(stack.getTagCompound(), TAG_OUTPUT_ORES, "OutputOreName");
    }

    public List<Integer> getInputCounts(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_INPUT_COUNTS, "InputCount");
    }

    public List<Integer> getOutputCounts(ItemStack stack) {
        if (!hasEncodedItem(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        return readIntList(stack.getTagCompound(), TAG_OUTPUT_COUNTS, "OutputCount");
    }

    /**
     * 获取矿物辞典名称（向后兼容）
     */
    public String getOreName(ItemStack stack) {
        if (!hasEncodedItem(stack)) return "";
        if (!stack.hasTagCompound()) return "";

        // 优先返回输入矿物辞典名称，如果不存在则返回旧的OreName
        String inputOreName = stack.getTagCompound().getString("InputOreName");
        if (!inputOreName.isEmpty()) {
            String outputOreName = stack.getTagCompound().getString("OutputOreName");
            return inputOreName + " → " + outputOreName;
        }

        return stack.getTagCompound().getString("OreName");
    }

    /**
     * 添加工具提示
     */
    @Override
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (hasEncodedItem(stack)) {
            tooltip.add("§a已标记: §f" + getEncodedItemName(stack));
            
            List<String> inputOres = getInputOreNames(stack);
            List<String> outputOres = getOutputOreNames(stack);
            List<Integer> inputCounts = getInputCounts(stack);
            List<Integer> outputCounts = getOutputCounts(stack);
            List<String> inputFluids = getInputFluids(stack);
            List<String> outputFluids = getOutputFluids(stack);
            List<Integer> inputFluidAmounts = getInputFluidAmounts(stack);
            List<Integer> outputFluidAmounts = getOutputFluidAmounts(stack);
            List<String> inputGases = getInputGases(stack);
            List<String> outputGases = getOutputGases(stack);
            List<Integer> inputGasAmounts = getInputGasAmounts(stack);
            List<Integer> outputGasAmounts = getOutputGasAmounts(stack);
                if (!inputOres.isEmpty() || !outputOres.isEmpty() || !inputFluids.isEmpty() || !outputFluids.isEmpty()
                    || !inputGases.isEmpty() || !outputGases.isEmpty()) {
                // 显示物品输入
                for (int i = 0; i < inputOres.size(); i++) {
                    int count = i < inputCounts.size() ? inputCounts.get(i) : 1;
                    tooltip.add("§7输入" + (i + 1) + ": §f" + inputOres.get(i) + " §7x§f" + count);
                }
                // 显示流体输入
                for (int i = 0; i < inputFluids.size(); i++) {
                    int amount = i < inputFluidAmounts.size() ? inputFluidAmounts.get(i) : 0;
                    tooltip.add("§7流体" + (i + 1) + ": §f" + inputFluids.get(i) + " §7x§f" + amount + "mB");
                }
                // 显示气体输入
                for (int i = 0; i < inputGases.size(); i++) {
                    int amount = i < inputGasAmounts.size() ? inputGasAmounts.get(i) : 0;
                    tooltip.add("§7气体" + (i + 1) + ": §f" + inputGases.get(i) + " §7x§f" + amount + "mB");
                }
                
                // 显示物品输出
                for (int i = 0; i < outputOres.size(); i++) {
                    int count = i < outputCounts.size() ? outputCounts.get(i) : 1;
                    tooltip.add("§7输出" + (i + 1) + ": §f" + outputOres.get(i) + " §7x§f" + count);
                }
                // 显示流体输出
                for (int i = 0; i < outputFluids.size(); i++) {
                    int amount = i < outputFluidAmounts.size() ? outputFluidAmounts.get(i) : 0;
                    tooltip.add("§7流出" + (i + 1) + ": §f" + outputFluids.get(i) + " §7x§f" + amount + "mB");
                }
                // 显示气体输出
                for (int i = 0; i < outputGases.size(); i++) {
                    int amount = i < outputGasAmounts.size() ? outputGasAmounts.get(i) : 0;
                    tooltip.add("§7气出" + (i + 1) + ": §f" + outputGases.get(i) + " §7x§f" + amount + "mB");
                }
            } else {
                tooltip.add("§7矿物类型: §f" + getOreName(stack));
            }
            
            tooltip.add("§7右键打开编辑器");
        } else {
            tooltip.add("§e右键打开样板编辑器");
            tooltip.add("§7在GUI中放入矿物辞典物品、流体或气体容器进行标记");
        }
    }

    /**
     * 静态方法：获取输入矿物辞典名称（供SmartPatternDetails使用）
     */
    public static String getInputOreNameStatic(ItemStack stack) {
        List<String> names = getInputOreNamesStatic(stack);
        return names.isEmpty() ? "" : names.get(0);
    }

    /**
     * 静态方法：获取输出矿物辞典名称（供SmartPatternDetails使用）
     */
    public static String getOutputOreNameStatic(ItemStack stack) {
        List<String> names = getOutputOreNamesStatic(stack);
        return names.isEmpty() ? "" : names.get(0);
    }

    /**
     * 静态方法：获取矿物辞典名称（供SmartPatternDetails使用，向后兼容）
     */
    public static String getOreNameStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack)) return "";
        if (!stack.hasTagCompound()) return "";
        if (stack.getTagCompound().hasKey("VirtualInputOreName") && stack.getTagCompound().hasKey("VirtualOutputOreName")) {
            String inputOreName = stack.getTagCompound().getString("VirtualInputOreName");
            String outputOreName = stack.getTagCompound().getString("VirtualOutputOreName");
            return inputOreName + " → " + outputOreName;
        }

        String inputOreName = stack.getTagCompound().getString("InputOreName");
        if (!inputOreName.isEmpty()) {
            String outputOreName = stack.getTagCompound().getString("OutputOreName");
            return inputOreName + " → " + outputOreName;
        }

        return stack.getTagCompound().getString("OreName");
    }

    /**
     * 静态方法：获取编码物品名称（供SmartPatternDetails使用）
     */
    public static String getEncodedItemNameStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack)) return "";
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("VirtualDisplayName")) {
            return stack.getTagCompound().getString("VirtualDisplayName");
        }
        return stack.getTagCompound().getString("EncodedItem");
    }

    /**
     * 静态方法：检查样板是否已标记原料（供SmartPatternDetails使用）
     */
    public static boolean hasEncodedItemStatic(ItemStack stack) {
        if (!stack.hasTagCompound()) return false;
        return stack.getTagCompound().hasKey("EncodedItem");
    }

    /**
     * 静态方法：获取输入数量
     */
    public static int getInputCountStatic(ItemStack stack) {
        List<Integer> counts = getInputCountsStatic(stack);
        return counts.isEmpty() ? 1 : counts.get(0);
    }

    /**
     * 静态方法：获取输出数量
     */
    public static int getOutputCountStatic(ItemStack stack) {
        List<Integer> counts = getOutputCountsStatic(stack);
        return counts.isEmpty() ? 1 : counts.get(0);
    }

    public static List<String> getInputOreNamesStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        NBTTagCompound tag = stack.getTagCompound();
        if (tag.hasKey("VirtualInputOreNames")) {
            return readStringList(tag, "VirtualInputOreNames", "VirtualInputOreName");
        }
        if (tag.hasKey("VirtualInputOreName")) {
            List<String> result = new ArrayList<>();
            result.add(tag.getString("VirtualInputOreName"));
            return result;
        }
        return readStringList(tag, TAG_INPUT_ORES, "InputOreName");
    }

    public static List<String> getOutputOreNamesStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        NBTTagCompound tag = stack.getTagCompound();
        if (tag.hasKey("VirtualOutputOreNames")) {
            return readStringList(tag, "VirtualOutputOreNames", "VirtualOutputOreName");
        }
        if (tag.hasKey("VirtualOutputOreName")) {
            List<String> result = new ArrayList<>();
            result.add(tag.getString("VirtualOutputOreName"));
            return result;
        }
        return readStringList(tag, TAG_OUTPUT_ORES, "OutputOreName");
    }

    public static List<Integer> getInputCountsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        NBTTagCompound tag = stack.getTagCompound();
        return readIntList(tag, TAG_INPUT_COUNTS, "InputCount");
    }

    public static List<Integer> getOutputCountsStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack) || !stack.hasTagCompound()) return new ArrayList<>();
        NBTTagCompound tag = stack.getTagCompound();
        return readIntList(tag, TAG_OUTPUT_COUNTS, "OutputCount");
    }

    private static List<String> readStringList(NBTTagCompound tag, String listKey, String fallbackKey) {
        List<String> result = new ArrayList<>();
        if (tag.hasKey(listKey)) {
            NBTTagList list = tag.getTagList(listKey, 8);
            for (int i = 0; i < list.tagCount(); i++) {
                result.add(list.getStringTagAt(i));
            }
            return result;
        }
        if (tag.hasKey(fallbackKey)) {
            result.add(tag.getString(fallbackKey));
        } else if (tag.hasKey("OreName")) {
            result.add(tag.getString("OreName"));
        }
        return result;
    }

    private static List<Integer> readIntList(NBTTagCompound tag, String listKey, String fallbackKey) {
        List<Integer> result = new ArrayList<>();
        if (tag.hasKey(listKey)) {
            NBTTagList list = tag.getTagList(listKey, 3);
            for (int i = 0; i < list.tagCount(); i++) {
                result.add(((NBTTagInt) list.get(i)).getInt());
            }
            return result;
        }
        if (tag.hasKey(fallbackKey)) {
            result.add(tag.getInteger(fallbackKey));
        }
        return result;
    }

    private static NBTTagList writeItemStackList(List<ItemStack> stacks) {
        NBTTagList list = new NBTTagList();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            NBTTagCompound stackTag = new NBTTagCompound();
            stack.writeToNBT(stackTag);
            list.appendTag(stackTag);
        }
        return list;
    }

    private static List<ItemStack> readItemStackList(NBTTagCompound tag, String listKey) {
        List<ItemStack> result = new ArrayList<>();
        if (!tag.hasKey(listKey)) {
            return result;
        }
        NBTTagList list = tag.getTagList(listKey, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound stackTag = list.getCompoundTagAt(i);
            ItemStack stack = new ItemStack(stackTag);
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }
        return result;
    }

    /**
     * 实现ICraftingPatternItem接口 - 返回样板详情
     */
    @Override
    public appeng.api.networking.crafting.ICraftingPatternDetails getPatternForItem(ItemStack stack, net.minecraft.world.World world) {
        if (hasEncodedItem(stack)) {
            // 读取输入输出矿物名
            List<String> inputOres = getInputOreNamesStatic(stack);
            List<String> outputOres = getOutputOreNamesStatic(stack);
            
            // 检测是否是虚拟样板（已展开的具体材料样板）
            if (stack.hasTagCompound() &&
                (stack.getTagCompound().hasKey("VirtualInputOreNames") ||
                 stack.getTagCompound().hasKey("VirtualInputOreName"))) {
                return new SmartPatternDetails(stack);
            }
            
            // 对于通配符样板，创建虚拟样板对象（不是通配符包装器）
            // 这个虚拟样板会根据实际输入动态返回对应的输出
            boolean hasWildcard = false;
            for (String ore : inputOres) {
                if (ore.contains("*")) {
                    hasWildcard = true;
                    break;
                }
            }
            if (!hasWildcard) {
                for (String ore : outputOres) {
                    if (ore.contains("*")) {
                        hasWildcard = true;
                        break;
                    }
                }
            }

            if (hasWildcard) {
                // 返回一个虚拟样板，它支持所有19个材料的substitute机制
                return new SmartPatternDetails(stack);
            }
            
            // 普通样板
            return new SmartPatternDetails(stack);
        }
        return null;
    }
}
