package com.lwx1145.techstart;


import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

@Mod.EventBusSubscriber(modid = TechStart.MODID)
public class ModConfig {
    private static Configuration config;

    public static int patternExpanderScanIntervalTicks = 300;
    // EN: Original comment text was corrupted by encoding.
    // ZH: 原注释因编码问题已损坏。
    public static String[] customOrePrefixes = new String[]{"gem", "cluster"};
    // EN: Original comment text was corrupted by encoding.
    // ZH: 原注释因编码问题已损坏。
    public static String[] customOreNames = new String[]{"ingotWeirdCopper", "mymod_special_plate"};
    // EN: Original comment text was corrupted by encoding.
    // ZH: 原注释因编码问题已损坏。
    public static String[] customRecipePairs = new String[]{"ingotCopper -> plateCopper"};

    public static void init(File configFile) {
        config = new Configuration(configFile);
        sync();
    }

    private static void sync() {
        String category = "pattern_expander";
        config.addCustomCategoryComment(category, "Pattern Expander settings");
        patternExpanderScanIntervalTicks = config.getInt(
            "scanIntervalTicks",
            category,
            patternExpanderScanIntervalTicks,
            20,
            1200,
            "Scan interval in ticks (20 ticks = 1 second)."
        );
        customOrePrefixes = config.getStringList(
            "customOrePrefixes",
            category,
            customOrePrefixes,
            "Additional ore name prefixes to support (e.g., ingot, plate, inogt)."
        );
        customOreNames = config.getStringList(
            "customOreNames",
            category,
            customOreNames,
            "Ore dictionary names to seed before scanning (e.g., ingotCopper, plateCopper)."
        );
        customRecipePairs = config.getStringList(
            "customRecipePairs",
            category,
            customRecipePairs,
            "Explicit ore pairs (e.g., 'ingotCopper -> plateCopper'). Config pairs are applied before OreDictionary scan."
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
