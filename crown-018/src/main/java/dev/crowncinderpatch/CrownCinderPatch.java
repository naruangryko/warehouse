package dev.crowncinderpatch;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("rpg")
                .then(CommandManager.literal("level")
                    .then(CommandManager.literal("add")
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 100))
                            .executes(ctx -> {
                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                boolean integrated = RpgProgressBridge.addLevels(p, amount);
                                RpgProgressBridge.feedback(p, "+" + amount + " 레벨 → Lv." + RpgProgressBridge.getLevel(p), integrated);
                                return 1;
                            })))
                    .then(CommandManager.literal("set").requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 100))
                            .executes(ctx -> {
                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                int level = IntegerArgumentType.getInteger(ctx, "level");
                                boolean integrated = RpgProgressBridge.setLevel(p, level);
                                RpgProgressBridge.feedback(p, "레벨을 " + level + "로 설정", integrated);
                                return 1;
                            })))
                    .then(CommandManager.literal("max").requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            boolean integrated = RpgProgressBridge.setLevel(p, 100);
                            RpgProgressBridge.feedback(p, "최대 레벨 Lv.100", integrated);
                            return 1;
                        }))
                    .then(CommandManager.literal("item")
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            p.giveItemStack(new ItemStack(HERO_EXPERIENCE_TOME));
                            p.giveItemStack(new ItemStack(ROYAL_GROWTH_TOME));
                            p.sendMessage(Text.literal("§d영웅의 경험서§f와 §6왕실 성장의 서§f를 지급했습니다."), false);
                            return 1;
                        })))
                .then(CommandManager.literal("xp")
                    .then(CommandManager.literal("give")
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                            .executes(ctx -> {
                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                boolean integrated = RpgProgressBridge.addXp(p, amount);
                                RpgProgressBridge.feedback(p, "+" + amount + " XP", integrated);
                                return 1;
                            }))))
                .then(CommandManager.literal("kingdom").requires(s -> s.hasPermissionLevel(2))
                    .then(CommandManager.literal("repairground")
                        .executes(ctx -> {
                            ServerWorld w = ctx.getSource().getWorld();
                            ctx.getSource().sendFeedback(() -> Text.literal("§6왕국 지면 보정 + 중세 수도 재건을 시작합니다. 기존 월드는 반드시 백업하세요."), true);
                            MedievalKingdoms.rebuildAll(w);
                            ctx.getSource().sendFeedback(() -> Text.literal("§a6개 수도의 지면 보정과 중세식 재건이 완료되었습니다."), true);
                            return 1;
                        }))
                    .then(CommandManager.literal("rebuildall")
                        .executes(ctx -> {
                            MedievalKingdoms.rebuildAll(ctx.getSource().getWorld());
                            ctx.getSource().sendFeedback(() -> Text.literal("§a중앙 제국 + 5개 왕국을 지표면 기준으로 다시 세웠습니다."), true);
                            return 1;
                        }))
                    .then(CommandManager.literal("rebuild")
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
                            }))));
        });
    }
}
