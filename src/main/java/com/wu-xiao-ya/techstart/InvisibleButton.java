package com.lwx1145.techstart;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/**
 * 不渲染但保留交互的按钮类。
 * drawButton 被重写为空实现以隐藏显示，但 mousePressed 等方法仍然按 GuiButton 的逻辑生效。
 */
public class InvisibleButton extends GuiButton {

    private final String tooltip;

    public InvisibleButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        this(buttonId, x, y, widthIn, heightIn, buttonText, null);
    }

    public InvisibleButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, String tooltip) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        // 保持 visible=true 以便 mousePressed 可被触发
        this.visible = true;
        this.tooltip = tooltip;
    }

    public String getTooltip() {
        return this.tooltip;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        // 故意不绘制任何内容 — 保持不可见
    }
}
