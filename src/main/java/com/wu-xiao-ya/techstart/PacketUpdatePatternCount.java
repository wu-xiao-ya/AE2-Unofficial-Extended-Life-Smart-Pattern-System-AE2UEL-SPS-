package com.lwx1145.techstart;


import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 缃戠粶鍖咃細鏇存柊鏍锋澘鐗╁搧鐨勬暟閲忚缃?
 */
public class PacketUpdatePatternCount implements IMessage {
    
    private boolean isInput; // true = 杈撳叆鏁伴噺, false = 杈撳嚭鏁伴噺
    private int count;
    
    // 蹇呴』鏈夋棤鍙傛瀯閫犲嚱鏁?
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
            
            // 鍦ㄤ富绾跨▼涓鐞?
            player.getServerWorld().addScheduledTask(() -> {
                // 鑾峰彇鐜╁鎵嬩腑鐨勬牱鏉跨墿鍝?
                ItemStack patternStack = player.getHeldItemMainhand();
                if (patternStack.isEmpty() || !(patternStack.getItem() instanceof ItemTest)) {
                    patternStack = player.getHeldItemOffhand();
                }
                
                if (!patternStack.isEmpty() && patternStack.getItem() instanceof ItemTest) {
                    ItemTest patternItem = (ItemTest) patternStack.getItem();
                    
                    // 鑾峰彇褰撳墠鐨勬暟鎹?
                    String inputOre = patternItem.getInputOreName(patternStack);
                    String outputOre = patternItem.getOutputOreName(patternStack);
                    String displayName = patternItem.getEncodedItemName(patternStack);
                    int inputCount = patternItem.getInputCount(patternStack);
                    int outputCount = patternItem.getOutputCount(patternStack);
                    
                    // 鏇存柊瀵瑰簲鐨勬暟閲?
                    if (message.isInput) {
                        inputCount = message.count;
                    } else {
                        outputCount = message.count;
                    }
                    
                    // 淇濆瓨鍒癗BT
                    if (!inputOre.isEmpty() && !outputOre.isEmpty()) {
                        patternItem.setEncodedItem(patternStack, inputOre, outputOre, displayName, inputCount, outputCount);
                    }
                }
            });
            
            return null;
        }
    }
}
