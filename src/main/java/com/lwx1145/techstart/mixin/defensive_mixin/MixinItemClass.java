package com.lwx1145.techstart.mixin.defensive_mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

@Pseudo
@Mixin(targets = "org.orecruncher.dsurround.registry.item.ItemClass", remap = false)
public class MixinItemClass {
    @Inject(method = "effectiveArmorStack", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onEffectiveArmorStack(net.minecraft.entity.Entity entity, int slot, CallbackInfoReturnable<ItemStack> cir) {
        System.out.println("[DefensiveMixin-DEBUG] MixinItemClass.effectiveArmorStack HEAD 注入已执行【发布前请移除此日志】");
        try {
            if (entity == null) {
                System.out.println("[DefensiveMixin] ItemClass.effectiveArmorStack: entity为空，返回空ItemStack【发布前请移除此日志】");
                cir.setReturnValue(ItemStack.EMPTY);
                cir.cancel();
                return;
            }
            ItemStack stack = null;
            try {
                stack = (ItemStack) entity.getClass().getMethod("getItemStack", int.class).invoke(entity, slot);
            } catch (Throwable t) {
                System.out.println("[DefensiveMixin-DEBUG] 反射获取装备异常【发布前请移除此日志】");
            }
            if (stack == null || stack.isEmpty()) {
                System.out.println("[DefensiveMixin] ItemClass.effectiveArmorStack: stack为空，返回空ItemStack【发布前请移除此日志】");
                cir.setReturnValue(ItemStack.EMPTY);
                cir.cancel();
                return;
            }
            Class<?> regMgrClazz = Class.forName("org.orecruncher.dsurround.registry.RegistryManager");
            Object itemRegistry = regMgrClazz.getField("ITEMS").get(null);
            Object itemData = itemRegistry.getClass().getMethod("getItemClass", ItemStack.class).invoke(itemRegistry, stack);
            if (itemData == null) {
                System.out.println("[DefensiveMixin] ItemClass.effectiveArmorStack: getItemClass(stack)返回null，返回空ItemStack【发布前请移除此日志】");
                cir.setReturnValue(ItemStack.EMPTY);
                cir.cancel();
                return;
            }
        } catch (Throwable t) {
            System.out.println("[DefensiveMixin] ItemClass.effectiveArmorStack: 捕获异常，返回空ItemStack【发布前请移除此日志】");
            cir.setReturnValue(ItemStack.EMPTY);
            cir.cancel();
        }
    }
}
