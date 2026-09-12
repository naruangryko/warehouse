package dev.crowncinderpatch.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents the legacy 0.17 central castle from being generated underneath the 0.19 city. */
@Mixin(targets = "dev.crowncinder.world.CentralCastle", remap = false)
public abstract class LegacyCentralCastleMixin {
    @Inject(method = "ensure", at = @At("HEAD"), cancellable = true, remap = false)
    private static void crown019$disableEnsure(ServerWorld world, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "build", at = @At("HEAD"), cancellable = true, remap = false)
    private static void crown019$disableBuild(ServerWorld world, boolean force, CallbackInfo ci) {
        ci.cancel();
    }
}
