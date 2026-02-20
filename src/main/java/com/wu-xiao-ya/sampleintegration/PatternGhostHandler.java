package com.lwx1145.sampleintegration;

import mezz.jei.api.gui.IGhostIngredientHandler;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class PatternGhostHandler implements IGhostIngredientHandler<GuiPatternEditor> {

    @Override
    public <I> List<Target<I>> getTargets(GuiPatternEditor gui, I ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        if (ingredient == null) {
            return targets;
        }

        ContainerPatternEditor container = (ContainerPatternEditor) gui.inventorySlots;
        ItemStack marker = container.createMarkerFromIngredient(ingredient);
        if (marker.isEmpty()) {
            return targets;
        }

        for (int i = 0; i < container.inventorySlots.size(); i++) {
            if (!container.isPatternSlotId(i)) {
                continue;
            }
            Slot slot = container.getSlot(i);
            final int slotNumber = slot.slotNumber;
            final int x = gui.getGuiLeft() + slot.xPos;
            final int y = gui.getGuiTop() + slot.yPos;

            targets.add(new Target<I>() {
                @Override
                public Rectangle getArea() {
                    return new Rectangle(x, y, 16, 16);
                }

                @Override
                public void accept(I acceptedIngredient) {
                    container.setMarkerStackInSlot(slotNumber, marker.copy());
                    PacketHandler.INSTANCE.sendToServer(new PacketSetPatternSlot(slotNumber, marker.copy()));
                }
            });
        }

        return targets;
    }

    @Override
    public void onComplete() {
        // no-op
    }

    public boolean shouldHighlightTargets() {
        return true;
    }
}

