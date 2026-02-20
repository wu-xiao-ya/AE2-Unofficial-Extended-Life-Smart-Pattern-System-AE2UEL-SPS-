package com.lwx1145.sampleintegration.mixin;


import appeng.helpers.DualityInterface;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import com.lwx1145.sampleintegration.ItemTest;
import com.lwx1145.sampleintegration.SmartPatternDetails;
import com.lwx1145.sampleintegration.WildcardPatternWrapper;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

@Mixin(value = DualityInterface.class, remap = false)
public abstract class MixinDualityInterface {

    @Shadow
    private Set<ICraftingPatternDetails> craftingList;

    /**
     */
    @Inject(method = "addToCraftingList(Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void sampleintegration$expandWildcardPatterns(ItemStack stack, CallbackInfo ci) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        if (!(stack.getItem() instanceof ItemTest)) {
            return;
        }

        if (!ItemTest.hasEncodedItemStatic(stack)) {
            return;
        }

        // Prevent the interface from providing ItemTest patterns; PatternExpander will provide them.
        ci.cancel();
        return;
    }

    @Inject(method = "provideCrafting", at = @At("HEAD"))
    private void sampleintegration$filterWildcardPatterns(appeng.api.networking.crafting.ICraftingProviderHelper helper, CallbackInfo ci) {
        if (craftingList == null || craftingList.isEmpty()) {
            return;
        }


        craftingList.removeIf(pattern -> {
            if (pattern instanceof SmartPatternDetails) {
                SmartPatternDetails sp = (SmartPatternDetails) pattern;
                if (sp.isWildcardPattern() && !sp.isVirtual()) {
                    return true;
                }
            }

            if (pattern instanceof WildcardPatternWrapper) {
                return true;
            }
            return false;
        });
    }
}

