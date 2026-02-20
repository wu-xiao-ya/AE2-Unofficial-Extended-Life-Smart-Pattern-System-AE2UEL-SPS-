package com.lwx1145.sampleintegration;


import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 */
public class PacketHandler {
    
    private static int packetId = 0;
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(TechStart.MODID);
    
    public static void register() {
        INSTANCE.registerMessage(PacketUpdatePatternCount.Handler.class, PacketUpdatePatternCount.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(PacketSetFluidAmount.Handler.class, PacketSetFluidAmount.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(PacketUpdatePatternFilter.Handler.class, PacketUpdatePatternFilter.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(PacketRequestEncodePattern.Handler.class, PacketRequestEncodePattern.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(PacketOpenPatternGui.Handler.class, PacketOpenPatternGui.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(PacketSetPatternSlot.Handler.class, PacketSetPatternSlot.class, packetId++, Side.SERVER);
    }
}


