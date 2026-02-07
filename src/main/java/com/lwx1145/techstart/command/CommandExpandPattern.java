package com.lwx1145.techstart.command;

import com.lwx1145.techstart.ItemTest;
import com.lwx1145.techstart.SmartPatternDetails;
import com.lwx1145.techstart.TechStart;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import java.util.List;

/**
 * 命令：/expandpattern
 * 将玩家手中的通配符样板展开为19个虚拟样板并放入玩家背包
 */
public class CommandExpandPattern extends CommandBase {

    @Override
    public String getName() {
        return "expandpattern";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/expandpattern - 将手中的通配符样板展开为19个虚拟样板";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // 所有玩家都可以使用
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.sendMessage(new TextComponentString("§c此命令只能由玩家执行！"));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        ItemStack heldStack = player.getHeldItemMainhand();

        if (heldStack.isEmpty() || !(heldStack.getItem() instanceof ItemTest)) {
            player.sendMessage(new TextComponentString("§c请手持一个智能样板！"));
            return;
        }

        if (!ItemTest.hasEncodedItemStatic(heldStack)) {
            player.sendMessage(new TextComponentString("§c这个样板没有编码！"));
            return;
        }

        String inputOre = ItemTest.getInputOreNameStatic(heldStack);
        String outputOre = ItemTest.getOutputOreNameStatic(heldStack);

        if (!inputOre.contains("*") && !outputOre.contains("*")) {
            player.sendMessage(new TextComponentString("§c这不是通配符样板！"));
            return;
        }

        player.sendMessage(new TextComponentString("§a正在展开通配符样板: " + inputOre + " → " + outputOre));

        // 创建主样板并展开
        SmartPatternDetails mainPattern = new SmartPatternDetails(heldStack);
        List<SmartPatternDetails> virtualPatterns = mainPattern.expandToVirtualPatterns();

        player.sendMessage(new TextComponentString("§a展开为 " + virtualPatterns.size() + " 个虚拟样板"));

        // 为每个虚拟样板创建ItemStack
        int addedCount = 0;
        for (SmartPatternDetails virtualPattern : virtualPatterns) {
            // 直接从虚拟样板的patternStack复制
            ItemStack virtualStack = virtualPattern.getPattern().copy();
            
            // 尝试添加到玩家背包
            if (!player.inventory.addItemStackToInventory(virtualStack)) {
                // 背包满了，掉落到地上
                player.dropItem(virtualStack, false);
            }
            addedCount++;
        }

        player.sendMessage(new TextComponentString("§a成功创建 " + addedCount + " 个虚拟样板！"));
        
        // 移除手中的通配符样板
        heldStack.shrink(1);
    }
}
