package com.lwx1145.techstart;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSetPatternSlot implements IMessage {
    private int slotId;
    private ItemStack stack;

    public PacketSetPatternSlot() {
        this.stack = ItemStack.EMPTY;
    }

    public PacketSetPatternSlot(int slotId, ItemStack stack) {
        this.slotId = slotId;
        this.stack = stack;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.slotId = buf.readInt();
        this.stack = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.slotId);
        ByteBufUtils.writeItemStack(buf, this.stack);
    }

    public static class Handler implements IMessageHandler<PacketSetPatternSlot, IMessage> {
        @Override
        public IMessage onMessage(PacketSetPatternSlot message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Container container = player.openContainer;
                if (!(container instanceof ContainerPatternEditor)) {
                    return;
                }

                ContainerPatternEditor editor = (ContainerPatternEditor) container;
                if (!editor.isPatternSlotId(message.slotId)) {
                    return;
                }

                ItemStack marker = editor.createMarkerFromIngredient(message.stack);
                if (!marker.isEmpty()) {
                    editor.setMarkerStackInSlot(message.slotId, marker);
                }
            });
            return null;
        }
    }
}
