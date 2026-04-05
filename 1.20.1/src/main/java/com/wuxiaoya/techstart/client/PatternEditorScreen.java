package com.wuxiaoya.techstart.client;

import appeng.api.crafting.IPatternDetails;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wuxiaoya.techstart.TechStartForge;
import com.wuxiaoya.techstart.integration.ae2.TechStartPatternExpansion;
import com.wuxiaoya.techstart.integration.mekanism.MekanismGasHelper;
import com.wuxiaoya.techstart.menu.PatternEditorMenu;
import com.wuxiaoya.techstart.network.SetPatternSlotAmountPacket;
import com.wuxiaoya.techstart.network.TechStartNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PatternEditorScreen extends AbstractContainerScreen<PatternEditorMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TechStartForge.MODID, "textures/gui/pattern_editor.png");
    private static final String TAG_FLUID_MARKER = "TechStartFluidMarker";
    private static final String TAG_FLUID_NAME = "TechStartFluidName";
    private static final String TAG_FLUID_AMOUNT = "TechStartFluidAmount";
    private static final String TAG_GAS_MARKER = "TechStartGasMarker";
    private static final String TAG_GAS_NAME = "TechStartGasName";
    private static final String TAG_GAS_AMOUNT = "TechStartGasAmount";
    private static final String TAG_ITEM_MARKER = "TechStartItemMarker";
    private static final String TAG_ITEM_AMOUNT = "TechStartItemAmount";
    private static final String TAG_INPUTS = "TechStartInputs";
    private static final String TAG_OUTPUTS = "TechStartOutputs";
    private static final String TAG_STACK = "Stack";
    private static final String TAG_VIRTUAL_INPUT_STACKS = "VirtualInputStacks";
    private static final String TAG_VIRTUAL_OUTPUT_STACKS = "VirtualOutputStacks";
    private static final String TAG_VIRTUAL_FILTER_ENTRY_ID = "VirtualFilterEntryId";

    private static final int LIST_ROWS = 3;
    private static final int LIST_COLS = 3;
    private static final int SLOT_SIZE = 18;
    private static final int INPUT_X = 26;
    private static final int INPUT_Y = 35;
    private static final int OUTPUT_X = 98;
    private static final int OUTPUT_Y = 35;
    private static final int SWITCH_X = 180;
    private static final int SWITCH_Y = 2;
    private static final int MODE_X = 180;
    private static final int MODE_Y = 26;

    private InvisibleButton switchButton;
    private InvisibleButton modeButton;
    private boolean filterView = false;
    private int listScroll = 0;
    private final Set<String> selectedFilterEntries = new LinkedHashSet<>();

    public PatternEditorScreen(PatternEditorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 200;
        this.imageHeight = 207;
        this.inventoryLabelY = 110;
    }

    @Override
    protected void init() {
        super.init();

        this.switchButton = this.addRenderableWidget(new InvisibleButton(
                this.leftPos + SWITCH_X,
                this.topPos + SWITCH_Y,
                20,
                20,
                Component.literal("switch"),
                Component.translatable("gui.techstart.switch_to_filter"),
                ignored -> {
                    if (!this.filterView) {
                        this.menu.refreshPatternStackSnapshot();
                        this.selectedFilterEntries.clear();
                        this.selectedFilterEntries.addAll(this.menu.getFilterEntriesSnapshot());
                    }
                    this.filterView = !this.filterView;
                    if (!this.filterView) {
                        this.listScroll = 0;
                    }
                    updateModeButtons();
                }
        ));
        this.modeButton = this.addRenderableWidget(new InvisibleButton(
                this.leftPos + MODE_X,
                this.topPos + MODE_Y,
                20,
                20,
                Component.literal("mode"),
                Component.translatable("gui.techstart.toggle_filter_mode"),
                ignored -> {
                    int mode = this.menu.getFilterMode();
                    int next = mode == PatternEditorMenu.FILTER_MODE_WHITELIST
                            ? PatternEditorMenu.FILTER_MODE_BLACKLIST
                            : PatternEditorMenu.FILTER_MODE_WHITELIST;
                    sendMenuButton(next);
                    this.selectedFilterEntries.clear();
                    this.listScroll = 0;
                }
        ));

        this.selectedFilterEntries.clear();
        this.selectedFilterEntries.addAll(this.menu.getFilterEntriesSnapshot());

        updateModeButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateModeButtons();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component screenTitle = this.filterView
                ? Component.translatable("gui.techstart.pattern_filter")
                : this.title;
        guiGraphics.drawString(this.font, screenTitle, (this.imageWidth - this.font.width(screenTitle)) / 2, 2, 0x404040, false);

        if (!this.filterView) {
            guiGraphics.drawString(this.font, Component.translatable("gui.techstart.input"), 38, 20, 0x404040, false);
            guiGraphics.drawString(this.font, Component.translatable("gui.techstart.output"), 109, 20, 0x404040, false);
        }

        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        updatePatternSlotVisibility();
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.filterView) {
            redrawFilterSlotBackgrounds(guiGraphics);
            drawFilterOverlay(guiGraphics, mouseX, mouseY);
            renderFilterTooltip(guiGraphics, mouseX, mouseY);
        } else {
            renderPatternMarkerMaterials(guiGraphics);
            renderMarkerAmounts(guiGraphics);
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
        renderButtonTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (this.filterView) {
            if (handleFilterClick(mouseX, mouseY, mouseButton)) {
                return true;
            }
            if (isWithinPatternArea(mouseX, mouseY)) {
                return true;
            }
        }
        if (!this.filterView && mouseButton == 2) {
            Slot slot = this.getSlotUnderMouse();
                int slotId = resolvePatternSlotId(slot);
                if (slotId >= 0 && slot != null && slot.hasItem()) {
                    int amount = getLogicalSlotAmount(slot.getItem());
                    int maxAmount = getEditableAmountLimit(slot.getItem());
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new PatternAmountInputScreen(
                                this,
                            slotId,
                            amount,
                            maxAmount,
                            Component.translatable("gui.techstart.amount.set")
                    ));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.filterView) {
            return;
        }
        Slot slot = this.getSlotUnderMouse();
        List<Component> customTooltip = buildPatternSlotTooltip(slot);
        if (!customTooltip.isEmpty()) {
            guiGraphics.renderTooltip(this.font, customTooltip, Optional.empty(), mouseX, mouseY);
            return;
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.filterView && delta != 0 && isWithinPatternArea(mouseX, mouseY)) {
            int maxScroll = getMaxScroll(getFilterEntries().size());
            if (maxScroll > 0) {
                int step = delta > 0 ? -1 : 1;
                this.listScroll = Mth.clamp(this.listScroll + step, 0, maxScroll);
                return true;
            }
        }
        if (!this.filterView && delta != 0) {
            Slot slot = this.getSlotUnderMouse();
            int slotId = resolvePatternSlotId(slot);
            if (slotId >= 0 && slot != null && slot.hasItem()) {
                int current = getLogicalSlotAmount(slot.getItem());
                int step = hasShiftDown() ? 10 : 1;
                int max = getEditableAmountLimit(slot.getItem());
                int target = Mth.clamp(current + (delta > 0 ? step : -step), 1, max);
                if (target != current) {
                    applyPatternSlotAmount(slotId, target);
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void sendMenuButton(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    private void updatePatternSlotVisibility() {
        boolean visible = !this.filterView;
        for (int i = 0; i < 18 && i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
            if (slot instanceof PatternEditorMenu.PatternSlot patternSlot) {
                patternSlot.setActive(visible);
            }
        }
    }
    private void updateModeButtons() {
        if (this.switchButton == null || this.modeButton == null) {
            return;
        }
        this.modeButton.visible = this.filterView;
        this.modeButton.active = this.filterView;
        this.modeButton.setMessage(getModeComponent());
        this.modeButton.setHint(Component.translatable("gui.techstart.toggle_filter_mode"));
        this.switchButton.setHint(Component.translatable(this.filterView
                ? "gui.techstart.switch_to_editor"
                : "gui.techstart.switch_to_filter"));
        updatePatternSlotVisibility();
    }

    private Component getModeComponent() {
        return this.menu.getFilterMode() == PatternEditorMenu.FILTER_MODE_WHITELIST
                ? Component.translatable("gui.techstart.filter_mode_whitelist")
                : Component.translatable("gui.techstart.filter_mode_blacklist");
    }

    private void redrawFilterSlotBackgrounds(GuiGraphics guiGraphics) {
        int gridWidth = LIST_COLS * SLOT_SIZE;
        int gridHeight = LIST_ROWS * SLOT_SIZE;
        guiGraphics.blit(TEXTURE, this.leftPos + INPUT_X, this.topPos + INPUT_Y, INPUT_X, INPUT_Y, gridWidth, gridHeight, 256, 256);
        guiGraphics.blit(TEXTURE, this.leftPos + OUTPUT_X, this.topPos + OUTPUT_Y, OUTPUT_X, OUTPUT_Y, gridWidth, gridHeight, 256, 256);
    }

    private void drawFilterOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<FilterEntry> entries = getFilterEntries();
        int maxScroll = getMaxScroll(entries.size());
        if (this.listScroll > maxScroll) {
            this.listScroll = maxScroll;
        }

        int inputStartX = this.leftPos + INPUT_X;
        int inputStartY = this.topPos + INPUT_Y;
        int outputStartX = this.leftPos + OUTPUT_X;
        int outputStartY = this.topPos + OUTPUT_Y;
        int areaWidth = LIST_COLS * SLOT_SIZE;
        int areaHeight = LIST_ROWS * SLOT_SIZE;

        int startIndex = this.listScroll * 9;
        int mode = this.menu.getFilterMode();
        for (int row = 0; row < LIST_ROWS; row++) {
            for (int col = 0; col < LIST_COLS; col++) {
                int entryIndex = startIndex + row * LIST_COLS + col;
                if (entryIndex >= entries.size()) {
                    continue;
                }
                FilterEntry entry = entries.get(entryIndex);
                int inputX = inputStartX + col * SLOT_SIZE;
                int inputY = inputStartY + row * SLOT_SIZE;
                int outputX = outputStartX + col * SLOT_SIZE;
                int outputY = outputStartY + row * SLOT_SIZE;

                renderFilterEntryStack(guiGraphics, entry.input(), inputX, inputY);
                renderFilterEntryStack(guiGraphics, entry.output(), outputX, outputY);

                if (isFilterEntrySelected(entry)) {
                    int color = mode == PatternEditorMenu.FILTER_MODE_BLACKLIST ? 0x66FF0000 : 0x6600FF00;
                    guiGraphics.fill(inputX, inputY, inputX + 16, inputY + 16, color);
                    guiGraphics.fill(outputX, outputY, outputX + 16, outputY + 16, color);
                }
            }
        }

        if (maxScroll > 0) {
            drawScrollBar(guiGraphics, this.leftPos + OUTPUT_X + LIST_COLS * SLOT_SIZE + 2, this.topPos + INPUT_Y, entries.size(), maxScroll);
        }
    }

    private void drawScrollBar(GuiGraphics guiGraphics, int x, int y, int total, int maxScroll) {
        int height = LIST_ROWS * SLOT_SIZE;
        int barHeight = Math.max(12, height / 3);
        int barY = y;
        if (maxScroll > 0 && total > 0) {
            float ratio = this.listScroll / (float) maxScroll;
            barY = y + Math.round((height - barHeight) * ratio);
        }
        guiGraphics.fill(x, y, x + 6, y + height, 0xFFCCCCCC);
        guiGraphics.fill(x, barY, x + 6, barY + barHeight, 0xFF888888);
    }

    private void renderFilterEntryStack(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }
        boolean renderedCustom = renderMarkerMaterial(guiGraphics, stack, x, y);
        if (!renderedCustom) {
            guiGraphics.renderItem(stack, x, y);
            guiGraphics.renderItemDecorations(this.font, stack, x, y);
        }
        if (isAmountMarkerStack(stack)) {
            renderAmountText(guiGraphics, x, y, getLogicalSlotAmount(stack));
        }
    }

    private boolean handleFilterClick(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton != 0 && mouseButton != 1) {
            return false;
        }

        List<FilterEntry> entries = getFilterEntries();
        int maxScroll = getMaxScroll(entries.size());
        if (this.listScroll > maxScroll) {
            this.listScroll = maxScroll;
        }

        int entryIndex = resolveEntryIndex(mouseX, mouseY, entries.size());
        if (entryIndex >= 0 && entryIndex < entries.size()) {
            FilterEntry entry = entries.get(entryIndex);
            boolean contains = this.selectedFilterEntries.contains(entry.id());
            if ((mouseButton == 0 && !contains) || (mouseButton == 1 && contains)) {
                if (!this.selectedFilterEntries.add(entry.id())) {
                    this.selectedFilterEntries.remove(entry.id());
                }
                sendMenuButton(PatternEditorMenu.encodeFilterEntryButtonId(entryIndex));
            }
            return true;
        }

        int scrollX = this.leftPos + OUTPUT_X + LIST_COLS * SLOT_SIZE + 2;
        int listTop = this.topPos + INPUT_Y;
        int listHeight = LIST_ROWS * SLOT_SIZE;
        if (maxScroll > 0 && mouseX >= scrollX && mouseX < scrollX + 6 && mouseY >= listTop && mouseY < listTop + listHeight) {
            if (maxScroll > 0) {
                float ratio = (float) ((mouseY - listTop) / listHeight);
                this.listScroll = Mth.clamp(Math.round(ratio * maxScroll), 0, maxScroll);
            }
            return true;
        }

        return false;
    }

    private void renderFilterTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<FilterEntry> entries = getFilterEntries();
        int entryIndex = resolveEntryIndex(mouseX, mouseY, entries.size());
        if (entryIndex < 0 || entryIndex >= entries.size()) {
            return;
        }

        List<Component> lines = buildDetailLines(entries.get(entryIndex));
        if (!lines.isEmpty()) {
            guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderButtonTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.switchButton != null && this.switchButton.visible && this.switchButton.isMouseOver(mouseX, mouseY)) {
            Component tip = this.switchButton.getHint();
            if (tip != null && tip != CommonComponents.EMPTY) {
                guiGraphics.renderTooltip(this.font, tip, mouseX, mouseY);
            }
            return;
        }
        if (this.modeButton != null && this.modeButton.visible && this.modeButton.isMouseOver(mouseX, mouseY)) {
            Component tip = this.modeButton.getHint();
            if (tip != null && tip != CommonComponents.EMPTY) {
                guiGraphics.renderTooltip(this.font, tip, mouseX, mouseY);
            }
        }
    }

    private List<FilterEntry> getFilterEntries() {
        ItemStack patternStack = this.menu.getPatternStackSnapshot();
        if (!patternStack.isEmpty() && this.minecraft != null && this.minecraft.level != null) {
            List<FilterEntry> expanded = new ArrayList<>();
            try {
                for (IPatternDetails detail : TechStartPatternExpansion.expandFilterCandidates(patternStack, this.minecraft.level)) {
                    ItemStack variantStack = detail.getDefinition().toStack();
                    if (variantStack.isEmpty()) {
                        continue;
                    }
                    ItemStack input = getPrimaryPatternStack(variantStack, true);
                    ItemStack output = getPrimaryPatternStack(variantStack, false);
                    if (input.isEmpty() || output.isEmpty()) {
                        continue;
                    }
                    String id = getFilterEntryId(variantStack, input, output);
                    String legacyId = getLegacyPairId(id, input, output);
                    expanded.add(new FilterEntry(id, legacyId, input, output, variantStack));
                }
            } catch (RuntimeException ignored) {
                expanded.clear();
            }
            if (!expanded.isEmpty()) {
                return expanded;
            }
        }

        List<ItemStack> inputs = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack input = this.menu.slots.get(i).getItem();
            if (!input.isEmpty()) {
                inputs.add(input.copy());
            }
        }
        for (int i = 9; i < 18; i++) {
            ItemStack output = this.menu.slots.get(i).getItem();
            if (!output.isEmpty()) {
                outputs.add(output.copy());
            }
        }

        List<FilterEntry> entries = new ArrayList<>();
        if (inputs.isEmpty() || outputs.isEmpty()) {
            return entries;
        }

        for (ItemStack input : inputs) {
            for (ItemStack output : outputs) {
                String id = PatternEditorMenu.buildFilterEntryId(input, output);
                entries.add(new FilterEntry(id, id, input, output, ItemStack.EMPTY));
            }
        }
        return entries;
    }

    private List<Component> buildDetailLines(FilterEntry hoveredEntry) {
        List<Component> lines = new ArrayList<>();
        ItemStack detailPattern = hoveredEntry.pattern().isEmpty() ? this.menu.getPatternStackSnapshot() : hoveredEntry.pattern();
        appendStoredItemLines(lines, detailPattern, true);
        int inputSize = lines.size();
        appendLegacyAmountLines(lines, detailPattern, true, "InputFluids", "InputFluidAmounts", "tooltip.techstart.input_fluid");
        if (lines.size() == inputSize) {
            appendLegacyAmountLines(lines, detailPattern, true, "InputGases", "InputGasAmounts", "tooltip.techstart.input_gas");
        } else {
            appendLegacyAmountLines(lines, detailPattern, true, "InputGases", "InputGasAmounts", "tooltip.techstart.input_gas");
        }
        appendStoredItemLines(lines, detailPattern, false);
        int outputSize = lines.size();
        appendLegacyAmountLines(lines, detailPattern, false, "OutputFluids", "OutputFluidAmounts", "tooltip.techstart.output_fluid");
        if (lines.size() == outputSize) {
            appendLegacyAmountLines(lines, detailPattern, false, "OutputGases", "OutputGasAmounts", "tooltip.techstart.output_gas");
        } else {
            appendLegacyAmountLines(lines, detailPattern, false, "OutputGases", "OutputGasAmounts", "tooltip.techstart.output_gas");
        }

        if (lines.isEmpty()) {
            lines.add(Component.translatable("tooltip.techstart.input_item", 1, hoveredEntry.input().getHoverName(), Math.max(1, hoveredEntry.input().getCount()))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.techstart.output_item", 1, hoveredEntry.output().getHoverName(), Math.max(1, hoveredEntry.output().getCount()))
                    .withStyle(ChatFormatting.GRAY));
        }

        return lines;
    }

    private void appendStoredItemLines(List<Component> lines, ItemStack patternStack, boolean input) {
        String key = input ? "tooltip.techstart.input_item" : "tooltip.techstart.output_item";
        int index = 1;
        for (ItemStack stack : readStoredStacks(patternStack, input)) {
            if (stack.isEmpty() || isFluidMarkerStack(stack) || isGasMarkerStack(stack)) {
                continue;
            }
            lines.add(Component.translatable(key, index, stack.getHoverName(), getLogicalSlotAmount(stack))
                    .withStyle(ChatFormatting.GRAY));
            index++;
        }
    }

    private List<ItemStack> readStoredStacks(ItemStack patternStack, boolean input) {
        List<ItemStack> stacks = new ArrayList<>();
        if (patternStack.isEmpty() || !patternStack.hasTag()) {
            return stacks;
        }
        CompoundTag tag = patternStack.getTag();
        if (tag == null) {
            return stacks;
        }
        String virtualKey = input ? TAG_VIRTUAL_INPUT_STACKS : TAG_VIRTUAL_OUTPUT_STACKS;
        if (tag.contains(virtualKey, Tag.TAG_LIST)) {
            ListTag list = tag.getList(virtualKey, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack stack = ItemStack.of(list.getCompound(i));
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
            return stacks;
        }
        String modernKey = input ? TAG_INPUTS : TAG_OUTPUTS;
        if (tag.contains(modernKey, Tag.TAG_LIST)) {
            ListTag list = tag.getList(modernKey, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (!entry.contains(TAG_STACK, Tag.TAG_COMPOUND)) {
                    continue;
                }
                ItemStack stack = ItemStack.of(entry.getCompound(TAG_STACK));
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
        }
        return stacks;
    }

    private void appendLegacyAmountLines(List<Component> lines, ItemStack pattern, boolean input, String listKey, String amountKey, String lineKey) {
        if (pattern.isEmpty() || !pattern.hasTag()) {
            return;
        }
        CompoundTag tag = pattern.getTag();
        if (tag == null || !tag.contains(listKey, Tag.TAG_LIST)) {
            return;
        }

        ListTag names = tag.getList(listKey, Tag.TAG_STRING);
        ListTag amounts = tag.contains(amountKey, Tag.TAG_LIST) ? tag.getList(amountKey, Tag.TAG_INT) : new ListTag();
        int index = 1;
        for (int i = 0; i < names.size(); i++) {
            String name = names.getString(i);
            if (name == null || name.isBlank()) {
                continue;
            }
            int amount = i < amounts.size() ? amounts.getInt(i) : 1;
            Object displayName = lineKey.contains("fluid")
                    ? getFluidDisplayName(name, Math.max(1, amount))
                    : lineKey.contains("gas")
                    ? getGasDisplayName(name)
                    : name;
            lines.add(Component.translatable(lineKey, index, displayName, Math.max(1, amount))
                    .withStyle(input ? ChatFormatting.DARK_AQUA : ChatFormatting.BLUE));
            index++;
        }
    }

    private ItemStack getPrimaryPatternStack(ItemStack patternStack, boolean input) {
        List<ItemStack> stacks = readStoredStacks(patternStack, input);
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(0).copy();
    }

    private String getFilterEntryId(ItemStack patternStack, ItemStack input, ItemStack output) {
        if (!patternStack.isEmpty() && patternStack.hasTag()) {
            CompoundTag tag = patternStack.getTag();
            if (tag != null) {
                String id = tag.getString(TAG_VIRTUAL_FILTER_ENTRY_ID);
                if (id != null && !id.isBlank()) {
                    return id;
                }
            }
        }
        return PatternEditorMenu.buildFilterEntryId(input, output);
    }

    private String getLegacyPairId(String id, ItemStack input, ItemStack output) {
        if (id != null && id.contains("|")) {
            return id.substring(0, id.indexOf('|'));
        }
        return PatternEditorMenu.buildFilterEntryId(input, output);
    }

    private boolean isFilterEntrySelected(FilterEntry entry) {
        if (entry == null || this.selectedFilterEntries.isEmpty()) {
            return false;
        }
        if (entry.id() != null && this.selectedFilterEntries.contains(entry.id())) {
            return true;
        }
        String legacyId = entry.legacyId();
        if (legacyId == null || legacyId.isBlank()) {
            return false;
        }
        String prefix = legacyId + "|";
        for (String selected : this.selectedFilterEntries) {
            if (selected != null && selected.startsWith(prefix)) {
                return false;
            }
        }
        return this.selectedFilterEntries.contains(legacyId);
    }

    private Component getFluidDisplayName(String fluidId, int amount) {
        if (fluidId == null || fluidId.isBlank()) {
            return Component.literal("unknown");
        }
        ResourceLocation key = ResourceLocation.tryParse(fluidId.trim());
        if (key == null) {
            return Component.literal(fluidId);
        }
        return BuiltInRegistries.FLUID.getOptional(key)
                .filter(fluid -> fluid != Fluids.EMPTY)
                .map(fluid -> new FluidStack(fluid, Math.max(1, amount)).getDisplayName())
                .orElse(Component.literal(fluidId));
    }

    private Component getGasDisplayName(String gasId) {
        return MekanismGasHelper.getDisplayName(gasId);
    }

    private int getMaxScroll(int entryCount) {
        return Math.max(0, (int) Math.ceil(entryCount / 9.0D) - 1);
    }

    private int resolveEntryIndex(double mouseX, double mouseY, int totalEntries) {
        int localX = Mth.floor(mouseX) - this.leftPos;
        int localY = Mth.floor(mouseY) - this.topPos;
        int listHeight = LIST_ROWS * SLOT_SIZE;
        int startIndex = this.listScroll * 9;

        if (localX >= INPUT_X && localX < INPUT_X + LIST_COLS * SLOT_SIZE && localY >= INPUT_Y && localY < INPUT_Y + listHeight) {
            int col = (localX - INPUT_X) / SLOT_SIZE;
            int row = (localY - INPUT_Y) / SLOT_SIZE;
            int idx = startIndex + row * LIST_COLS + col;
            return idx < totalEntries ? idx : -1;
        }

        if (localX >= OUTPUT_X && localX < OUTPUT_X + LIST_COLS * SLOT_SIZE && localY >= OUTPUT_Y && localY < OUTPUT_Y + listHeight) {
            int col = (localX - OUTPUT_X) / SLOT_SIZE;
            int row = (localY - OUTPUT_Y) / SLOT_SIZE;
            int idx = startIndex + row * LIST_COLS + col;
            return idx < totalEntries ? idx : -1;
        }

        return -1;
    }

    private boolean isWithinPatternArea(double mouseX, double mouseY) {
        int localX = Mth.floor(mouseX) - this.leftPos;
        int localY = Mth.floor(mouseY) - this.topPos;
        int listHeight = LIST_ROWS * SLOT_SIZE;
        boolean inputArea = localX >= INPUT_X && localX < INPUT_X + LIST_COLS * SLOT_SIZE
                && localY >= INPUT_Y && localY < INPUT_Y + listHeight;
        boolean outputArea = localX >= OUTPUT_X && localX < OUTPUT_X + LIST_COLS * SLOT_SIZE
                && localY >= OUTPUT_Y && localY < OUTPUT_Y + listHeight;
        return inputArea || outputArea;
    }

    private List<Component> buildPatternSlotTooltip(Slot slot) {
        if (slot == null || resolvePatternSlotId(slot) < 0 || !slot.hasItem()) {
            return List.of();
        }
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return List.of();
        }

        if (isFluidMarkerStack(stack)) {
            CompoundTag tag = stack.getTag();
            if (tag == null) {
                return List.of();
            }
            int amount = Math.max(1, tag.getInt(TAG_FLUID_AMOUNT));
            return List.of(
                    getFluidDisplayName(tag.getString(TAG_FLUID_NAME), amount),
                    Component.literal(amount + " mB").withStyle(ChatFormatting.GRAY)
            );
        }

        if (isGasMarkerStack(stack)) {
            CompoundTag tag = stack.getTag();
            if (tag == null) {
                return List.of();
            }
            int amount = Math.max(1, tag.getInt(TAG_GAS_AMOUNT));
            return List.of(
                    getGasDisplayName(tag.getString(TAG_GAS_NAME)),
                    Component.literal(amount + " mB").withStyle(ChatFormatting.GRAY)
            );
        }

        MekanismGasHelper.GasStackView gas = MekanismGasHelper.extractGas(stack);
        if (gas != null && gas.gasId() != null && !gas.gasId().isBlank()) {
            int amount = Math.max(1, gas.amount());
            return List.of(
                    gas.displayName(),
                    Component.literal(amount + " mB").withStyle(ChatFormatting.GRAY)
            );
        }

        FluidStack contained = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        if (!contained.isEmpty()) {
            int amount = Math.max(1, contained.getAmount());
            String fluidId = BuiltInRegistries.FLUID.getKey(contained.getFluid()).toString();
            return List.of(
                    getFluidDisplayName(fluidId, amount),
                    Component.literal(amount + " mB").withStyle(ChatFormatting.GRAY)
            );
        }

        if (stack.getItem() instanceof BucketItem bucketItem && bucketItem.getFluid() != Fluids.EMPTY) {
            return List.of(
                    getFluidDisplayName(BuiltInRegistries.FLUID.getKey(bucketItem.getFluid()).toString(), 1000),
                    Component.literal("1000 mB").withStyle(ChatFormatting.GRAY)
            );
        }

        return List.of();
    }

    private void renderPatternMarkerMaterials(GuiGraphics guiGraphics) {
        for (int slotId = 0; slotId < 18 && slotId < this.menu.slots.size(); slotId++) {
            Slot slot = this.menu.slots.get(slotId);
            if (!slot.hasItem()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            renderMarkerMaterial(guiGraphics, stack, x, y);
        }
    }

    private boolean renderMarkerMaterial(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return false;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0D, 0.0D, 250.0D);
        RenderSystem.disableDepthTest();
        boolean rendered = false;
        if (isFluidMarkerStack(stack)) {
            rendered = renderFluidMarkerMaterial(guiGraphics, stack, x, y);
        } else if (isGasMarkerStack(stack)) {
            rendered = renderGasMarkerMaterial(guiGraphics, stack, x, y);
        } else {
            if (renderGasFromContainerFallback(guiGraphics, stack, x, y)) {
                rendered = true;
            } else if (renderFluidFromContainerFallback(guiGraphics, stack, x, y)) {
                rendered = true;
            }
        }
        RenderSystem.enableDepthTest();
        guiGraphics.pose().popPose();
        return rendered;
    }

    private void redrawMarkerSlotBackground(GuiGraphics guiGraphics, int x, int y) {
        int u = x - this.leftPos;
        int v = y - this.topPos;
        guiGraphics.blit(TEXTURE, x, y, u, v, 16, 16);
    }

    private boolean renderFluidMarkerMaterial(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        CompoundTag tag = stack.getTag();
        if (tag == null || this.minecraft == null) {
            return false;
        }
        String fluidId = tag.getString(TAG_FLUID_NAME);
        if (fluidId == null || fluidId.isBlank()) {
            return false;
        }
        ResourceLocation key = ResourceLocation.tryParse(fluidId.trim());
        if (key == null) {
            return false;
        }
        var fluid = BuiltInRegistries.FLUID.getOptional(key).orElse(Fluids.EMPTY);
        if (fluid == Fluids.EMPTY) {
            return false;
        }
        FluidStack fluidStack = new FluidStack(fluid, Math.max(1, tag.getInt(TAG_FLUID_AMOUNT)));
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation texture = extensions.getStillTexture(fluidStack);
        int tint = extensions.getTintColor(fluidStack);
        TextureAtlasSprite sprite = texture == null ? null : this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        return renderFluidSprite(guiGraphics, x, y, sprite, tint);
    }

    private boolean renderGasMarkerMaterial(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return false;
        }
        String gasId = tag.getString(TAG_GAS_NAME);
        if (gasId == null || gasId.isBlank()) {
            return false;
        }
        MekanismGasHelper.GasRenderData renderData = MekanismGasHelper.getRenderData(gasId);
        if (renderData == null) {
            return renderGasPlaceholder(guiGraphics, x, y, 0xFF7FC8C8, getGasDisplayName(gasId).getString());
        }
        if (renderData.icon() == null) {
            return renderGasPlaceholder(guiGraphics, x, y, renderData.tint(), renderData.displayName().getString());
        }
        int tint = renderData.tint();
        float alpha = 1.0F;
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        redrawMarkerSlotBackground(guiGraphics, x, y);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(red, green, blue, alpha);
        ResourceLocation icon = renderData.icon();
        if (this.minecraft != null) {
            TextureAtlasSprite sprite = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(icon);
            TextureAtlasSprite missing = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(MissingTextureAtlasSprite.getLocation());
            if (sprite != null && sprite != missing) {
                RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
                guiGraphics.blit(x, y, 0, 16, 16, sprite);
            } else {
                RenderSystem.setShaderTexture(0, icon);
                guiGraphics.blit(icon, x, y, 0, 0, 16, 16, 16, 16);
            }
        } else {
            RenderSystem.setShaderTexture(0, icon);
            guiGraphics.blit(icon, x, y, 0, 0, 16, 16, 16, 16);
        }
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        return true;
    }

    private boolean renderFluidFromContainerFallback(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        FluidStack fluidStack = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        if (fluidStack.isEmpty() && stack.getItem() instanceof BucketItem bucketItem && bucketItem.getFluid() != Fluids.EMPTY) {
            fluidStack = new FluidStack(bucketItem.getFluid(), 1000);
        }
        if (fluidStack.isEmpty() || fluidStack.getFluid() == null || fluidStack.getFluid() == Fluids.EMPTY) {
            return false;
        }

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        ResourceLocation texture = extensions.getStillTexture(fluidStack);
        if (this.minecraft == null) {
            return false;
        }
        int tint = extensions.getTintColor(fluidStack);
        TextureAtlasSprite sprite = texture == null ? null : this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        return renderFluidSprite(guiGraphics, x, y, sprite, tint);
    }

    private boolean renderFluidSprite(GuiGraphics guiGraphics, int x, int y, TextureAtlasSprite sprite, int tint) {
        redrawMarkerSlotBackground(guiGraphics, x, y);

        if (this.minecraft == null) {
            return true;
        }

        TextureAtlasSprite missing = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(MissingTextureAtlasSprite.getLocation());
        if (sprite == null || sprite == missing) {
            return true;
        }

        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        guiGraphics.blit(x, y, 0, 16, 16, sprite);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        return true;
    }

    private boolean renderGasPlaceholder(GuiGraphics guiGraphics, int x, int y, int tint, String label) {
        int resolvedTint = tint == -1 ? 0xFF7FC8C8 : (0xFF000000 | (tint & 0x00FFFFFF));
        redrawMarkerSlotBackground(guiGraphics, x, y);
        guiGraphics.fill(x + 2, y + 2, x + 14, y + 14, resolvedTint);
        String markerText = (label == null || label.isBlank()) ? "G" : label.substring(0, 1).toUpperCase();
        guiGraphics.drawString(this.font, markerText, x + 5, y + 4, 0xFFFFFFFF, true);
        return true;
    }

    private boolean renderGasFromContainerFallback(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        MekanismGasHelper.GasStackView gas = MekanismGasHelper.extractGas(stack);
        if (gas == null || gas.gasId() == null || gas.gasId().isBlank()) {
            return false;
        }
        MekanismGasHelper.GasRenderData data = MekanismGasHelper.getRenderData(gas.gasId());
        if (data == null) {
            return renderGasPlaceholder(guiGraphics, x, y, gas.tint(), gas.displayName().getString());
        }
        if (data.icon() == null) {
            return renderGasPlaceholder(guiGraphics, x, y, data.tint(), data.displayName().getString());
        }
        int tint = data.tint();
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        redrawMarkerSlotBackground(guiGraphics, x, y);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ResourceLocation icon = data.icon();
        if (this.minecraft != null) {
            TextureAtlasSprite sprite = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(icon);
            TextureAtlasSprite missing = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(MissingTextureAtlasSprite.getLocation());
            if (sprite != null && sprite != missing) {
                RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
                guiGraphics.blit(x, y, 0, 16, 16, sprite);
            } else {
                RenderSystem.setShaderTexture(0, icon);
                guiGraphics.blit(icon, x, y, 0, 0, 16, 16, 16, 16);
            }
        } else {
            RenderSystem.setShaderTexture(0, icon);
            guiGraphics.blit(icon, x, y, 0, 0, 16, 16, 16, 16);
        }
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        return true;
    }

    public void applyPatternSlotAmount(int slotId, int amount) {
        if (!isPatternSlot(slotId)) {
            return;
        }
        Slot slot = this.menu.slots.get(slotId);
        if (slot.hasItem()) {
            ItemStack current = slot.getItem();
            ItemStack updated = current.copy();
            int markerLimit = getEditableAmountLimit(current);
            TechStartNetwork.CHANNEL.sendToServer(new SetPatternSlotAmountPacket(slotId, Mth.clamp(amount, 1, markerLimit)));
            if (isFluidMarkerStack(current)) {
                CompoundTag tag = updated.getOrCreateTag();
                tag.putBoolean(TAG_FLUID_MARKER, true);
                tag.putInt(TAG_FLUID_AMOUNT, Mth.clamp(amount, 1, markerLimit));
                updated.setCount(1);
            } else if (isGasMarkerStack(current)) {
                CompoundTag tag = updated.getOrCreateTag();
                tag.putBoolean(TAG_GAS_MARKER, true);
                tag.putInt(TAG_GAS_AMOUNT, Mth.clamp(amount, 1, markerLimit));
                updated.setCount(1);
            } else if (isItemMarkerStack(current)) {
                CompoundTag tag = updated.getOrCreateTag();
                tag.putBoolean(TAG_ITEM_MARKER, true);
                tag.putInt(TAG_ITEM_AMOUNT, Mth.clamp(amount, 1, markerLimit));
                updated.setCount(1);
            } else {
                int capped = Mth.clamp(amount, 1, Math.min(markerLimit, current.getMaxStackSize()));
                updated.setCount(capped);
            }
            slot.set(updated);
            this.menu.refreshPatternStackSnapshot();
        }
    }

    private int resolvePatternSlotId(Slot slot) {
        if (slot == null) {
            return -1;
        }
        int slotId = this.menu.slots.indexOf(slot);
        return isPatternSlot(slotId) ? slotId : -1;
    }

    private boolean isPatternSlot(int slotId) {
        return slotId >= 0 && slotId < 18;
    }

    private int getEditableAmountLimit(ItemStack stack) {
        if (stack.isEmpty()) {
            return 1;
        }
        if (isAmountMarkerStack(stack)) {
            return this.menu.getAmountLimitForStack(stack);
        }
        return Math.min(this.menu.getAmountLimitForStack(stack), stack.getMaxStackSize());
    }

    private void renderMarkerAmounts(GuiGraphics guiGraphics) {
        for (int slotId = 0; slotId < 18; slotId++) {
            Slot slot = this.menu.slots.get(slotId);
            ItemStack stack = slot.getItem();
            if (!isAmountMarkerStack(stack)) {
                continue;
            }
            renderAmountText(guiGraphics, this.leftPos + slot.x, this.topPos + slot.y, getLogicalSlotAmount(stack));
        }
    }

    private void renderAmountText(GuiGraphics guiGraphics, int slotX, int slotY, int amount) {
        if (amount <= 1) {
            return;
        }
        String text = formatAmountShort(amount);
        int x = slotX + 17 - this.font.width(text);
        int y = slotY + 9;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0D, 0.0D, 260.0D);
        guiGraphics.drawString(this.font, text, x, y, 0xFF00E5FF, true);
        guiGraphics.pose().popPose();
    }

    private String formatAmountShort(int amount) {
        if (amount < 1000) {
            return Integer.toString(amount);
        }
        if (amount < 1_000_000) {
            return (amount / 1_000) + "k";
        }
        if (amount < 1_000_000_000) {
            return (amount / 1_000_000) + "m";
        }
        return (amount / 1_000_000_000) + "b";
    }

    private int getLogicalSlotAmount(ItemStack stack) {
        if (stack.isEmpty()) {
            return 1;
        }
        if (isGasMarkerStack(stack)) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                int amount = tag.getInt(TAG_GAS_AMOUNT);
                if (amount > 0) {
                    return amount;
                }
            }
        }
        if (isFluidMarkerStack(stack)) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                int amount = tag.getInt(TAG_FLUID_AMOUNT);
                if (amount > 0) {
                    return amount;
                }
            }
        }
        if (isItemMarkerStack(stack)) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                return Math.max(1, tag.getInt(TAG_ITEM_AMOUNT));
            }
        }
        MekanismGasHelper.GasStackView gas = MekanismGasHelper.extractGas(stack);
        if (gas != null) {
            return Math.max(1, gas.amount());
        }
        FluidStack contained = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        if (!contained.isEmpty()) {
            return Math.max(1, contained.getAmount());
        }
        if (stack.getItem() instanceof BucketItem bucketItem && bucketItem.getFluid() != Fluids.EMPTY) {
            return 1000;
        }
        return Math.max(1, stack.getCount());
    }

    private boolean isFluidMarkerStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean(TAG_FLUID_MARKER)) {
            return false;
        }
        String fluidName = tag.getString(TAG_FLUID_NAME);
        return fluidName != null && !fluidName.isBlank();
    }

    private boolean isGasMarkerStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean(TAG_GAS_MARKER)) {
            return false;
        }
        String gasName = tag.getString(TAG_GAS_NAME);
        return gasName != null && !gasName.isBlank();
    }

    private boolean isItemMarkerStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_ITEM_MARKER);
    }

    private boolean isAmountMarkerStack(ItemStack stack) {
        if (isFluidMarkerStack(stack) || isGasMarkerStack(stack) || isItemMarkerStack(stack)) {
            return true;
        }
        if (MekanismGasHelper.extractGas(stack) != null) {
            return true;
        }
        FluidStack contained = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        if (!contained.isEmpty()) {
            return true;
        }
        return stack.getItem() instanceof BucketItem bucketItem && bucketItem.getFluid() != Fluids.EMPTY;
    }

    private record FilterEntry(String id, String legacyId, ItemStack input, ItemStack output, ItemStack pattern) {
    }
}

