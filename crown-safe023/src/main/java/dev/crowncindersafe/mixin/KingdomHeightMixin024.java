package dev.crowncindersafe.mixin;

import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Raises Crown & Cinder capital build plane slightly above sampled terrain to prevent buried floors. */
@Pseudo
@Mixin(targets = "dev.crowncinderpatch.MedievalKingdoms", remap = false)
public abstract class KingdomHeightMixin024 {
    @Inject(method = "sampleBuildHeight", at = @At("RETURN"), cancellable = true, remap = false)
    private static void crownSafe024$raiseBuildPlane(ServerWorld world, int cx, int cz, CallbackInfoReturnable<Integer> cir) {
        Integer base = cir.getReturnValue();
        if (base == null) return;
        int raised = Math.min(world.getTopY() - 42, base + 3);
        cir.setReturnValue(raised);
    }
}
