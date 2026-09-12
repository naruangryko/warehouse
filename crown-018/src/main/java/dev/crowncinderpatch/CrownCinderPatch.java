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
            server.execute(() -> {
                boolean built = OneCapitalCoordinator.ensure020(player.getServerWorld());
                RpgProgressBridge.refreshCombatStats(player);
                OneCapitalCoordinator.placePlayerAtSafeSpawn(player, built);
                if (built) {
                    player.sendMessage(Text.literal("§a[Crown & Cinder 0.20] §f왕국을 지표면 위에 다시 만들고 왕·기사단·용병단·주민 NPC를 배치했습니다."), false);
                }
            });
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = CommandManager.literal("rpg");

            // /rpg lv 50 : exact level
            root.then(CommandManager.literal("lv").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 100)).executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    int value = IntegerArgumentType.getInteger(ctx, "level");
                    boolean ok = RpgProgressBridge.setLevel(p, value);
                    RpgProgressBridge.feedback(p, "Lv." + value + " 설정", ok);
                    return 1;
                })));

            // /rpg xp 5000
            root.then(CommandManager.literal("xp").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 1_000_000)).executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                    boolean ok = RpgProgressBridge.addXp(p, amount);
                    RpgProgressBridge.feedback(p, "+" + amount + " XP", ok);
                    return 1;
                })));

            // /rpg pt 100 : add points
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

            // /rpg str 50 : add strength quickly
            root.then(CommandManager.literal("str").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 10000)).executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    int now = RpgProgressBridge.addStat(p, "strength", IntegerArgumentType.getInteger(ctx, "amount"));
                    RpgProgressBridge.refreshCombatStats(p);
                    ctx.getSource().sendFeedback(() -> Text.literal("§c힘(STR) → " + now), false);
                    return 1;
                })));

            // /rpg stat strength 100 : exact stat value
            root.then(CommandManager.literal("stat").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("stat", StringArgumentType.word())
                    .then(CommandManager.argument("value", IntegerArgumentType.integer(0, 10000)).executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        String stat = StringArgumentType.getString(ctx, "stat");
                        int now = RpgProgressBridge.setStat(p, stat, IntegerArgumentType.getInteger(ctx, "value"));
                        if (now == Integer.MIN_VALUE) {
                            ctx.getSource().sendError(Text.literal("스탯 예: strength, vitality, defense, agility, attackspeed, movespeed"));
                            return 0;
                        }
                        RpgProgressBridge.refreshCombatStats(p);
                        ctx.getSource().sendFeedback(() -> Text.literal("§a" + stat + " = " + now), false);
                        return 1;
                    }))));

            root.then(CommandManager.literal("book").executes(ctx -> {
                ServerPlayerEntity p = ctx.getSource().getPlayer();
                p.giveItemStack(new ItemStack(HERO_EXPERIENCE_TOME));
                p.giveItemStack(new ItemStack(ROYAL_GROWTH_TOME));
                p.sendMessage(Text.literal("§d영웅의 경험서§f + §6왕실 성장의 서§f 지급"), false);
                return 1;
            }));

            // /rpg city = all, /rpg city aurelia = one
            var city = CommandManager.literal("city").requires(s -> s.hasPermissionLevel(2));
            city.executes(ctx -> {
                int npcs = OneCapitalCoordinator.rebuildAll(ctx.getSource().getWorld());
                ctx.getSource().sendFeedback(() -> Text.literal("§a6개 지상 왕국 재건 완료 · NPC " + npcs + "명 배치"), true);
                return 1;
            });
            city.then(CommandManager.argument("nation", StringArgumentType.word()).executes(ctx -> {
                String id = StringArgumentType.getString(ctx, "nation");
                int npcs = OneCapitalCoordinator.rebuildOne(ctx.getSource().getWorld(), id);
                if (npcs < 0) {
                    ctx.getSource().sendError(Text.literal("국가: " + MedievalKingdoms.nationList()));
                    return 0;
                }
                ctx.getSource().sendFeedback(() -> Text.literal("§a" + id + " 재건 완료 · NPC " + npcs + "명 배치"), true);
                return 1;
            }));
            root.then(city);

            // /rpg here aurelia
            root.then(CommandManager.literal("here").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("nation", StringArgumentType.word()).executes(ctx -> {
                    String id = StringArgumentType.getString(ctx, "nation");
                    MedievalKingdoms.Nation nation = MedievalKingdoms.nation(id);
                    if (nation == null) {
                        ctx.getSource().sendError(Text.literal("국가: " + MedievalKingdoms.nationList()));
                        return 0;
                    }
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    MedievalKingdoms.rebuild(ctx.getSource().getWorld(), p.getBlockPos(), nation);
                    int npcs = NpcBootstrap020.respawnOne(ctx.getSource().getWorld(), id);
                    ctx.getSource().sendFeedback(() -> Text.literal("§a현재 위치에 " + nation.name() + " 생성 · NPC " + npcs + "명"), true);
                    return 1;
                })));

            // /rpg npc : repair/populate all capital NPCs without rebuilding blocks
            root.then(CommandManager.literal("npc").requires(s -> s.hasPermissionLevel(2)).executes(ctx -> {
                int npcs = NpcBootstrap020.respawnAll(ctx.getSource().getWorld());
                ctx.getSource().sendFeedback(() -> Text.literal("§a왕·기사·용병·주민 NPC 재배치 완료: " + npcs + "명"), true);
                return 1;
            }));

            // /rpg home : safe central plaza
            root.then(CommandManager.literal("home").executes(ctx -> {
                OneCapitalCoordinator.placePlayerAtSafeSpawn(ctx.getSource().getPlayer(), true);
                return 1;
            }));

            dispatcher.register(root);
        });
    }
}
