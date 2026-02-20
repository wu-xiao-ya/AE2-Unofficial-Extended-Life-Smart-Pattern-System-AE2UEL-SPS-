package com.lwx1145.sampleintegration;


import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketUpdatePatternFilter implements IMessage {

    private byte action;
    private int mode;
    private String entry;

    public PacketUpdatePatternFilter() {
    }

    public static PacketUpdatePatternFilter forMode(int mode) {
        PacketUpdatePatternFilter packet = new PacketUpdatePatternFilter();
        packet.action = 0;
        packet.mode = mode;
        return packet;
    }

    public static PacketUpdatePatternFilter forToggle(String entry) {
        PacketUpdatePatternFilter packet = new PacketUpdatePatternFilter();
        packet.action = 1;
        packet.entry = entry == null ? "" : entry;
        return packet;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.action = buf.readByte();
        this.mode = buf.readInt();
        this.entry = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.action);
        buf.writeInt(this.mode);
        ByteBufUtils.writeUTF8String(buf, this.entry == null ? "" : this.entry);
    }

    public static class Handler implements IMessageHandler<PacketUpdatePatternFilter, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdatePatternFilter message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerPatternEditor) {
                    ContainerPatternEditor container = (ContainerPatternEditor) player.openContainer;
                    if (message.action == 0) {
                        container.applyFilterMode(message.mode);
                        container.clearFilterEntries();
                    } else if (message.action == 1 && message.entry != null && !message.entry.isEmpty()) {
                        container.toggleFilterEntry(message.entry);
                    }
                    return;
                }
                if (player.openContainer instanceof ContainerPatternFilter) {
                    ContainerPatternFilter container = (ContainerPatternFilter) player.openContainer;
                    if (message.action == 0) {
                        container.applyFilterMode(message.mode);
                        container.clearFilterEntries();
                    } else if (message.action == 1 && message.entry != null && !message.entry.isEmpty()) {
                        container.toggleFilterEntry(message.entry);
                    }
                }
            });
            return null;
        }
    }
}

