package com.wuxiaoya.techstart.network;

import com.wuxiaoya.techstart.TechStartForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class TechStartNetwork {
    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(TechStartForge.MODID, "network"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int nextId = 0;

    private TechStartNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                nextId++,
                SetPatternSlotPacket.class,
                SetPatternSlotPacket::encode,
                SetPatternSlotPacket::decode,
                SetPatternSlotPacket::handle
        );
        CHANNEL.registerMessage(
                nextId++,
                SetPatternSlotAmountPacket.class,
                SetPatternSlotAmountPacket::encode,
                SetPatternSlotAmountPacket::decode,
                SetPatternSlotAmountPacket::handle
        );
    }
}
