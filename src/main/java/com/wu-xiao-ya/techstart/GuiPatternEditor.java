// 样板编辑器GUI
package com.lwx1145.techstart;

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
    }

    @Override
    public void drawSlot(Slot slot) {
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
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        if (mouseButton == 2) {
            Slot slot = this.getSlotUnderMouse();
            if (slot != null && container.isPatternSlotId(slot.slotNumber)) {
                ItemStack stack = slot.getStack();
                if (container.isFluidMarkerStack(stack)) {
                    int amount = container.getFluidAmountForSlot(slot.slotNumber);
                    this.mc.displayGuiScreen(new GuiFluidAmountInput(this, container, slot.slotNumber, amount));
                    return;
                }
            }
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = "智能模式编辑器";
        this.fontRenderer.drawString(title, (this.xSize - this.fontRenderer.getStringWidth(title)) / 2, 2, 4210752);

        // 显示输入和输出区域标签
        this.fontRenderer.drawString("输入内容", 38, 20, 0x404040);
        this.fontRenderer.drawString("输出内容", 109, 20, 0x404040);
        
        // 显示流体信息提示
        this.fontRenderer.drawString("§e提示: 放入流体或气体容器来添加需求", 15, 110, 0x404040);

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