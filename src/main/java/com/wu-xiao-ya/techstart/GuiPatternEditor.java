// 样板编辑器GUI
package com.lwx1145.techstart;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class GuiPatternEditor extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation("sampleintegration", "textures/gui/pattern_editor.png");
    private final EntityPlayer player;
    private final ContainerPatternEditor container;
    private GuiTextField inputCountField;
    private GuiTextField outputCountField;

    public GuiPatternEditor(ContainerPatternEditor container, EntityPlayer player) {
        super(container);
        this.container = container;
        this.player = player;
        this.xSize = 176;
        this.ySize = 220; // 增加高度以容纳所有元素
    }

    @Override
    public void initGui() {
        super.initGui();
        // 输入数量文本输入框 (在输入槽位下方)
        this.inputCountField = new GuiTextField(1, this.fontRenderer, this.guiLeft + 43, this.guiTop + 55, 18, 12);
        this.inputCountField.setMaxStringLength(2);
        this.inputCountField.setText("1");

        // 输出数量文本输入框 (在输出槽位下方)
        this.outputCountField = new GuiTextField(2, this.fontRenderer, this.guiLeft + 115, this.guiTop + 55, 18, 12);
        this.outputCountField.setMaxStringLength(2);
        this.outputCountField.setText("1");
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        super.keyTyped(typedChar, keyCode);
        this.inputCountField.textboxKeyTyped(typedChar, keyCode);
        this.outputCountField.textboxKeyTyped(typedChar, keyCode);
        
        // 回车键提交
        if (keyCode == 28) { // 28 是回车键
            updateCountFromFields();
        }
    }

    private void updateCountFromFields() {
        ItemStack actualPatternStack = player.getHeldItemMainhand();
        if (actualPatternStack.isEmpty() || !(actualPatternStack.getItem() instanceof ItemTest)) {
            actualPatternStack = player.getHeldItemOffhand();
        }
        if (actualPatternStack.isEmpty() || !(actualPatternStack.getItem() instanceof ItemTest)) {
            return;
        }

        ItemTest patternItem = (ItemTest) actualPatternStack.getItem();

        // 更新输入数量
        try {
            int inputCount = Integer.parseInt(this.inputCountField.getText());
            if (inputCount > 0 && inputCount <= 64) {
                PacketHandler.INSTANCE.sendToServer(new PacketUpdatePatternCount(true, inputCount));
            } else {
                this.inputCountField.setText(String.valueOf(patternItem.getInputCount(actualPatternStack)));
            }
        } catch (NumberFormatException e) {
            this.inputCountField.setText(String.valueOf(patternItem.getInputCount(actualPatternStack)));
        }

        // 更新输出数量
        try {
            int outputCount = Integer.parseInt(this.outputCountField.getText());
            if (outputCount > 0 && outputCount <= 64) {
                PacketHandler.INSTANCE.sendToServer(new PacketUpdatePatternCount(false, outputCount));
            } else {
                this.outputCountField.setText(String.valueOf(patternItem.getOutputCount(actualPatternStack)));
            }
        } catch (NumberFormatException e) {
            this.outputCountField.setText(String.valueOf(patternItem.getOutputCount(actualPatternStack)));
        }
    }

    @Override
    public void drawSlot(Slot slot) {
        // 只隐藏槽位0和1的背景和边框，但保留物品显示
        if (slot.slotNumber == 0 || slot.slotNumber == 1) {
            ItemStack itemStack = slot.getStack();
            if (!itemStack.isEmpty()) {
                // 只绘制物品，不绘制槽位背景
                int x = slot.xPos;
                int y = slot.yPos;
                this.itemRender.renderItemIntoGUI(itemStack, x, y);
                this.itemRender.renderItemOverlays(this.fontRenderer, itemStack, x, y);
            }
            return;
        }
        super.drawSlot(slot);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.inputCountField.updateCursorCounter();
        this.outputCountField.updateCursorCounter();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.inputCountField.mouseClicked(mouseX, mouseY, mouseButton);
        this.outputCountField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = "智能模式编辑器";
        this.fontRenderer.drawString(title, (this.xSize - this.fontRenderer.getStringWidth(title)) / 2, 6, 4210752);

        // 显示输入和输出区域标签
        this.fontRenderer.drawString("输入内容", 25, 20, 0x404040);
        this.fontRenderer.drawString("输出内容", 100, 20, 0x404040);

        // 数量标签
        this.fontRenderer.drawString("输入数量", 38, 70, 0x808080);
        this.fontRenderer.drawString("输出数量", 120, 70, 0x808080);
        
            // 显示输入输出数量数字（不使用文本框，直接绘制数字）
            String inputCountStr = this.inputCountField.getText();
            String outputCountStr = this.outputCountField.getText();
            this.fontRenderer.drawString(inputCountStr, 42, 56, 0xFFFFFF);
            this.fontRenderer.drawString(outputCountStr, 114, 56, 0xFFFFFF);
        ItemStack actualPatternStack = player.getHeldItemMainhand();
        if (actualPatternStack.isEmpty() || !(actualPatternStack.getItem() instanceof ItemTest)) {
            actualPatternStack = player.getHeldItemOffhand();
        }
        
        if (!actualPatternStack.isEmpty() && actualPatternStack.getItem() instanceof ItemTest) {
            ItemTest patternItem = (ItemTest) actualPatternStack.getItem();
            int inputCount = patternItem.getInputCount(actualPatternStack);
            int outputCount = patternItem.getOutputCount(actualPatternStack);
            
            // 同步输入框的值
            try {
                if (!this.inputCountField.isFocused() && Integer.parseInt(this.inputCountField.getText()) != inputCount) {
                    this.inputCountField.setText(String.valueOf(inputCount));
                }
            } catch (NumberFormatException e) {
                this.inputCountField.setText(String.valueOf(inputCount));
            }
            try {
                if (!this.outputCountField.isFocused() && Integer.parseInt(this.outputCountField.getText()) != outputCount) {
                    this.outputCountField.setText(String.valueOf(outputCount));
                }
            } catch (NumberFormatException e) {
                this.outputCountField.setText(String.valueOf(outputCount));
            }

            // 显示输入物品信息
            ItemStack inputStack = container.getInputStack();
            if (!inputStack.isEmpty()) {
                String inputOreName = container.getOreName(inputStack);
                String inputText = "输入: " + inputStack.getDisplayName() + " (" + (inputOreName != null ? inputOreName : "未知") + ") x" + inputCount;
                this.fontRenderer.drawString(inputText, 25, 90, 0x00AA00);
            } else {
                this.fontRenderer.drawString("请放入输入物品", 25, 90, 0xFF5555);
            }

            // 显示输出物品信息
            ItemStack outputStack = container.getOutputStack();
            if (!outputStack.isEmpty()) {
                String outputOreName = container.getOreName(outputStack);
                String outputText = "输出: " + outputStack.getDisplayName() + " (" + (outputOreName != null ? outputOreName : "未知") + ") x" + outputCount;
                this.fontRenderer.drawString(outputText, 25, 105, 0x00AA00);
            } else {
                this.fontRenderer.drawString("请放入输出物品", 25, 105, 0xFF5555);
            }

        } else {
            this.fontRenderer.drawString("未找到智能模式物品", 8, 140, 0xFF5555);
            this.fontRenderer.drawString("请手持智能模式物品", 8, 150, 0xCCCCCC);
            this.fontRenderer.drawString("或将物品放入快捷栏", 8, 160, 0xCCCCCC);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    /**
     * GUI关闭时的处理
     */
    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        // 当关闭GUI时，容器的onContainerClosed会被自动调用
        // 这里不需要额外处理，服务器已经在onContainerClosed中返还物品了
    }
}