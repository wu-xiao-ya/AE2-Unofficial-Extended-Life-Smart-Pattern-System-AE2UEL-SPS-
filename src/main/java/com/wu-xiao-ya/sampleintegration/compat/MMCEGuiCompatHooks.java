package com.lwx1145.sampleintegration.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Runtime hook factory for MMCE GUI slot suppliers.
 * Avoids lambda metafactory linkage against unstable method handles.
 */
public final class MMCEGuiCompatHooks {

    private MMCEGuiCompatHooks() {
    }

    public static Function<Object, Object> itemSlotFunction() {
        return stack -> createSlot(
            "github.kasuminova.mmce.client.gui.widget.slot.SlotItemVirtual",
            "github.kasuminova.mmce.client.gui.widget.slot.SlotItemVirtualJEI",
            stack
        );
    }

    public static Function<Object, Object> fluidSlotFunction() {
        return fluid -> createSlot(
            "github.kasuminova.mmce.client.gui.widget.slot.SlotFluidVirtual",
            "github.kasuminova.mmce.client.gui.widget.slot.SlotFluidVirtualJEI",
            fluid
        );
    }

    public static Function<Object, Object> gasSlotFunction() {
        return gas -> createSlot(
            "github.kasuminova.mmce.client.gui.widget.slot.SlotGasVirtual",
            "github.kasuminova.mmce.client.gui.widget.slot.SlotGasVirtualJEI",
            gas
        );
    }

    private static Object createSlot(String normalClassName, String jeiClassName, Object arg) {
        try {
            String className = isJeiPresent() ? jeiClassName : normalClassName;
            Class<?> cls = Class.forName(className);
            if (arg != null) {
                Constructor<?> oneArg = findOneArgCtor(cls, arg.getClass());
                if (oneArg != null) {
                    return oneArg.newInstance(arg);
                }
            }
            Constructor<?> noArg = cls.getDeclaredConstructor();
            noArg.setAccessible(true);
            return noArg.newInstance();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create MMCE GUI slot: " + normalClassName, t);
        }
    }

    private static Constructor<?> findOneArgCtor(Class<?> cls, Class<?> argClass) {
        for (Constructor<?> ctor : cls.getDeclaredConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length == 1 && params[0].isAssignableFrom(argClass)) {
                ctor.setAccessible(true);
                return ctor;
            }
        }
        return null;
    }

    private static boolean isJeiPresent() {
        try {
            Class<?> modsClass = Class.forName("hellfirepvp.modularmachinery.common.base.Mods");
            Field jeiField = modsClass.getField("JEI");
            Object jeiConst = jeiField.get(null);
            Method isPresent = modsClass.getMethod("isPresent");
            Object result = isPresent.invoke(jeiConst);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }
}

