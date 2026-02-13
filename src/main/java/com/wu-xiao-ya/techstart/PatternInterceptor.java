package com.lwx1145.techstart;


import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.helpers.DualityInterface;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * MMCE/AE2 bridge helpers for smart patterns.
 * Keeps ItemTest behavior compatible with multiple provider implementations.
 */
public class PatternInterceptor {
    
    private static Field craftingListField;
    private static Field mmcePatternsField;
    private static Field mmceDetailsField;
    private static Method mmcePatternsGetStackInSlot;
    private static Method mmceFakeFluidCheckMethod;
    private static Method mmceFakeGasCheckMethod;
    private static Method mmceFakeItemGetStackMethod;
    private static Method mmceHandlerFillMethod;
    private static Method mmceHandlerReceiveGasMethod;
    private static int mmceDebugCount = 0;
    
    static {
        try {
            craftingListField = DualityInterface.class.getDeclaredField("craftingList");
            craftingListField.setAccessible(true);
        } catch (Exception e) {
            System.err.println("[PatternInterceptor] Failed to resolve DualityInterface.craftingList: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
    public static boolean interceptAndExpand(DualityInterface duality, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        if (!(stack.getItem() instanceof ItemTest)) {
            return false;
        }
        
        if (!ItemTest.hasEncodedItemStatic(stack)) {
            return false;
        }
        return true;
    }

    public static boolean allowMMCEPatternInsert(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemTest;
    }

    public static boolean shouldHandleMMCEPattern(ICraftingPatternDetails detail) {
        if (detail == null) {
            return false;
        }
        try {
            ItemStack pattern = detail.getPattern();
            return pattern != null && !pattern.isEmpty() && pattern.getItem() instanceof ItemTest;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean acceptsMMCEPattern(Object provider, ICraftingPatternDetails detail) {
        if (provider == null || !shouldHandleMMCEPattern(detail)) {
            return false;
        }
        try {
            Class<?> cls = provider.getClass();

            Object proxy = getFieldValue(cls, provider, "proxy");
            if (proxy == null) {
                mmceDebug("acceptsMMCEPattern: proxy is null");
                return false;
            }
            Boolean active = invokeBoolean(proxy, "isActive");
            Boolean powered = invokeBoolean(proxy, "isPowered");
            if (!Boolean.TRUE.equals(active) || !Boolean.TRUE.equals(powered)) {
                mmceDebug("acceptsMMCEPattern: inactive or unpowered, active=" + active + ", powered=" + powered);
                return false;
            }

            Object workMode = getFieldValue(cls, provider, "workMode");
            boolean enhanced = workMode != null && "ENHANCED_BLOCKING_MODE".equals(String.valueOf(workMode));
            if (!enhanced) {
                mmceDebug("acceptsMMCEPattern: accepted in non-enhanced mode");
                return true;
            }

            Object handler = getFieldValue(cls, provider, "handler");
            boolean handlerEmpty = handler == null || Boolean.TRUE.equals(invokeBoolean(handler, "isEmpty"));
            if (handlerEmpty) {
                mmceDebug("acceptsMMCEPattern: accepted in enhanced mode with empty handler");
                return true;
            }

            Object currentPattern = getFieldValue(cls, provider, "currentPattern");
            boolean accepted = currentPattern == null || currentPattern.equals(detail);
            mmceDebug("acceptsMMCEPattern: enhanced mode compare currentPattern => " + accepted);
            return accepted;
        } catch (Throwable t) {
            mmceDebug("acceptsMMCEPattern: exception " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
    }

    public static int interceptMMCEPushPattern(Object provider, ICraftingPatternDetails detail, InventoryCrafting table) {
        if (!shouldHandleMMCEPattern(detail)) {
            return -1;
        }
        mmceDebug("interceptMMCEPushPattern: handling ItemTest pattern");
        if (!acceptsMMCEPattern(provider, detail)) {
            mmceDebug("interceptMMCEPushPattern: rejected by acceptsMMCEPattern");
            return 0;
        }
        try {
            Object handler = resolveMMCEHandler(provider, detail);
            if (handler == null) {
                mmceDebug("interceptMMCEPushPattern: handler is null");
                return 0;
            }

            Method appendItem = handler.getClass().getMethod("appendItem", ItemStack.class);
            appendItem.setAccessible(true);
            for (int slot = 0; slot < table.getSizeInventory(); slot++) {
                ItemStack stackInSlot = table.getStackInSlot(slot);
                if (stackInSlot == null || stackInSlot.isEmpty()) {
                    continue;
                }

                Object fluidStack = tryExtractMMCEFluidStack(stackInSlot);
                if (fluidStack != null) {
                    if (invokeMMCEHandlerFill(handler, fluidStack)) {
                        continue;
                    }
                    mmceDebug("interceptMMCEPushPattern: failed to fill fake fluid, fallback appendItem");
                }

                Object gasStack = tryExtractMMCEGasStack(stackInSlot);
                if (gasStack != null) {
                    if (invokeMMCEHandlerReceiveGas(handler, gasStack)) {
                        continue;
                    }
                    mmceDebug("interceptMMCEPushPattern: failed to receive fake gas, fallback appendItem");
                }

                appendItem.invoke(handler, stackInSlot);
            }

            try {
                Method handleNewPattern = provider.getClass().getDeclaredMethod(
                    "handleNewPattern", ICraftingPatternDetails.class);
                handleNewPattern.setAccessible(true);
                handleNewPattern.invoke(provider, detail);
            } catch (Throwable ignored) {
            }

            Object workMode = getFieldValue(provider.getClass(), provider, "workMode");
            boolean craftingLock = workMode != null && "CRAFTING_LOCK_MODE".equals(String.valueOf(workMode));
            setFieldValue(provider.getClass(), provider, "machineCompleted", !craftingLock);
            mmceDebug("interceptMMCEPushPattern: accepted and injected items");
            return 1;
        } catch (Throwable t) {
            mmceDebug("interceptMMCEPushPattern: exception " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return 0;
        }
    }

    public static boolean interceptMMCEProvideCrafting(Object provider, Object craftingTracker) {
        if (provider == null || craftingTracker == null) {
            return false;
        }
        try {
            if (!Boolean.TRUE.equals(invokeBoolean(getFieldValue(provider.getClass(), provider, "proxy"), "isActive"))) {
                return false;
            }

            Object patternsObj = getFieldValue(provider.getClass(), provider, "patterns");
            if (patternsObj == null) {
                return false;
            }
            Method getSlots = patternsObj.getClass().getMethod("getSlots");
            Method getStackInSlot = patternsObj.getClass().getMethod("getStackInSlot", int.class);
            getSlots.setAccessible(true);
            getStackInSlot.setAccessible(true);

            Method addCraftingOption = null;
            for (Method m : craftingTracker.getClass().getMethods()) {
                if ("addCraftingOption".equals(m.getName()) && m.getParameterCount() == 2) {
                    addCraftingOption = m;
                    addCraftingOption.setAccessible(true);
                    break;
                }
            }
            if (addCraftingOption == null) {
                return false;
            }

            int slots = (Integer) getSlots.invoke(patternsObj);
            boolean handledAny = false;
            for (int i = 0; i < slots; i++) {
                Object raw = getStackInSlot.invoke(patternsObj, i);
                if (!(raw instanceof ItemStack)) {
                    continue;
                }
                ItemStack patternStack = (ItemStack) raw;
                if (patternStack.isEmpty() || !(patternStack.getItem() instanceof ItemTest) || !ItemTest.hasEncodedItemStatic(patternStack)) {
                    continue;
                }

                SmartPatternDetails main = new SmartPatternDetails(patternStack);
                List<SmartPatternDetails> candidates = main.isWildcardPattern()
                    ? main.expandToVirtualPatterns()
                    : Collections.singletonList(main);

                List<SmartPatternDetails> filtered = new ArrayList<>();
                for (SmartPatternDetails detail : candidates) {
                    if (detail != null && detail.isAllowedByFilter()) {
                        filtered.add(detail);
                    }
                }
                if (filtered.isEmpty()) {
                    continue;
                }

                for (SmartPatternDetails detail : filtered) {
                    addCraftingOption.invoke(craftingTracker, provider, detail);
                }
                handledAny = true;
            }
            if (handledAny) {
                mmceDebug("interceptMMCEProvideCrafting: registered expanded ItemTest patterns");
            }
            return handledAny;
        } catch (Throwable t) {
            mmceDebug("interceptMMCEProvideCrafting: exception " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
    }

    public static boolean interceptMMCERefreshPattern(Object provider, int slot) {
        if (provider == null || slot < 0) {
            return false;
        }
        try {
            Class<?> cls = provider.getClass();

            if (mmcePatternsField == null || mmcePatternsField.getDeclaringClass() != cls) {
                Field f = cls.getDeclaredField("patterns");
                f.setAccessible(true);
                mmcePatternsField = f;
                mmcePatternsGetStackInSlot = null;
            }
            Object patternsObj = mmcePatternsField.get(provider);
            if (patternsObj == null) {
                return false;
            }

            if (mmcePatternsGetStackInSlot == null || mmcePatternsGetStackInSlot.getDeclaringClass() != patternsObj.getClass()) {
                Method m = patternsObj.getClass().getMethod("getStackInSlot", int.class);
                m.setAccessible(true);
                mmcePatternsGetStackInSlot = m;
            }

            Object raw = mmcePatternsGetStackInSlot.invoke(patternsObj, slot);
            if (!(raw instanceof ItemStack)) {
                return false;
            }
            ItemStack pattern = (ItemStack) raw;
            if (pattern == null || pattern.isEmpty()) {
                return false;
            }

            Item item = pattern.getItem();
            if (!(item instanceof ItemTest) || !(item instanceof ICraftingPatternItem)) {
                return false;
            }

            World world = null;
            if (provider instanceof net.minecraft.tileentity.TileEntity) {
                world = ((net.minecraft.tileentity.TileEntity) provider).getWorld();
            }
            ICraftingPatternDetails detail = ((ICraftingPatternItem) item).getPatternForItem(pattern, world);
            if (detail == null) {
                return false;
            }

            if (mmceDetailsField == null || mmceDetailsField.getDeclaringClass() != cls) {
                Field f = cls.getDeclaredField("details");
                f.setAccessible(true);
                mmceDetailsField = f;
            }
            Object detailsObj = mmceDetailsField.get(provider);
            if (!(detailsObj instanceof ICraftingPatternDetails[])) {
                return false;
            }
            ICraftingPatternDetails[] details = (ICraftingPatternDetails[]) detailsObj;
            if (slot >= details.length) {
                return false;
            }
            details[slot] = detail;
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object getFieldValue(Class<?> startClass, Object instance, String fieldName) throws Exception {
        Class<?> current = startClass;
        while (current != null && current != Object.class) {
            try {
                Field f = current.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(instance);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Boolean invokeBoolean(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            m.setAccessible(true);
            Object r = m.invoke(target);
            if (r instanceof Boolean) {
                return (Boolean) r;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object resolveMMCEHandler(Object provider, ICraftingPatternDetails detail) throws Exception {
        Class<?> cls = provider.getClass();
        Object workMode = getFieldValue(cls, provider, "workMode");
        boolean isolation = workMode != null && "ISOLATION_INPUT".equals(String.valueOf(workMode));
        if (!isolation) {
            return getFieldValue(cls, provider, "handler");
        }

        Object detailsObj = getFieldValue(cls, provider, "details");
        Object componentsObj = getFieldValue(cls, provider, "combinationComponents");
        if (!(detailsObj instanceof Object[]) || !(componentsObj instanceof List)) {
            return null;
        }

        Object[] details = (Object[]) detailsObj;
        @SuppressWarnings("unchecked")
        List<Object> components = (List<Object>) componentsObj;
        int limit = Math.min(details.length, components.size());
        for (int i = 0; i < limit; i++) {
            Object d = details[i];
            if (d != null && d.equals(detail)) {
                Object component = components.get(i);
                Method getContainerProvider = component.getClass().getMethod("getContainerProvider");
                getContainerProvider.setAccessible(true);
                return getContainerProvider.invoke(component);
            }
        }
        if (!components.isEmpty()) {
            Object component = components.get(0);
            Method getContainerProvider = component.getClass().getMethod("getContainerProvider");
            getContainerProvider.setAccessible(true);
            return getContainerProvider.invoke(component);
        }
        return null;
    }

    private static void setFieldValue(Class<?> startClass, Object instance, String fieldName, Object value) throws Exception {
        Class<?> current = startClass;
        while (current != null && current != Object.class) {
            try {
                Field f = current.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(instance, value);
                return;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
    }

    private static Object tryExtractMMCEFluidStack(ItemStack stack) {
        try {
            initMMCEFakeMethods();
            if (mmceFakeFluidCheckMethod == null || mmceFakeItemGetStackMethod == null) {
                return null;
            }
            Object isFake = mmceFakeFluidCheckMethod.invoke(null, stack);
            if (!(isFake instanceof Boolean) || !((Boolean) isFake)) {
                return null;
            }
            Object data = mmceFakeItemGetStackMethod.invoke(null, stack);
            return isFluidStackObject(data) ? data : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object tryExtractMMCEGasStack(ItemStack stack) {
        try {
            initMMCEFakeMethods();
            if (mmceFakeGasCheckMethod == null || mmceFakeItemGetStackMethod == null) {
                return null;
            }
            Object isFake = mmceFakeGasCheckMethod.invoke(null, stack);
            if (!(isFake instanceof Boolean) || !((Boolean) isFake)) {
                return null;
            }
            Object data = mmceFakeItemGetStackMethod.invoke(null, stack);
            return isGasStackObject(data) ? data : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean invokeMMCEHandlerFill(Object handler, Object fluidStack) {
        if (handler == null || fluidStack == null) {
            return false;
        }
        try {
            if (mmceHandlerFillMethod == null || mmceHandlerFillMethod.getDeclaringClass() != handler.getClass()) {
                mmceHandlerFillMethod = findCompatibleMethod(handler.getClass(), "fill", fluidStack.getClass(), boolean.class);
            }
            if (mmceHandlerFillMethod == null) {
                return false;
            }
            mmceHandlerFillMethod.invoke(handler, fluidStack, true);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean invokeMMCEHandlerReceiveGas(Object handler, Object gasStack) {
        if (handler == null || gasStack == null) {
            return false;
        }
        try {
            if (mmceHandlerReceiveGasMethod == null || mmceHandlerReceiveGasMethod.getDeclaringClass() != handler.getClass()) {
                mmceHandlerReceiveGasMethod = findReceiveGasMethod(handler.getClass(), gasStack.getClass());
            }
            if (mmceHandlerReceiveGasMethod == null) {
                return false;
            }
            mmceHandlerReceiveGasMethod.invoke(handler, null, gasStack, true);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Method findCompatibleMethod(Class<?> owner, String name, Class<?> arg0, Class<?> arg1) {
        for (Method m : owner.getMethods()) {
            if (!name.equals(m.getName()) || m.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] p = m.getParameterTypes();
            if (p[0].isAssignableFrom(arg0) && (p[1] == arg1 || p[1] == Boolean.class)) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    private static Method findReceiveGasMethod(Class<?> owner, Class<?> gasStackClass) {
        for (Method m : owner.getMethods()) {
            if (!"receiveGas".equals(m.getName()) || m.getParameterCount() != 3) {
                continue;
            }
            Class<?>[] p = m.getParameterTypes();
            if (p[1].isAssignableFrom(gasStackClass) && (p[2] == boolean.class || p[2] == Boolean.class)) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    private static boolean isFluidStackObject(Object obj) {
        if (obj == null) {
            return false;
        }
        try {
            Class<?> fluidStackClass = Class.forName("net.minecraftforge.fluids.FluidStack");
            return fluidStackClass.isInstance(obj);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isGasStackObject(Object obj) {
        if (obj == null) {
            return false;
        }
        try {
            Class<?> gasStackClass = Class.forName("mekanism.api.gas.GasStack");
            return gasStackClass.isInstance(obj);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void initMMCEFakeMethods() {
        if (mmceFakeFluidCheckMethod == null) {
            try {
                Class<?> c = Class.forName("com.glodblock.github.common.item.fake.FakeFluids");
                mmceFakeFluidCheckMethod = c.getMethod("isFluidFakeItem", ItemStack.class);
                mmceFakeFluidCheckMethod.setAccessible(true);
            } catch (Throwable ignored) {
                mmceFakeFluidCheckMethod = null;
            }
        }
        if (mmceFakeGasCheckMethod == null) {
            try {
                Class<?> c = Class.forName("com.glodblock.github.integration.mek.FakeGases");
                mmceFakeGasCheckMethod = c.getMethod("isGasFakeItem", ItemStack.class);
                mmceFakeGasCheckMethod.setAccessible(true);
            } catch (Throwable ignored) {
                mmceFakeGasCheckMethod = null;
            }
        }
        if (mmceFakeItemGetStackMethod == null) {
            try {
                Class<?> c = Class.forName("com.glodblock.github.common.item.fake.FakeItemRegister");
                mmceFakeItemGetStackMethod = c.getMethod("getStack", ItemStack.class);
                mmceFakeItemGetStackMethod.setAccessible(true);
            } catch (Throwable ignored) {
                mmceFakeItemGetStackMethod = null;
            }
        }
    }

    private static void mmceDebug(String msg) {
        if (mmceDebugCount < 20) {
            mmceDebugCount++;
            System.out.println("[PatternInterceptor/MMCE] " + msg);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static void cleanupWildcardPatterns(DualityInterface duality) {
        try {
            Set<ICraftingPatternDetails> craftingList = (Set<ICraftingPatternDetails>) craftingListField.get(duality);
            
            if (craftingList == null || craftingList.isEmpty()) {
                return;
            }
            craftingList.removeIf(pattern -> {
                if (pattern instanceof SmartPatternDetails) {
                    SmartPatternDetails sp = (SmartPatternDetails) pattern;
                    if (sp.isWildcardPattern() && !sp.isVirtual()) {
                        return true;
                    }
                }
                return false;
            });
            
        } catch (Exception e) {
            System.err.println("[PatternInterceptor] cleanupWildcardPatterns failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
