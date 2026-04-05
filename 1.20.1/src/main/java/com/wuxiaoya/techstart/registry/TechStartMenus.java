package com.wuxiaoya.techstart.registry;

import com.wuxiaoya.techstart.TechStartForge;
import com.wuxiaoya.techstart.menu.PatternEditorMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TechStartMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, TechStartForge.MODID);

    public static final RegistryObject<MenuType<PatternEditorMenu>> PATTERN_EDITOR_ITEM =
            MENUS.register("pattern_editor_item", () -> IForgeMenuType.create(PatternEditorMenu::createItemMenu));

    private TechStartMenus() {
    }
}
