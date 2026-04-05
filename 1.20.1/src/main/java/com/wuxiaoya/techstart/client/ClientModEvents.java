package com.wuxiaoya.techstart.client;

import com.wuxiaoya.techstart.TechStartForge;
import com.wuxiaoya.techstart.registry.TechStartMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientModEvents {
    private ClientModEvents() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(TechStartMenus.PATTERN_EDITOR_ITEM.get(), PatternEditorScreen::new);
            TechStartForge.LOGGER.info("Registered screen for menu: {}:pattern_editor", TechStartForge.MODID);
        });
    }
}
