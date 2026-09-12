package dev.crowncinderpatch;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

/** Keeps Strength/other RPG attack modifiers active even when custom weapons swap equipment attributes. */
public final class CombatStatFix {
    private CombatStatFix() {}

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                // Re-apply immediately before vanilla resolves the melee attack.
                RpgProgressBridge.refreshCombatStats(serverPlayer);
            }
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if ((server.getTicks() % 10) != 0) return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                // Also refresh after held-item/equipment changes so the displayed/effective attack value stays correct.
                RpgProgressBridge.refreshCombatStats(player);
            }
        });
    }
}
