package com.lwx1145.sampleintegration;


import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.input.Keyboard;

public class GuiFluidAmountInput extends GuiScreen {

    private final GuiPatternEditor parent;
    private final ContainerPatternEditor container;
    private final int slotId;
    private final int initialAmount;
    private final String titleKey;
    private GuiTextField amountField;

    public GuiFluidAmountInput(GuiPatternEditor parent, ContainerPatternEditor container, int slotId, int initialAmount, String titleKey) {
        this.parent = parent;
        this.container = container;
        this.slotId = slotId;
        this.initialAmount = Math.max(1, initialAmount);
        this.titleKey = titleKey == null || titleKey.trim().isEmpty() ? "gui.sampleintegration.amount.set" : titleKey;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.amountField = new GuiTextField(0, this.fontRenderer, centerX - 60, centerY - 10, 120, 20);
        this.amountField.setText(Integer.toString(this.initialAmount));
        this.amountField.setFocused(true);
        this.buttonList.add(new GuiButton(0, centerX - 62, centerY + 20, 60, 20, I18n.format("gui.done")));
        this.buttonList.add(new GuiButton(1, centerX + 2, centerY + 20, 60, 20, I18n.format("gui.cancel")));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (keyCode == 1) {
            this.mc.displayGuiScreen(parent);
            return;
        }
        if (keyCode == 28 || keyCode == 156) {
            applyAmount();
            return;
        }
        if (amountField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws java.io.IOException {
        if (button.id == 0) {
            applyAmount();
            return;
        }
        if (button.id == 1) {
            this.mc.displayGuiScreen(parent);
            return;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.drawCenteredString(this.fontRenderer, I18n.format(this.titleKey), centerX, centerY - 30, 0xFFFFFF);
        this.amountField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void applyAmount() {
        int amount = this.initialAmount;
        try {
            amount = Integer.parseInt(this.amountField.getText());
        } catch (NumberFormatException e) {
            this.mc.player.sendMessage(new TextComponentTranslation("message.sampleintegration.invalid_amount"));
        }
        amount = Math.max(1, amount);
        PacketHandler.INSTANCE.sendToServer(new PacketSetFluidAmount(slotId, amount));
        this.mc.displayGuiScreen(parent);
    }
}

