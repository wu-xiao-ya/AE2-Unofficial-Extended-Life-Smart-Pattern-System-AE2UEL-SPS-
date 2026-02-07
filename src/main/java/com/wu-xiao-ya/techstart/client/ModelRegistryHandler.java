package com.lwx1145.techstart.client;

import com.lwx1145.techstart.TechStart;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = TechStart.MODID, value = Side.CLIENT)
public final class ModelRegistryHandler {

    private ModelRegistryHandler() {}

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerItemModel(TechStart.ITEM_TEST);
        registerItemModel(Item.getItemFromBlock(TechStart.PATTERN_EXPANDER));
    }

    private static void registerItemModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
            new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
