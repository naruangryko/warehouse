package dev.crowncinderpatch;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CrownCinderPatch implements ModInitializer {
    public static final String MOD_ID = "crowncinder018";
    public static final Item HERO_EXPERIENCE_TOME = new LevelTomeItem(new Item.Settings().maxCount(16), 5, "영웅의 경험서");
    public static final Item ROYAL_GROWTH_TOME = new LevelTomeItem(new Item.Settings().maxCount(8), 20, "왕실 성장의 서");

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "hero_experience_tome"), HERO_EXPERIENCE_TOME);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "royal_growth_tome"), ROYAL_GROWTH_TOME);
        CombatStatFix.init();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            ServerWorld world = player.getServerWorld();
            server.execute(() -> {
                RpgProgressBridge.refreshCombatStats(player);
                if (!MedievalKingdoms.hasGenerated(world) && world.getTime() <= 6000L) {
                    player.sendMessage(Text.literal("§6[Crown & Cinder] §f새 월드의 자연 지표면을 계산해 6개 왕국을 땅 위에 건설합니다."), false);
                    MedievalKingdoms.ensureFreshWorld(world);
                    player.sendMessage(Text.literal("§a[Crown & Cinder] §f이중 성벽 도시와 NPC 배치가 완료되었습니다."), false);
                }
            });
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = CommandManager.literal("rpg");

            var level = CommandManager.literal("level");
            level.then(CommandManager.literal("add")
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 100))
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        int amount = IntegerArgumentType.getInteger(ctx, "amount");
                        boolean integrated = RpgProgressBridge.addLevels(p, amount);
                        RpgProgressBridge.feedback(p, "+" + amount + " 레벨 → Lv." + RpgProgressBridge.getLevel(p), integrated);
                        return 1;
                    })));
            level.then(CommandManager.literal("set").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 100))
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        int target = IntegerArgumentType.getInteger(ctx, "level");
                        boolean integrated = RpgProgressBridge.setLevel(p, target);
                        RpgProgressBridge.feedback(p, "레벨을 " + target + "로 설정", integrated);
                        return 1;
                    })));
            level.then(CommandManager.literal("max").requires(s -> s.hasPermissionLevel(2))
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    boolean integrated = RpgProgressBridge.setLevel(p, 100);
                    RpgProgressBridge.feedback(p, "최대 레벨 Lv.100", integrated);
                    return 1;
                }));
            level.then(CommandManager.literal("item")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    p.giveItemStack(new ItemStack(HERO_EXPERIENCE_TOME));
                    p.giveItemStack(new ItemStack(ROYAL_GROWTH_TOME));
                    p.sendMessage(Text.literal("§d영웅의 경험서§f와 §6왕실 성장의 서§f를 지급했습니다."), false);
                    return 1;
                }));
            root.then(level);

            var xp = CommandManager.literal("xp");
            xp.then(CommandManager.literal("give")
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        int amount = IntegerArgumentType.getInteger(ctx, "amount");
                        boolean integrated = RpgProgressBridge.addXp(p, amount);
                        RpgProgressBridge.feedback(p, "+" + amount + " XP", integrated);
                        return 1;
                    })));
            root.then(xp);

            var points = CommandManager.literal("points").requires(s -> s.hasPermissionLevel(2));
            points.then(CommandManager.literal("add")
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        int amount = IntegerArgumentType.getInteger(ctx, "amount");
                        int value = RpgProgressBridge.addPoints(p, amount);
                        if (value < 0) {
                            ctx.getSource().sendError(Text.literal("RPG 포인트 데이터에 접근하지 못했습니다."));
                            return 0;
                        }
                        ctx.getSource().sendFeedback(() -> Text.literal("§a스탯 포인트 +" + amount + " 요청 → 현재 " + value + "P §7(현재 레벨의 허용 예산까지)"), false);
                        return 1;
                    })));
            points.then(CommandManager.literal("set")
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(0, 1_000_000))
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        int amount = IntegerArgumentType.getInteger(ctx, "amount");
                        int value = RpgProgressBridge.setPoints(p, amount);
                        if (value < 0) return 0;
                        ctx.getSource().sendFeedback(() -> Text.literal("§a스탯 포인트를 " + value + "P로 설정했습니다."), false);
                        return 1;
                    })));
            points.then(CommandManager.literal("max")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    int value = RpgProgressBridge.maxPoints(p);
                    if (value < 0) return 0;
                    ctx.getSource().sendFeedback(() -> Text.literal("§a현재 레벨에서 사용 가능한 포인트를 최대치 " + value + "P로 채웠습니다."), false);
                    return 1;
                }));
            root.then(points);

            var stats = CommandManager.literal("stats").requires(s -> s.hasPermissionLevel(2));
            stats.then(CommandManager.literal("add")
                .then(CommandManager.argument("stat", StringArgumentType.word())
                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 10000))
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            String stat = StringArgumentType.getString(ctx, "stat");
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            int value = RpgProgressBridge.addStat(p, stat, amount);
                            if (value == Integer.MIN_VALUE) {
                                ctx.getSource().sendError(Text.literal("알 수 없는 스탯입니다. 예: strength, vitality, defense, agility"));
                                return 0;
                            }
                            RpgProgressBridge.refreshCombatStats(p);
                            ctx.getSource().sendFeedback(() -> Text.literal("§a" + stat + " +" + amount + " → " + value), false);
                            return 1;
                        }))));
            stats.then(CommandManager.literal("set")
                .then(CommandManager.argument("stat", StringArgumentType.word())
                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(0, 10000))
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            String stat = StringArgumentType.getString(ctx, "stat");
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            int value = RpgProgressBridge.setStat(p, stat, amount);
                            if (value == Integer.MIN_VALUE) return 0;
                            RpgProgressBridge.refreshCombatStats(p);
                            ctx.getSource().sendFeedback(() -> Text.literal("§a" + stat + " = " + value), false);
                            return 1;
                        }))));
            root.then(stats);

            var kingdom = CommandManager.literal("kingdom").requires(s -> s.hasPermissionLevel(2));
            kingdom.then(CommandManager.literal("repairground")
                .executes(ctx -> {
                    ServerWorld w = ctx.getSource().getWorld();
                    ctx.getSource().sendFeedback(() -> Text.literal("§6지표면 재측정 후 이중 성벽 도시를 다시 건설합니다. 월드 백업을 권장합니다."), true);
                    MedievalKingdoms.rebuildAll(w);
                    ctx.getSource().sendFeedback(() -> Text.literal("§a6개 왕국 재건 완료: 외성벽·내성벽·마을·기사단·용병단·NPC 포함"), true);
                    return 1;
                }));
            kingdom.then(CommandManager.literal("rebuildall")
                .executes(ctx -> {
                    MedievalKingdoms.rebuildAll(ctx.getSource().getWorld());
                    ctx.getSource().sendFeedback(() -> Text.literal("§a중앙 제국 + 5개 왕국을 지표면 위 이중 성곽도시로 다시 세웠습니다."), true);
                    return 1;
                }));
            kingdom.then(CommandManager.literal("rebuild")
                .then(CommandManager.argument("nation", StringArgumentType.word())
                    .executes(ctx -> {
                        String id = StringArgumentType.getString(ctx, "nation");
                        MedievalKingdoms.Nation n = MedievalKingdoms.nation(id);
                        if (n == null) {
                            ctx.getSource().sendError(Text.literal("국가: " + MedievalKingdoms.nationList()));
                            return 0;
                        }
                        ServerWorld w = ctx.getSource().getWorld();
                        var spawn = w.getSpawnPos();
                        MedievalKingdoms.rebuild(w, spawn.add(n.dx(), 0, n.dz()), n);
                        ctx.getSource().sendFeedback(() -> Text.literal("§a" + n.name() + " 재건 완료"), true);
                        return 1;
                    })));
            kingdom.then(CommandManager.literal("rebuildhere")
                .then(CommandManager.argument("nation", StringArgumentType.word())
                    .executes(ctx -> {
                        String id = StringArgumentType.getString(ctx, "nation");
                        MedievalKingdoms.Nation n = MedievalKingdoms.nation(id);
                        if (n == null) {
                            ctx.getSource().sendError(Text.literal("국가: " + MedievalKingdoms.nationList()));
                            return 0;
                        }
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        MedievalKingdoms.rebuild(ctx.getSource().getWorld(), p.getBlockPos(), n);
                        ctx.getSource().sendFeedback(() -> Text.literal("§a현재 위치를 중심으로 " + n.name() + " 재건 완료"), true);
                        return 1;
                    })));
            root.then(kingdom);

            dispatcher.register(root);
        });
    }
}
