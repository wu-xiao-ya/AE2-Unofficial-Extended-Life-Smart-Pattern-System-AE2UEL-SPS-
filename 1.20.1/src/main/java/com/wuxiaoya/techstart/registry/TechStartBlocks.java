package com.wuxiaoya.techstart.registry;

import com.wuxiaoya.techstart.TechStartForge;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class TechStartBlocks {
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, TechStartForge.MODID);

    private TechStartBlocks() {
    }
}
