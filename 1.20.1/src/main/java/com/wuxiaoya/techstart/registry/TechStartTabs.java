package com.wuxiaoya.techstart.registry;

import com.wuxiaoya.techstart.TechStartForge;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class TechStartTabs {
    private static final ResourceKey<Registry<CreativeModeTab>> CREATIVE_TAB_REGISTRY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("minecraft", "creative_mode_tab"));

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(CREATIVE_TAB_REGISTRY, TechStartForge.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.sampleintegration.main"))
                    .icon(() -> new ItemStack(TechStartItems.PATTERN_INTEGRATIONS.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(TechStartItems.PATTERN_INTEGRATIONS.get());
                    })
                    .build());

    private TechStartTabs() {
    }
}

