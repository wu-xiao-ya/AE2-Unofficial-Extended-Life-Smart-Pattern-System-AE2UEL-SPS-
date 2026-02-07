package com.lwx1145.techstart.command;

import com.lwx1145.techstart.VirtualPatternExpander;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;

/**
 * 虚拟样板生成命令
 * 使用方式: /generatevirtual
 * 用于将通配符样板批量展开为19个虚拟样板
 * 这个命令实现了自定义的"Mixin功能"来处理通配符样板的展开
 */
public class CommandGenerateVirtual extends CommandBase {
    
    @Override
    public String getName() {
        return "generatevirtual";
    }
    
    @Override
    public String getUsage(ICommandSender sender) {
        return "/generatevirtual - 将手持的通配符样板展开为19个虚拟样板";
    }
    
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.RED + "此命令只能由玩家执行"));
            return;
        }
        
        EntityPlayerMP player = (EntityPlayerMP) sender;
        ItemStack heldStack = player.getHeldItemMainhand();
        
        // 检查是否拿着样板
        if (heldStack.isEmpty()) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.RED + "请手持一个通配符样板"));
            return;
        }
        
        // 检查是否是通配符样板
        if (!heldStack.hasTagCompound()) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.RED + "此物品不是样板"));
            return;
        }
        
        String inputOre = heldStack.getTagCompound().getString("inputOre");
        if (!inputOre.contains("*")) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.RED + "此样板不是通配符样板"));
            return;
        }
        
        // 展开通配符样板
        List<ItemStack> virtualPatterns = VirtualPatternExpander.expandWildcardPatternToVirtual(heldStack);
        
        if (virtualPatterns.isEmpty()) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.RED + "无法展开此样板，可能没有有效的矿物辞典条目"));
            return;
        }
        
        // 将虚拟样板放入玩家物品栏
        for (ItemStack pattern : virtualPatterns) {
            if (!player.inventory.addItemStackToInventory(pattern)) {
                // 如果物品栏满，丢在地上
                player.dropItem(pattern, false);
            }
        }
        
        sender.sendMessage(new TextComponentString(
            TextFormatting.GREEN + "成功生成 " + virtualPatterns.size() + " 个虚拟样板！"));
    }
    
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, net.minecraft.util.math.BlockPos targetPos) {
        return new ArrayList<>();
    }
    
    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
