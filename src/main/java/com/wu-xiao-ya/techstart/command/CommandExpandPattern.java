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
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 */
public class CommandExpandPattern extends CommandBase {

    @Override
    public String getName() {
        return "expandpattern";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/expandpattern - Expand the wildcard pattern in your hand into virtual patterns.";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.sendMessage(new TextComponentString("Only players can run this command."));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        ItemStack heldStack = player.getHeldItemMainhand();

        if (heldStack.isEmpty() || !(heldStack.getItem() instanceof ItemTest)) {
            player.sendMessage(new TextComponentString("Hold a smart pattern in your main hand."));
            return;
        }

        if (!ItemTest.hasEncodedItemStatic(heldStack)) {
            player.sendMessage(new TextComponentString("This pattern is not encoded."));
            return;
        }

        String inputOre = ItemTest.getInputOreNameStatic(heldStack);
        String outputOre = ItemTest.getOutputOreNameStatic(heldStack);

        if (!inputOre.contains("*") && !outputOre.contains("*")) {
            player.sendMessage(new TextComponentString("This is not a wildcard pattern."));
            return;
        }

        player.sendMessage(new TextComponentString("Expanding wildcard pattern: " + inputOre + " -> " + outputOre));

        // EN: Temporary fallback comment after encoding recovery.
        // ZH: 编码修复后使用的临时兜底注释。
        SmartPatternDetails mainPattern = new SmartPatternDetails(heldStack);
        List<SmartPatternDetails> virtualPatterns = mainPattern.expandToVirtualPatterns();

        player.sendMessage(new TextComponentString("Expanded into " + virtualPatterns.size() + " virtual patterns."));

        // EN: Temporary fallback comment after encoding recovery.
        // ZH: 编码修复后使用的临时兜底注释。
        int addedCount = 0;
        for (SmartPatternDetails virtualPattern : virtualPatterns) {
            // EN: Original comment text was corrupted by encoding.
            // ZH: 原注释因编码问题已损坏。
            ItemStack virtualStack = virtualPattern.getPattern().copy();
            
            // EN: Original comment text was corrupted by encoding.
            // ZH: 原注释因编码问题已损坏。
            if (!player.inventory.addItemStackToInventory(virtualStack)) {
                // EN: Original comment text was corrupted by encoding.
                // ZH: 原注释因编码问题已损坏。
                player.dropItem(virtualStack, false);
            }
            addedCount++;
        }

        player.sendMessage(new TextComponentString("Created " + addedCount + " virtual patterns."));
        
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        heldStack.shrink(1);
    }
}
