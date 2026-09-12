package dev.crowncinderpatch.mixin;

import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops old emperor/royal NPCs from spawning below the new 0.19 throne room. */
@Mixin(targets = "dev.crowncinder.world.RoyalCapital", remap = false)
public abstract class LegacyRoyalCapitalMixin {
    @Inject(method = "ensure", at = @At("HEAD"), cancellable = true, remap = false)
    private static void crown019$disableEnsure(ServerWorld world, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "populate", at = @At("HEAD"), cancellable = true, remap = false)
    private static void crown019$disablePopulate(ServerWorld world, CallbackInfo ci) {
        ci.cancel();
    }
}
