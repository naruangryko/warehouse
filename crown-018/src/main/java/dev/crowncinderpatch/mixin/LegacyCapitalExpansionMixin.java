package dev.crowncinderpatch.mixin;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.crowncinder.world.CapitalExpansion", remap = false)
public abstract class LegacyCapitalExpansionMixin {
    @Inject(method = "central", at = @At("HEAD"), cancellable = true, remap = false)
    private static void crown019$disableCentral(ServerWorld world, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "remote", at = @At("HEAD"), cancellable = true, remap = false)
    private static void crown019$disableRemote(ServerWorld world, BlockPos center, String nation, CallbackInfo ci) {
        ci.cancel();
    }
}
