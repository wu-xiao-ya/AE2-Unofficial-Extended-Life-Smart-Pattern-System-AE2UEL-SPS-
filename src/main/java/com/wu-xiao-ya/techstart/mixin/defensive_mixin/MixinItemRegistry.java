package com.lwx1145.techstart.mixin.defensive_mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.item.ItemStack;

@Pseudo
@Mixin(targets = "org.orecruncher.dsurround.registry.item.ItemRegistry", remap = false)
public class MixinItemRegistry {
    @Inject(method = "getItemClass", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetItemClass(ItemStack stack, CallbackInfoReturnable<Object> cir) {
        if (stack == null || stack.isEmpty()) {
            System.out.println("[DefensiveMixin] ItemRegistry.getItemClass: stack is empty, returning NONE_DATA [remove before release]");
            cir.setReturnValue(getNoneDataReflect());
            cir.cancel();
            return;
        }
        Object result = cir.getReturnValue();
        if (result == null) {
            System.out.println("[DefensiveMixin] ItemRegistry.getItemClass: result is null, forcing NONE_DATA [remove before release]");
            cir.setReturnValue(getNoneDataReflect());
            cir.cancel();
        }
    }

    /* EN: Original comment text was corrupted by encoding.
     * ZH: 原注释因编码问题已损坏。 */
    private Object getNoneDataReflect() {
        try {
            Class<?> clazz = Class.forName("org.orecruncher.dsurround.registry.item.ItemRegistry");
            java.lang.reflect.Field f = clazz.getDeclaredField("NONE_DATA");
            f.setAccessible(true);
            return f.get(null);
        } catch (Throwable t) {
            return null;
        }
    }
}
