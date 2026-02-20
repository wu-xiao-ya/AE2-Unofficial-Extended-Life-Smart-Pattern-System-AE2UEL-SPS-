package com.lwx1145.sampleintegration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/**
 */
public class InvisibleButton extends GuiButton {

    private final String tooltip;

    public InvisibleButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        this(buttonId, x, y, widthIn, heightIn, buttonText, null);
    }

    public InvisibleButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, String tooltip) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);

        this.visible = true;
        this.tooltip = tooltip;
    }

    public String getTooltip() {
        return this.tooltip;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {

    }
}

