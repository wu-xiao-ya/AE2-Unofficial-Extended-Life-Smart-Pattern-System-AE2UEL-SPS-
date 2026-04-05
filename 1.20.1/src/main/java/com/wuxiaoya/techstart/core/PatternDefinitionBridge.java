package com.wuxiaoya.techstart.core;

import com.wuxiaoya.techstart.core.codec.LegacyPatternDefinitionReader;
import com.wuxiaoya.techstart.core.codec.ModernPatternDefinitionReader;
import com.wuxiaoya.techstart.core.model.PatternDefinition;
import net.minecraft.nbt.CompoundTag;

public final class PatternDefinitionBridge {
    private static final ModernPatternDefinitionReader MODERN_READER = new ModernPatternDefinitionReader();
    private static final LegacyPatternDefinitionReader LEGACY_READER = new LegacyPatternDefinitionReader();

    private PatternDefinitionBridge() {
    }

    public static PatternDefinition read(CompoundTag tag) {
        CompoundTag safeTag = tag == null ? new CompoundTag() : tag;
        PatternDefinition modern = MODERN_READER.read(new ForgeCompoundTagAdapter(safeTag));
        if (!modern.entries().isEmpty() || modern.encoded()) {
            return modern;
        }
        return LEGACY_READER.read(new ForgeCompoundTagAdapter(safeTag));
    }

    public static PatternDefinition readModern(CompoundTag tag) {
        return MODERN_READER.read(new ForgeCompoundTagAdapter(tag == null ? new CompoundTag() : tag));
    }

    public static PatternDefinition readLegacy(CompoundTag tag) {
        return LEGACY_READER.read(new ForgeCompoundTagAdapter(tag == null ? new CompoundTag() : tag));
    }
}
