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
                if (!OneCapitalCoordinator.has019Marker(world) && world.getTime() <= 6000L) {
                    player.sendMessage(Text.literal("§6[Crown & Cinder] §f기존 수도와 같은 위치를 정리한 뒤 지표면 위에 수도 하나만 건설합니다."), false);
                    OneCapitalCoordinator.ensureFreshWorld(world);
                    player.sendMessage(Text.literal("§a[Crown & Cinder] §f지상 단일 수도·이중 성벽·NPC 배치가 완료되었습니다."), false);
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
                        int amount = IntegerArgumentType.getInteger(ctx, "amount");
                        int value = RpgProgressBridge.addPoints(ctx.getSource().getPlayer(), amount);
                        if (value < 0) return 0;
                        ctx.getSource().sendFeedback(() -> Text.literal("§a스탯 포인트 +" + amount + " → 현재 " + value + "P"), false);
                        return 1;
                    })));
            points.then(CommandManager.literal("set")
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(0, 1_000_000))
                    .executes(ctx -> {
                        int value = RpgProgressBridge.setPoints(ctx.getSource().getPlayer(), IntegerArgumentType.getInteger(ctx, "amount"));
                        if (value < 0) return 0;
                        ctx.getSource().sendFeedback(() -> Text.literal("§a스탯 포인트 = " + value + "P"), false);
                        return 1;
                    })));
            points.then(CommandManager.literal("max").executes(ctx -> {
                int value = RpgProgressBridge.maxPoints(ctx.getSource().getPlayer());
                if (value < 0) return 0;
                ctx.getSource().sendFeedback(() -> Text.literal("§a사용 가능한 포인트를 최대치 " + value + "P로 채웠습니다."), false);
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
                            int value = RpgProgressBridge.addStat(p, stat, IntegerArgumentType.getInteger(ctx, "amount"));
                            if (value == Integer.MIN_VALUE) {
                                ctx.getSource().sendError(Text.literal("알 수 없는 스탯입니다. 예: strength, vitality, defense, agility"));
                                return 0;
                            }
                            RpgProgressBridge.refreshCombatStats(p);
                            ctx.getSource().sendFeedback(() -> Text.literal("§a" + stat + " → " + value), false);
                            return 1;
                        }))));
            stats.then(CommandManager.literal("set")
                .then(CommandManager.argument("stat", StringArgumentType.word())
                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(0, 10000))
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            String stat = StringArgumentType.getString(ctx, "stat");
                            int value = RpgProgressBridge.setStat(p, stat, IntegerArgumentType.getInteger(ctx, "amount"));
                            if (value == Integer.MIN_VALUE) return 0;
                            RpgProgressBridge.refreshCombatStats(p);
                            ctx.getSource().sendFeedback(() -> Text.literal("§a" + stat + " = " + value), false);
                            return 1;
                        }))));
            root.then(stats);

            var kingdom = CommandManager.literal("kingdom").requires(s -> s.hasPermissionLevel(2));
            kingdom.then(CommandManager.literal("repairground").executes(ctx -> {
                OneCapitalCoordinator.rebuildAll(ctx.getSource().getWorld());
                ctx.getSource().sendFeedback(() -> Text.literal("§a옛 지하 수도/NPC를 정리하고 6개 수도를 지표면 위 하나씩 다시 세웠습니다."), true);
                return 1;
            }));
            kingdom.then(CommandManager.literal("rebuildall").executes(ctx -> {
                OneCapitalCoordinator.rebuildAll(ctx.getSource().getWorld());
                ctx.getSource().sendFeedback(() -> Text.literal("§a6개 수도 단일 지상 재건 완료"), true);
                return 1;
            }));
            kingdom.then(CommandManager.literal("rebuild")
                .then(CommandManager.argument("nation", StringArgumentType.word()).executes(ctx -> {
                    String id = StringArgumentType.getString(ctx, "nation");
                    if (!OneCapitalCoordinator.rebuildOne(ctx.getSource().getWorld(), id)) {
                        ctx.getSource().sendError(Text.literal("국가: " + MedievalKingdoms.nationList()));
                        return 0;
                    }
                    ctx.getSource().sendFeedback(() -> Text.literal("§a" + id + " 지상 수도 재건 완료"), true);
                    return 1;
                })));
            kingdom.then(CommandManager.literal("rebuildhere")
                .then(CommandManager.argument("nation", StringArgumentType.word()).executes(ctx -> {
                    String id = StringArgumentType.getString(ctx, "nation");
                    MedievalKingdoms.Nation nation = MedievalKingdoms.nation(id);
                    if (nation == null) {
                        ctx.getSource().sendError(Text.literal("국가: " + MedievalKingdoms.nationList()));
                        return 0;
                    }
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    MedievalKingdoms.rebuild(ctx.getSource().getWorld(), p.getBlockPos(), nation);
                    ctx.getSource().sendFeedback(() -> Text.literal("§a현재 위치를 중심으로 " + nation.name() + " 재건 완료"), true);
                    return 1;
                })));
            root.then(kingdom);

            dispatcher.register(root);
        });
    }
}
