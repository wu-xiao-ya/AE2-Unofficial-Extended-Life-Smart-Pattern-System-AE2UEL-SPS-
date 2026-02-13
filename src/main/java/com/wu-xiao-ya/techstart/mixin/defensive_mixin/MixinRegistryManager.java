package com.lwx1145.techstart.mixin.defensive_mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.orecruncher.dsurround.registry.RegistryManager", remap = false)
public class MixinRegistryManager {
    @Inject(method = "load", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onLoad(CallbackInfo ci) {
        System.out.println("[DefensiveMixin-Inject] RegistryManager.load() 被 Mixin 拦截，兼容补丁已生效！【发布前请移除此日志】");
        ci.cancel();
    }
}
