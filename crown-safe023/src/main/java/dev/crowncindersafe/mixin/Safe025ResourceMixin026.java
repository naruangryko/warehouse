package dev.crowncindersafe.mixin;

import dev.crowncindersafe.Safe025;
import dev.crowncindersafe.Safe026;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents the 0.25 one-second MP/ST regeneration from running alongside 0.26. */
@Mixin(value = Safe025.class, remap = false)
public abstract class Safe025ResourceMixin026 {
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void crowncinder026$cancelOldResourceTick(MinecraftServer server, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "consumeMana", at = @At("RETURN"), remap = false)
    private static void crowncinder026$markMagicUse(ServerPlayerEntity player, double amount, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) Safe026.markMagicUsed(player);
    }
}
