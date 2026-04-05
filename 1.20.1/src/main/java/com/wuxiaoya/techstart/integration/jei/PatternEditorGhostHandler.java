package com.wuxiaoya.techstart.integration.jei;

import com.wuxiaoya.techstart.client.PatternEditorScreen;
import com.wuxiaoya.techstart.menu.PatternEditorMenu;
import com.wuxiaoya.techstart.network.SetPatternSlotPacket;
import com.wuxiaoya.techstart.network.TechStartNetwork;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PatternEditorGhostHandler implements IGhostIngredientHandler<PatternEditorScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(PatternEditorScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        PatternEditorMenu menu = screen.getMenu();
        ItemStack marker = createMarker(menu, ingredient);
        if (marker.isEmpty()) {
            return List.of();
        }

        List<Target<I>> targets = new ArrayList<>();
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        for (int slotId = 0; slotId < menu.slots.size(); slotId++) {
            if (!menu.isPatternSlotId(slotId)) {
                continue;
            }
            Slot slot = menu.slots.get(slotId);
            Rect2i area = new Rect2i(left + slot.x, top + slot.y, 16, 16);
            int targetSlotId = slotId;
            ItemStack targetMarker = marker.copy();
            targets.add(new Target<>() {
                @Override
                public Rect2i getArea() {
                    return area;
                }

                @Override
                public void accept(I ignored) {
                    applyMarker(screen, targetSlotId, targetMarker);
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }

    @Override
    public boolean shouldHighlightTargets() {
        return true;
    }

    private <I> ItemStack createMarker(PatternEditorMenu menu, ITypedIngredient<I> ingredient) {
        Object rawIngredient = ingredient.getIngredient();
        String typeName = ingredient.getType().toString().toLowerCase(Locale.ROOT);
        boolean fluidOrGasType = typeName.contains("fluid")
                || typeName.contains("gas")
                || typeName.contains("chemical");

        ItemStack fromTyped = menu.createMarkerFromIngredient(rawIngredient);
        if (!fromTyped.isEmpty()) {
            return fromTyped;
        }
        if (fluidOrGasType) {
            return ItemStack.EMPTY;
        }
        ItemStack itemStack = ingredient.getItemStack().orElse(ItemStack.EMPTY);
        if (!itemStack.isEmpty()) {
            return menu.createMarkerFromIngredient(itemStack);
        }
        return ItemStack.EMPTY;
    }

    private void applyMarker(PatternEditorScreen screen, int slotId, ItemStack marker) {
        if (marker.isEmpty()) {
            return;
        }
        PatternEditorMenu menu = screen.getMenu();
        if (!menu.isPatternSlotId(slotId)) {
            return;
        }

        menu.slots.get(slotId).set(marker.copy());
        TechStartNetwork.CHANNEL.sendToServer(new SetPatternSlotPacket(slotId, marker.copy()));
    }
}
