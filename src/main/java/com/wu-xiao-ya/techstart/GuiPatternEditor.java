package com.lwx1145.techstart;

// EN: Original comment text was corrupted by encoding.
// ZH: 原注释因编码问题已损坏。

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import java.util.Collections;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class GuiPatternEditor extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation("sampleintegration", "textures/gui/pattern_editor.png");
    private final EntityPlayer player;
    private final ContainerPatternEditor container;
    private static final int BUTTON_SWITCH = 0;

    public GuiPatternEditor(ContainerPatternEditor container, EntityPlayer player) {
        super(container);
        this.container = container;
        this.player = player;
        this.xSize = 201;
        this.ySize = 220;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;
        // EN: Original comment text was corrupted by encoding.
        // ZH: 原注释因编码问题已损坏。
        this.buttonList.add(new InvisibleButton(BUTTON_SWITCH, guiLeft + 180, guiTop + 2, 20, 20, "2",
            "Switch smart pattern / blacklist mode"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BUTTON_SWITCH) {
            PacketHandler.INSTANCE.sendToServer(new PacketRequestEncodePattern());
            PacketHandler.INSTANCE.sendToServer(new PacketOpenPatternGui(GuiHandler.PATTERN_FILTER_GUI));
        }
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
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        if (mouseButton == 2) {
            Slot slot = this.getSlotUnderMouse();
            if (slot != null && container.isPatternSlotId(slot.slotNumber)) {
                ItemStack stack = slot.getStack();
                if (container.isItemMarkerStack(stack)) {
                    int amount = container.getMarkerAmountForSlot(slot.slotNumber);
                    this.mc.displayGuiScreen(new GuiFluidAmountInput(this, container, slot.slotNumber, amount, "Set Amount"));
                    return;
                }
                if (container.isFluidMarkerStack(stack)) {
                    int amount = container.getMarkerAmountForSlot(slot.slotNumber);
                    this.mc.displayGuiScreen(new GuiFluidAmountInput(this, container, slot.slotNumber, amount, "Set Amount (mB)"));
                    return;
                }
            }
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = "Smart Mode Editor";
        this.fontRenderer.drawString(title, (this.xSize - this.fontRenderer.getStringWidth(title)) / 2, 2, 4210752);
        this.fontRenderer.drawString("Input", 38, 20, 0x404040);
        this.fontRenderer.drawString("Output", 109, 20, 0x404040);
        this.fontRenderer.drawString("Tip: place a fluid/gas container to add a marker", 15, 110, 0x404040);
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta == 0) {
            return;
        }
        Slot slot = this.getSlotUnderMouse();
        if (slot == null || !container.isPatternSlotId(slot.slotNumber)) {
            return;
        }
        ItemStack stack = slot.getStack();
        if (stack.isEmpty() || !container.isMarkerStack(stack)) {
            return;
        }
        int step = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) ? 10 : 1;
        int amount = container.getMarkerAmountForSlot(slot.slotNumber);
        int next = delta > 0 ? amount + step : amount - step;
        next = Math.max(1, next);
        PacketHandler.INSTANCE.sendToServer(new PacketSetFluidAmount(slot.slotNumber, next));
        container.applyMarkerAmountToSlot(slot.slotNumber, next);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
        // EN: Temporary fallback comment after encoding recovery.
        // ZH: 编码修复后使用的临时兜底注释。
        for (GuiButton btn : this.buttonList) {
            if (!(btn instanceof InvisibleButton)) continue;
            if (!btn.visible) continue;
            int bx = btn.x;
            int by = btn.y;
            if (mouseX >= bx && mouseX < bx + btn.width && mouseY >= by && mouseY < by + btn.height) {
                String tip = ((InvisibleButton) btn).getTooltip();
                if (tip != null && !tip.isEmpty()) {
                    this.drawHoveringText(Collections.singletonList(tip), mouseX, mouseY);
                }
            }
        }
    }

    public int getGuiLeft() {
        return this.guiLeft;
    }

    public int getGuiTop() {
        return this.guiTop;
    }

}
