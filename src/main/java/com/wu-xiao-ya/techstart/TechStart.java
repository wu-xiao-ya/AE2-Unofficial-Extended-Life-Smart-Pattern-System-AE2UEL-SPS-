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
import org.apache.logging.log4j.Logger; // ← 确保此行存在
import net.minecraftforge.fml.common.FMLCommonHandler;
import java.io.File;

@Mod(modid = TechStart.MODID, name = TechStart.NAME, version = TechStart.VERSION)
@Mod.EventBusSubscriber(modid = TechStart.MODID)
public class TechStart {
    public static final String MODID = "sampleintegration";
    public static final String NAME = "sampleintegration";
    public static final String VERSION = "1.0.3";
    
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    // Mod实例
    public static TechStart INSTANCE;

    // 物品实例
    public static final ItemTest itemTest = new ItemTest();
    public static final ItemTest ITEM_TEST = itemTest; // 保持向后兼容

    // 方块实例
    public static final BlockPatternExpander PATTERN_EXPANDER = new BlockPatternExpander();

    // GUI处理器
    public static final GuiHandler GUI_HANDLER = new GuiHandler();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        INSTANCE = this;
        LOGGER.info("Hello From {}!", NAME);

        // 注册网络包处理器
        PacketHandler.register();

        // 读取配置文件
        ModConfig.init(event.getSuggestedConfigurationFile());
        
        // 不再注册ServerEventHandler - FMLServerStartingEvent通过@Mod.EventHandler处理
        
        // 检查AE2/AE2UEL依赖
        if (!net.minecraftforge.fml.common.Loader.isModLoaded("appliedenergistics2")) {
            LOGGER.warn("Applied Energistics 2 (AE2) or AE2UEL not found!");
            LOGGER.warn("The intelligent pattern system will not function without AE2UEL.");
            LOGGER.warn("This is normal in development environment. Install AE2UEL for full functionality.");
            // 暂时注释掉强制检查，允许模组在开发环境中加载
            // throw new RuntimeException("Missing required dependency: Applied Energistics 2 / AE2UEL");
        } else {
            LOGGER.info("AE2/AE2UEL detected, initializing intelligent pattern system for AE2UEL...");
        }

        // 注册GUI处理器
        NetworkRegistry.INSTANCE.registerGuiHandler(this, GUI_HANDLER);
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(ITEM_TEST);
        // 注册方块对应的ItemBlock
        event.getRegistry().register(new net.minecraft.item.ItemBlock(PATTERN_EXPANDER)
            .setRegistryName(PATTERN_EXPANDER.getRegistryName()));
    }

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(PATTERN_EXPANDER);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // 注册TileEntity
        GameRegistry.registerTileEntity(TileEntityPatternExpander.class, 
            new net.minecraft.util.ResourceLocation(MODID, "pattern_expander"));
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // 启动时扫描矿物辞典并缓存配方
        OreDictRecipeCache.init();
    }
    
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        // 注册通配符样板展开命令
        event.registerServerCommand(new com.lwx1145.techstart.command.CommandExpandPattern());
    }
}