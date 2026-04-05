package com.wuxiaoya.techstart.integration.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import com.wuxiaoya.techstart.core.PatternDefinitionBridge;
import com.wuxiaoya.techstart.core.model.EntryKind;
import com.wuxiaoya.techstart.core.model.FilterEntry;
import com.wuxiaoya.techstart.core.model.FilterMode;
import com.wuxiaoya.techstart.core.model.PatternDefinition;
import com.wuxiaoya.techstart.core.model.PatternEntry;
import com.wuxiaoya.techstart.core.model.WildcardRecipe;
import com.wuxiaoya.techstart.core.model.WildcardRuleConfig;
import com.wuxiaoya.techstart.core.service.WildcardRecipeFilter;
import com.wuxiaoya.techstart.core.service.WildcardRecipeResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class TechStartPatternExpansion {
    private static final String TAG_ENCODED = "TechStartEncoded";
    private static final String TAG_ENCODED_ITEM = "EncodedItem";
    private static final String TAG_INPUTS = "TechStartInputs";
    private static final String TAG_OUTPUTS = "TechStartOutputs";
    private static final String TAG_FILTER_MODE = "TechStartFilterMode";
    private static final String TAG_FILTER_ENTRIES = "FilterEntries";
    private static final String TAG_FILTER_MODE_LEGACY = "FilterMode";
    private static final String TAG_VIRTUAL_INPUT_STACKS = "VirtualInputStacks";
    private static final String TAG_VIRTUAL_OUTPUT_STACKS = "VirtualOutputStacks";
    private static final String TAG_VIRTUAL_DISPLAY_NAME = "VirtualDisplayName";
    private static final String TAG_VIRTUAL_FILTER_ENTRY_ID = "VirtualFilterEntryId";
    private static final String TAG_ITEM_MARKER = "TechStartItemMarker";
    private static final String TAG_ITEM_AMOUNT = "TechStartItemAmount";
    private static final WildcardRecipeResolver WILDCARD_RESOLVER = new WildcardRecipeResolver(WildcardRuleConfig.defaults());
    private static final Map<String, String> TAG_PREFIXES = Map.ofEntries(
            Map.entry("ingots", "ingot"),
            Map.entry("plates", "plate"),
            Map.entry("storage_blocks", "block"),
            Map.entry("nuggets", "nugget"),
            Map.entry("rods", "rod"),
            Map.entry("gears", "gear"),
            Map.entry("wires", "wire"),
            Map.entry("dusts", "dust"),
            Map.entry("ores", "ore"),
            Map.entry("gems", "gem")
    );

    private TechStartPatternExpansion() {
    }

    public static List<IPatternDetails> expand(ItemStack patternStack, Level level) {
        return expandInternal(patternStack, level, true);
    }

    public static List<IPatternDetails> expandFilterCandidates(ItemStack patternStack, Level level) {
        return expandInternal(patternStack, level, false);
    }

    private static List<IPatternDetails> expandInternal(ItemStack patternStack, Level level, boolean applyFilter) {
        if (patternStack.isEmpty() || level == null) {
            return List.of();
        }

        CompoundTag tag = patternStack.getTag();
        if (tag == null) {
            return List.of();
        }

        if (tag.contains(TAG_VIRTUAL_INPUT_STACKS, Tag.TAG_LIST) || tag.contains(TAG_VIRTUAL_OUTPUT_STACKS, Tag.TAG_LIST)) {
            AEItemKey key = AEItemKey.of(patternStack.copy());
            if (key == null) {
                return List.of();
            }
            return List.of(new TechStartPatternDetails(key, patternStack.copy()));
        }

        PatternDefinition definition = hasLegacyCategoryKeys(tag)
                ? PatternDefinitionBridge.readLegacy(tag)
                : PatternDefinitionBridge.read(tag);
        List<PatternEntry> itemInputs = definition.inputs().stream().filter(entry -> entry.kind() == EntryKind.ITEM).toList();
        List<PatternEntry> itemOutputs = definition.outputs().stream().filter(entry -> entry.kind() == EntryKind.ITEM).toList();
        if (itemInputs.isEmpty() || itemOutputs.isEmpty()) {
            return List.of();
        }

        String primaryInputKey = itemInputs.get(0).key();
        String primaryOutputKey = itemOutputs.get(0).key();
        if (!isLegacyCategoryKey(primaryInputKey) || !isLegacyCategoryKey(primaryOutputKey)) {
            return List.of();
        }

        TagIndex tagIndex = TagIndex.build();
        if (tagIndex.availableKeys().isEmpty()) {
            return List.of();
        }

        String sourceMaterial = resolveSourceMaterial(primaryInputKey, primaryOutputKey);
        List<WildcardRecipe> recipes = resolveRecipes(primaryInputKey, primaryOutputKey, tagIndex.availableKeys());
        if (recipes.isEmpty()) {
            return List.of();
        }
        if (applyFilter) {
            recipes = WildcardRecipeFilter.apply(definition.filterMode(), definition.filterEntries(), recipes);
            if (recipes.isEmpty()) {
                return List.of();
            }
        }

        List<IPatternDetails> expanded = new ArrayList<>();
        Set<String> seenVariants = new LinkedHashSet<>();
        for (WildcardRecipe recipe : recipes) {
            String material = resolveRecipeMaterial(recipe, sourceMaterial);
            if (material == null || material.isBlank()) {
                continue;
            }

            List<ItemStack> inputCandidates = tagIndex.resolve(recipe.inputKey());
            List<ItemStack> outputCandidates = tagIndex.resolve(recipe.outputKey());
            if (inputCandidates.isEmpty() || outputCandidates.isEmpty()) {
                continue;
            }

            for (ItemStack inputCandidate : inputCandidates) {
                for (ItemStack outputCandidate : orderCandidatesByAnchor(inputCandidate, outputCandidates)) {
                    String variantId = buildVariantFilterEntryId(recipe.inputKey(), recipe.outputKey(), inputCandidate, outputCandidate);
                    if (!seenVariants.add(variantId)) {
                        continue;
                    }
                    if (applyFilter && !isAllowedByFilter(tag, recipe.inputKey(), recipe.outputKey(), variantId)) {
                        continue;
                    }

                    List<ItemStack> concreteInputs = buildConcreteStacks(itemInputs, material, sourceMaterial, tagIndex, inputCandidate);
                    List<ItemStack> concreteOutputs = buildConcreteStacks(itemOutputs, material, sourceMaterial, tagIndex, outputCandidate);
                    if (concreteInputs.isEmpty() || concreteOutputs.isEmpty()) {
                        continue;
                    }

                    String displayName = inputCandidate.getHoverName().getString() + " -> " + outputCandidate.getHoverName().getString();
                    ItemStack variantStack = createVirtualPatternStack(patternStack, concreteInputs, concreteOutputs, displayName, variantId);
                    AEItemKey key = AEItemKey.of(variantStack);
                    if (key != null) {
                        expanded.add(new TechStartPatternDetails(key, variantStack));
                    }
                }
            }
        }

        return expanded;
    }

    private static boolean hasLegacyCategoryKeys(CompoundTag tag) {
        return tag.contains("InputOreName", Tag.TAG_STRING)
                || tag.contains("OutputOreName", Tag.TAG_STRING)
                || tag.contains("InputOreNames", Tag.TAG_LIST)
                || tag.contains("OutputOreNames", Tag.TAG_LIST)
                || tag.contains("VirtualInputOreName", Tag.TAG_STRING)
                || tag.contains("VirtualOutputOreName", Tag.TAG_STRING)
                || tag.contains("VirtualInputOreNames", Tag.TAG_LIST)
                || tag.contains("VirtualOutputOreNames", Tag.TAG_LIST);
    }
    private static List<WildcardRecipe> resolveRecipes(String inputKey, String outputKey, Collection<String> availableKeys) {
        if (inputKey.endsWith("*") && outputKey.endsWith("*")) {
            return WILDCARD_RESOLVER.resolve(inputKey, outputKey, List.copyOf(availableKeys));
        }

        String inputPrefix = WILDCARD_RESOLVER.findPrefix(inputKey);
        String outputPrefix = WILDCARD_RESOLVER.findPrefix(outputKey);
        String inputMaterial = WILDCARD_RESOLVER.extractMaterial(inputKey);
        String outputMaterial = WILDCARD_RESOLVER.extractMaterial(outputKey);
        if (inputPrefix == null || outputPrefix == null || inputMaterial == null || outputMaterial == null) {
            return List.of();
        }
        if (!inputMaterial.equals(outputMaterial)) {
            return List.of();
        }
        return WILDCARD_RESOLVER.resolve(inputPrefix + "*", outputPrefix + "*", List.copyOf(availableKeys));
    }

    private static String resolveSourceMaterial(String inputKey, String outputKey) {
        String inputMaterial = WILDCARD_RESOLVER.extractMaterial(inputKey);
        if (inputMaterial != null) {
            return inputMaterial;
        }
        String outputMaterial = WILDCARD_RESOLVER.extractMaterial(outputKey);
        return outputMaterial != null ? outputMaterial : "";
    }

    private static String resolveRecipeMaterial(WildcardRecipe recipe, String fallbackMaterial) {
        String material = WILDCARD_RESOLVER.extractMaterial(recipe.inputKey());
        if (material == null || material.isBlank()) {
            material = WILDCARD_RESOLVER.extractMaterial(recipe.outputKey());
        }
        if (material == null || material.isBlank()) {
            material = fallbackMaterial;
        }
        return material == null ? "" : material;
    }

    private static List<ItemStack> buildConcreteStacks(
            List<PatternEntry> entries,
            String targetMaterial,
            String sourceMaterial,
            TagIndex tagIndex,
            ItemStack selectedPrimary) {
        List<ItemStack> result = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            PatternEntry entry = entries.get(i);
            String concreteKey = resolveConcreteKey(entry.key(), targetMaterial, sourceMaterial);
            List<ItemStack> candidates = tagIndex.resolve(concreteKey);
            if (candidates.isEmpty()) {
                return List.of();
            }

            ItemStack chosen = i == 0 && !selectedPrimary.isEmpty()
                    ? selectedPrimary.copy()
                    : chooseCompatibleFallback(candidates, selectedPrimary).copy();
            if (chosen.isEmpty()) {
                return List.of();
            }
            applyLogicalAmount(chosen, entry.amount());
            result.add(chosen);
        }
        return result;
    }

    private static String resolveConcreteKey(String key, String targetMaterial, String sourceMaterial) {
        if (key == null || key.isBlank()) {
            return "";
        }
        if (key.endsWith("*")) {
            return key.substring(0, key.length() - 1) + targetMaterial;
        }
        String prefix = WILDCARD_RESOLVER.findPrefix(key);
        String material = WILDCARD_RESOLVER.extractMaterial(key);
        if (prefix != null && material != null && !sourceMaterial.isBlank() && Objects.equals(material, sourceMaterial)) {
            return prefix + targetMaterial;
        }
        return key;
    }

    private static void applyLogicalAmount(ItemStack stack, long amount) {
        int safeAmount = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, amount));
        if (safeAmount <= stack.getMaxStackSize()) {
            stack.setCount(safeAmount);
            return;
        }
        stack.setCount(1);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(TAG_ITEM_MARKER, true);
        tag.putInt(TAG_ITEM_AMOUNT, safeAmount);
    }

    private static ItemStack createVirtualPatternStack(
            ItemStack original,
            List<ItemStack> concreteInputs,
            List<ItemStack> concreteOutputs,
            String displayName,
            String variantId) {
        ItemStack variant = original.copy();
        CompoundTag tag = variant.getOrCreateTag().copy();
        tag.putBoolean(TAG_ENCODED, true);
        tag.putString(TAG_ENCODED_ITEM, displayName == null ? "" : displayName);
        tag.putString(TAG_VIRTUAL_DISPLAY_NAME, displayName == null ? "" : displayName);
        tag.putString(TAG_VIRTUAL_FILTER_ENTRY_ID, variantId == null ? "" : variantId);
        tag.remove(TAG_INPUTS);
        tag.remove(TAG_OUTPUTS);
        tag.remove(TAG_FILTER_MODE);
        tag.remove(TAG_FILTER_MODE_LEGACY);
        tag.remove(TAG_FILTER_ENTRIES);
        tag.put(TAG_VIRTUAL_INPUT_STACKS, writeItemStackList(concreteInputs));
        tag.put(TAG_VIRTUAL_OUTPUT_STACKS, writeItemStackList(concreteOutputs));
        variant.setTag(tag);
        return variant;
    }

    private static ListTag writeItemStackList(List<ItemStack> stacks) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            list.add(stack.save(new CompoundTag()));
        }
        return list;
    }

    private static boolean isAllowedByFilter(CompoundTag tag, String inputKey, String outputKey, String variantId) {
        if (tag == null || !tag.contains(TAG_FILTER_ENTRIES, Tag.TAG_LIST)) {
            return true;
        }
        ListTag rawEntries = tag.getList(TAG_FILTER_ENTRIES, Tag.TAG_STRING);
        if (rawEntries.isEmpty()) {
            return true;
        }

        List<String> entries = new ArrayList<>(rawEntries.size());
        for (int i = 0; i < rawEntries.size(); i++) {
            String value = rawEntries.getString(i);
            if (value != null && !value.isBlank()) {
                entries.add(value);
            }
        }
        if (entries.isEmpty()) {
            return true;
        }

        int rawMode = tag.contains(TAG_FILTER_MODE, Tag.TAG_INT) ? tag.getInt(TAG_FILTER_MODE) : 1;
        FilterMode mode = rawMode == 0 ? FilterMode.WHITELIST : FilterMode.BLACKLIST;
        String pairId = inputKey + "->" + outputKey;
        boolean hasVariantEntriesForPair = entries.stream().anyMatch(entry -> entry.startsWith(pairId + "|"));

        boolean contains;
        if (variantId != null && !variantId.isBlank() && hasVariantEntriesForPair) {
            contains = entries.contains(variantId);
        } else {
            contains = entries.contains(pairId) || (variantId != null && !variantId.isBlank() && entries.contains(variantId));
        }
        return mode == FilterMode.BLACKLIST ? !contains : contains;
    }

    private static String buildVariantFilterEntryId(String inputKey, String outputKey, ItemStack inputItem, ItemStack outputItem) {
        return inputKey + "->" + outputKey + "|" + buildItemKey(inputItem) + "=>" + buildItemKey(outputItem);
    }

    private static String buildItemKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return (key == null ? "unknown:unknown" : key.toString()) + "@0";
    }

    private static List<ItemStack> orderCandidatesByAnchor(ItemStack anchorCandidate, List<ItemStack> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        String anchorNamespace = getRegistryNamespace(anchorCandidate);
        String anchorPath = getRegistryPath(anchorCandidate);
        List<ScoredOutputCandidate> scored = new ArrayList<>(candidates.size());
        int order = 0;
        for (ItemStack candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            scored.add(new ScoredOutputCandidate(candidate, scoreCandidateAgainstAnchor(anchorNamespace, anchorPath, candidate), order++));
        }
        scored.sort((left, right) -> {
            int scoreCompare = Integer.compare(right.score(), left.score());
            return scoreCompare != 0 ? scoreCompare : Integer.compare(left.order(), right.order());
        });

        List<ItemStack> ordered = new ArrayList<>(scored.size());
        Set<String> seenOutputs = new LinkedHashSet<>();
        for (ScoredOutputCandidate candidate : scored) {
            String outputId = buildItemKey(candidate.stack());
            if (seenOutputs.add(outputId)) {
                ordered.add(candidate.stack());
            }
        }
        return ordered;
    }

    private static int scoreCandidateAgainstAnchor(String anchorNamespace, String anchorPath, ItemStack candidate) {
        int score = commonPrefixLength(anchorPath, getRegistryPath(candidate));
        if (!anchorNamespace.isEmpty() && anchorNamespace.equals(getRegistryNamespace(candidate))) {
            score += 1000;
        }
        return score;
    }

    private static ItemStack chooseCompatibleFallback(List<ItemStack> candidates, ItemStack anchorCandidate) {
        List<ItemStack> ordered = orderCandidatesByAnchor(anchorCandidate, candidates);
        return ordered.isEmpty() ? ItemStack.EMPTY : ordered.get(0);
    }

    private static String getRegistryNamespace(ItemStack stack) {
        ResourceLocation key = stack == null || stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? "" : key.getNamespace();
    }

    private static String getRegistryPath(ItemStack stack) {
        ResourceLocation key = stack == null || stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? "" : key.getPath();
    }

    private static int commonPrefixLength(String left, String right) {
        if (left == null || right == null) {
            return 0;
        }
        int max = Math.min(left.length(), right.length());
        int i = 0;
        while (i < max && left.charAt(i) == right.charAt(i)) {
            i++;
        }
        return i;
    }

    private static boolean isLegacyCategoryKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        if (key.contains(":")) {
            return false;
        }
        if (key.endsWith("*")) {
            return WILDCARD_RESOLVER.findPrefix(key.substring(0, key.length() - 1)) != null;
        }
        return WILDCARD_RESOLVER.findPrefix(key) != null;
    }

    private static String legacyKeyFromTag(ResourceLocation location) {
        if (location == null) {
            return null;
        }
        String namespace = location.getNamespace();
        if (!"forge".equals(namespace) && !"c".equals(namespace)) {
            return null;
        }
        String[] parts = location.getPath().split("/");
        if (parts.length < 2) {
            return null;
        }
        String prefix = TAG_PREFIXES.get(parts[0]);
        if (prefix == null) {
            return null;
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
            return null;
        }
        return prefix + toUpperCamel(material.toString());
    }

    private static String toUpperCamel(String raw) {
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

    private record TagIndex(Map<String, List<ItemStack>> byLegacyKey) {
        private static TagIndex build() {
            Map<String, List<ItemStack>> map = new LinkedHashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = item.getDefaultInstance();
                if (stack.isEmpty()) {
                    continue;
                }
                List<TagKey<Item>> tags = stack.getTags().toList();
                for (TagKey<Item> tagKey : tags) {
                    String legacyKey = legacyKeyFromTag(tagKey.location());
                    if (legacyKey == null || legacyKey.isBlank()) {
                        continue;
                    }
                    map.computeIfAbsent(legacyKey, ignored -> new ArrayList<>()).add(stack.copy());
                }
            }
            return new TagIndex(map);
        }

        private Collection<String> availableKeys() {
            return byLegacyKey.keySet();
        }

        private List<ItemStack> resolve(String key) {
            if (key == null || key.isBlank()) {
                return List.of();
            }
            List<ItemStack> byTag = byLegacyKey.get(key);
            if (byTag != null && !byTag.isEmpty()) {
                return byTag;
            }
            ResourceLocation location = ResourceLocation.tryParse(key);
            if (location == null) {
                return List.of();
            }
            return BuiltInRegistries.ITEM.getOptional(location)
                    .map(item -> List.of(item.getDefaultInstance()))
                    .orElse(List.of());
        }
    }

    private record ScoredOutputCandidate(ItemStack stack, int score, int order) {
    }
}

