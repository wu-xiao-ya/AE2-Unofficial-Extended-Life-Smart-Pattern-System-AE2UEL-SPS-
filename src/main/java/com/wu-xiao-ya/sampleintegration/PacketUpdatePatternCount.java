package com.lwx1145.sampleintegration;


import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 */
public class PacketUpdatePatternCount implements IMessage {
    
    private boolean isInput;
    private int count;
    

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
            

            player.getServerWorld().addScheduledTask(() -> {

                ItemStack patternStack = player.getHeldItemMainhand();
                if (patternStack.isEmpty() || !(patternStack.getItem() instanceof ItemTest)) {
                    patternStack = player.getHeldItemOffhand();
                }
                
                if (!patternStack.isEmpty() && patternStack.getItem() instanceof ItemTest) {
                    ItemTest patternItem = (ItemTest) patternStack.getItem();
                    

                    String inputOre = patternItem.getInputOreName(patternStack);
                    String outputOre = patternItem.getOutputOreName(patternStack);
                    String displayName = patternItem.getEncodedItemName(patternStack);
                    int inputCount = patternItem.getInputCount(patternStack);
                    int outputCount = patternItem.getOutputCount(patternStack);
                    

                    if (message.isInput) {
                        inputCount = message.count;
                    } else {
                        outputCount = message.count;
                    }
                    

                    if (!inputOre.isEmpty() && !outputOre.isEmpty()) {
                        patternItem.setEncodedItem(patternStack, inputOre, outputOre, displayName, inputCount, outputCount);
                    }
                }
            });
            
            return null;
        }
    }
}

