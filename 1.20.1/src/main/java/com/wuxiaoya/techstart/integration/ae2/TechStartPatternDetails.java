package com.wuxiaoya.techstart.integration.ae2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * Lightweight processing-pattern details decoded from TechStart encoded pattern items.
 */
public final class TechStartPatternDetails implements IPatternDetails {
    private static final String TAG_INPUTS = "TechStartInputs";
    private static final String TAG_OUTPUTS = "TechStartOutputs";
    private static final String TAG_INPUT_FLUIDS = "InputFluids";
    private static final String TAG_INPUT_FLUID_AMOUNTS = "InputFluidAmounts";
    private static final String TAG_OUTPUT_FLUIDS = "OutputFluids";
    private static final String TAG_OUTPUT_FLUID_AMOUNTS = "OutputFluidAmounts";
    private static final String TAG_INPUT_GASES = "InputGases";
    private static final String TAG_INPUT_GAS_AMOUNTS = "InputGasAmounts";
    private static final String TAG_OUTPUT_GASES = "OutputGases";
    private static final String TAG_OUTPUT_GAS_AMOUNTS = "OutputGasAmounts";
    private static final String TAG_FLUID_MARKER = "TechStartFluidMarker";
    private static final String TAG_FLUID_NAME = "TechStartFluidName";
    private static final String TAG_FLUID_AMOUNT = "TechStartFluidAmount";
    private static final String TAG_GAS_MARKER = "TechStartGasMarker";
    private static final String TAG_GAS_NAME = "TechStartGasName";
    private static final String TAG_GAS_AMOUNT = "TechStartGasAmount";
    private static final String TAG_ITEM_MARKER = "TechStartItemMarker";
    private static final String TAG_ITEM_AMOUNT = "TechStartItemAmount";
    private static final String TAG_SLOT = "Slot";
    private static final String TAG_STACK = "Stack";
    private static final String TAG_FILTER_MODE = "TechStartFilterMode";
    private static final String TAG_FILTER_MODE_LEGACY = "FilterMode";
    private static final String TAG_FILTER_ENTRIES = "FilterEntries";
    private static final String TAG_VIRTUAL_INPUT_STACKS = "VirtualInputStacks";
    private static final String TAG_VIRTUAL_OUTPUT_STACKS = "VirtualOutputStacks";
    private static final int FILTER_MODE_WHITELIST = 0;
    private static final int FILTER_MODE_BLACKLIST = 1;

    private final AEItemKey definition;
    private final IInput[] inputs;
    private final GenericStack[] outputs;

    public TechStartPatternDetails(AEItemKey definition, ItemStack encodedStack) {
        this.definition = definition;
        CompoundTag tag = encodedStack.getTag();
        if (tag == null) {
            throw new IllegalArgumentException("Encoded stack has no tag.");
        }

        List<SlotStack> inputEntries = sortBySlot(tag.getList(TAG_INPUTS, Tag.TAG_COMPOUND));
        if (inputEntries.isEmpty()) {
            inputEntries = readLegacyVirtualStacks(tag.getList(TAG_VIRTUAL_INPUT_STACKS, Tag.TAG_COMPOUND));
        }
        List<SlotStack> outputEntries = sortBySlot(tag.getList(TAG_OUTPUTS, Tag.TAG_COMPOUND));
        if (outputEntries.isEmpty()) {
            outputEntries = readLegacyVirtualStacks(tag.getList(TAG_VIRTUAL_OUTPUT_STACKS, Tag.TAG_COMPOUND));
        }
        FilterSelection filter = resolveFilterSelection(tag, inputEntries, outputEntries);

        this.inputs = readInputs(tag, inputEntries, filter);
        this.outputs = readOutputs(tag, outputEntries, filter);

        if (this.inputs.length == 0 || this.outputs.length == 0) {
            throw new IllegalArgumentException("Encoded stack has no valid inputs/outputs.");
        }
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public IInput[] getInputs() {
        return inputs;
    }

    @Override
    public GenericStack[] getOutputs() {
        return outputs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TechStartPatternDetails other)) {
            return false;
        }
        return definition.equals(other.definition);
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    private static IInput[] readInputs(CompoundTag tag, List<SlotStack> ordered, FilterSelection filter) {
        var fallbackFluidInputs = new ArrayList<GenericStack>();
        var fallbackGasInputs = new ArrayList<GenericStack>();
        var result = new ArrayList<IInput>(ordered.size());
        for (SlotStack entry : ordered) {
            if (filter.active() && !filter.allowedInputSlots().contains(entry.slot())) {
                continue;
            }
            ItemStack stack = entry.stack();
            GenericStack markerGas = decodeMarkerGas(stack);
            if (markerGas != null) {
                fallbackGasInputs.add(markerGas);
                continue;
            }
            GenericStack markerFluid = decodeMarkerFluid(stack);
            if (markerFluid != null) {
                fallbackFluidInputs.add(markerFluid);
                continue;
            }
            ItemStack decoded = stripItemMarkerTags(stack);
            GenericStack generic = GenericStack.fromItemStack(decoded);
            if (generic == null || generic.amount() <= 0) {
                continue;
            }
            long itemAmount = resolveItemAmount(stack, generic.amount());
            result.add(new SimpleInput(new GenericStack(generic.what(), 1), itemAmount));
        }

        List<GenericStack> fluidInputs = filter.active() ? fallbackFluidInputs : readFluidStacks(tag, true);
        if (fluidInputs.isEmpty()) {
            fluidInputs = fallbackFluidInputs;
        }
        for (GenericStack fluidInput : fluidInputs) {
            result.add(new SimpleInput(new GenericStack(fluidInput.what(), 1), fluidInput.amount()));
        }

        List<GenericStack> gasInputs = filter.active() ? fallbackGasInputs : readGasStacks(tag, true);
        if (gasInputs.isEmpty()) {
            gasInputs = fallbackGasInputs;
        }
        for (GenericStack gasInput : gasInputs) {
            result.add(new SimpleInput(new GenericStack(gasInput.what(), 1), gasInput.amount()));
        }

        return result.toArray(IInput[]::new);
    }

