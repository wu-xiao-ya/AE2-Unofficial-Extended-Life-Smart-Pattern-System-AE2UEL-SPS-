package com.lwx1145.sampleintegration;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSetFluidAmount implements IMessage {
    private int slotId;
    private int amount;

    public PacketSetFluidAmount() {}

    public PacketSetFluidAmount(int slotId, int amount) {
        this.slotId = slotId;
        this.amount = amount;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.slotId = buf.readInt();
        this.amount = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.slotId);
        buf.writeInt(this.amount);
    }

    public static class Handler implements IMessageHandler<PacketSetFluidAmount, IMessage> {
        @Override
        public IMessage onMessage(PacketSetFluidAmount message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Container container = player.openContainer;
                if (container instanceof ContainerPatternEditor) {
                    ((ContainerPatternEditor) container).applyMarkerAmountToSlot(message.slotId, message.amount);
                }
            });
            return null;
        }
    }
}
