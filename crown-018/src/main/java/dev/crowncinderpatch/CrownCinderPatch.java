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
    public static final Item ARCANE_STAFF = new ArcaneStaffItem023(new Item.Settings().maxCount(1));
    public static final Item SPELLBOOK = new SpellbookItem023(new Item.Settings().maxCount(16));

    private static final List<String> STAT_NAMES = List.of(
        "strength", "vitality", "defense", "agility", "attackspeed", "movespeed",
        "magic", "magicdefense", "critchance", "critdamage", "regeneration", "stamina"
    );

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "hero_experience_tome"), HERO_EXPERIENCE_TOME);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "royal_growth_tome"), ROYAL_GROWTH_TOME);
        Registry.register(Registries.ITEM, new Identifier("crowncinder", "arcane_staff"), ARCANE_STAFF);
        Registry.register(Registries.ITEM, new Identifier("crowncinder", "spellbook"), SPELLBOOK);

        CombatStatFix.init();
        KnightProofQuest022.init();
        MagicSystem023.init();
        LevelMilestoneGrowth023.init();
        NaturalMonsterSpawner023.init();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            server.execute(() -> {
                boolean built = OneCapitalCoordinator.ensure022(player.getServerWorld());
                RpgProgressBridge.refreshCombatStats(player);
                OneCapitalCoordinator.placePlayerAtSafeSpawn(player, built);
                StarterKit021.giveOnce(player);
                MagicSystem023.giveStaffOnce(player, ARCANE_STAFF);
                MagicSystem023.syncLearning(player);
                LevelMilestoneGrowth023.sync(player);
                if (built) {
                    player.sendMessage(Text.literal("§a[Crown & Cinder 0.23] §f마법·10레벨 성장·자연 몬스터 스폰 시스템이 적용되었습니다."), false);
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
                    LevelMilestoneGrowth023.sync(p);
                    MagicSystem023.syncLearning(p);
                    RpgProgressBridge.feedback(p, "Lv." + value + " 설정", ok);
                    return 1;
                })));

            root.then(CommandManager.literal("xp").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 1_000_000)).executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                    boolean ok = RpgProgressBridge.addXp(p, amount);
                    LevelMilestoneGrowth023.sync(p);
                    MagicSystem023.syncLearning(p);
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

            // 0.23 fix: no operator permission required. /rpg stats up <stat> <amount>
            root.then(CommandManager.literal("stats")
                .then(CommandManager.literal("up")
                    .then(CommandManager.argument("stat", StringArgumentType.word())
                        .suggests((ctx, builder) -> CommandSource.suggestMatching(STAT_NAMES, builder))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 10000)).executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            String stat = StringArgumentType.getString(ctx, "stat");
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            int now = RpgProgressBridge.addStat(p, stat, amount);
                            if (now == Integer.MIN_VALUE) {
                                ctx.getSource().sendError(Text.literal("사용 가능한 스탯: " + String.join(", ", STAT_NAMES)));
                                return 0;
                            }
                            RpgProgressBridge.refreshCombatStats(p);
                            ctx.getSource().sendFeedback(() -> Text.literal("§a" + stat + " +" + amount + " → " + now), false);
                            return 1;
                        })))));

            root.then(CommandManager.literal("magic")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    p.sendMessage(Text.literal("§d[마법] §f" + MagicSystem023.status(p)), false);
                    return 1;
                })
                .then(CommandManager.literal("book").requires(s -> s.hasPermissionLevel(2)).executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    p.giveItemStack(new ItemStack(SPELLBOOK));
                    p.giveItemStack(new ItemStack(ARCANE_STAFF));
                    p.sendMessage(Text.literal("§d마법서§f와 §b마법 스태프§f를 지급했습니다."), false);
                    return 1;
                })));

            root.then(CommandManager.literal("book").executes(ctx -> {
                ServerPlayerEntity p = ctx.getSource().getPlayer();
                p.giveItemStack(new ItemStack(HERO_EXPERIENCE_TOME));
                p.giveItemStack(new ItemStack(ROYAL_GROWTH_TOME));
                p.giveItemStack(new ItemStack(SPELLBOOK));
                p.sendMessage(Text.literal("§d영웅의 경험서§f + §6왕실 성장의 서§f + §b마법서§f 지급"), false);
                return 1;
            }));

            var city = CommandManager.literal("city").requires(s -> s.hasPermissionLevel(2));
            city.executes(ctx -> {
                int npcs = OneCapitalCoordinator.rebuildAll(ctx.getSource().getWorld());
                ctx.getSource().sendFeedback(() -> Text.literal("§a6개 왕국 재건 완료 · 주민 NPC " + npcs + "명 + 기사단 자동 배치"), true);
                return 1;
            });
            city.then(CommandManager.argument("nation", StringArgumentType.word()).executes(ctx -> {
                String id = StringArgumentType.getString(ctx, "nation");
                int npcs = OneCapitalCoordinator.rebuildOne(ctx.getSource().getWorld(), id);
                if (npcs < 0) {
                    ctx.getSource().sendError(Text.literal("국가: " + MedievalKingdoms.nationList()));
                    return 0;
                }
                ctx.getSource().sendFeedback(() -> Text.literal("§a" + id + " 왕궁/기사단 재건 완료"), true);
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
                    ctx.getSource().sendFeedback(() -> Text.literal("§a현재 위치에 " + nation.name() + " 왕궁과 기사단 생성"), true);
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
