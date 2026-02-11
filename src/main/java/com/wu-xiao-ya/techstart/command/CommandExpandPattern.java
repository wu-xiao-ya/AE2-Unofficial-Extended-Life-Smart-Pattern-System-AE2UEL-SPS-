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
 * 鍛戒护锛?expandpattern
 * 灏嗙帺瀹舵墜涓殑閫氶厤绗︽牱鏉垮睍寮€涓?9涓櫄鎷熸牱鏉垮苟鏀惧叆鐜╁鑳屽寘
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
        return 0; // 鎵€鏈夌帺瀹堕兘鍙互浣跨敤
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

        // 鍒涘缓涓绘牱鏉垮苟灞曞紑
        SmartPatternDetails mainPattern = new SmartPatternDetails(heldStack);
        List<SmartPatternDetails> virtualPatterns = mainPattern.expandToVirtualPatterns();

        player.sendMessage(new TextComponentString("Expanded into " + virtualPatterns.size() + " virtual patterns."));

        // 涓烘瘡涓櫄鎷熸牱鏉垮垱寤篒temStack
        int addedCount = 0;
        for (SmartPatternDetails virtualPattern : virtualPatterns) {
            // 鐩存帴浠庤櫄鎷熸牱鏉跨殑patternStack澶嶅埗
            ItemStack virtualStack = virtualPattern.getPattern().copy();
            
            // 灏濊瘯娣诲姞鍒扮帺瀹惰儗鍖?
            if (!player.inventory.addItemStackToInventory(virtualStack)) {
                // 鑳屽寘婊′簡锛屾帀钀藉埌鍦颁笂
                player.dropItem(virtualStack, false);
            }
            addedCount++;
        }

        player.sendMessage(new TextComponentString("Created " + addedCount + " virtual patterns."));
        
        // 绉婚櫎鎵嬩腑鐨勯€氶厤绗︽牱鏉?
        heldStack.shrink(1);
    }
}
