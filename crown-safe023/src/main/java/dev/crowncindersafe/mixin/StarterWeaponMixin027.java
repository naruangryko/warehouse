package dev.crowncindersafe.mixin;

import dev.crowncindersafe.Safe027;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.crowncindersafe.Safe025")
public abstract class StarterWeaponMixin027 {
    @Inject(method = "chooseJob", at = @At("RETURN"), remap = false, require = 0)
    private static void crowncinder027$starterWeapon(ServerPlayerEntity player, String job, CallbackInfo ci) {
        Safe027.onJobChosen(player, job);
    }
}
