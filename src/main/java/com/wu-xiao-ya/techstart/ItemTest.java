
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
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraft.client.util.ITooltipFlag;
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
        setEncodedItem(stack, inputOreName, outputOreName, displayName, 1, 1);
    }

    public void setEncodedItem(ItemStack stack, String inputOreName, String outputOreName, String displayName, int inputCount, int outputCount) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound nbt = stack.getTagCompound();
        nbt.setString("InputOreName", inputOreName);
        nbt.setString("OutputOreName", outputOreName);
        nbt.setString("EncodedItem", displayName);
        nbt.setInteger("InputCount", inputCount);
        nbt.setInteger("OutputCount", outputCount);
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
        }
    }

    /**
     * 获取输入矿物辞典名称
     */
    public String getInputOreName(ItemStack stack) {
        if (!hasEncodedItem(stack)) return "";
        if (!stack.hasTagCompound()) return "";
        return stack.getTagCompound().getString("InputOreName");
    }

    public int getInputCount(ItemStack stack) {
        if (!hasEncodedItem(stack)) return 1;
        if (!stack.hasTagCompound()) return 1;
        return stack.getTagCompound().hasKey("InputCount") ? stack.getTagCompound().getInteger("InputCount") : 1;
    }

    public int getOutputCount(ItemStack stack) {
        if (!hasEncodedItem(stack)) return 1;
        if (!stack.hasTagCompound()) return 1;
        return stack.getTagCompound().hasKey("OutputCount") ? stack.getTagCompound().getInteger("OutputCount") : 1;
    }

    /**
     * 获取输出矿物辞典名称
     */
    public String getOutputOreName(ItemStack stack) {
        if (!hasEncodedItem(stack)) return "";
        if (!stack.hasTagCompound()) return "";
        return stack.getTagCompound().getString("OutputOreName");
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
            
            // 获取输入和输出的矿物类型和数量
            String inputOre = getInputOreName(stack);
            String outputOre = getOutputOreName(stack);
            int inputCount = getInputCount(stack);
            int outputCount = getOutputCount(stack);
            
            // 显示输入和输出的矿物类型及数量
            if (!inputOre.isEmpty() && !outputOre.isEmpty()) {
                tooltip.add("§7输入: §f" + inputOre + " §7x§f" + inputCount);
                tooltip.add("§7输出: §f" + outputOre + " §7x§f" + outputCount);
            } else {
                // 向后兼容旧版本
                tooltip.add("§7矿物类型: §f" + getOreName(stack));
            }
            
            tooltip.add("§7右键打开编辑器");
        } else {
            tooltip.add("§e右键打开样板编辑器");
            tooltip.add("§7在GUI中放入矿物辞典物品进行标记");
        }
    }

    /**
     * 静态方法：获取输入矿物辞典名称（供SmartPatternDetails使用）
     */
    public static String getInputOreNameStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack)) return "";
        if (!stack.hasTagCompound()) return "";
        if (stack.getTagCompound().hasKey("VirtualInputOreName")) {
            return stack.getTagCompound().getString("VirtualInputOreName");
        }
        return stack.getTagCompound().getString("InputOreName");
    }

    /**
     * 静态方法：获取输出矿物辞典名称（供SmartPatternDetails使用）
     */
    public static String getOutputOreNameStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack)) return "";
        if (!stack.hasTagCompound()) return "";
        if (stack.getTagCompound().hasKey("VirtualOutputOreName")) {
            return stack.getTagCompound().getString("VirtualOutputOreName");
        }
        return stack.getTagCompound().getString("OutputOreName");
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
        if (!hasEncodedItemStatic(stack)) return 1;
        if (!stack.hasTagCompound()) return 1;
        return stack.getTagCompound().hasKey("InputCount") ? 
               stack.getTagCompound().getInteger("InputCount") : 1;
    }

    /**
     * 静态方法：获取输出数量
     */
    public static int getOutputCountStatic(ItemStack stack) {
        if (!hasEncodedItemStatic(stack)) return 1;
        if (!stack.hasTagCompound()) return 1;
        return stack.getTagCompound().hasKey("OutputCount") ? 
               stack.getTagCompound().getInteger("OutputCount") : 1;
    }

    /**
     * 实现ICraftingPatternItem接口 - 返回样板详情
     */
    @Override
    public appeng.api.networking.crafting.ICraftingPatternDetails getPatternForItem(ItemStack stack, net.minecraft.world.World world) {
        if (hasEncodedItem(stack)) {
            // 读取输入输出矿物名
            String inputOre = getInputOreNameStatic(stack);
            String outputOre = getOutputOreNameStatic(stack);
            
            // 检测是否是虚拟样板（已展开的具体材料样板）
            if (stack.hasTagCompound() && stack.getTagCompound().hasKey("VirtualInputOreName")) {
                String virtualInput = stack.getTagCompound().getString("VirtualInputOreName");
                String virtualOutput = stack.getTagCompound().getString("VirtualOutputOreName");
                String virtualDisplay = stack.getTagCompound().getString("VirtualDisplayName");
                return new SmartPatternDetails(stack, virtualInput, virtualOutput, virtualDisplay);
            }
            
            // 对于通配符样板，创建虚拟样板对象（不是通配符包装器）
            // 这个虚拟样板会根据实际输入动态返回对应的输出
            if (inputOre.contains("*") || outputOre.contains("*")) {
                // 返回一个虚拟样板，它支持所有19个材料的substitute机制
                return new SmartPatternDetails(stack);
            }
            
            // 普通样板
            return new SmartPatternDetails(stack);
        }
        return null;
    }
}
