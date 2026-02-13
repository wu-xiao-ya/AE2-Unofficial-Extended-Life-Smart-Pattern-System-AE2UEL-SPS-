package com.lwx1145.techstart;

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
import org.apache.logging.log4j.Logger; // EN: Original comment text was corrupted by encoding.
// ZH: 原注释因编码问题已损坏。
import net.minecraftforge.fml.common.FMLCommonHandler;
import java.io.File;

@Mod(modid = TechStart.MODID, name = TechStart.NAME, version = TechStart.VERSION)
@Mod.EventBusSubscriber(modid = TechStart.MODID)
public class TechStart {
    public static final String MODID = "sampleintegration";
    public static final String NAME = "sampleintegration";
    public static final String VERSION = "1.0.3";
    
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    // EN: Temporary fallback comment after encoding recovery.
    // ZH: 编码修复后使用的临时兜底注释。
    public static TechStart INSTANCE;

    // EN: Temporary fallback comment after encoding recovery.
    // ZH: 编码修复后使用的临时兜底注释。
    public static final ItemTest itemTest = new ItemTest();
    public static final ItemTest ITEM_TEST = itemTest; // EN: Original comment text was corrupted by encoding.
    // ZH: 原注释因编码问题已损坏。

    // EN: Original comment text was corrupted by encoding.
    // ZH: 原注释因编码问题已损坏。
    public static final BlockPatternExpander PATTERN_EXPANDER = new BlockPatternExpander();

    // EN: Original comment text was corrupted by encoding.
    // ZH: 原注释因编码问题已损坏。
    public static final GuiHandler GUI_HANDLER = new GuiHandler();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        INSTANCE = this;
        LOGGER.info("Hello From {}!", NAME);

        // EN: Temporary fallback comment after encoding recovery.
        // ZH: 编码修复后使用的临时兜底注释。
        PacketHandler.register();

        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        ModConfig.init(event.getSuggestedConfigurationFile());
        
        // EN: Temporary fallback comment after encoding recovery.
        // ZH: 编码修复后使用的临时兜底注释。
        
        // EN: Temporary fallback comment after encoding recovery.
        // ZH: 编码修复后使用的临时兜底注释。
        if (!net.minecraftforge.fml.common.Loader.isModLoaded("appliedenergistics2")) {
            LOGGER.warn("Applied Energistics 2 (AE2) or AE2UEL not found!");
            LOGGER.warn("The intelligent pattern system will not function without AE2UEL.");
            LOGGER.warn("This is normal in development environment. Install AE2UEL for full functionality.");
            // EN: Temporary fallback comment after encoding recovery.
            // ZH: 编码修复后使用的临时兜底注释。
            // throw new RuntimeException("Missing required dependency: Applied Energistics 2 / AE2UEL");
        } else {
            LOGGER.info("AE2/AE2UEL detected, initializing intelligent pattern system for AE2UEL...");
        }

        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        NetworkRegistry.INSTANCE.registerGuiHandler(this, GUI_HANDLER);
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(ITEM_TEST);
        // EN: Temporary fallback comment after encoding recovery.
        // ZH: 编码修复后使用的临时兜底注释。
        event.getRegistry().register(new net.minecraft.item.ItemBlock(PATTERN_EXPANDER)
            .setRegistryName(PATTERN_EXPANDER.getRegistryName()));
    }

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(PATTERN_EXPANDER);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // EN: Temporary fallback comment after encoding recovery.
        // ZH: 编码修复后使用的临时兜底注释。
        GameRegistry.registerTileEntity(TileEntityPatternExpander.class, 
            new net.minecraft.util.ResourceLocation(MODID, "pattern_expander"));
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        OreDictRecipeCache.init();
    }
    
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        event.registerServerCommand(new com.lwx1145.techstart.command.CommandExpandPattern());
    }
}
