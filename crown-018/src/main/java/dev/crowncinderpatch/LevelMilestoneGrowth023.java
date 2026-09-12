package dev.crowncinderpatch;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Every 10 RPG levels grants permanent bonus STR/DEF/VIT without consuming points. */
public final class LevelMilestoneGrowth023 {
    private LevelMilestoneGrowth023() {}
    private static int ticks;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % 40 != 0) return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) sync(player);
        });
    }

    public static void sync(ServerPlayerEntity player) {
        int level = Math.max(1, RpgProgressBridge.getLevel(player));
        int milestone = Math.min(10, level / 10);
        boolean changed = false;
        for (int i = 1; i <= milestone; i++) {
            String tag = "crown023_growth_" + i;
            if (player.getCommandTags().contains(tag)) continue;
            RpgProgressBridge.addStat(player, "strength", 2);
            RpgProgressBridge.addStat(player, "defense", 2);
            RpgProgressBridge.addStat(player, "vitality", 3);
            player.addCommandTag(tag);
            changed = true;
            player.sendMessage(Text.literal("§6[10레벨 성장] §fLv." + (i * 10) + " 보너스: §c공격 +2 §9방어 +2 §a생명 +3"), false);
        }
        if (changed) RpgProgressBridge.refreshCombatStats(player);
    }
}
