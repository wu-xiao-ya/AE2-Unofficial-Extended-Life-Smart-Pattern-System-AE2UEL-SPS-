package com.lwx1145.techstart;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/**
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 */
public class InvisibleButton extends GuiButton {

    private final String tooltip;

    public InvisibleButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        this(buttonId, x, y, widthIn, heightIn, buttonText, null);
    }

    public InvisibleButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, String tooltip) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        // EN: Temporary fallback comment after encoding recovery.
        // ZH: 编码修复后使用的临时兜底注释。
        this.visible = true;
        this.tooltip = tooltip;
    }

    public String getTooltip() {
        return this.tooltip;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
    }
}
