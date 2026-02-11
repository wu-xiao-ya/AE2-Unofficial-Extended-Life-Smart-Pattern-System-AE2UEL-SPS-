package com.lwx1145.techstart.mixin;


import appeng.helpers.DualityInterface;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import com.lwx1145.techstart.ItemTest;
import com.lwx1145.techstart.SmartPatternDetails;
import com.lwx1145.techstart.WildcardPatternWrapper;
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
     * 褰?AE 鎺ュ彛鏀堕泦鏍锋澘鏃讹紝澶勭悊閫氶厤绗︽牱鏉垮睍寮€
     */
    @Inject(method = "addToCraftingList(Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void techstart$expandWildcardPatterns(ItemStack stack, CallbackInfo ci) {
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
    private void techstart$filterWildcardPatterns(appeng.api.networking.crafting.ICraftingProviderHelper helper, CallbackInfo ci) {
        if (craftingList == null || craftingList.isEmpty()) {
            return;
        }

        // 绉婚櫎閫氶厤绗︽牱鏉匡紙鍙繚鐣欒櫄鎷熸牱鏉匡級
        craftingList.removeIf(pattern -> {
            if (pattern instanceof SmartPatternDetails) {
                SmartPatternDetails sp = (SmartPatternDetails) pattern;
                if (sp.isWildcardPattern() && !sp.isVirtual()) {
                    return true;
                }
            }
            // 绉婚櫎鍖呰鍣?
            if (pattern instanceof WildcardPatternWrapper) {
                return true;
            }
            return false;
        });
    }
}
