package com.lwx1145.techstart;


import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 */
public class PacketUpdatePatternCount implements IMessage {
    
    private boolean isInput; // EN: Temporary fallback comment after encoding recovery.
    // ZH: 编码修复后使用的临时兜底注释。
    private int count;
    
    // EN: Original comment text was corrupted by encoding.
    // ZH: 原注释因编码问题已损坏。
    public PacketUpdatePatternCount() {}
    
    public PacketUpdatePatternCount(boolean isInput, int count) {
        this.isInput = isInput;
        this.count = count;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        this.isInput = buf.readBoolean();
        this.count = buf.readInt();
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.isInput);
        buf.writeInt(this.count);
    }
    
    public static class Handler implements IMessageHandler<PacketUpdatePatternCount, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdatePatternCount message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            
            // EN: Original comment text was corrupted by encoding.
            // ZH: 原注释因编码问题已损坏。
            player.getServerWorld().addScheduledTask(() -> {
                // EN: Original comment text was corrupted by encoding.
                // ZH: 原注释因编码问题已损坏。
                ItemStack patternStack = player.getHeldItemMainhand();
                if (patternStack.isEmpty() || !(patternStack.getItem() instanceof ItemTest)) {
                    patternStack = player.getHeldItemOffhand();
                }
                
                if (!patternStack.isEmpty() && patternStack.getItem() instanceof ItemTest) {
                    ItemTest patternItem = (ItemTest) patternStack.getItem();
                    
                    // EN: Original comment text was corrupted by encoding.
                    // ZH: 原注释因编码问题已损坏。
                    String inputOre = patternItem.getInputOreName(patternStack);
                    String outputOre = patternItem.getOutputOreName(patternStack);
                    String displayName = patternItem.getEncodedItemName(patternStack);
                    int inputCount = patternItem.getInputCount(patternStack);
                    int outputCount = patternItem.getOutputCount(patternStack);
                    
                    // EN: Original comment text was corrupted by encoding.
                    // ZH: 原注释因编码问题已损坏。
                    if (message.isInput) {
                        inputCount = message.count;
                    } else {
                        outputCount = message.count;
                    }
                    
                    // EN: Temporary fallback comment after encoding recovery.
                    // ZH: 编码修复后使用的临时兜底注释。
                    if (!inputOre.isEmpty() && !outputOre.isEmpty()) {
                        patternItem.setEncodedItem(patternStack, inputOre, outputOre, displayName, inputCount, outputCount);
                    }
                }
            });
            
            return null;
        }
    }
}
