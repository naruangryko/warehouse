package dev.crowncinderpatch.mixin;

import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.crowncinder.world.CapitalVillage", remap = false)
public abstract class LegacyCapitalVillageMixin {
    @Inject(method = "ensure", at = @At("HEAD"), cancellable = true, remap = false)
    private static void crown019$disableLegacyVillage(ServerWorld world, CallbackInfo ci) {
        ci.cancel();
    }
}
