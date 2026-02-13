package com.lwx1145.techstart;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;

/**
 * Register JEI ghost handlers for our pattern GUIs.
 */
@JEIPlugin
public class JEIIntegrationPlugin implements IModPlugin {

    @Override
    public void register(IModRegistry registry) {
        try {
            PatternGhostHandler handler = new PatternGhostHandler();
            registry.addGhostIngredientHandler(GuiPatternEditor.class, handler);
        } catch (Throwable t) {
            // Defensive: do not crash if JEI API differs or registration fails
        }
    }

}
