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
     * 当 AE 接口收集样板时，处理通配符样板展开
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

        String inputOre = ItemTest.getInputOreNameStatic(stack);
        String outputOre = ItemTest.getOutputOreNameStatic(stack);
        
        // 对于虚拟样板（已展开的），使用原逻辑
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("VirtualInputOreName")) {
            return;
        }
        
        // 对于通配符样板，展开为虚拟样板
        if (inputOre.contains("*") || outputOre.contains("*")) {
            SmartPatternDetails mainPattern = new SmartPatternDetails(stack);
            List<SmartPatternDetails> virtualPatterns = mainPattern.expandToVirtualPatterns();

            for (SmartPatternDetails virtualPattern : virtualPatterns) {
                craftingList.add(virtualPattern);
            }

            // 取消原始方法，不添加通配符样板或包装器本身
            ci.cancel();
            return;
        }
    }

    @Inject(method = "provideCrafting", at = @At("HEAD"))
    private void techstart$filterWildcardPatterns(appeng.api.networking.crafting.ICraftingProviderHelper helper, CallbackInfo ci) {
        if (craftingList == null || craftingList.isEmpty()) {
            return;
        }

        // 移除通配符样板（只保留虚拟样板）
        craftingList.removeIf(pattern -> {
            if (pattern instanceof SmartPatternDetails) {
                SmartPatternDetails sp = (SmartPatternDetails) pattern;
                if (sp.isWildcardPattern() && !sp.isVirtual()) {
                    return true;
                }
            }
            // 移除包装器
            if (pattern instanceof WildcardPatternWrapper) {
                return true;
            }
            return false;
        });
    }
}
