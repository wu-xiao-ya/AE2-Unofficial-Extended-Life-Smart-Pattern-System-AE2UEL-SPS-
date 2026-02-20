package com.lwx1145.sampleintegration;


import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OreDictRecipeCache {
    private static final Map<String, Map<String, String>> PREFIX_TO_MATERIAL = new HashMap<>();
    private static final List<String> SUPPORTED_PREFIXES = new ArrayList<>();
    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        SUPPORTED_PREFIXES.add("ingot");
        SUPPORTED_PREFIXES.add("plate");
        SUPPORTED_PREFIXES.add("block");
        SUPPORTED_PREFIXES.add("nugget");
        SUPPORTED_PREFIXES.add("rod");
        SUPPORTED_PREFIXES.add("gear");
        SUPPORTED_PREFIXES.add("wire");
        SUPPORTED_PREFIXES.add("dust");

        for (String prefix : ModConfig.customOrePrefixes) {
            if (prefix == null) {
                continue;
            }
            String trimmed = prefix.trim();
            if (!trimmed.isEmpty() && !SUPPORTED_PREFIXES.contains(trimmed)) {
                SUPPORTED_PREFIXES.add(trimmed);
            }
        }

        buildPrefixCache();
    }

    public static List<String[]> getRecipes(String inputOreName, String outputOreName) {
        init();

        if (inputOreName == null || outputOreName == null) {
            return new ArrayList<>();
        }
        if (!inputOreName.endsWith("*") || !outputOreName.endsWith("*")) {
            return new ArrayList<>();
        }

        String inputPrefix = inputOreName.substring(0, inputOreName.length() - 1);
        String outputPrefix = outputOreName.substring(0, outputOreName.length() - 1);

        Map<String, String> inputMap = PREFIX_TO_MATERIAL.get(inputPrefix);
        Map<String, String> outputMap = PREFIX_TO_MATERIAL.get(outputPrefix);

        List<String[]> recipes = new ArrayList<>();
        Set<String> seenPairs = new HashSet<>();
        addCustomRecipePairs(inputPrefix, outputPrefix, recipes, seenPairs);

        if (inputMap == null || outputMap == null) {
            return recipes;
        }
        for (Map.Entry<String, String> entry : inputMap.entrySet()) {
            String material = entry.getKey();
            String inputOre = entry.getValue();
            String outputOre = outputMap.get(material);
            if (outputOre == null) {
                continue;
            }

            List<ItemStack> inputItems = OreDictionary.getOres(inputOre);
            List<ItemStack> outputItems = OreDictionary.getOres(outputOre);
            if (inputItems.isEmpty() || outputItems.isEmpty()) {
                continue;
            }

            String displayName = inputItems.get(0).getDisplayName() + " -> " + outputItems.get(0).getDisplayName();
            addRecipe(recipes, seenPairs, inputOre, outputOre, displayName);
        }

        return recipes;
    }

    private static void buildPrefixCache() {
        String[] allOreNames = OreDictionary.getOreNames();
        for (String prefix : SUPPORTED_PREFIXES) {
            Map<String, String> materialMap = new LinkedHashMap<>();
            addCustomOreNames(prefix, materialMap);
            for (String oreName : allOreNames) {
                if (!oreName.startsWith(prefix)) {
                    continue;
                }
                String material = oreName.substring(prefix.length());
                if (material.isEmpty()) {
                    continue;
                }
                if (materialMap.containsKey(material)) {
                    continue;
                }
                materialMap.put(material, oreName);
            }
            PREFIX_TO_MATERIAL.put(prefix, materialMap);
        }
    }

    // Expose supported prefixes and helper utilities so other classes can rely on the same configured set.
    public static synchronized java.util.List<String> getSupportedPrefixes() {
        init();
        return new ArrayList<>(SUPPORTED_PREFIXES);
    }

    public static synchronized String findPrefix(String oreName) {
        if (oreName == null) return null;
        init();
        for (String prefix : SUPPORTED_PREFIXES) {
            if (oreName.startsWith(prefix)) return prefix;
        }
        return null;
    }

    public static synchronized String extractMaterial(String oreName) {
        if (oreName == null) return null;
        init();
        String prefix = findPrefix(oreName);
        if (prefix == null) return null;
        String material = oreName.substring(prefix.length());
        if (material.isEmpty()) return null;
        // ensure extracted material doesn't itself start with a known prefix (avoid mis-parsing)
        for (String p : SUPPORTED_PREFIXES) {
            if (material.startsWith(p)) return null;
        }
        return material;
    }

    private static void addCustomOreNames(String prefix, Map<String, String> materialMap) {
        for (String oreName : ModConfig.customOreNames) {
            if (oreName == null) {
                continue;
            }
            String trimmed = oreName.trim();
            if (trimmed.isEmpty() || !trimmed.startsWith(prefix)) {
                continue;
            }
            String material = trimmed.substring(prefix.length());
            if (material.isEmpty() || materialMap.containsKey(material)) {
                continue;
            }
            materialMap.put(material, trimmed);
        }
    }

    private static void addCustomRecipePairs(String inputPrefix, String outputPrefix,
                                             List<String[]> recipes, Set<String> seenPairs) {
        for (String pair : ModConfig.customRecipePairs) {
            String[] parsed = parsePair(pair);
            if (parsed == null) {
                continue;
            }
            String inputOre = parsed[0];
            String outputOre = parsed[1];
            if (!inputOre.startsWith(inputPrefix) || !outputOre.startsWith(outputPrefix)) {
                continue;
            }
            String displayName = buildDisplayName(inputOre, outputOre);
            addRecipe(recipes, seenPairs, inputOre, outputOre, displayName);
        }
    }

    private static String[] parsePair(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        int arrowIndex = trimmed.indexOf("->");
        if (arrowIndex < 0) {
            return null;
        }
        String input = trimmed.substring(0, arrowIndex).trim();
        String output = trimmed.substring(arrowIndex + 2).trim();
        if (input.isEmpty() || output.isEmpty()) {
            return null;
        }
        return new String[]{input, output};
    }

    private static String buildDisplayName(String inputOre, String outputOre) {
        List<ItemStack> inputItems = OreDictionary.getOres(inputOre);
        List<ItemStack> outputItems = OreDictionary.getOres(outputOre);
        if (!inputItems.isEmpty() && !outputItems.isEmpty()) {
            return inputItems.get(0).getDisplayName() + " 闁?" + outputItems.get(0).getDisplayName();
        }
        return inputOre + " 闁?" + outputOre;
    }

    private static void addRecipe(List<String[]> recipes, Set<String> seenPairs,
                                  String inputOre, String outputOre, String displayName) {
        String key = inputOre + "->" + outputOre;
        if (seenPairs.add(key)) {
            recipes.add(new String[]{inputOre, outputOre, displayName});
        }
    }
}


