package com.wuxiaoya.techstart.integration.ae2;

import org.jetbrains.annotations.Nullable;

import com.wuxiaoya.techstart.registry.TechStartItems;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.stacks.AEItemKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class TechStartPatternDecoder implements IPatternDetailsDecoder {
    static final String TAG_ENCODED = "TechStartEncoded";
    static final String TAG_ENCODED_ITEM = "EncodedItem";
    static final String TAG_INPUT_ORE = "InputOreName";
    static final String TAG_OUTPUT_ORE = "OutputOreName";
    static final String TAG_INPUT_ORES = "InputOreNames";
    static final String TAG_OUTPUT_ORES = "OutputOreNames";
    static final String TAG_VIRTUAL_INPUT_ORES = "VirtualInputOreNames";
    static final String TAG_VIRTUAL_OUTPUT_ORES = "VirtualOutputOreNames";
    static final String TAG_VIRTUAL_INPUT_STACKS = "VirtualInputStacks";
    static final String TAG_VIRTUAL_OUTPUT_STACKS = "VirtualOutputStacks";

    @Override
    public boolean isEncodedPattern(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(TechStartItems.PATTERN_INTEGRATIONS.get())) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return isEncodedTag(tag);
    }

    private boolean isEncodedTag(@Nullable CompoundTag tag) {
        return tag != null && (
                tag.getBoolean(TAG_ENCODED)
                        || tag.contains(TAG_ENCODED_ITEM)
                        || tag.contains(TAG_INPUT_ORE)
                        || tag.contains(TAG_OUTPUT_ORE)
                        || tag.contains(TAG_INPUT_ORES)
                        || tag.contains(TAG_OUTPUT_ORES)
                        || tag.contains(TAG_VIRTUAL_INPUT_ORES)
                        || tag.contains(TAG_VIRTUAL_OUTPUT_ORES)
                        || tag.contains(TAG_VIRTUAL_INPUT_STACKS)
                        || tag.contains(TAG_VIRTUAL_OUTPUT_STACKS));
    }

    @Override
    public @Nullable IPatternDetails decodePattern(AEItemKey what, Level level) {
        if (what == null || what.getItem() != TechStartItems.PATTERN_INTEGRATIONS.get()) {
            return null;
        }
        ItemStack stack = what.toStack();
        if (!isEncodedPattern(stack)) {
            return null;
        }
        return new TechStartPatternDetails(what, stack);
    }

    @Override
    public @Nullable IPatternDetails decodePattern(ItemStack what, Level level, boolean allowInaccessible) {
        if (!isEncodedPattern(what)) {
            return null;
        }
        var key = AEItemKey.of(what);
        if (key == null) {
            return null;
        }
        return new TechStartPatternDetails(key, what.copy());
    }
}
