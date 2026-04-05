package com.wuxiaoya.techstart.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class InvisibleButton extends AbstractWidget {
    @FunctionalInterface
    public interface OnPress {
        void onPress(InvisibleButton button);
    }

    private final OnPress onPress;
    private Component hint;

    public InvisibleButton(int x, int y, int width, int height, Component message, Component tooltip, OnPress onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.hint = tooltip;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (this.active && this.visible) {
            this.onPress.onPress(this);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }

    public Component getHint() {
        return this.hint;
    }

    public void setHint(Component tooltip) {
        this.hint = tooltip;
    }
}
