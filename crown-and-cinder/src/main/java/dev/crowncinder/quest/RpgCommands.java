package dev.crowncinder.quest;

import dev.crowncinder.CrownCinder;
import dev.crowncinder.progress.Progress;
import dev.crowncinder.progress.ProgressService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class RpgCommands {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> {
            var root = CommandManager.literal("rpg")
                .executes(c -> status(c.getSource().getPlayerOrThrow()))
                .then(CommandManager.literal("stats").executes(c -> status(c.getSource().getPlayerOrThrow())))
                .then(CommandManager.literal("hud").executes(c -> {
                    var player = c.getSource().getPlayerOrThrow();
                    Progress p = ProgressService.get(player); p.hud = !p.hud;
                    ProgressService.store(player).markDirty();
                    player.sendMessage(Text.translatable(p.hud ? "message.crowncinder.hud_on" : "message.crowncinder.hud_off"), false);
                    return 1;
                }));
            var spend = CommandManager.literal("spend");
            for (String stat : new String[]{"strength", "vitality", "agility"}) {
                spend.then(CommandManager.literal(stat).executes(c -> {
                    var player = c.getSource().getPlayerOrThrow();
                    if (!ProgressService.get(player).allocate(stat)) {
                        player.sendMessage(Text.translatable("message.crowncinder.no_points"), false); return 0;
                    }
                    ProgressService.store(player).markDirty(); ProgressService.apply(player);
                    return status(player);
                }));
            }
            root.then(spend);
            root.then(CommandManager.literal("quest")
                .executes(c -> questStatus(c.getSource().getPlayerOrThrow()))
                .then(CommandManager.literal("accept").executes(c -> {
                    var player = c.getSource().getPlayerOrThrow();
                    boolean accepted = ProgressService.get(player).acceptQuest();
                    ProgressService.store(player).markDirty();
                    player.sendMessage(Text.translatable(accepted ? "message.crowncinder.quest_accept" : "message.crowncinder.quest_unavailable"), false);
                    return accepted ? 1 : 0;
                }))
                .then(CommandManager.literal("claim").executes(c -> {
                    var player = c.getSource().getPlayerOrThrow();
                    // Mutate quest first on server thread: repeated commands cannot duplicate rewards.
                    if (!ProgressService.get(player).claimQuest()) {
                        player.sendMessage(Text.translatable("message.crowncinder.quest_incomplete"), false); return 0;
                    }
                    ProgressService.store(player).markDirty();
                    ProgressService.award(player, CrownCinder.CONFIG.questXp);
                    if (CrownCinder.CONFIG.questEmeralds > 0) {
                        ItemStack reward = new ItemStack(Items.EMERALD, CrownCinder.CONFIG.questEmeralds);
                        if (!player.getInventory().insertStack(reward)) player.dropItem(reward, false);
                    }
                    player.sendMessage(Text.translatable("message.crowncinder.quest_reward", CrownCinder.CONFIG.questEmeralds), false);
                    return 1;
                })));
            dispatcher.register(root);
        });
    }
    private static int status(ServerPlayerEntity player) {
        Progress p = ProgressService.get(player);
        player.sendMessage(Text.translatable("message.crowncinder.stats", p.level, p.xp,
            p.requiredXp(CrownCinder.CONFIG.xpBase), p.points, p.strength, p.vitality, p.agility), false);
        player.sendMessage(Text.translatable("message.crowncinder.help"), false);
        return 1;
    }
    private static int questStatus(ServerPlayerEntity player) {
        Progress p = ProgressService.get(player);
        player.sendMessage(Text.translatable(p.questClaimed ? "message.crowncinder.quest_done" :
            p.questActive ? "message.crowncinder.quest_progress" : "message.crowncinder.quest_offer",
            p.questKills, Progress.QUEST_TARGET), false);
        return 1;
    }
}
