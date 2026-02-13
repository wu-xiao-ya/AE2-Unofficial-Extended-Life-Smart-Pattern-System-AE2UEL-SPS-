package com.lwx1145.techstart;


import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketRequestEncodePattern implements IMessage {

    public PacketRequestEncodePattern() {
    }

    @Override
    public void fromBytes(io.netty.buffer.ByteBuf buf) {
    }

    @Override
    public void toBytes(io.netty.buffer.ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<PacketRequestEncodePattern, IMessage> {
        @Override
        public IMessage onMessage(PacketRequestEncodePattern message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (!(player.openContainer instanceof ContainerPatternEditor)) {
                    return;
                }
                ContainerPatternEditor container = (ContainerPatternEditor) player.openContainer;
                container.savePattern();
            });
            return null;
        }
    }
}
