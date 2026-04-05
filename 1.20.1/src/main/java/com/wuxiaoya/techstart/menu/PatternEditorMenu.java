package com.wuxiaoya.techstart.menu;

import com.wuxiaoya.techstart.config.TechStartConfig;
import com.wuxiaoya.techstart.integration.ae2.TechStartPatternExpansion;
import com.wuxiaoya.techstart.integration.mekanism.MekanismGasHelper;
import com.wuxiaoya.techstart.registry.TechStartItems;
import com.wuxiaoya.techstart.registry.TechStartMenus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternEditorMenu extends AbstractContainerMenu {
    private static final Pattern RESOURCE_LOCATION_PATTERN =
            Pattern.compile("([a-z0-9_.-]+:[a-z0-9_./-]+)");
    private static final int INPUT_SLOTS = 9;
    private static final int OUTPUT_SLOTS = 9;
    private static final int TOTAL_PATTERN_SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;
    private static final int PLAYER_INV_START = TOTAL_PATTERN_SLOTS;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private static final String TAG_ENCODED = "TechStartEncoded";
    private static final String TAG_INPUTS = "TechStartInputs";
    private static final String TAG_OUTPUTS = "TechStartOutputs";
    private static final String TAG_SLOT = "Slot";
    private static final String TAG_STACK = "Stack";
    private static final String TAG_ENCODED_ITEM = "EncodedItem";
    private static final String TAG_FILTER_MODE = "TechStartFilterMode";
    private static final String TAG_FILTER_MODE_LEGACY = "FilterMode";
    private static final String TAG_FILTER_ENTRIES = "FilterEntries";
    private static final String TAG_INPUT_FLUIDS = "InputFluids";
    private static final String TAG_INPUT_FLUID_AMOUNTS = "InputFluidAmounts";
    private static final String TAG_OUTPUT_FLUIDS = "OutputFluids";
    private static final String TAG_OUTPUT_FLUID_AMOUNTS = "OutputFluidAmounts";
    private static final String TAG_INPUT_GASES = "InputGases";
    private static final String TAG_INPUT_GAS_AMOUNTS = "InputGasAmounts";
    private static final String TAG_OUTPUT_GASES = "OutputGases";
    private static final String TAG_OUTPUT_GAS_AMOUNTS = "OutputGasAmounts";
    private static final String TAG_INPUT_ORES = "InputOreNames";
    private static final String TAG_OUTPUT_ORES = "OutputOreNames";
    private static final String TAG_INPUT_COUNTS = "InputCounts";
    private static final String TAG_OUTPUT_COUNTS = "OutputCounts";
    private static final String TAG_FLUID_MARKER = "TechStartFluidMarker";
    private static final String TAG_FLUID_NAME = "TechStartFluidName";
    private static final String TAG_FLUID_AMOUNT = "TechStartFluidAmount";
    private static final String TAG_GAS_MARKER = "TechStartGasMarker";
    private static final String TAG_GAS_NAME = "TechStartGasName";
    private static final String TAG_GAS_AMOUNT = "TechStartGasAmount";
    private static final String TAG_ITEM_MARKER = "TechStartItemMarker";
    private static final String TAG_ITEM_AMOUNT = "TechStartItemAmount";
    private static final String TAG_VIRTUAL_FILTER_ENTRY_ID = "VirtualFilterEntryId";
    private static final int BUTTON_FILTER_ENTRY_BASE = 1000;
    private static final int MAX_ENCODED_SLOT_COUNT = Integer.MAX_VALUE;

    public static final int FILTER_MODE_WHITELIST = 0;
    public static final int FILTER_MODE_BLACKLIST = 1;

    private final ItemStackHandler itemHandler;
    private final ContainerLevelAccess access;
    private final Player player;
    private final @Nullable InteractionHand boundPatternHand;
    private final ItemStack boundPatternStack;
    private final ContainerData data = new SimpleContainerData(1);
    private final LinkedHashSet<String> filterEntries = new LinkedHashSet<>();

    public static class PatternSlot extends SlotItemHandler {
        private boolean active = true;

        public PatternSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        @Override
        public boolean isActive() {
            return this.active;
        }
    }

    public static PatternEditorMenu createItemMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        return new PatternEditorMenu(containerId, playerInventory, data.readEnum(InteractionHand.class));
    }

    public PatternEditorMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(TechStartMenus.PATTERN_EDITOR_ITEM.get(), containerId);
        this.itemHandler = new ItemStackHandler(TOTAL_PATTERN_SLOTS);
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.NULL;
        this.boundPatternHand = hand;
        this.boundPatternStack = locateBoundPatternStack(hand);

        setFilterMode(FILTER_MODE_BLACKLIST);
        this.addDataSlots(this.data);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = row * 3 + col;
                this.addSlot(createPatternSlot(slot, 26 + col * 18, 35 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = INPUT_SLOTS + row * 3 + col;
                this.addSlot(createPatternSlot(slot, 98 + col * 18, 35 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = col + row * 9 + 9;
                this.addSlot(new Slot(playerInventory, slot, 8 + col * 18, 125 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 183));
        }

        loadFilterStateFromPatternItem();
        if (!this.player.level().isClientSide) {
            loadFromPatternItem();
        }
    }

    private Slot createPatternSlot(int slot, int x, int y) {
        return new PatternSlot(this.itemHandler, slot, x, y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return !stack.is(TechStartItems.PATTERN_INTEGRATIONS.get());
            }

            @Override
            public void set(@NotNull ItemStack stack) {
                super.set(normalizePatternSlotStack(stack));
            }

            @Override
            public void setByPlayer(@NotNull ItemStack stack) {
                super.setByPlayer(normalizePatternSlotStack(stack));
            }

            @Override
            public @NotNull ItemStack safeInsert(@NotNull ItemStack stack) {
                return super.safeInsert(normalizePatternSlotStack(stack));
            }
        };
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return !resolvePatternStack().isEmpty();
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == FILTER_MODE_WHITELIST || id == FILTER_MODE_BLACKLIST) {
            setFilterMode(id);
            this.filterEntries.clear();
            saveToPatternItem();
            return true;
        }
        if (id >= BUTTON_FILTER_ENTRY_BASE) {
            int entryIndex = id - BUTTON_FILTER_ENTRY_BASE;
            toggleFilterEntryByIndex(entryIndex);
            saveToPatternItem();
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    public int getFilterMode() {
        return normalizeFilterMode(this.data.get(0));
    }

    public static int encodeFilterEntryButtonId(int entryIndex) {
        return BUTTON_FILTER_ENTRY_BASE + Math.max(0, entryIndex);
    }

    public List<String> getFilterEntriesSnapshot() {
        return List.copyOf(this.filterEntries);
    }

    public ItemStack getPatternStackSnapshot() {
        return resolvePatternStack().copy();
    }

    public void refreshPatternStackSnapshot() {
        saveToPatternItem();
    }

    public int getAmountLimitForStack(ItemStack stack) {
        return extractFluidMarker(stack) != null || extractGasMarker(stack) != null
                ? TechStartConfig.getFluidGasMarkerMaxAmount()
                : TechStartConfig.getItemMarkerMaxAmount();
    }

    public void applyPatternSlotAmountFromClient(int slotId, int amount, Player player) {
        if (!isPatternSlot(slotId) || player.level().isClientSide) {
            return;
        }
        applyPatternSlotCount(slotId, amount);
        onPatternSlotsMutated(player);
    }

    public static String buildFilterEntryId(ItemStack input, ItemStack output) {
        return buildStackDescriptor(input) + "->" + buildStackDescriptor(output);
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player player) {
        if (isPatternSlot(slotId)) {
            if (clickType != ClickType.PICKUP) {
                return;
            }
            if (dragType != 0 && dragType != 1) {
                return;
            }

            Slot slot = this.slots.get(slotId);
            ItemStack current = slot.getItem();
            if (!current.isEmpty()) {
                slot.set(ItemStack.EMPTY);
                onPatternSlotsMutated(player);
                return;
            }

            ItemStack carried = this.getCarried();
            if (!carried.isEmpty() && !carried.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
                ItemStack marker = normalizePatternSlotStack(carried);
                if (!marker.isEmpty()) {
                    ItemStack placed = marker.copy();
                    placed.setCount(1);
                    slot.set(placed);
                    onPatternSlotsMutated(player);
                }
            }
            return;
        }
        super.clicked(slotId, dragType, clickType, player);
        if (isPatternSlot(slotId)) {
            onPatternSlotsMutated(player);
        }
    }

    @Override
    public boolean canDragTo(@NotNull Slot slot) {
        int slotId = this.slots.indexOf(slot);
        if (isPatternSlot(slotId)) {
            return false;
        }
        return super.canDragTo(slot);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            saveToPatternItem();
        }
    }

    private void setFilterMode(int mode) {
        this.data.set(0, normalizeFilterMode(mode));
    }

    private int normalizeFilterMode(int mode) {
        return mode == FILTER_MODE_WHITELIST ? FILTER_MODE_WHITELIST : FILTER_MODE_BLACKLIST;
    }

    private int readFilterMode(CompoundTag tag) {
        if (tag.contains(TAG_FILTER_MODE, Tag.TAG_INT)) {
            return normalizeFilterMode(tag.getInt(TAG_FILTER_MODE));
        }
        if (tag.contains(TAG_FILTER_MODE_LEGACY, Tag.TAG_INT)) {
            return normalizeFilterMode(tag.getInt(TAG_FILTER_MODE_LEGACY));
        }
        return FILTER_MODE_BLACKLIST;
    }

    private void loadFilterStateFromPatternItem() {
        this.filterEntries.clear();
        setFilterMode(FILTER_MODE_BLACKLIST);

        ItemStack patternStack = resolvePatternStack();
        if (patternStack.isEmpty()) {
            return;
        }
        CompoundTag tag = patternStack.getTag();
        if (tag == null) {
            return;
        }
        setFilterMode(readFilterMode(tag));
        readFilterEntries(tag);
    }

    private ItemStack locateBoundPatternStack() {
        return locateBoundPatternStack(this.boundPatternHand);
    }

    private ItemStack locateBoundPatternStack(@Nullable InteractionHand preferredHand) {
        if (preferredHand != null) {
            ItemStack preferred = player.getItemInHand(preferredHand);
            if (preferred.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
                return preferred;
            }
        }
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
            return mainHand;
        }
        ItemStack offHand = player.getOffhandItem();
        if (offHand.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
            return offHand;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack resolvePatternStack() {
        if (this.boundPatternHand != null) {
            ItemStack preferred = this.player.getItemInHand(this.boundPatternHand);
            if (preferred.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
                return preferred;
            }
        }
        if (!boundPatternStack.isEmpty() && boundPatternStack.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
            return boundPatternStack;
        }
        return locateBoundPatternStack();
    }

    private void clearPatternSlots() {
        ItemStackHandler handler = itemHandler;
        for (int i = 0; i < TOTAL_PATTERN_SLOTS; i++) {
            handler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    private void loadFromPatternItem() {
        clearPatternSlots();
        loadFilterStateFromPatternItem();

        ItemStack patternStack = resolvePatternStack();
        if (patternStack.isEmpty()) {
            return;
        }

        CompoundTag tag = patternStack.getTag();
        if (tag == null) {
            return;
        }

        readSlotList(tag.getList(TAG_INPUTS, Tag.TAG_COMPOUND), 0, INPUT_SLOTS);
        readSlotList(tag.getList(TAG_OUTPUTS, Tag.TAG_COMPOUND), INPUT_SLOTS, OUTPUT_SLOTS);
    }

    private void readSlotList(ListTag listTag, int baseSlot, int maxSlots) {
        ItemStackHandler handler = itemHandler;
        for (Tag rawTag : listTag) {
            if (!(rawTag instanceof CompoundTag entry)) {
                continue;
            }
            if (!entry.contains(TAG_SLOT, Tag.TAG_INT) || !entry.contains(TAG_STACK, Tag.TAG_COMPOUND)) {
                continue;
            }
            int relativeSlot = entry.getInt(TAG_SLOT);
            if (relativeSlot < 0 || relativeSlot >= maxSlots) {
                continue;
            }
            ItemStack stack = normalizeLoadedPatternSlotStack(ItemStack.of(entry.getCompound(TAG_STACK)));
            handler.setStackInSlot(baseSlot + relativeSlot, stack);
        }
    }

    private ListTag writeSlotList(int baseSlot, int maxSlots) {
        ListTag list = new ListTag();
        ItemStackHandler handler = itemHandler;
        for (int i = 0; i < maxSlots; i++) {
            ItemStack stack = handler.getStackInSlot(baseSlot + i);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt(TAG_SLOT, i);
            entry.put(TAG_STACK, stack.save(new CompoundTag()));
            list.add(entry);
        }
        return list;
    }

    private String buildEncodedDisplayName() {
        ItemStackHandler handler = itemHandler;
        String inputName = "";
        String outputName = "";
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && !isFluidMarkerStack(stack) && !isGasMarkerStack(stack)) {
                inputName = stack.getHoverName().getString();
                break;
            }
        }
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack stack = handler.getStackInSlot(INPUT_SLOTS + i);
            if (!stack.isEmpty() && !isFluidMarkerStack(stack) && !isGasMarkerStack(stack)) {
                outputName = stack.getHoverName().getString();
                break;
            }
        }
        if (inputName.isEmpty()) {
            FluidMarker marker = findFirstFluidMarker(0, INPUT_SLOTS);
            if (marker != null) {
                inputName = getFluidDisplayName(marker.fluidName, marker.amount);
            }
            if (inputName.isEmpty()) {
                GasMarker gasMarker = findFirstGasMarker(0, INPUT_SLOTS);
                if (gasMarker != null) {
                    inputName = getGasDisplayName(gasMarker.gasName);
                }
            }
        }
        if (outputName.isEmpty()) {
            FluidMarker marker = findFirstFluidMarker(INPUT_SLOTS, OUTPUT_SLOTS);
            if (marker != null) {
                outputName = getFluidDisplayName(marker.fluidName, marker.amount);
            }
            if (outputName.isEmpty()) {
                GasMarker gasMarker = findFirstGasMarker(INPUT_SLOTS, OUTPUT_SLOTS);
                if (gasMarker != null) {
                    outputName = getGasDisplayName(gasMarker.gasName);
                }
            }
        }
        if (inputName.isEmpty() && outputName.isEmpty()) {
            return "";
        }
        return inputName + " -> " + outputName;
    }

    private void saveToPatternItem() {
        ItemStack patternStack = resolvePatternStack();
        if (patternStack.isEmpty()) {
            return;
        }

        ListTag inputs = writeSlotList(0, INPUT_SLOTS);
        ListTag outputs = writeSlotList(INPUT_SLOTS, OUTPUT_SLOTS);
        boolean encoded = !inputs.isEmpty() || !outputs.isEmpty();
        boolean hasFilterableRecipe = !inputs.isEmpty() && !outputs.isEmpty();

        CompoundTag tag = patternStack.getOrCreateTag();
        tag.putInt(TAG_FILTER_MODE, getFilterMode());
        tag.putBoolean(TAG_ENCODED, encoded);

        if (encoded) {
            tag.put(TAG_INPUTS, inputs);
            tag.put(TAG_OUTPUTS, outputs);
            writeFluidLists(tag);
            writeGasLists(tag);
            writeLegacyCategoryLists(tag);
            if (hasFilterableRecipe) {
                writeFilterEntries(tag);
            } else {
                this.filterEntries.clear();
                tag.remove(TAG_FILTER_ENTRIES);
            }
            String encodedName = buildEncodedDisplayName();
            if (!encodedName.isEmpty()) {
                tag.putString(TAG_ENCODED_ITEM, encodedName);
            } else {
                tag.remove(TAG_ENCODED_ITEM);
            }
        } else {
            this.filterEntries.clear();
            tag.remove(TAG_INPUTS);
            tag.remove(TAG_OUTPUTS);
            tag.remove(TAG_FILTER_ENTRIES);
            removeFluidLists(tag);
            removeGasLists(tag);
            removeLegacyCategoryLists(tag);
            tag.remove(TAG_ENCODED_ITEM);
        }

        player.getInventory().setChanged();
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Keep main/off hand pattern stacks synced, especially when the pattern is in offhand.
            serverPlayer.inventoryMenu.broadcastChanges();
        }
    }

    private void readFilterEntries(CompoundTag tag) {
        if (!tag.contains(TAG_FILTER_ENTRIES, Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList(TAG_FILTER_ENTRIES, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String value = list.getString(i);
            if (value != null && !value.isBlank()) {
                this.filterEntries.add(value);
            }
        }
    }

    private void writeFilterEntries(CompoundTag tag) {
        if (this.filterEntries.isEmpty()) {
            tag.remove(TAG_FILTER_ENTRIES);
            return;
        }
        ListTag list = new ListTag();
        for (String entry : this.filterEntries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            list.add(net.minecraft.nbt.StringTag.valueOf(entry));
        }
        if (list.isEmpty()) {
            tag.remove(TAG_FILTER_ENTRIES);
        } else {
            tag.put(TAG_FILTER_ENTRIES, list);
        }
    }

    private void toggleFilterEntryByIndex(int entryIndex) {
        List<String> ids = buildFilterEntryIds();
        if (entryIndex < 0 || entryIndex >= ids.size()) {
            return;
        }
        String id = ids.get(entryIndex);
        if (!this.filterEntries.add(id)) {
            this.filterEntries.remove(id);
        }
    }

    private List<String> buildFilterEntryIds() {
        ItemStack patternStack = resolvePatternStack();
        if (!patternStack.isEmpty() && this.player.level() != null) {
            LinkedHashSet<String> expandedIds = new LinkedHashSet<>();
            try {
                for (var detail : TechStartPatternExpansion.expandFilterCandidates(patternStack, this.player.level())) {
                    ItemStack variantStack = detail.getDefinition().toStack();
                    if (variantStack.isEmpty() || !variantStack.hasTag()) {
                        continue;
                    }
                    CompoundTag tag = variantStack.getTag();
                    if (tag == null) {
                        continue;
                    }
                    String id = tag.getString(TAG_VIRTUAL_FILTER_ENTRY_ID);
                    if (id != null && !id.isBlank()) {
                        expandedIds.add(id);
                    }
                }
            } catch (RuntimeException ignored) {
                expandedIds.clear();
            }
            if (!expandedIds.isEmpty()) {
                return new ArrayList<>(expandedIds);
            }
        }

        List<ItemStack> inputs = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();
        ItemStackHandler handler = itemHandler;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                inputs.add(stack.copy());
            }
        }
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack stack = handler.getStackInSlot(INPUT_SLOTS + i);
            if (!stack.isEmpty()) {
                outputs.add(stack.copy());
            }
        }

        List<String> ids = new ArrayList<>();
        if (inputs.isEmpty() || outputs.isEmpty()) {
            return ids;
        }
        for (ItemStack input : inputs) {
            for (ItemStack output : outputs) {
                ids.add(buildFilterEntryId(input, output));
            }
        }
        return ids;
    }

    private static String buildStackDescriptor(ItemStack stack) {
        if (stack.isEmpty()) {
            return "empty";
        }
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.getBoolean(TAG_FLUID_MARKER) && tag.contains(TAG_FLUID_NAME, Tag.TAG_STRING)) {
                return "fluid|" + tag.getString(TAG_FLUID_NAME) + "|" + Math.max(1, tag.getInt(TAG_FLUID_AMOUNT));
            }
            if (tag.getBoolean(TAG_GAS_MARKER) && tag.contains(TAG_GAS_NAME, Tag.TAG_STRING)) {
                return "gas|" + tag.getString(TAG_GAS_NAME) + "|" + Math.max(1, tag.getInt(TAG_GAS_AMOUNT));
            }
            if (tag.getBoolean(TAG_ITEM_MARKER)) {
                CompoundTag cleaned = tag.copy();
                cleaned.remove(TAG_ITEM_MARKER);
                cleaned.remove(TAG_ITEM_AMOUNT);
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                String nbt = cleaned.isEmpty() ? "" : cleaned.toString();
                return itemId + "|item|" + Math.max(1, tag.getInt(TAG_ITEM_AMOUNT)) + "|" + nbt;
            }
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String nbt = stack.hasTag() ? stack.getTag().toString() : "";
        return itemId + "|" + stack.getCount() + "|" + nbt;
    }

    private void applyPatternSlotCount(int slotId, int count) {
        ItemStackHandler handler = this.itemHandler;
        ItemStack stack = handler.getStackInSlot(slotId);
        if (stack.isEmpty()) {
            return;
        }

        int amountLimit = Math.min(MAX_ENCODED_SLOT_COUNT, getAmountLimitForStack(stack));

        GasMarker gasMarker = extractGasMarker(stack);
        if (gasMarker != null) {
            int target = Mth.clamp(count, 1, amountLimit);
            if (gasMarker.amount == target) {
                return;
            }
            ItemStack updated = stack.copy();
            CompoundTag tag = updated.getOrCreateTag();
            tag.putBoolean(TAG_GAS_MARKER, true);
            tag.putString(TAG_GAS_NAME, gasMarker.gasName);
            tag.putInt(TAG_GAS_AMOUNT, target);
            tag.remove(TAG_FLUID_MARKER);
            tag.remove(TAG_FLUID_NAME);
            tag.remove(TAG_FLUID_AMOUNT);
            updated.setCount(1);
            handler.setStackInSlot(slotId, updated);
            return;
        }

        FluidMarker marker = extractFluidMarker(stack);
        if (marker != null) {
            int target = Mth.clamp(count, 1, amountLimit);
            if (marker.amount == target) {
                return;
            }
            ItemStack updated = stack.copy();
            CompoundTag tag = updated.getOrCreateTag();
            tag.putBoolean(TAG_FLUID_MARKER, true);
            tag.putString(TAG_FLUID_NAME, marker.fluidName);
            tag.putInt(TAG_FLUID_AMOUNT, target);
            tag.remove(TAG_GAS_MARKER);
            tag.remove(TAG_GAS_NAME);
            tag.remove(TAG_GAS_AMOUNT);
            updated.setCount(1);
            handler.setStackInSlot(slotId, updated);
            return;
        }

        if (isItemMarkerStack(stack)) {
            int target = Mth.clamp(count, 1, amountLimit);
            int current = getItemMarkerAmount(stack);
            if (current == target) {
                return;
            }
            ItemStack updated = stack.copy();
            CompoundTag tag = updated.getOrCreateTag();
            tag.putBoolean(TAG_ITEM_MARKER, true);
            tag.putInt(TAG_ITEM_AMOUNT, target);
            updated.setCount(1);
            handler.setStackInSlot(slotId, updated);
            return;
        }

        int target = Mth.clamp(count, 1, amountLimit);
        ItemStack updated = createItemMarkerStack(stack, target);
        if (!updated.isEmpty()) {
            handler.setStackInSlot(slotId, updated);
        }
    }

    private ItemStack normalizePatternSlotStack(ItemStack incoming) {
        if (incoming.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (isFluidMarkerStack(incoming)) {
            FluidMarker marker = extractFluidMarker(incoming);
            return marker == null ? ItemStack.EMPTY : createFluidMarkerStack(incoming, marker);
        }
        if (isGasMarkerStack(incoming)) {
            GasMarker marker = extractGasMarker(incoming);
            return marker == null ? ItemStack.EMPTY : createGasMarkerStack(incoming, marker);
        }
        if (isItemMarkerStack(incoming)) {
            ItemStack normalized = incoming.copy();
            CompoundTag tag = normalized.getOrCreateTag();
            tag.putBoolean(TAG_ITEM_MARKER, true);
            tag.putInt(TAG_ITEM_AMOUNT, getItemMarkerAmount(incoming));
            normalized.setCount(1);
            return normalized;
        }
        GasMarker gasMarker = extractGasFromContainer(incoming);
        if (gasMarker != null) {
            return createGasMarkerStack(incoming, gasMarker);
        }
        FluidMarker marker = extractFluidFromContainer(incoming);
        if (marker != null) {
            return createFluidMarkerStack(incoming, marker);
        }
        return createItemMarkerStack(incoming, 1);
    }

    private ItemStack normalizeLoadedPatternSlotStack(ItemStack loaded) {
        if (loaded.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (isFluidMarkerStack(loaded) || isGasMarkerStack(loaded) || isItemMarkerStack(loaded)) {
            return normalizePatternSlotStack(loaded);
        }
        GasMarker gasMarker = extractGasFromContainer(loaded);
        if (gasMarker != null) {
            return createGasMarkerStack(loaded, gasMarker);
        }
        FluidMarker marker = extractFluidFromContainer(loaded);
        if (marker != null) {
            return createFluidMarkerStack(loaded, marker);
        }
        return createItemMarkerStack(loaded, Math.max(1, loaded.getCount()));
    }

    private void writeFluidLists(CompoundTag tag) {
        ListTag inputFluidNames = new ListTag();
        ListTag inputFluidAmounts = new ListTag();
        collectFluidMarkers(0, INPUT_SLOTS, inputFluidNames, inputFluidAmounts);

        ListTag outputFluidNames = new ListTag();
        ListTag outputFluidAmounts = new ListTag();
        collectFluidMarkers(INPUT_SLOTS, OUTPUT_SLOTS, outputFluidNames, outputFluidAmounts);

        putOrRemoveFluidList(tag, TAG_INPUT_FLUIDS, inputFluidNames);
        putOrRemoveFluidList(tag, TAG_INPUT_FLUID_AMOUNTS, inputFluidAmounts);
        putOrRemoveFluidList(tag, TAG_OUTPUT_FLUIDS, outputFluidNames);
        putOrRemoveFluidList(tag, TAG_OUTPUT_FLUID_AMOUNTS, outputFluidAmounts);
    }

    private void writeGasLists(CompoundTag tag) {
        ListTag inputGasNames = new ListTag();
        ListTag inputGasAmounts = new ListTag();
        collectGasMarkers(0, INPUT_SLOTS, inputGasNames, inputGasAmounts);

        ListTag outputGasNames = new ListTag();
        ListTag outputGasAmounts = new ListTag();
        collectGasMarkers(INPUT_SLOTS, OUTPUT_SLOTS, outputGasNames, outputGasAmounts);

        putOrRemoveFluidList(tag, TAG_INPUT_GASES, inputGasNames);
        putOrRemoveFluidList(tag, TAG_INPUT_GAS_AMOUNTS, inputGasAmounts);
        putOrRemoveFluidList(tag, TAG_OUTPUT_GASES, outputGasNames);
        putOrRemoveFluidList(tag, TAG_OUTPUT_GAS_AMOUNTS, outputGasAmounts);
    }

    private void removeFluidLists(CompoundTag tag) {
        tag.remove(TAG_INPUT_FLUIDS);
        tag.remove(TAG_INPUT_FLUID_AMOUNTS);
        tag.remove(TAG_OUTPUT_FLUIDS);
        tag.remove(TAG_OUTPUT_FLUID_AMOUNTS);
    }

    private void removeGasLists(CompoundTag tag) {
        tag.remove(TAG_INPUT_GASES);
        tag.remove(TAG_INPUT_GAS_AMOUNTS);
        tag.remove(TAG_OUTPUT_GASES);
        tag.remove(TAG_OUTPUT_GAS_AMOUNTS);
    }

    private void writeLegacyCategoryLists(CompoundTag tag) {
        writeLegacyCategorySide(tag, true);
        writeLegacyCategorySide(tag, false);
    }

    private void writeLegacyCategorySide(CompoundTag tag, boolean input) {
        int baseSlot = input ? 0 : INPUT_SLOTS;
        int maxSlots = input ? INPUT_SLOTS : OUTPUT_SLOTS;
        String listKey = input ? TAG_INPUT_ORES : TAG_OUTPUT_ORES;
        String countsKey = input ? TAG_INPUT_COUNTS : TAG_OUTPUT_COUNTS;
        String singleKey = input ? "InputOreName" : "OutputOreName";
        String singleCountKey = input ? "InputCount" : "OutputCount";

        ListTag names = new ListTag();
        ListTag counts = new ListTag();
        ItemStackHandler handler = itemHandler;
        for (int i = 0; i < maxSlots; i++) {
            ItemStack stack = handler.getStackInSlot(baseSlot + i);
            if (stack.isEmpty() || isFluidMarkerStack(stack) || isGasMarkerStack(stack)) {
                continue;
            }
            String legacyKey = inferLegacyCategoryKey(stack);
            if (legacyKey == null || legacyKey.isBlank()) {
                continue;
            }
            names.add(StringTag.valueOf(legacyKey));
            counts.add(IntTag.valueOf(getItemMarkerAmount(stack)));
        }

        if (names.isEmpty()) {
            tag.remove(listKey);
            tag.remove(countsKey);
            tag.remove(singleKey);
            tag.remove(singleCountKey);
            return;
        }

        tag.put(listKey, names);
        tag.put(countsKey, counts);
        tag.putString(singleKey, names.getString(0));
        tag.putInt(singleCountKey, counts.getInt(0));
    }

    private void removeLegacyCategoryLists(CompoundTag tag) {
        tag.remove(TAG_INPUT_ORES);
        tag.remove(TAG_OUTPUT_ORES);
        tag.remove(TAG_INPUT_COUNTS);
        tag.remove(TAG_OUTPUT_COUNTS);
        tag.remove("InputOreName");
        tag.remove("OutputOreName");
        tag.remove("InputCount");
        tag.remove("OutputCount");
    }

    private String inferLegacyCategoryKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        for (var tagKey : stack.getTags().toList()) {
            ResourceLocation location = tagKey.location();
            if (!"forge".equals(location.getNamespace()) && !"c".equals(location.getNamespace())) {
                continue;
            }
            String[] parts = location.getPath().split("/");
            if (parts.length < 2) {
                continue;
            }
            String prefix = switch (parts[0]) {
                case "ingots" -> "ingot";
                case "plates" -> "plate";
                case "storage_blocks" -> "block";
                case "nuggets" -> "nugget";
                case "rods" -> "rod";
                case "gears" -> "gear";
                case "wires" -> "wire";
                case "dusts" -> "dust";
                case "ores" -> "ore";
                case "gems" -> "gem";
                default -> null;
            };
            if (prefix == null) {
                continue;
            }
            StringBuilder material = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (parts[i].isBlank()) {
                    continue;
                }
                if (!material.isEmpty()) {
                    material.append('_');
                }
                material.append(parts[i]);
            }
            if (material.isEmpty()) {
                continue;
            }
            return prefix + toUpperCamel(material.toString());
        }
        return null;
    }

    private String toUpperCamel(String raw) {
        StringBuilder builder = new StringBuilder();
        boolean upper = true;
        for (int i = 0; i < raw.length(); i++) {
            char current = raw.charAt(i);
            if (current == '_' || current == '-' || current == '/') {
                upper = true;
                continue;
            }
            builder.append(upper ? Character.toUpperCase(current) : current);
            upper = false;
        }
        return builder.toString();
    }

    private void putOrRemoveFluidList(CompoundTag tag, String key, ListTag list) {
        if (list.isEmpty()) {
            tag.remove(key);
        } else {
            tag.put(key, list);
        }
    }

    private void collectFluidMarkers(int baseSlot, int maxSlots, ListTag names, ListTag amounts) {
        ItemStackHandler handler = itemHandler;
        for (int i = 0; i < maxSlots; i++) {
            ItemStack stack = handler.getStackInSlot(baseSlot + i);
            FluidMarker marker = extractFluidMarker(stack);
            if (marker == null) {
                continue;
            }
            names.add(StringTag.valueOf(marker.fluidName));
            amounts.add(IntTag.valueOf(marker.amount));
        }
    }

    private void collectGasMarkers(int baseSlot, int maxSlots, ListTag names, ListTag amounts) {
        ItemStackHandler handler = itemHandler;
        for (int i = 0; i < maxSlots; i++) {
            ItemStack stack = handler.getStackInSlot(baseSlot + i);
            GasMarker marker = extractGasMarker(stack);
            if (marker == null) {
                continue;
            }
            names.add(StringTag.valueOf(marker.gasName));
            amounts.add(IntTag.valueOf(marker.amount));
        }
    }

    private boolean isFluidMarkerStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null
                && tag.getBoolean(TAG_FLUID_MARKER)
                && tag.contains(TAG_FLUID_NAME, Tag.TAG_STRING);
    }

    private boolean isGasMarkerStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null
                && tag.getBoolean(TAG_GAS_MARKER)
                && tag.contains(TAG_GAS_NAME, Tag.TAG_STRING);
    }

    private boolean isItemMarkerStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_ITEM_MARKER);
    }

    private int getItemMarkerAmount(ItemStack stack) {
        if (stack.isEmpty()) {
            return 1;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.getBoolean(TAG_ITEM_MARKER)) {
            return Math.max(1, tag.getInt(TAG_ITEM_AMOUNT));
        }
        return Math.max(1, stack.getCount());
    }

    private ItemStack createItemMarkerStack(ItemStack source, int amount) {
        if (source.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack marker = source.copy();
        marker.setCount(1);
        CompoundTag tag = marker.getOrCreateTag();
        tag.putBoolean(TAG_ITEM_MARKER, true);
        tag.putInt(TAG_ITEM_AMOUNT, Mth.clamp(amount, 1, TechStartConfig.getItemMarkerMaxAmount()));
        return marker;
    }

    private ItemStack createGasMarkerStack(ItemStack source, GasMarker marker) {
        if (source.isEmpty() || marker == null || marker.gasName == null || marker.gasName.isBlank()) {
            return ItemStack.EMPTY;
        }
        ItemStack markerStack = new ItemStack(Items.GLASS_BOTTLE);
        markerStack.setCount(1);
        CompoundTag tag = markerStack.getOrCreateTag();
        tag.putBoolean(TAG_GAS_MARKER, true);
        tag.putString(TAG_GAS_NAME, marker.gasName);
        tag.putInt(TAG_GAS_AMOUNT, sanitizeMarkerAmount(marker.amount));
        String displayName = getGasDisplayName(marker.gasName);
        if (displayName != null && !displayName.isBlank()) {
            markerStack.setHoverName(Component.literal(displayName));
        }
        return markerStack;
    }

    private ItemStack createFluidMarkerStack(ItemStack source, FluidMarker marker) {
        if (source.isEmpty() || marker == null || marker.fluidName == null || marker.fluidName.isBlank()) {
            return ItemStack.EMPTY;
        }
        ItemStack markerStack = createFluidDisplayStack(source, marker.fluidName);
        markerStack.setCount(1);
        CompoundTag tag = markerStack.getOrCreateTag();
        tag.putBoolean(TAG_FLUID_MARKER, true);
        tag.putString(TAG_FLUID_NAME, marker.fluidName);
        tag.putInt(TAG_FLUID_AMOUNT, sanitizeMarkerAmount(marker.amount));
        String displayName = getFluidDisplayName(marker.fluidName, marker.amount);
        if (displayName != null && !displayName.isBlank()) {
            markerStack.setHoverName(Component.literal(displayName));
        }
        return markerStack;
    }

    private ItemStack createFluidDisplayStack(ItemStack source, String fluidName) {
        ResourceLocation key = ResourceLocation.tryParse(fluidName == null ? "" : fluidName.trim());
        if (key != null) {
            ItemStack bucketStack = BuiltInRegistries.FLUID.getOptional(key)
                    .filter(fluid -> fluid != Fluids.EMPTY)
                    .map(fluid -> fluid.getBucket())
                    .filter(bucket -> bucket != Items.AIR)
                    .map(ItemStack::new)
                    .orElse(ItemStack.EMPTY);
            if (!bucketStack.isEmpty()) {
                return bucketStack;
            }
        }
        return new ItemStack(Items.BUCKET);
    }

    private @Nullable FluidMarker extractFluidMarker(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        CompoundTag rootTag = stack.getTag();
        if (rootTag != null && rootTag.getBoolean(TAG_GAS_MARKER) && rootTag.contains(TAG_GAS_NAME, Tag.TAG_STRING)) {
            return null;
        }
        if (isFluidMarkerStack(stack)) {
            CompoundTag tag = stack.getTag();
            if (tag == null) {
                return null;
            }
            String name = tag.getString(TAG_FLUID_NAME);
            if (name.isBlank()) {
                return null;
            }
            int amount = tag.contains(TAG_FLUID_AMOUNT, Tag.TAG_INT) ? tag.getInt(TAG_FLUID_AMOUNT) : 0;
            if (amount <= 0) {
                FluidMarker fromContainer = extractFluidFromContainer(stack);
                if (fromContainer != null && name.equals(fromContainer.fluidName)) {
                    amount = fromContainer.amount;
                } else {
                    amount = 1000;
                }
            }
            amount = sanitizeMarkerAmount(amount);
            return new FluidMarker(name, amount);
        }
        return extractFluidFromContainer(stack);
    }

    private @Nullable GasMarker extractGasMarker(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (isGasMarkerStack(stack)) {
            CompoundTag tag = stack.getTag();
            if (tag == null) {
                return null;
            }
            String name = tag.getString(TAG_GAS_NAME);
            if (name.isBlank()) {
                return null;
            }
            int amount = tag.contains(TAG_GAS_AMOUNT, Tag.TAG_INT) ? tag.getInt(TAG_GAS_AMOUNT) : 0;
            if (amount <= 0) {
                GasMarker fromContainer = extractGasFromContainer(stack);
                if (fromContainer != null && name.equals(fromContainer.gasName)) {
                    amount = fromContainer.amount;
                } else {
                    amount = 1;
                }
            }
            amount = sanitizeMarkerAmount(amount);
            return new GasMarker(name, amount);
        }
        return extractGasFromContainer(stack);
    }

    private @Nullable FluidMarker extractFluidFromContainer(ItemStack stack) {
        if (isGasMarkerStack(stack)) {
            return null;
        }
        FluidStack contained = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        if (!contained.isEmpty() && contained.getFluid() != null) {
            String key = BuiltInRegistries.FLUID.getKey(contained.getFluid()).toString();
            int amount = sanitizeMarkerAmount(contained.getAmount());
            return new FluidMarker(key, amount);
        }
        if (stack.getItem() instanceof BucketItem bucketItem && bucketItem.getFluid() != Fluids.EMPTY) {
            String key = BuiltInRegistries.FLUID.getKey(bucketItem.getFluid()).toString();
            return new FluidMarker(key, sanitizeMarkerAmount(1_000));
        }
        FluidMarker byHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .resolve()
                .map(this::extractFirstFluid)
                .orElse(null);
        if (byHandler != null) {
            return byHandler;
        }
        return extractFluidFromTag(stack);
    }

    private @Nullable FluidMarker extractFirstFluid(IFluidHandlerItem handler) {
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluidStack = handler.getFluidInTank(tank);
            if (fluidStack.isEmpty() || fluidStack.getFluid() == null) {
                continue;
            }
            String key = BuiltInRegistries.FLUID.getKey(fluidStack.getFluid()).toString();
            int amount = sanitizeMarkerAmount(fluidStack.getAmount());
            return new FluidMarker(key, amount);
        }
        return null;
    }

    private @Nullable GasMarker extractGasFromContainer(ItemStack stack) {
        MekanismGasHelper.GasStackView gas = MekanismGasHelper.extractGasOrTag(stack);
        if (gas == null || gas.gasId() == null || gas.gasId().isBlank()) {
            return null;
        }
        return new GasMarker(gas.gasId(), sanitizeMarkerAmount(gas.amount()));
    }

    private @Nullable FluidMarker extractFluidFromTag(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || (tag.getBoolean(TAG_GAS_MARKER) && tag.contains(TAG_GAS_NAME, Tag.TAG_STRING))) {
            return null;
        }
        Matcher matcher = RESOURCE_LOCATION_PATTERN.matcher(tag.toString().toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(candidate);
            if (id == null) {
                continue;
            }
            var fluid = BuiltInRegistries.FLUID.getOptional(id).orElse(Fluids.EMPTY);
            if (fluid == Fluids.EMPTY) {
                continue;
            }
            return new FluidMarker(id.toString(), sanitizeMarkerAmount(1_000));
        }
        return null;
    }

    private int sanitizeMarkerAmount(int amount) {
        return Mth.clamp(amount, 1, TechStartConfig.getFluidGasMarkerMaxAmount());
    }

    private @Nullable FluidMarker findFirstFluidMarker(int baseSlot, int maxSlots) {
        ItemStackHandler handler = this.itemHandler;
        for (int i = 0; i < maxSlots; i++) {
            FluidMarker marker = extractFluidMarker(handler.getStackInSlot(baseSlot + i));
            if (marker != null) {
                return marker;
            }
        }
        return null;
    }

    private @Nullable GasMarker findFirstGasMarker(int baseSlot, int maxSlots) {
        ItemStackHandler handler = this.itemHandler;
        for (int i = 0; i < maxSlots; i++) {
            GasMarker marker = extractGasMarker(handler.getStackInSlot(baseSlot + i));
            if (marker != null) {
                return marker;
            }
        }
        return null;
    }

    private String getFluidDisplayName(String fluidId, int amount) {
        if (fluidId == null || fluidId.isBlank()) {
            return "unknown";
        }
        ResourceLocation key = ResourceLocation.tryParse(fluidId.trim());
        if (key == null) {
            return fluidId;
        }
        return BuiltInRegistries.FLUID.getOptional(key)
                .filter(fluid -> fluid != Fluids.EMPTY)
                .map(fluid -> new FluidStack(fluid, Math.max(1, amount)).getDisplayName().getString())
                .orElse(fluidId);
    }

    private String getGasDisplayName(String gasId) {
        return MekanismGasHelper.getDisplayName(gasId).getString();
    }

    private record FluidMarker(String fluidName, int amount) {
    }

    private record GasMarker(String gasName, int amount) {
    }

    public boolean isPatternSlotId(int slotId) {
        return isPatternSlot(slotId);
    }

    public ItemStack createMarkerFromIngredient(Object ingredient) {
        if (ingredient instanceof ItemStack itemStack) {
            if (itemStack.isEmpty() || itemStack.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
                return ItemStack.EMPTY;
            }
            return normalizePatternSlotStack(itemStack);
        }
        if (ingredient instanceof FluidStack fluidStack && !fluidStack.isEmpty() && fluidStack.getFluid() != null) {
            ItemStack source = ItemStack.EMPTY;
            if (fluidStack.getFluid().getBucket() != Items.AIR) {
                source = new ItemStack(fluidStack.getFluid().getBucket());
            }
            if (source.isEmpty()) {
                source = new ItemStack(Items.BUCKET);
            }
            String fluidId = BuiltInRegistries.FLUID.getKey(fluidStack.getFluid()).toString();
            return createFluidMarkerStack(source, new FluidMarker(fluidId, Math.max(1, fluidStack.getAmount())));
        }
        MekanismGasHelper.GasStackView gasStack = MekanismGasHelper.extractGas(ingredient);
        if (gasStack != null && gasStack.gasId() != null && !gasStack.gasId().isBlank()) {
            ItemStack source = new ItemStack(Items.GLASS_BOTTLE);
            return createGasMarkerStack(source, new GasMarker(gasStack.gasId(), Math.max(1, gasStack.amount())));
        }
        return ItemStack.EMPTY;
    }

    public void applyMarkerStackFromClient(int slotId, ItemStack marker, Player actor) {
        if (!isPatternSlot(slotId) || marker.isEmpty() || marker.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
            return;
        }
        Slot slot = this.slots.get(slotId);
        ItemStack normalized = normalizePatternSlotStack(marker);
        if (normalized.isEmpty()) {
            return;
        }
        slot.set(normalized);
        onPatternSlotsMutated(actor);
    }

    private boolean isPatternSlot(int slotId) {
        return slotId >= 0 && slotId < TOTAL_PATTERN_SLOTS;
    }

    private void onPatternSlotsMutated(Player player) {
        saveToPatternItem();
        this.broadcastChanges();
    }
}


