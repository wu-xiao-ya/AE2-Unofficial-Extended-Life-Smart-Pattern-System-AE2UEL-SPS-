package com.wuxiaoya.techstart.integration.mekanism;

import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MekanismGasHelper {
    private static final Pattern RESOURCE_LOCATION_PATTERN =
            Pattern.compile("([a-z0-9_.-]+:[a-z0-9_./-]+)");

    private static boolean initialized;
    private static Capability<?> gasHandlerCapability;
    private static Method handlerGetTanksMethod;
    private static Method handlerGetChemicalInTankMethod;
    private static Method chemicalStackIsEmptyMethod;
    private static Method chemicalStackGetAmountMethod;
    private static Method chemicalStackGetTypeRegistryNameMethod;
    private static Method chemicalStackGetTextComponentMethod;
    private static Method gasRegistryMethod;
    private static Method registryGetValueMethod;
    private static Method gasGetTextComponentMethod;
    private static Method gasGetIconMethod;
    private static Method gasGetTintMethod;

    private MekanismGasHelper() {
    }

    public static @Nullable GasStackView extractGas(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        init();
        if (gasHandlerCapability == null
                || handlerGetTanksMethod == null
                || handlerGetChemicalInTankMethod == null
                || chemicalStackIsEmptyMethod == null
                || chemicalStackGetAmountMethod == null
                || chemicalStackGetTypeRegistryNameMethod == null) {
            return null;
        }

        LazyOptional<?> optional = stack.getCapability(castCapability(gasHandlerCapability));
        Object handler = optional.resolve().orElse(null);
        if (handler == null) {
            return null;
        }

        try {
            int tanks = ((Number) handlerGetTanksMethod.invoke(handler)).intValue();
            for (int tank = 0; tank < tanks; tank++) {
                Object gasStack = handlerGetChemicalInTankMethod.invoke(handler, tank);
                if (gasStack == null || isChemicalStackEmpty(gasStack)) {
                    continue;
                }
                ResourceLocation gasId = (ResourceLocation) chemicalStackGetTypeRegistryNameMethod.invoke(gasStack);
                if (gasId == null) {
                    continue;
                }
                long amount = readLong(chemicalStackGetAmountMethod.invoke(gasStack));
                if (amount <= 0) {
                    continue;
                }
                Component displayName = resolveChemicalStackDisplayName(gasStack, gasId);
                GasRenderData renderData = getRenderData(gasId.toString());
                ResourceLocation icon = renderData == null ? null : renderData.icon();
                int tint = renderData == null ? -1 : renderData.tint();
                return new GasStackView(gasId.toString(), displayName, icon, tint, clampToInt(amount));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static @Nullable GasStackView extractGasOrTag(ItemStack stack) {
        GasStackView byCapability = extractGas(stack);
        if (byCapability != null) {
            return byCapability;
        }
        return extractGasFromTag(stack);
    }

    public static @Nullable GasStackView extractGas(Object ingredient) {
        return extractGas(ingredient, -1);
    }

    public static @Nullable GasStackView extractGas(Object ingredient, long preferredAmount) {
        if (ingredient == null) {
            return null;
        }
        if (ingredient instanceof ResourceLocation resourceLocation) {
            return buildGasView(resourceLocation, preferredAmount);
        }
        if (ingredient instanceof CharSequence text) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(text.toString().trim());
            if (resourceLocation != null) {
                GasStackView view = buildGasView(resourceLocation, preferredAmount);
                if (view != null) {
                    return view;
                }
            }
        }
        if (ingredient instanceof ItemStack itemStack) {
            return extractGas(itemStack);
        }
        Object unwrapped = unwrapChemicalCarrier(ingredient, 0);
        if (unwrapped != null && unwrapped != ingredient) {
            GasStackView view = extractGas(unwrapped, preferredAmount);
            if (view != null) {
                return view;
            }
        }
        init();
        if (chemicalStackIsEmptyMethod == null
                || chemicalStackGetAmountMethod == null
                || chemicalStackGetTypeRegistryNameMethod == null) {
            return null;
        }

        try {
            if (isChemicalStackEmpty(ingredient)) {
                return null;
            }
            ResourceLocation gasId = (ResourceLocation) chemicalStackGetTypeRegistryNameMethod.invoke(ingredient);
            if (gasId == null) {
                return null;
            }
            long amount = readLong(chemicalStackGetAmountMethod.invoke(ingredient));
            if (preferredAmount > 0) {
                amount = preferredAmount;
            }
            if (amount <= 0) {
                return null;
            }
            Component displayName = resolveChemicalStackDisplayName(ingredient, gasId);
            GasRenderData renderData = getRenderData(gasId.toString());
            ResourceLocation icon = renderData == null ? null : renderData.icon();
            int tint = renderData == null ? -1 : renderData.tint();
            return new GasStackView(gasId.toString(), displayName, icon, tint, clampToInt(amount));
        } catch (Exception ignored) {
        }

        ResourceLocation gasId = getChemicalRegistryName(ingredient);
        if (gasId == null) {
            return null;
        }
        return buildGasView(gasId, preferredAmount);
    }

    public static Component getDisplayName(String gasId) {
        GasRenderData renderData = getRenderData(gasId);
        return renderData == null ? Component.literal(gasId == null ? "unknown" : gasId) : renderData.displayName();
    }

    public static @Nullable GasRenderData getRenderData(String gasId) {
        if (gasId == null || gasId.isBlank()) {
            return null;
        }
        String normalizedId = normalizeGasId(gasId);
        if (!normalizedId.equals(gasId)) {
            GasRenderData normalized = getRenderDataDirect(normalizedId);
            if (normalized != null) {
                return normalized;
            }
        }
        return getRenderDataDirect(gasId);
    }

    private static @Nullable GasRenderData getRenderDataDirect(String gasId) {
        if (gasId == null || gasId.isBlank()) {
            return null;
        }
        init();
        if (gasRegistryMethod == null || registryGetValueMethod == null) {
            return null;
        }

        ResourceLocation key = ResourceLocation.tryParse(gasId.trim());
        if (key == null) {
            return null;
        }

        try {
            Object registry = gasRegistryMethod.invoke(null);
            if (registry == null) {
                return null;
            }
            Object gas = registryGetValueMethod.invoke(registry, key);
            if (gas == null) {
                return null;
            }
            Component displayName = gasGetTextComponentMethod == null
                    ? Component.literal(gasId)
                    : (Component) gasGetTextComponentMethod.invoke(gas);
            ResourceLocation icon = gasGetIconMethod == null ? null : (ResourceLocation) gasGetIconMethod.invoke(gas);
            int tint = gasGetTintMethod == null ? -1 : ((Number) gasGetTintMethod.invoke(gas)).intValue();
            return new GasRenderData(displayName == null ? Component.literal(gasId) : displayName, icon, tint);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static @Nullable ResourceLocation getChemicalRegistryName(Object chemical) {
        init();
        if (chemical == null || gasRegistryMethod == null) {
            return null;
        }
        try {
            Object registry = gasRegistryMethod.invoke(null);
            if (registry == null) {
                return null;
            }
            Method getKeyMethod = registry.getClass().getMethod("getKey", Object.class);
            Object key = getKeyMethod.invoke(registry, chemical);
            return key instanceof ResourceLocation resourceLocation ? resourceLocation : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Capability<Object> castCapability(Capability<?> capability) {
        return (Capability<Object>) capability;
    }

    private static boolean isChemicalStackEmpty(Object gasStack) {
        try {
            Object result = chemicalStackIsEmptyMethod.invoke(gasStack);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception ignored) {
            return true;
        }
    }

    private static Component resolveChemicalStackDisplayName(Object gasStack, ResourceLocation gasId) {
        if (chemicalStackGetTextComponentMethod != null) {
            try {
                Object value = chemicalStackGetTextComponentMethod.invoke(gasStack);
                if (value instanceof Component component) {
                    return component;
                }
            } catch (Exception ignored) {
            }
        }
        GasRenderData renderData = getRenderData(gasId.toString());
        return renderData == null ? Component.literal(gasId.toString()) : renderData.displayName();
    }

    private static @Nullable GasStackView buildGasView(ResourceLocation gasId, long preferredAmount) {
        if (gasId == null) {
            return null;
        }
        ResourceLocation normalizedGasId = normalizeGasId(gasId);
        GasRenderData renderData = getRenderData(normalizedGasId.toString());
        ResourceLocation icon = renderData == null ? null : renderData.icon();
        int tint = renderData == null ? -1 : renderData.tint();
        Component displayName = renderData == null ? Component.literal(normalizedGasId.toString()) : renderData.displayName();
        return new GasStackView(normalizedGasId.toString(), displayName, icon, tint, clampToInt(preferredAmount > 0 ? preferredAmount : 1));
    }

    private static String normalizeGasId(String gasId) {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(gasId.trim());
        if (resourceLocation == null) {
            return gasId;
        }
        return normalizeGasId(resourceLocation).toString();
    }

    private static ResourceLocation normalizeGasId(ResourceLocation gasId) {
        String path = gasId.getPath();
        for (String prefix : new String[]{"liquid_", "fluid_"}) {
            if (!path.startsWith(prefix) || path.length() <= prefix.length()) {
                continue;
            }
            ResourceLocation stripped = ResourceLocation.fromNamespaceAndPath(gasId.getNamespace(), path.substring(prefix.length()));
            if (getRenderDataDirect(stripped.toString()) != null) {
                return stripped;
            }
        }
        return gasId;
    }

    private static @Nullable Object unwrapChemicalCarrier(Object candidate, int depth) {
        if (candidate == null || depth > 4) {
            return null;
        }
        for (String accessor : new String[]{
                "getChemicalStack",
                "getStack",
                "getChemical",
                "getType",
                "getIngredient",
                "getRepresentedStack",
                "chemicalStack",
                "stack",
                "chemical",
                "type",
                "ingredient"
        }) {
            Object value = readAccessor(candidate, accessor);
            if (value == null || value == candidate) {
                continue;
            }
            if (value instanceof ResourceLocation || value instanceof CharSequence || value instanceof ItemStack) {
                return value;
            }
            Object nested = unwrapChemicalCarrier(value, depth + 1);
            return nested != null ? nested : value;
        }
        return null;
    }

    private static @Nullable Object readAccessor(Object candidate, String accessor) {
        Class<?> type = candidate.getClass();
        try {
            Method method = type.getMethod(accessor);
            return method.invoke(candidate);
        } catch (Exception ignored) {
        }
        try {
            Method method = type.getDeclaredMethod(accessor);
            method.setAccessible(true);
            return method.invoke(candidate);
        } catch (Exception ignored) {
        }
        try {
            Field field = type.getField(accessor);
            return field.get(candidate);
        } catch (Exception ignored) {
        }
        try {
            Field field = type.getDeclaredField(accessor);
            field.setAccessible(true);
            return field.get(candidate);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static long readLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static int clampToInt(long amount) {
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, amount));
    }

    private static @Nullable GasStackView extractGasFromTag(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return null;
        }
        String serialized = tag.toString().toLowerCase(Locale.ROOT);
        Matcher matcher = RESOURCE_LOCATION_PATTERN.matcher(serialized);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            if (!candidate.contains("mekanism")) {
                continue;
            }
            GasRenderData renderData = getRenderData(candidate);
            if (renderData == null) {
                continue;
            }
            return new GasStackView(candidate, renderData.displayName(), renderData.icon(), renderData.tint(), 1);
        }
        return null;
    }

    private static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        try {
            Class<?> capabilitiesClass = Class.forName("mekanism.common.capabilities.Capabilities");
            Field gasHandlerField = capabilitiesClass.getField("GAS_HANDLER");
            gasHandlerCapability = (Capability<?>) gasHandlerField.get(null);
        } catch (Exception ignored) {
            gasHandlerCapability = null;
        }

        try {
            Class<?> gasHandlerClass = Class.forName("mekanism.api.chemical.gas.IGasHandler");
            handlerGetTanksMethod = gasHandlerClass.getMethod("getTanks");
            handlerGetChemicalInTankMethod = gasHandlerClass.getMethod("getChemicalInTank", int.class);
        } catch (Exception ignored) {
            handlerGetTanksMethod = null;
            handlerGetChemicalInTankMethod = null;
        }

        try {
            Class<?> chemicalStackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            chemicalStackIsEmptyMethod = chemicalStackClass.getMethod("isEmpty");
            chemicalStackGetAmountMethod = chemicalStackClass.getMethod("getAmount");
            chemicalStackGetTypeRegistryNameMethod = chemicalStackClass.getMethod("getTypeRegistryName");
            chemicalStackGetTextComponentMethod = chemicalStackClass.getMethod("getTextComponent");
        } catch (Exception ignored) {
            chemicalStackIsEmptyMethod = null;
            chemicalStackGetAmountMethod = null;
            chemicalStackGetTypeRegistryNameMethod = null;
            chemicalStackGetTextComponentMethod = null;
        }

        try {
            Class<?> mekanismApiClass = Class.forName("mekanism.api.MekanismAPI");
            gasRegistryMethod = mekanismApiClass.getMethod("gasRegistry");
            Class<?> forgeRegistryClass = Class.forName("net.minecraftforge.registries.IForgeRegistry");
            registryGetValueMethod = forgeRegistryClass.getMethod("getValue", ResourceLocation.class);
        } catch (Exception ignored) {
            gasRegistryMethod = null;
            registryGetValueMethod = null;
        }

        try {
            Class<?> gasClass = Class.forName("mekanism.api.chemical.gas.Gas");
            gasGetTextComponentMethod = gasClass.getMethod("getTextComponent");
            gasGetIconMethod = gasClass.getMethod("getIcon");
            gasGetTintMethod = gasClass.getMethod("getTint");
        } catch (Exception ignored) {
            gasGetTextComponentMethod = null;
            gasGetIconMethod = null;
            gasGetTintMethod = null;
        }
    }

    public record GasStackView(String gasId, Component displayName, @Nullable ResourceLocation icon, int tint, int amount) {
    }

    public record GasRenderData(Component displayName, @Nullable ResourceLocation icon, int tint) {
    }
}
