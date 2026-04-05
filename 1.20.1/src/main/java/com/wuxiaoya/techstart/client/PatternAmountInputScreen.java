package com.wuxiaoya.techstart.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class PatternAmountInputScreen extends Screen {
    private final PatternEditorScreen parent;
    private final int slotId;
    private final int currentAmount;
    private final int maxAmount;

    private EditBox amountInput;
    private Component validationMessage = Component.empty();

    public PatternAmountInputScreen(PatternEditorScreen parent, int slotId, int currentAmount, int maxAmount, Component title) {
        super(title);
        this.parent = parent;
        this.slotId = slotId;
        this.maxAmount = Math.max(1, maxAmount);
        this.currentAmount = Mth.clamp(currentAmount, 1, this.maxAmount);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.amountInput = new EditBox(this.font, centerX - 60, centerY - 10, 120, 20, Component.empty());
        this.amountInput.setValue(String.valueOf(this.currentAmount));
        this.amountInput.setMaxLength(Math.max(1, String.valueOf(this.maxAmount).length()));
        this.amountInput.setFilter(value -> value.isEmpty() || value.matches("\\d+"));
        this.amountInput.setResponder(ignored -> this.validationMessage = Component.empty());
        this.addRenderableWidget(this.amountInput);
        this.setInitialFocus(this.amountInput);

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.techstart.amount.confirm"),
                        button -> applyAmountAndClose())
                .bounds(centerX - 60, centerY + 18, 58, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.techstart.amount.cancel"),
                        button -> closeToParent())
                .bounds(centerX + 2, centerY + 18, 58, 20)
                .build());
    }

    @Override
    public void onClose() {
        closeToParent();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            applyAmountAndClose();
            return true;
        }
        if (keyCode == 256) {
            closeToParent();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 36, 0xFFFFFF);
        Component hint = Component.translatable("gui.techstart.amount.hint", this.maxAmount);
        guiGraphics.drawCenteredString(this.font, hint, this.width / 2, this.height / 2 - 22, 0xA0A0A0);
        if (!this.validationMessage.getString().isBlank()) {
            guiGraphics.drawCenteredString(this.font, this.validationMessage, this.width / 2, this.height / 2 + 42, 0xFF5555);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void applyAmountAndClose() {
        if (this.amountInput == null) {
            closeToParent();
            return;
        }
        String raw = this.amountInput.getValue().trim();
        if (raw.isEmpty()) {
            this.validationMessage = Component.translatable("gui.techstart.amount.invalid");
            return;
        }

        int parsed;
        try {
            parsed = Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            this.validationMessage = Component.translatable("gui.techstart.amount.invalid");
            return;
        }

        int amount = Mth.clamp(parsed, 1, this.maxAmount);
        this.parent.applyPatternSlotAmount(this.slotId, amount);
        closeToParent();
    }

    private void closeToParent() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