    private static GenericStack[] readOutputs(CompoundTag tag, List<SlotStack> ordered, FilterSelection filter) {
        var fallbackFluidOutputs = new ArrayList<GenericStack>();
        var fallbackGasOutputs = new ArrayList<GenericStack>();
        Map<AEKey, Long> condensed = new LinkedHashMap<>();
        for (SlotStack entry : ordered) {
            if (filter.active() && !filter.allowedOutputSlots().contains(entry.slot())) {
                continue;
            }
            ItemStack stack = entry.stack();
            GenericStack markerGas = decodeMarkerGas(stack);
            if (markerGas != null) {
                fallbackGasOutputs.add(markerGas);
                continue;
            }
            GenericStack markerFluid = decodeMarkerFluid(stack);
            if (markerFluid != null) {
                fallbackFluidOutputs.add(markerFluid);
                continue;
            }
            ItemStack decoded = stripItemMarkerTags(stack);
            GenericStack generic = GenericStack.fromItemStack(decoded);
            if (generic == null || generic.amount() <= 0) {
                continue;
            }
            long itemAmount = resolveItemAmount(stack, generic.amount());
            condensed.merge(generic.what(), itemAmount, Long::sum);
        }

        List<GenericStack> fluidOutputs = filter.active() ? fallbackFluidOutputs : readFluidStacks(tag, false);
        if (fluidOutputs.isEmpty()) {
            fluidOutputs = fallbackFluidOutputs;
        }
        for (GenericStack fluidOutput : fluidOutputs) {
            condensed.merge(fluidOutput.what(), fluidOutput.amount(), Long::sum);
        }

        List<GenericStack> gasOutputs = filter.active() ? fallbackGasOutputs : readGasStacks(tag, false);
        if (gasOutputs.isEmpty()) {
            gasOutputs = fallbackGasOutputs;
        }
        for (GenericStack gasOutput : gasOutputs) {
            condensed.merge(gasOutput.what(), gasOutput.amount(), Long::sum);
        }

        var outputs = new ArrayList<GenericStack>(condensed.size());
        for (var it : condensed.entrySet()) {
            outputs.add(new GenericStack(it.getKey(), it.getValue()));
        }
        return outputs.toArray(GenericStack[]::new);
    }

