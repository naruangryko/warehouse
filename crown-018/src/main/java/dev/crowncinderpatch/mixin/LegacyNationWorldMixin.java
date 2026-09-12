package dev.crowncinderpatch.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blocks every legacy world-generation entry point that can recreate the old buried capitals. */
@Mixin(targets = "dev.crowncinder.world.NationWorld", remap = false)
public abstract class LegacyNationWorldMixin {
    @Inject(method = "ensureNearby", at = @At("HEAD"), cancellable = true, remap = false)
    private static void crown019$disableEnsureNearby(ServerWorld world, CallbackInfo ci) { ci.cancel(); }

    @Inject(method = "ensureRemoteNations", at = @At("HEAD"), cancellable = true, remap = false)
    private static void crown019$disableEnsureRemote(ServerWorld world, CallbackInfo ci) { ci.cancel(); }

    @Inject(method = "buildAll", at = @At("HEAD"), cancellable = true, remap = false)
    private static void crown019$disableBuildAll(ServerPlayerEntity player, CallbackInfo ci) { ci.cancel(); }

    @Inject(method = "buildVillages", at = @At("HEAD"), cancellable = true, remap = false)
    private static void crown019$disableBuildVillages(ServerPlayerEntity player, CallbackInfo ci) { ci.cancel(); }

    @Inject(method = "buildCentralCastle", at = @At("HEAD"), cancellable = true, remap = false)
    private static void crown019$disableBuildCentral(ServerPlayerEntity player, CallbackInfo ci) { ci.cancel(); }
}
