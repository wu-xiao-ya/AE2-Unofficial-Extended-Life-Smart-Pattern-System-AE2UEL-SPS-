package com.lwx1145.sampleintegration.mixin.defensive_mixin;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.orecruncher.dsurround.registry.item.ItemClass", remap = false)
public class MixinItemClass {

    @Inject(method = "effectiveArmorStack", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onEffectiveArmorStack(net.minecraft.entity.Entity entity, int slot, CallbackInfoReturnable<ItemStack> cir) {
        System.out.println("[DefensiveMixin-DEBUG] MixinItemClass.effectiveArmorStack HEAD");
        try {
            if (entity == null) {
                System.out.println("[DefensiveMixin] ItemClass.effectiveArmorStack: entity is null, returning EMPTY");
                cir.setReturnValue(ItemStack.EMPTY);
                cir.cancel();
                return;
            }

            ItemStack stack = null;
            try {
                stack = (ItemStack) entity.getClass().getMethod("getItemStack", int.class).invoke(entity, slot);
            } catch (Throwable t) {
                System.out.println("[DefensiveMixin-DEBUG] failed to reflect getItemStack");
            }

            if (stack == null || stack.isEmpty()) {
                System.out.println("[DefensiveMixin] ItemClass.effectiveArmorStack: stack is empty, returning EMPTY");
                cir.setReturnValue(ItemStack.EMPTY);
                cir.cancel();
                return;
            }

            Class<?> regMgrClazz = Class.forName("org.orecruncher.dsurround.registry.RegistryManager");
            Object itemRegistry = regMgrClazz.getField("ITEMS").get(null);
            Object itemData = itemRegistry.getClass().getMethod("getItemClass", ItemStack.class).invoke(itemRegistry, stack);
            if (itemData == null) {
                System.out.println("[DefensiveMixin] ItemClass.effectiveArmorStack: itemData is null, returning EMPTY");
                cir.setReturnValue(ItemStack.EMPTY);
                cir.cancel();
            }
        } catch (Throwable t) {
            System.out.println("[DefensiveMixin] ItemClass.effectiveArmorStack: caught exception, returning EMPTY");
            cir.setReturnValue(ItemStack.EMPTY);
            cir.cancel();
        }
    }
}
