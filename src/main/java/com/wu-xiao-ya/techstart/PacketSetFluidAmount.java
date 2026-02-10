package com.lwx1145.techstart;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSetFluidAmount implements IMessage {

    private int slotId;
    private int amount;

    public PacketSetFluidAmount() {
    }

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
                if (!(player.openContainer instanceof ContainerPatternEditor)) {
                    return;
                }
                ContainerPatternEditor container = (ContainerPatternEditor) player.openContainer;
                int amount = Math.max(1, message.amount);
                container.applyFluidAmountToSlot(message.slotId, amount);
            });
            return null;
        }
    }
}
