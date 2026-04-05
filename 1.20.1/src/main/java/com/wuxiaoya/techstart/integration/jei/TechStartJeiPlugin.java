package com.wuxiaoya.techstart.integration.jei;

import com.wuxiaoya.techstart.TechStartForge;
import com.wuxiaoya.techstart.client.PatternEditorScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class TechStartJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(TechStartForge.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(PatternEditorScreen.class, new PatternEditorGhostHandler());
    }
}
