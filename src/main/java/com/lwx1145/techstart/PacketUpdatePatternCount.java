package com.lwx1145.techstart;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 网络包：更新样板物品的数量设置
 */
public class PacketUpdatePatternCount implements IMessage {
    
    private boolean isInput; // true = 输入数量, false = 输出数量
    private int count;
    
    // 必须有无参构造函数
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
            
            // 在主线程中处理
            player.getServerWorld().addScheduledTask(() -> {
                // 获取玩家手中的样板物品
                ItemStack patternStack = player.getHeldItemMainhand();
                if (patternStack.isEmpty() || !(patternStack.getItem() instanceof ItemTest)) {
                    patternStack = player.getHeldItemOffhand();
                }
                
                if (!patternStack.isEmpty() && patternStack.getItem() instanceof ItemTest) {
                    ItemTest patternItem = (ItemTest) patternStack.getItem();
                    
                    // 获取当前的数据
                    String inputOre = patternItem.getInputOreName(patternStack);
                    String outputOre = patternItem.getOutputOreName(patternStack);
                    String displayName = patternItem.getEncodedItemName(patternStack);
                    int inputCount = patternItem.getInputCount(patternStack);
                    int outputCount = patternItem.getOutputCount(patternStack);
                    
                    // 更新对应的数量
                    if (message.isInput) {
                        inputCount = message.count;
                    } else {
                        outputCount = message.count;
                    }
                    
                    // 保存到NBT
                    if (!inputOre.isEmpty() && !outputOre.isEmpty()) {
                        patternItem.setEncodedItem(patternStack, inputOre, outputOre, displayName, inputCount, outputCount);
                    }
                }
            });
            
            return null;
        }
    }
}
