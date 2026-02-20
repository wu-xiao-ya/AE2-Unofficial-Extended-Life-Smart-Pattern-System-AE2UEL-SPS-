package com.lwx1145.sampleintegration.compat;

import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;

/**
 * Runtime bridge for environments where ItemStack empty-field mappings differ.
 */
public final class ItemStackCompat {

    private static final Field EMPTY_FIELD = resolveEmptyField();

    private ItemStackCompat() {
    }

    public static ItemStack empty() {
        if (EMPTY_FIELD != null) {
            try {
                Object value = EMPTY_FIELD.get(null);
                if (value instanceof ItemStack) {
                    return (ItemStack) value;
                }
            } catch (Throwable ignored) {
            }
        }
        return ItemStack.EMPTY;
    }

    private static Field resolveEmptyField() {
        Field field = findField("EMPTY");
        if (field != null) {
            return field;
        }
        return findField("field_190927_a");
    }

    private static Field findField(String name) {
        try {
            Field field = ItemStack.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable ignored) {
            return null;
        }
    }
}

