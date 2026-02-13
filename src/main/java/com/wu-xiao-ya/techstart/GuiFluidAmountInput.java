package com.lwx1145.techstart;


import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.input.Keyboard;

public class GuiFluidAmountInput extends GuiScreen {

    private final GuiPatternEditor parent;
    private final ContainerPatternEditor container;
    private final int slotId;
    private final int initialAmount;
    private final String title;
    private GuiTextField amountField;

    public GuiFluidAmountInput(GuiPatternEditor parent, ContainerPatternEditor container, int slotId, int initialAmount, String title) {
        this.parent = parent;
        this.container = container;
        this.slotId = slotId;
        this.initialAmount = Math.max(1, initialAmount);
        this.title = title == null || title.trim().isEmpty() ? "Set Amount" : title;
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
        this.buttonList.add(new GuiButton(0, centerX - 62, centerY + 20, 60, 20, "OK"));
        this.buttonList.add(new GuiButton(1, centerX + 2, centerY + 20, 60, 20, "Cancel"));
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
        this.drawCenteredString(this.fontRenderer, this.title, centerX, centerY - 30, 0xFFFFFF);
        this.amountField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void applyAmount() {
        int amount = this.initialAmount;
        try {
            amount = Integer.parseInt(this.amountField.getText());
        } catch (NumberFormatException e) {
            this.mc.player.sendMessage(new TextComponentString("Invalid amount"));
        }
        amount = Math.max(1, amount);
        PacketHandler.INSTANCE.sendToServer(new PacketSetFluidAmount(slotId, amount));
        this.mc.displayGuiScreen(parent);
    }
}
