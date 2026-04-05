package com.wuxiaoya.techstart.registry;

import com.wuxiaoya.techstart.TechStartForge;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class TechStartBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TechStartForge.MODID);

    private TechStartBlockEntities() {
    }
}
