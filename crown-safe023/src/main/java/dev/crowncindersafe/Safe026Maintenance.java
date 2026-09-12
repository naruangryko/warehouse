package dev.crowncindersafe;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;

/** Keeps the Safe025 non-resource maintenance while 0.26 owns MP/ST regeneration. */
public final class Safe026Maintenance implements ModInitializer {
    private static int ticks = 0;

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> {
            Safe026.markMagicUsed(handler.getPlayer());
        }));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ticks++;
            if (ticks % 20 == 0) {
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    try { invoke("syncMilestones", p); } catch (Throwable ignored) {}
                }
            }
            if (ticks % 40 == 0) {
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    try {
                        Method m = Safe025.class.getDeclaredMethod("showResources", ServerPlayerEntity.class, boolean.class);
                        m.setAccessible(true);
                        m.invoke(null, p, true);
                    } catch (Throwable ignored) {}
                }
            }
        });
    }

    private static void invoke(String method, ServerPlayerEntity p) throws Exception {
        Method m = Safe025.class.getDeclaredMethod(method, ServerPlayerEntity.class);
        m.setAccessible(true);
        m.invoke(null, p);
    }
}
