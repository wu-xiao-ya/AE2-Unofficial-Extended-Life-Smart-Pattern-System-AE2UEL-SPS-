package com.wuxiaoya.techstart.network;

import com.wuxiaoya.techstart.menu.PatternEditorMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetPatternSlotAmountPacket(int slotId, int amount) {
    public static void encode(SetPatternSlotAmountPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.slotId);
        buf.writeVarInt(packet.amount);
    }

    public static SetPatternSlotAmountPacket decode(FriendlyByteBuf buf) {
        return new SetPatternSlotAmountPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(SetPatternSlotAmountPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            if (sender.containerMenu instanceof PatternEditorMenu menu) {
                menu.applyPatternSlotAmountFromClient(packet.slotId, packet.amount, sender);
            }
        });
        context.setPacketHandled(true);
    }
}
