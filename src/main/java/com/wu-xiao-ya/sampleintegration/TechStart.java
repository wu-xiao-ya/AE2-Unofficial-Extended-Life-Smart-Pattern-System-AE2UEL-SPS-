package com.lwx1145.sampleintegration;

import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = TechStart.MODID, name = TechStart.NAME, version = TechStart.VERSION)
@Mod.EventBusSubscriber(modid = TechStart.MODID)
public class TechStart {
    public static final String MODID = "sampleintegration";
    public static final String NAME = "AE2UEL Smart Pattern System";
    public static final String VERSION = "1.0.6-alpha";
    
    public static final Logger LOGGER = LogManager.getLogger(NAME);
    public static TechStart INSTANCE;
    public static final ItemTest ITEM_TEST = new ItemTest();
    public static final BlockPatternExpander PATTERN_EXPANDER = new BlockPatternExpander();
    public static final GuiHandler GUI_HANDLER = new GuiHandler();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        INSTANCE = this;
        LOGGER.info("Hello From {}!", NAME);
        PacketHandler.register();
        ModConfig.init(event.getSuggestedConfigurationFile());
        if (!net.minecraftforge.fml.common.Loader.isModLoaded("appliedenergistics2")) {
            LOGGER.warn("Applied Energistics 2 (AE2) or AE2UEL not found!");
            LOGGER.warn("The intelligent pattern system will not function without AE2UEL.");
            LOGGER.warn("This is normal in development environment. Install AE2UEL for full functionality.");
        } else {
            LOGGER.info("AE2/AE2UEL detected, initializing intelligent pattern system for AE2UEL...");
        }
        NetworkRegistry.INSTANCE.registerGuiHandler(this, GUI_HANDLER);
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(ITEM_TEST);

        event.getRegistry().register(new net.minecraft.item.ItemBlock(PATTERN_EXPANDER)
            .setRegistryName(PATTERN_EXPANDER.getRegistryName()));
    }

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(PATTERN_EXPANDER);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

        GameRegistry.registerTileEntity(TileEntityPatternExpander.class, 
            new net.minecraft.util.ResourceLocation(MODID, "pattern_expander"));
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {

        OreDictRecipeCache.init();
    }
    
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {

        event.registerServerCommand(new com.lwx1145.sampleintegration.command.CommandExpandPattern());
    }
}


