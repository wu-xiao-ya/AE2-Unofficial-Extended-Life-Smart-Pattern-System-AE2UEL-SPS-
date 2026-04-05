package com.wuxiaoya.techstart.core;

import com.wuxiaoya.techstart.core.codec.tag.TagObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public final class ForgeCompoundTagAdapter implements TagObject {
    private static final ForgeCompoundTagAdapter EMPTY = new ForgeCompoundTagAdapter(new CompoundTag());

    private final CompoundTag delegate;

    public ForgeCompoundTagAdapter(CompoundTag delegate) {
        this.delegate = delegate == null ? new CompoundTag() : delegate;
    }

    public static ForgeCompoundTagAdapter of(CompoundTag delegate) {
        return new ForgeCompoundTagAdapter(delegate);
    }

    @Override
    public boolean has(String key) {
        return delegate.contains(key);
    }

    @Override
    public String getString(String key) {
        return delegate.contains(key, Tag.TAG_STRING) ? delegate.getString(key).trim() : "";
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return delegate.contains(key, Tag.TAG_ANY_NUMERIC) ? delegate.getInt(key) : defaultValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return delegate.contains(key, Tag.TAG_BYTE) ? delegate.getBoolean(key) : defaultValue;
    }

    @Override
    public TagObject getObject(String key) {
        return delegate.contains(key, Tag.TAG_COMPOUND) ? new ForgeCompoundTagAdapter(delegate.getCompound(key)) : EMPTY;
    }

    @Override
    public List<String> getStringList(String key) {
        if (!delegate.contains(key, Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag list = delegate.getList(key, Tag.TAG_STRING);
        List<String> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Tag raw = list.get(i);
            if (raw instanceof StringTag stringTag) {
                result.add(stringTag.getAsString().trim());
            }
        }
        return List.copyOf(result);
    }

    @Override
    public List<Integer> getIntList(String key) {
        if (!delegate.contains(key, Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag list = delegate.getList(key, Tag.TAG_INT);
        List<Integer> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Tag raw = list.get(i);
            if (raw instanceof NumericTag numericTag) {
                result.add(numericTag.getAsInt());
            }
        }
        return List.copyOf(result);
    }

    @Override
    public List<TagObject> getObjectList(String key) {
        if (!delegate.contains(key, Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag list = delegate.getList(key, Tag.TAG_COMPOUND);
        List<TagObject> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Tag raw = list.get(i);
            if (raw instanceof CompoundTag compoundTag) {
                result.add(new ForgeCompoundTagAdapter(compoundTag));
            }
        }
        return List.copyOf(result);
    }
}
