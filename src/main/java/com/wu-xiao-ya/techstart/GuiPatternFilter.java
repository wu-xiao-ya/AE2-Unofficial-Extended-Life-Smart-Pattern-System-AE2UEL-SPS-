package com.lwx1145.techstart;

// 鏍锋澘榛戠櫧鍚嶅崟GUI

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import java.util.Collections;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

public class GuiPatternFilter extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation("sampleintegration", "textures/gui/pattern_editor.png");
    private final EntityPlayer player;
    private final ContainerPatternFilter container;
    private static final int BUTTON_SWITCH = 0;
    private static final int BUTTON_MODE = 1;
    private static final int LIST_ROWS = 3;
    private static final int LIST_COLS = 3;
    private static final int SLOT_SIZE = 18;
    private static final int INPUT_X = 26;
    private static final int INPUT_Y = 35;
    private static final int OUTPUT_X = 98;
    private static final int OUTPUT_Y = 35;
    private int listScroll = 0;

    public GuiPatternFilter(ContainerPatternFilter container, EntityPlayer player) {
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
        // 将切换按钮固定在 GUI 内部像素 (180,2) -> (199,21)，尺寸 20x20
        this.buttonList.add(new InvisibleButton(BUTTON_SWITCH, guiLeft + 180, guiTop + 2, 20, 20, "1",
            "切换到智能样板模式/黑白名单模式"));
        // 将黑白名单模式按钮固定在 GUI 内部像素 (180,26) -> (199,45)，尺寸 20x20（不可见但保留交互）
        this.buttonList.add(new InvisibleButton(BUTTON_MODE, guiLeft + 180, guiTop + 26, 20, 20, "W",
            "切换黑白名单功能"));
        updateButtonLabels();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BUTTON_SWITCH) {
            PacketHandler.INSTANCE.sendToServer(new PacketRequestEncodePattern());
            PacketHandler.INSTANCE.sendToServer(new PacketOpenPatternGui(GuiHandler.PATTERN_EDITOR_GUI));
            return;
        }
        if (button.id == BUTTON_MODE) {
            int mode = container.getFilterMode();
            int next = mode == ItemTest.FILTER_MODE_BLACKLIST ? ItemTest.FILTER_MODE_WHITELIST : ItemTest.FILTER_MODE_BLACKLIST;
            PacketHandler.INSTANCE.sendToServer(PacketUpdatePatternFilter.forMode(next));
            container.applyFilterMode(next);
            container.clearFilterEntries();
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
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0 || mouseButton == 1) {
            handleFilterClick(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = "黑白名单";
        this.fontRenderer.drawString(title, (this.xSize - this.fontRenderer.getStringWidth(title)) / 2, 2, 4210752);
        drawFilterScreen(mouseX, mouseY);
        drawFilterTooltip(mouseX, mouseY);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
        // 显示不可见按钮的悬浮提示
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

    @Override
    public void updateScreen() {
        super.updateScreen();
        updateButtonLabels();
    }

    private void updateButtonLabels() {
        for (GuiButton button : this.buttonList) {
            if (button.id == BUTTON_MODE) {
                int mode = container.getFilterMode();
                button.displayString = mode == ItemTest.FILTER_MODE_BLACKLIST ? "B" : "W";
            }
        }
    }

    private void drawFilterScreen(int mouseX, int mouseY) {
        int mode = container.getFilterMode();
        String modeText = mode == ItemTest.FILTER_MODE_BLACKLIST ? "黑名单" : "白名单";
        this.fontRenderer.drawString("模式: " + modeText, 26, 20, 0x404040);

        List<RecipeEntry> recipes = getRecipeEntries();
        int maxScroll = Math.max(0, (int) Math.ceil(recipes.size() / 9.0) - 1);
        if (listScroll > maxScroll) {
            listScroll = maxScroll;
        }

        List<String> selected = container.getFilterEntries();
        int startIndex = listScroll * 9;
        RenderHelper.enableGUIStandardItemLighting();
        for (int row = 0; row < LIST_ROWS; row++) {
            for (int col = 0; col < LIST_COLS; col++) {
                int idx = startIndex + row * LIST_COLS + col;
                if (idx >= recipes.size()) {
                    break;
                }
                RecipeEntry entry = recipes.get(idx);
                int inputX = INPUT_X + col * SLOT_SIZE;
                int inputY = INPUT_Y + row * SLOT_SIZE;
                int outputX = OUTPUT_X + col * SLOT_SIZE;
                int outputY = OUTPUT_Y + row * SLOT_SIZE;
                drawSlotItem(inputX, inputY, entry.inputOre, selected.contains(entry.id), mode);
                drawSlotItem(outputX, outputY, entry.outputOre, selected.contains(entry.id), mode);
            }
        }
        RenderHelper.disableStandardItemLighting();

        drawScrollBar(OUTPUT_X + LIST_COLS * SLOT_SIZE + 2, INPUT_Y, LIST_ROWS * SLOT_SIZE, recipes.size(), maxScroll);
    }

    private void drawScrollBar(int x, int y, int height, int total, int maxScroll) {
        int barHeight = Math.max(12, height / 3);
        int barY = y;
        if (maxScroll > 0) {
            float ratio = listScroll / (float) maxScroll;
            barY = y + Math.round((height - barHeight) * ratio);
        }
        drawRect(x, y, x + 6, y + height, 0xFFCCCCCC);
        drawRect(x, barY, x + 6, barY + barHeight, 0xFF888888);
    }

    private void handleFilterClick(int mouseX, int mouseY, int mouseButton) {
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;
        int listHeight = LIST_ROWS * SLOT_SIZE;
        int inputX = guiLeft + INPUT_X;
        int inputY = guiTop + INPUT_Y;
        int outputX = guiLeft + OUTPUT_X;
        int outputY = guiTop + OUTPUT_Y;
        List<RecipeEntry> recipes = getRecipeEntries();
        List<String> selected = container.getFilterEntries();
        int maxScroll = Math.max(0, (int) Math.ceil(recipes.size() / 9.0) - 1);

        if (mouseX >= inputX && mouseX < inputX + LIST_COLS * SLOT_SIZE && mouseY >= inputY && mouseY < inputY + listHeight) {
            int col = (mouseX - inputX) / SLOT_SIZE;
            int row = (mouseY - inputY) / SLOT_SIZE;
            int idx = listScroll * 9 + row * LIST_COLS + col;
            if (idx >= 0 && idx < recipes.size()) {
                updateFilterSelection(recipes.get(idx).id, selected, mouseButton);
                return;
            }
        }

        if (mouseX >= outputX && mouseX < outputX + LIST_COLS * SLOT_SIZE && mouseY >= outputY && mouseY < outputY + listHeight) {
            int col = (mouseX - outputX) / SLOT_SIZE;
            int row = (mouseY - outputY) / SLOT_SIZE;
            int idx = listScroll * 9 + row * LIST_COLS + col;
            if (idx >= 0 && idx < recipes.size()) {
                updateFilterSelection(recipes.get(idx).id, selected, mouseButton);
                return;
            }
        }

        int scrollX = guiLeft + OUTPUT_X + LIST_COLS * SLOT_SIZE + 2;
        if (mouseX >= scrollX && mouseX < scrollX + 6 && mouseY >= inputY && mouseY < inputY + listHeight) {
            if (maxScroll > 0) {
                float ratio = (mouseY - inputY) / (float) listHeight;
                listScroll = Math.min(maxScroll, Math.max(0, Math.round(ratio * maxScroll)));
            }
        }
    }

    private List<RecipeEntry> getRecipeEntries() {
        List<RecipeEntry> result = new ArrayList<>();
        List<String[]> recipes = container.getAvailableRecipeTypes();
        for (String[] recipe : recipes) {
            if (recipe.length < 2) {
                continue;
            }
            String input = recipe[0];
            String output = recipe[1];
            String display = recipe.length > 2 ? recipe[2] : (input + " -> " + output);
            if (input == null || output == null || input.isEmpty() || output.isEmpty()) {
                continue;
            }
            String id = input + "->" + output;
            result.add(new RecipeEntry(id, display, input, output));
        }
        return result;
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta == 0) {
            return;
        }
        int step = delta > 0 ? -1 : 1;
        int maxScroll = Math.max(0, (int) Math.ceil(getRecipeEntries().size() / 9.0) - 1);
        listScroll = Math.max(0, Math.min(maxScroll, listScroll + step));
    }

    private ItemStack resolveDisplayStack(String oreName) {
        if (oreName == null || oreName.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> ores = OreDictionary.getOres(oreName);
        if (ores == null || ores.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = ores.get(0).copy();
        stack.setCount(1);
        return stack;
    }

    private void drawSlotItem(int x, int y, String oreName, boolean selected, int mode) {
        ItemStack stack = resolveDisplayStack(oreName);
        if (!stack.isEmpty()) {
            this.itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        }
        if (selected) {
            int color = mode == ItemTest.FILTER_MODE_BLACKLIST ? 0x66FF0000 : 0x6600FF00;
            drawRect(x, y, x + 16, y + 16, color);
        }
    }

    private void updateFilterSelection(String id, List<String> selected, int mouseButton) {
        if (id == null || id.isEmpty()) {
            return;
        }
        boolean contains = selected != null && selected.contains(id);
        if (mouseButton == 0 && !contains) {
            PacketHandler.INSTANCE.sendToServer(PacketUpdatePatternFilter.forToggle(id));
            container.toggleFilterEntry(id);
        } else if (mouseButton == 1 && contains) {
            PacketHandler.INSTANCE.sendToServer(PacketUpdatePatternFilter.forToggle(id));
            container.toggleFilterEntry(id);
        }
    }

    private void drawFilterTooltip(int mouseX, int mouseY) {
        RecipeEntry entry = getEntryAt(mouseX, mouseY);
        if (entry == null) {
            return;
        }
        List<String> lines = new ArrayList<>();
        lines.add(entry.displayName);
        appendOreTooltip(lines, "杈撳叆", entry.inputOre);
        appendOreTooltip(lines, "杈撳嚭", entry.outputOre);
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;
        this.drawHoveringText(lines, mouseX + guiLeft, mouseY + guiTop);
    }

    private void appendOreTooltip(List<String> lines, String label, String oreName) {
        if (oreName == null || oreName.isEmpty()) {
            lines.add(label + ": <empty>");
            return;
        }
        List<ItemStack> ores = OreDictionary.getOres(oreName);
        if (ores == null || ores.isEmpty()) {
            lines.add(label + ": " + oreName);
            return;
        }
        lines.add(label + ": " + oreName);
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
        for (ItemStack stack : ores) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            names.add(stack.getDisplayName());
            if (names.size() >= 8) {
                break;
            }
        }
        for (String name : names) {
            lines.add("- " + name);
        }
        if (ores.size() > names.size()) {
            lines.add("- ...");
        }
    }

    private RecipeEntry getEntryAt(int mouseX, int mouseY) {
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;
        int listHeight = LIST_ROWS * SLOT_SIZE;
        int inputX = guiLeft + INPUT_X;
        int inputY = guiTop + INPUT_Y;
        int outputX = guiLeft + OUTPUT_X;
        int outputY = guiTop + OUTPUT_Y;
        List<RecipeEntry> recipes = getRecipeEntries();
        if (mouseX >= inputX && mouseX < inputX + LIST_COLS * SLOT_SIZE && mouseY >= inputY && mouseY < inputY + listHeight) {
            int col = (mouseX - inputX) / SLOT_SIZE;
            int row = (mouseY - inputY) / SLOT_SIZE;
            int idx = listScroll * 9 + row * LIST_COLS + col;
            if (idx >= 0 && idx < recipes.size()) {
                return recipes.get(idx);
            }
        }
        if (mouseX >= outputX && mouseX < outputX + LIST_COLS * SLOT_SIZE && mouseY >= outputY && mouseY < outputY + listHeight) {
            int col = (mouseX - outputX) / SLOT_SIZE;
            int row = (mouseY - outputY) / SLOT_SIZE;
            int idx = listScroll * 9 + row * LIST_COLS + col;
            if (idx >= 0 && idx < recipes.size()) {
                return recipes.get(idx);
            }
        }
        return null;
    }

    private static class RecipeEntry {
        final String id;
        final String displayName;
        final String inputOre;
        final String outputOre;

        RecipeEntry(String id, String displayName, String inputOre, String outputOre) {
            this.id = id;
            this.displayName = displayName;
            this.inputOre = inputOre;
            this.outputOre = outputOre;
        }
    }
}