    private static List<GenericStack> readFluidStacks(CompoundTag tag, boolean input) {
        String namesKey = input ? TAG_INPUT_FLUIDS : TAG_OUTPUT_FLUIDS;
        String amountsKey = input ? TAG_INPUT_FLUID_AMOUNTS : TAG_OUTPUT_FLUID_AMOUNTS;
        if (!tag.contains(namesKey, Tag.TAG_LIST)) {
            return List.of();
        }

        ListTag names = tag.getList(namesKey, Tag.TAG_STRING);
        if (names.isEmpty()) {
            return List.of();
        }

        ListTag amounts = tag.contains(amountsKey, Tag.TAG_LIST)
                ? tag.getList(amountsKey, Tag.TAG_INT)
                : new ListTag();
        var result = new ArrayList<GenericStack>(names.size());
        for (int i = 0; i < names.size(); i++) {
            AEFluidKey fluidKey = parseFluidKey(names.getString(i));
            if (fluidKey == null) {
                continue;
            }
            long amount = i < amounts.size() ? amounts.getInt(i) : 1;
            result.add(new GenericStack(fluidKey, Math.max(1L, amount)));
        }
        return result;
    }

    private static @Nullable GenericStack decodeMarkerFluid(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean(TAG_FLUID_MARKER)) {
            return null;
        }
        AEFluidKey fluidKey = parseFluidKey(tag.getString(TAG_FLUID_NAME));
        if (fluidKey == null) {
            return null;
        }
        long amount = Math.max(1L, tag.getInt(TAG_FLUID_AMOUNT));
        return new GenericStack(fluidKey, amount);
    }

    private static @Nullable GenericStack decodeMarkerGas(ItemStack stack) {
        if (!isGasMarkerStack(stack)) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return null;
        }
        String gasName = tag.getString(TAG_GAS_NAME);
        long amount = Math.max(1L, tag.getInt(TAG_GAS_AMOUNT));
        AEKey gasKey = AppliedMekanisticsCompat.createGasKey(gasName, amount);
        return gasKey == null ? null : new GenericStack(gasKey, amount);
    }

    private static @Nullable AEFluidKey parseFluidKey(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        ResourceLocation fluidId = ResourceLocation.tryParse(id.trim());
        if (fluidId == null) {
            return null;
        }
        return BuiltInRegistries.FLUID.getOptional(fluidId)
                .filter(fluid -> fluid != Fluids.EMPTY)
                .map(AEFluidKey::of)
                .orElse(null);
    }

    private static List<GenericStack> readGasStacks(CompoundTag tag, boolean input) {
        String namesKey = input ? TAG_INPUT_GASES : TAG_OUTPUT_GASES;
        String amountsKey = input ? TAG_INPUT_GAS_AMOUNTS : TAG_OUTPUT_GAS_AMOUNTS;
        if (!tag.contains(namesKey, Tag.TAG_LIST)) {
            return List.of();
        }

        ListTag names = tag.getList(namesKey, Tag.TAG_STRING);
        if (names.isEmpty()) {
            return List.of();
        }

        ListTag amounts = tag.contains(amountsKey, Tag.TAG_LIST)
                ? tag.getList(amountsKey, Tag.TAG_INT)
                : new ListTag();
        var result = new ArrayList<GenericStack>(names.size());
        for (int i = 0; i < names.size(); i++) {
            String gasName = names.getString(i);
            long amount = i < amounts.size() ? amounts.getInt(i) : 1;
            AEKey gasKey = AppliedMekanisticsCompat.createGasKey(gasName, amount);
            if (gasKey != null) {
                result.add(new GenericStack(gasKey, Math.max(1L, amount)));
            }
        }
        return result;
    }

    private static long resolveItemAmount(ItemStack stack, long fallback) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return Math.max(1L, fallback);
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.getBoolean(TAG_ITEM_MARKER)) {
            return Math.max(1L, tag.getInt(TAG_ITEM_AMOUNT));
        }
        return Math.max(1L, fallback);
    }

    private static ItemStack stripItemMarkerTags(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return stack;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean(TAG_ITEM_MARKER)) {
            return stack;
        }

        ItemStack cleaned = stack.copy();
        CompoundTag cleanedTag = cleaned.getTag();
        if (cleanedTag != null) {
            cleanedTag.remove(TAG_ITEM_MARKER);
            cleanedTag.remove(TAG_ITEM_AMOUNT);
            if (cleanedTag.isEmpty()) {
                cleaned.setTag(null);
            }
        }
        return cleaned;
    }

    private static boolean isGasMarkerStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_GAS_MARKER) && tag.contains(TAG_GAS_NAME, Tag.TAG_STRING);
    }

    private static int readFilterMode(CompoundTag tag) {
        if (tag.contains(TAG_FILTER_MODE, Tag.TAG_INT)) {
            return tag.getInt(TAG_FILTER_MODE) == FILTER_MODE_WHITELIST ? FILTER_MODE_WHITELIST : FILTER_MODE_BLACKLIST;
        }
        if (tag.contains(TAG_FILTER_MODE_LEGACY, Tag.TAG_INT)) {
            return tag.getInt(TAG_FILTER_MODE_LEGACY) == FILTER_MODE_WHITELIST ? FILTER_MODE_WHITELIST : FILTER_MODE_BLACKLIST;
        }
        return FILTER_MODE_BLACKLIST;
    }

    private static FilterSelection resolveFilterSelection(CompoundTag tag, List<SlotStack> inputs, List<SlotStack> outputs) {
        if (!tag.contains(TAG_FILTER_ENTRIES, Tag.TAG_LIST)) {
            return FilterSelection.NONE;
        }

        ListTag rawEntries = tag.getList(TAG_FILTER_ENTRIES, Tag.TAG_STRING);
        if (rawEntries.isEmpty()) {
            return FilterSelection.NONE;
        }

        Set<String> selected = new HashSet<>(rawEntries.size());
        for (int i = 0; i < rawEntries.size(); i++) {
            String value = rawEntries.getString(i);
            if (value != null && !value.isBlank()) {
                selected.add(value);
            }
        }
        if (selected.isEmpty() || inputs.isEmpty() || outputs.isEmpty()) {
            return FilterSelection.NONE;
        }
        boolean hasDescriptorEntry = selected.stream().anyMatch(value -> value.contains("|"));
        if (!hasDescriptorEntry) {
            // Legacy filter ids used ore-name format; keep backward compatibility by not applying slot filtering.
            return FilterSelection.NONE;
        }

        int mode = readFilterMode(tag);
        Set<Integer> allowedInputs = new HashSet<>();
        Set<Integer> allowedOutputs = new HashSet<>();
        for (SlotStack in : inputs) {
            for (SlotStack out : outputs) {
                String id = in.descriptor() + "->" + out.descriptor();
                boolean contains = selected.contains(id);
                boolean allowed = mode == FILTER_MODE_BLACKLIST ? !contains : contains;
                if (allowed) {
                    allowedInputs.add(in.slot());
                    allowedOutputs.add(out.slot());
                }
            }
        }
        return new FilterSelection(true, allowedInputs, allowedOutputs);
    }

    private static List<SlotStack> readLegacyVirtualStacks(ListTag listTag) {
        var entries = new ArrayList<SlotStack>(listTag.size());
        int slot = 0;
        for (Tag raw : listTag) {
            if (!(raw instanceof CompoundTag entry)) {
                continue;
            }
            ItemStack stack = ItemStack.of(entry);
            if (stack.isEmpty()) {
                continue;
            }
            entries.add(new SlotStack(slot++, stack, buildStackDescriptor(stack)));
        }
        return entries;
    }

    private static List<SlotStack> sortBySlot(ListTag listTag) {
        var entries = new ArrayList<SlotStack>(listTag.size());
        for (Tag raw : listTag) {
            if (!(raw instanceof CompoundTag entry)) {
                continue;
            }
            if (!entry.contains(TAG_SLOT, Tag.TAG_INT) || !entry.contains(TAG_STACK, Tag.TAG_COMPOUND)) {
                continue;
            }
            ItemStack stack = ItemStack.of(entry.getCompound(TAG_STACK));
            if (stack.isEmpty()) {
                continue;
            }
            int slot = entry.getInt(TAG_SLOT);
            entries.add(new SlotStack(slot, stack, buildStackDescriptor(stack)));
        }
        entries.sort((a, b) -> Integer.compare(a.slot(), b.slot()));
        return entries;
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

    private record SlotStack(int slot, ItemStack stack, String descriptor) {
    }

    private record FilterSelection(boolean active, Set<Integer> allowedInputSlots, Set<Integer> allowedOutputSlots) {
        private static final FilterSelection NONE =
                new FilterSelection(false, Collections.emptySet(), Collections.emptySet());
    }

    private static final class SimpleInput implements IInput {
        private final GenericStack[] possible;
        private final long multiplier;

        private SimpleInput(GenericStack template, long multiplier) {
            this.possible = new GenericStack[] { Objects.requireNonNull(template) };
            this.multiplier = Math.max(1L, multiplier);
        }

        @Override
        public GenericStack[] getPossibleInputs() {
            return possible;
        }

        @Override
        public long getMultiplier() {
            return multiplier;
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            return input.matches(possible[0]);
        }

        @Override
        public @Nullable AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }
}

