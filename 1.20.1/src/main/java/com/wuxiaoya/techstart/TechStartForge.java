package com.wuxiaoya.techstart;

import com.mojang.logging.LogUtils;
import com.wuxiaoya.techstart.client.ClientModEvents;
import com.wuxiaoya.techstart.config.TechStartConfig;
import com.wuxiaoya.techstart.integration.ae2.Ae2Compat;
import com.wuxiaoya.techstart.network.TechStartNetwork;
import com.wuxiaoya.techstart.registry.TechStartBlockEntities;
import com.wuxiaoya.techstart.registry.TechStartBlocks;
import com.wuxiaoya.techstart.registry.TechStartItems;
import com.wuxiaoya.techstart.registry.TechStartMenus;
import com.wuxiaoya.techstart.registry.TechStartTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(TechStartForge.MODID)
public class TechStartForge {
    public static final String MODID = "sampleintegration";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TechStartForge() {
        FMLJavaModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TechStartConfig.SPEC);
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onCommonSetup);
        TechStartBlocks.BLOCKS.register(modEventBus);
        TechStartItems.ITEMS.register(modEventBus);
        TechStartBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        TechStartMenus.MENUS.register(modEventBus);
        TechStartTabs.TABS.register(modEventBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientModEvents::onClientSetup);
        }
        MinecraftForge.EVENT_BUS.register(this);
        TechStartNetwork.register();
        LOGGER.info("TechStart Forge 1.20.1 bootstrap complete.");
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        if (ModList.get().isLoaded("ae2")) {
            event.enqueueWork(Ae2Compat::init);
        }
    }
}
