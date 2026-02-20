package com.lwx1145.sampleintegration;


import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketOpenPatternGui implements IMessage {

    private int guiId;

    public PacketOpenPatternGui() {
    }

    public PacketOpenPatternGui(int guiId) {
        this.guiId = guiId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.guiId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.guiId);
    }

    public static class Handler implements IMessageHandler<PacketOpenPatternGui, IMessage> {
        @Override
        public IMessage onMessage(PacketOpenPatternGui message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                player.openGui(TechStart.INSTANCE, message.guiId, player.world, 0, 0, 0);
            });
            return null;
        }
    }
}


