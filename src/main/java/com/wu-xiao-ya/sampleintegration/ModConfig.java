package com.lwx1145.sampleintegration;


import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

@Mod.EventBusSubscriber(modid = TechStart.MODID)
public class ModConfig {
    private static Configuration config;

    public static int patternExpanderScanIntervalTicks = 300;

    public static String[] customOrePrefixes = new String[]{"gem", "cluster"};

    public static String[] customOreNames = new String[]{"ingotWeirdCopper", "mymod_special_plate"};

    public static String[] customRecipePairs = new String[]{"ingotCopper -> plateCopper"};

    public static void init(File configFile) {
        config = new Configuration(configFile);
        sync();
    }

    private static void sync() {
        String category = "pattern_expander";
        config.addCustomCategoryComment(category, "模式扩展器设置 / Pattern Expander settings");
        patternExpanderScanIntervalTicks = config.getInt(
            "scanIntervalTicks",
            category,
            patternExpanderScanIntervalTicks,
            20,
            1200,
            "扫描间隔（单位：游戏刻，20刻 = 1秒） / Scan interval in ticks (20 ticks = 1 second)."
        );
        customOrePrefixes = config.getStringList(
            "customOrePrefixes",
            category,
            customOrePrefixes,
            "要支持的额外矿物名称前缀（例如：ingot, plate, ore）。\n"
                + "Additional ore name prefixes to support (e.g., ingot, plate, ore)."
        );
        customOreNames = config.getStringList(
            "customOreNames",
            category,
            customOreNames,
            "扫描前预置的矿物词典名称（例如：ingotCopper, plateCopper）。\n"
                + "Ore dictionary names to seed before scanning (e.g., ingotCopper, plateCopper)."
        );
        customRecipePairs = config.getStringList(
            "customRecipePairs",
            category,
            customRecipePairs,
            "显式矿物配对（例如：'ingotCopper -> plateCopper'），会在矿词扫描前应用。\n"
                + "Explicit ore pairs (e.g., 'ingotCopper -> plateCopper'). Applied before OreDictionary scan."
        );
        if (config.hasChanged()) {
            config.save();
        }
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(TechStart.MODID)) {
            sync();
        }
    }
}


