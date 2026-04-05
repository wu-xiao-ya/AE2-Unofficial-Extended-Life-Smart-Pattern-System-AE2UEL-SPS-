package com.wuxiaoya.techstart.registry;

import com.wuxiaoya.techstart.TechStartForge;
import com.wuxiaoya.techstart.content.PatternIntegrationsItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TechStartItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TechStartForge.MODID);

    public static final RegistryObject<Item> PATTERN_INTEGRATIONS = ITEMS.register("pattern_integrations",
            () -> new PatternIntegrationsItem(new Item.Properties().stacksTo(1)));

    private TechStartItems() {
    }
}
