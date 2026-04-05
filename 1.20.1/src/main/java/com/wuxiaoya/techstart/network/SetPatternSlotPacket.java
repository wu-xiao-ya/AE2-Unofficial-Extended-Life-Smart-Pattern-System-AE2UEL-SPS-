package com.wuxiaoya.techstart.network;

import com.wuxiaoya.techstart.menu.PatternEditorMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetPatternSlotPacket(int slotId, ItemStack marker) {
    public static void encode(SetPatternSlotPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.slotId);
        buf.writeItem(packet.marker);
    }

    public static SetPatternSlotPacket decode(FriendlyByteBuf buf) {
        return new SetPatternSlotPacket(buf.readVarInt(), buf.readItem());
    }

    public static void handle(SetPatternSlotPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            if (sender.containerMenu instanceof PatternEditorMenu menu) {
                menu.applyMarkerStackFromClient(packet.slotId, packet.marker.copy(), sender);
            }
        });
        context.setPacketHandled(true);
    }
}

