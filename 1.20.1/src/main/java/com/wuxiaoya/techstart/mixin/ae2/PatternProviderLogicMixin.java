package com.wuxiaoya.techstart.mixin.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.GenericStack;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.util.inv.AppEngInternalInventory;
import com.wuxiaoya.techstart.integration.ae2.TechStartPatternExpansion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

@Mixin(value = PatternProviderLogic.class, remap = false)
public abstract class PatternProviderLogicMixin {
    @Shadow @Final private AppEngInternalInventory patternInventory;
    @Shadow @Final private List<IPatternDetails> patterns;
    @Shadow @Final private Set<appeng.api.stacks.AEKey> patternInputs;
    @Shadow @Final private PatternProviderLogicHost host;
    @Shadow @Final private IManagedGridNode mainNode;

    @Inject(method = "updatePatterns", at = @At("HEAD"), cancellable = true)
    private void techstart$expandCustomPatterns(CallbackInfo ci) {
        this.patterns.clear();
        this.patternInputs.clear();

        Level level = this.host.getBlockEntity().getLevel();
        for (ItemStack stack : this.patternInventory) {
            List<IPatternDetails> expanded = TechStartPatternExpansion.expand(stack, level);
            if (!expanded.isEmpty()) {
                for (IPatternDetails pattern : expanded) {
                    addPattern(pattern);
                }
                continue;
            }

            IPatternDetails decoded = PatternDetailsHelper.decodePattern(stack, level);
            if (decoded != null) {
                addPattern(decoded);
            }
        }

        ICraftingProvider.requestUpdate(this.mainNode);
        ci.cancel();
    }

    private void addPattern(IPatternDetails pattern) {
        this.patterns.add(pattern);
        for (var input : pattern.getInputs()) {
            for (GenericStack possibleInput : input.getPossibleInputs()) {
                this.patternInputs.add(possibleInput.what().dropSecondary());
            }
        }
    }
}
