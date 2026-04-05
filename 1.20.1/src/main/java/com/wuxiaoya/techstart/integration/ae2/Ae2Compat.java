package com.wuxiaoya.techstart.integration.ae2;

import com.wuxiaoya.techstart.TechStartForge;

import appeng.api.crafting.PatternDetailsHelper;

public final class Ae2Compat {
    private static boolean initialized = false;

    private Ae2Compat() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        PatternDetailsHelper.registerDecoder(new TechStartPatternDecoder());
        TechStartForge.LOGGER.info("AE2 compat initialized: registered TechStart pattern decoder.");
    }
}
