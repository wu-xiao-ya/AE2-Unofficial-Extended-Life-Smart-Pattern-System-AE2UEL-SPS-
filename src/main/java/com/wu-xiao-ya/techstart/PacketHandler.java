package com.lwx1145.techstart;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 网络包处理器
 */
public class PacketHandler {
    
    private static int packetId = 0;
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(TechStart.MODID);
    
    public static void register() {
        INSTANCE.registerMessage(PacketUpdatePatternCount.Handler.class, PacketUpdatePatternCount.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(PacketSetFluidAmount.Handler.class, PacketSetFluidAmount.class, packetId++, Side.SERVER);
    }
}
