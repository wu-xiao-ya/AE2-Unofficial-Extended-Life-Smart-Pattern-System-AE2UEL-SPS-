package com.wuxiaoya.techstart.integration.ae2;

import appeng.api.stacks.AEKey;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

final class AppliedMekanisticsCompat {
    private AppliedMekanisticsCompat() {
    }

    static @Nullable AEKey createGasKey(String gasId, long amount) {
        if (gasId == null || gasId.isBlank()) {
            return null;
        }
        ResourceLocation key = ResourceLocation.tryParse(gasId.trim());
        if (key == null) {
            return null;
        }
        Gas gas = MekanismAPI.gasRegistry().getValue(key);
        if (gas == null || gas == MekanismAPI.EMPTY_GAS) {
            return null;
        }
        return MekanismKey.of(new GasStack(gas, Math.max(1L, amount)));
    }
}
