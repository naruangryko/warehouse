package dev.crowncinderpatch;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public final class CrownCinderPatch implements ModInitializer {
    public static final String MOD_ID = "crowncinder018";
    public static final Item HERO_EXPERIENCE_TOME = new LevelTomeItem(new Item.Settings().maxCount(16), 5, "영웅의 경험서");
    public static final Item ROYAL_GROWTH_TOME = new LevelTomeItem(new Item.Settings().maxCount(8), 20, "왕실 성장의 서");
    private static final List<String> STAT_NAMES = List.of("strength", "vitality", "defense", "agility", "attackspeed", "movespeed");

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "hero_experience_tome"), HERO_EXPERIENCE_TOME);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "royal_growth_tome"), ROYAL_GROWTH_TOME);
        CombatStatFix.init();
        KnightProofQuest022.init();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            server.execute(() -> {
                boolean built = OneCapitalCoordinator.ensure022(player.getServerWorld());
                RpgProgressBridge.refreshCombatStats(player);
                OneCapitalCoordinator.placePlayerAtSafeSpawn(player, built);
                StarterKit021.giveOnce(player);
                if (built) {
                    player.sendMessage(Text.literal("§a[Crown & Cinder 0.22] §f정예 기사 지휘관과 황제의 '기사의 증명' 퀘스트가 추가되었습니다."), false);
                }
            });
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = CommandManager.literal("rpg");

            root.then(CommandManager.literal("lv").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 100)).executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    int value = IntegerArgumentType.getInteger(ctx, "level");
                    boolean ok = RpgProgressBridge.setLevel(p, value);
                    RpgProgressBridge.feedback(p, "Lv." + value + " 설정", ok);
                    return 1;
                })));

            root.then(CommandManager.literal("xp").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 1_000_000)).executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                    boolean ok = RpgProgressBridge.addXp(p, amount);
                    RpgProgressBridge.feedback(p, "+" + amount + " XP", ok);
                    return 1;
                })));

            root.then(CommandManager.literal("pt").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 1_000_000)).executes(ctx -> {
                    int add = IntegerArgumentType.getInteger(ctx, "amount");
                    int now = RpgProgressBridge.addPoints(ctx.getSource().getPlayer(), add);
                    if (now < 0) return 0;
                    ctx.getSource().sendFeedback(() -> Text.literal("§a포인트 +" + add + " → " + now + "P"), false);
                    return 1;
                })));

            root.then(CommandManager.literal("ptmax").requires(s -> s.hasPermissionLevel(2)).executes(ctx -> {
                int now = RpgProgressBridge.maxPoints(ctx.getSource().getPlayer());
                if (now < 0) return 0;
                ctx.getSource().sendFeedback(() -> Text.literal("§a포인트 최대치 → " + now + "P"), false);
                return 1;
            }));

            // /rpg stats up <stat> <amount> with tab-completion guides.
            root.then(CommandManager.literal("stats").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("up")
                    .then(CommandManager.argument("stat", StringArgumentType.word())
                        .suggests((ctx, builder) -> CommandSource.suggestMatching(STAT_NAMES, builder))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 10000)).executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            String stat = StringArgumentType.getString(ctx, "stat");
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            int now = RpgProgressBridge.addStat(p, stat, amount);
                            if (now == Integer.MIN_VALUE) {
                                ctx.getSource().sendError(Text.literal("사용 가능한 스탯: strength, vitality, defense, agility, attackspeed, movespeed"));
                                return 0;
                            }
                            RpgProgressBridge.refreshCombatStats(p);
                            ctx.getSource().sendFeedback(() -> Text.literal("§a" + stat + " +" + amount + " → " + now), false);
                            return 1;
                        })))));

            root.then(CommandManager.literal("book").executes(ctx -> {
                ServerPlayerEntity p = ctx.getSource().getPlayer();
                p.giveItemStack(new ItemStack(HERO_EXPERIENCE_TOME));
                p.giveItemStack(new ItemStack(ROYAL_GROWTH_TOME));
                p.sendMessage(Text.literal("§d영웅의 경험서§f + §6왕실 성장의 서§f 지급"), false);
                return 1;
            }));

            var city = CommandManager.literal("city").requires(s -> s.hasPermissionLevel(2));
            city.executes(ctx -> {
                int npcs = OneCapitalCoordinator.rebuildAll(ctx.getSource().getWorld());
                ctx.getSource().sendFeedback(() -> Text.literal("§a6개 0.22 왕국 재건 완료 · 주민 NPC " + npcs + "명 + 기사 지휘관/기사단 자동 배치"), true);
                return 1;
            });
            city.then(CommandManager.argument("nation", StringArgumentType.word()).executes(ctx -> {
                String id = StringArgumentType.getString(ctx, "nation");
                int npcs = OneCapitalCoordinator.rebuildOne(ctx.getSource().getWorld(), id);
                if (npcs < 0) {
                    ctx.getSource().sendError(Text.literal("국가: " + MedievalKingdoms.nationList()));
                    return 0;
                }
                ctx.getSource().sendFeedback(() -> Text.literal("§a" + id + " 0.22 왕궁/기사단 재건 완료"), true);
                return 1;
            }));
            root.then(city);

            root.then(CommandManager.literal("here").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("nation", StringArgumentType.word()).executes(ctx -> {
                    String id = StringArgumentType.getString(ctx, "nation");
                    MedievalKingdoms.Nation nation = MedievalKingdoms.nation(id);
                    if (nation == null) {
                        ctx.getSource().sendError(Text.literal("국가: " + MedievalKingdoms.nationList()));
                        return 0;
                    }
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    var rough = p.getBlockPos();
                    MedievalKingdoms.rebuild(ctx.getSource().getWorld(), rough, nation);
                    RoyalInterior021.decorate(ctx.getSource().getWorld(), rough, nation);
                    MilitaryBootstrap021.populate(ctx.getSource().getWorld(), rough, nation);
                    ctx.getSource().sendFeedback(() -> Text.literal("§a현재 위치에 " + nation.name() + " 0.22 왕궁과 기사단 생성"), true);
                    return 1;
                })));

            root.then(CommandManager.literal("home").executes(ctx -> {
                OneCapitalCoordinator.placePlayerAtSafeSpawn(ctx.getSource().getPlayer(), true);
                return 1;
            }));

            dispatcher.register(root);
        });
    }
}
