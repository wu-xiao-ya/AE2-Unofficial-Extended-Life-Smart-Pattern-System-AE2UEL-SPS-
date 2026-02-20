package com.lwx1145.sampleintegration.command;


import com.lwx1145.sampleintegration.ItemTest;
import com.lwx1145.sampleintegration.SmartPatternDetails;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.List;

/**
 */
public class CommandExpandPattern extends CommandBase {

    @Override
    public String getName() {
        return "expandpattern";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "command.sampleintegration.expandpattern.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.sendMessage(new TextComponentTranslation("command.sampleintegration.expandpattern.only_player"));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        ItemStack heldStack = player.getHeldItemMainhand();

        if (heldStack.isEmpty() || !(heldStack.getItem() instanceof ItemTest)) {
            player.sendMessage(new TextComponentTranslation("command.sampleintegration.expandpattern.hold_pattern"));
            return;
        }

        if (!ItemTest.hasEncodedItemStatic(heldStack)) {
            player.sendMessage(new TextComponentTranslation("command.sampleintegration.expandpattern.not_encoded"));
            return;
        }

        String inputOre = ItemTest.getInputOreNameStatic(heldStack);
        String outputOre = ItemTest.getOutputOreNameStatic(heldStack);

        if (!inputOre.contains("*") && !outputOre.contains("*")) {
            player.sendMessage(new TextComponentTranslation("command.sampleintegration.expandpattern.not_wildcard"));
            return;
        }

        player.sendMessage(new TextComponentTranslation("command.sampleintegration.expandpattern.expanding", inputOre, outputOre));


        SmartPatternDetails mainPattern = new SmartPatternDetails(heldStack);
        List<SmartPatternDetails> virtualPatterns = mainPattern.expandToVirtualPatterns();

        player.sendMessage(new TextComponentTranslation("command.sampleintegration.expandpattern.expanded", virtualPatterns.size()));


        int addedCount = 0;
        for (SmartPatternDetails virtualPattern : virtualPatterns) {

            ItemStack virtualStack = virtualPattern.getPattern().copy();
            

            if (!player.inventory.addItemStackToInventory(virtualStack)) {

                player.dropItem(virtualStack, false);
            }
            addedCount++;
        }

        player.sendMessage(new TextComponentTranslation("command.sampleintegration.expandpattern.created", addedCount));
        

        heldStack.shrink(1);
    }
}

