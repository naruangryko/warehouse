package dev.crowncindersafe;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Crash-safe 0.23 feature layer. Uses a unique package/mod id so it can coexist with the 0.22 unified JAR.
 * All integration with the existing RPG progress system is guarded by reflection and failure fallbacks.
 */
public final class Safe023 implements ModInitializer {
    public static final String MOD_ID = "crowncinder023safe";
    public static final Item ARCANE_STAFF = new ArcaneStaff(new Item.Settings().maxCount(1));
    public static final Item SPELLBOOK = new SpellBook(new Item.Settings().maxCount(16));

    private static final List<String> STAT_NAMES = List.of(
        "strength", "vitality", "defense", "agility", "attackspeed", "movespeed",
        "magic", "magicdefense", "critchance", "critdamage", "regeneration", "stamina"
    );
    private static int serverTicks = 0;
    private static int worldTicks = 0;

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "arcane_staff"), ARCANE_STAFF);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "spellbook"), SPELLBOOK);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> {
            ServerPlayerEntity p = handler.getPlayer();
            try {
                if (!p.getScoreboardTags().contains("ccsafe_staff_granted")) {
                    p.addScoreboardTag("ccsafe_staff_granted");
                    p.giveItemStack(new ItemStack(ARCANE_STAFF));
                    p.sendMessage(Text.literal("§b[Crown & Cinder] §f마법 스태프를 지급했습니다. Lv.10 또는 마법서 사용 후 우클릭해 보세요."), false);
                }
                syncMilestones(p);
            } catch (Throwable t) {
                // Never fail login because an optional feature failed.
            }
        }));

        ServerTickEvents.END_SERVER_TICK.register(this::serverTick);
        ServerTickEvents.END_WORLD_TICK.register(this::worldTick);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var rpg = CommandManager.literal("rpg");

            // No OP permission requirement: /rpg stats up <stat> <amount>
            rpg.then(CommandManager.literal("stats")
                .then(CommandManager.literal("up")
                    .then(CommandManager.argument("stat", StringArgumentType.word())
                        .suggests((ctx, builder) -> CommandSource.suggestMatching(STAT_NAMES, builder))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 10000))
                            .executes(ctx -> {
                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                String stat = StringArgumentType.getString(ctx, "stat");
                                int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                int value = addStat(p, stat, amount);
                                if (value == Integer.MIN_VALUE) {
                                    ctx.getSource().sendError(Text.literal("사용 가능한 스탯: " + String.join(", ", STAT_NAMES)));
                                    return 0;
                                }
                                refresh(p);
                                ctx.getSource().sendFeedback(() -> Text.literal("§a" + stat + " +" + amount + " → " + value), false);
                                return 1;
                            })))));

            rpg.then(CommandManager.literal("magic")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    int level = getLevel(p);
                    int tier = magicTier(p);
                    p.sendMessage(Text.literal("§d[마법] §fLv." + level + " · " + spellName(tier) + " · 마법 단계 " + tier), false);
                    return 1;
                })
                .then(CommandManager.literal("book").requires(s -> s.hasPermissionLevel(2)).executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    p.giveItemStack(new ItemStack(SPELLBOOK));
                    p.giveItemStack(new ItemStack(ARCANE_STAFF));
                    p.sendMessage(Text.literal("§d마법서§f와 §b마법 스태프§f를 지급했습니다."), false);
                    return 1;
                })));

            dispatcher.register(rpg);
        });
    }

    private void serverTick(MinecraftServer server) {
        if (++serverTicks % 40 != 0) return;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            try { syncMilestones(p); } catch (Throwable ignored) {}
        }
    }

    /** Every 10 RPG levels: permanent strength +2, defense +2, vitality +3, exactly once per milestone. */
    private static void syncMilestones(ServerPlayerEntity p) {
        int level = Math.max(1, getLevel(p));
        int max = Math.min(100, (level / 10) * 10);
        for (int milestone = 10; milestone <= max; milestone += 10) {
            String tag = "ccsafe_growth_" + milestone;
            if (p.getScoreboardTags().contains(tag)) continue;
            // Mark first so a failed/partial follow-up never loops every tick and crashes a world.
            p.addScoreboardTag(tag);
            int s = addStat(p, "strength", 2);
            int d = addStat(p, "defense", 2);
            int v = addStat(p, "vitality", 3);
            refresh(p);
            if (s != Integer.MIN_VALUE && d != Integer.MIN_VALUE && v != Integer.MIN_VALUE) {
                p.sendMessage(Text.literal("§6[10레벨 성장] §fLv." + milestone + " 보너스: 힘 +2 · 방어 +2 · 생명력 +3"), false);
            }
            p.sendMessage(Text.literal("§d[마법 습득] §f" + spellName(milestone / 10) + " 사용 가능"), false);
        }
    }

    private void worldTick(ServerWorld world) {
        if (!world.getRegistryKey().equals(World.OVERWORLD)) return;
        if (++worldTicks % 240 != 0) return;
        if (world.getDifficulty() == Difficulty.PEACEFUL) return;
        long time = world.getTimeOfDay() % 24000L;
        boolean night = time >= 13000L && time <= 23000L;
        for (ServerPlayerEntity p : world.getPlayers()) {
            try { tryNaturalSpawn(world, p, night); } catch (Throwable ignored) {}
        }
    }

    private static void tryNaturalSpawn(ServerWorld world, ServerPlayerEntity player, boolean night) {
        // Protect populated towns/capitals without depending on private Crown & Cinder world classes.
        if (!world.getEntitiesByClass(VillagerEntity.class, player.getBoundingBox().expand(96.0), v -> v.isAlive()).isEmpty()) return;
        int hostile = world.getEntitiesByClass(HostileEntity.class, player.getBoundingBox().expand(48.0), e -> e.isAlive()).size();
        if (hostile >= 18) return;
        if (world.random.nextInt(3) != 0) return;

        int dx = 20 + world.random.nextInt(21);
        int dz = 20 + world.random.nextInt(21);
        if (world.random.nextBoolean()) dx = -dx;
        if (world.random.nextBoolean()) dz = -dz;
        int x = player.getBlockX() + dx;
        int z = player.getBlockZ() + dz;
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);

        String mobName = regionMonster(world, pos);
        boolean special = mobName != null;
        if (!night && !special) return;
        if (mobName == null) {
            String[] pool = {"goblin", "orc", "skeleton_knight", "werewolf"};
            mobName = pool[world.random.nextInt(pool.length)];
        }
        if (!special && world.getLightLevel(pos) > 7) return;
        if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) return;
        if (world.getBlockState(pos.down()).isAir()) return;

        Identifier id = new Identifier("crowncinder", mobName);
        // Critical safety check: never create fallback/pig/unknown entity ids.
        if (!Registries.ENTITY_TYPE.containsId(id)) return;
        EntityType<?> type = Registries.ENTITY_TYPE.get(id);
        if (!id.equals(Registries.ENTITY_TYPE.getId(type))) return;
        Entity created = type.create(world);
        if (!(created instanceof MobEntity mob)) return;
        mob.refreshPositionAndAngles(x + 0.5, y, z + 0.5, world.random.nextFloat() * 360.0f, 0.0f);
        if (!world.isSpaceEmpty(mob)) return;
        world.spawnEntity(mob);
    }

    private static String regionMonster(ServerWorld world, BlockPos pos) {
        var biome = world.getBiome(pos);
        if (biome.matchesKey(BiomeKeys.DESERT) || biome.matchesKey(BiomeKeys.BADLANDS)) return "fire_elemental";
        if (biome.matchesKey(BiomeKeys.DARK_FOREST)) return world.random.nextBoolean() ? "dark_knight" : "skeleton_knight";
        if (biome.matchesKey(BiomeKeys.TAIGA) || biome.matchesKey(BiomeKeys.OLD_GROWTH_PINE_TAIGA)) return world.random.nextBoolean() ? "troll" : "werewolf";
        if (biome.matchesKey(BiomeKeys.SAVANNA) || biome.matchesKey(BiomeKeys.WINDSWEPT_HILLS)) return world.random.nextBoolean() ? "ogre" : "minotaur";
        if (biome.matchesKey(BiomeKeys.FOREST)) return world.random.nextBoolean() ? "goblin" : "werewolf";
        if (biome.matchesKey(BiomeKeys.PLAINS)) return world.random.nextBoolean() ? "goblin" : "orc";
        return null;
    }

    private static int magicTier(ServerPlayerEntity p) {
        int fromLevel = Math.min(10, Math.max(0, getLevel(p) / 10));
        int fromBook = p.getScoreboardTags().contains("ccsafe_magic_book") ? 1 : 0;
        return Math.max(fromLevel, fromBook);
    }

    private static String spellName(int tier) {
        return switch (Math.max(0, tier)) {
            case 0 -> "미습득";
            case 1 -> "마력탄";
            case 2 -> "화염탄";
            case 3 -> "서리탄";
            case 4 -> "관통 마력탄";
            case 5 -> "약화의 탄환";
            case 6 -> "대화염탄";
            case 7 -> "빙결 마법";
            case 8 -> "성광탄";
            case 9 -> "심연의 탄환";
            default -> "대마도사의 일격";
        };
    }

    private static ParticleEffect particle(int tier) {
        if (tier >= 8) return ParticleTypes.END_ROD;
        if (tier == 2 || tier == 6 || tier == 10) return ParticleTypes.FLAME;
        if (tier == 3 || tier == 7) return ParticleTypes.SNOWFLAKE;
        if (tier >= 5) return ParticleTypes.PORTAL;
        return ParticleTypes.ENCHANT;
    }

    private static final class ArcaneStaff extends Item {
        ArcaneStaff(Settings settings) { super(settings); }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (world.isClient || !(user instanceof ServerPlayerEntity p)) return TypedActionResult.success(stack, world.isClient());
            try {
                int tier = magicTier(p);
                if (tier <= 0) {
                    p.sendMessage(Text.literal("§d[마법] §fLv.10을 달성하거나 마법서를 먼저 사용하세요."), false);
                    return TypedActionResult.fail(stack);
                }
                ServerWorld sw = p.getServerWorld();
                double damage = 5.0 + tier * 2.0 + Math.max(0, getStat(p, "magic")) * 0.30;
                double range = Math.min(36.0, 20.0 + tier * 1.5);
                Vec3d start = p.getEyePos();
                Vec3d dir = p.getRotationVec(1.0f).normalize();
                LivingEntity hit = null;
                ParticleEffect fx = particle(tier);
                for (double d = 0.8; d <= range && hit == null; d += 0.65) {
                    Vec3d at = start.add(dir.multiply(d));
                    sw.spawnParticles(fx, at.x, at.y, at.z, 2, 0.03, 0.03, 0.03, 0.002);
                    Box box = new Box(at.x - 0.7, at.y - 0.7, at.z - 0.7, at.x + 0.7, at.y + 0.7, at.z + 0.7);
                    List<HostileEntity> enemies = sw.getEntitiesByClass(HostileEntity.class, box, e -> e.isAlive() && p.canSee(e));
                    if (!enemies.isEmpty()) hit = enemies.get(0);
                }
                if (hit != null) {
                    hit.damage(p.getDamageSources().playerAttack(p), (float) damage);
                    sw.spawnParticles(fx, hit.getX(), hit.getBodyY(0.5), hit.getZ(), 18, 0.4, 0.5, 0.4, 0.04);
                }
                p.getItemCooldownManager().set(this, Math.max(12, 28 - tier));
                sw.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.7f, 1.0f + tier * 0.03f);
                p.sendMessage(Text.literal("§b" + spellName(tier) + " §7(" + String.format("%.1f", damage) + " 피해)"), true);
                return TypedActionResult.success(stack, false);
            } catch (Throwable t) {
                p.sendMessage(Text.literal("§c마법 사용 중 오류를 안전하게 차단했습니다."), false);
                return TypedActionResult.fail(stack);
            }
        }
    }

    private static final class SpellBook extends Item {
        SpellBook(Settings settings) { super(settings); }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (!world.isClient && user instanceof ServerPlayerEntity p) {
                try {
                    p.addScoreboardTag("ccsafe_magic_book");
                    p.sendMessage(Text.literal("§d[마법서] §f기본 마법 '마력탄'을 배웠습니다."), false);
                    if (!p.getAbilities().creativeMode) stack.decrement(1);
                } catch (Throwable ignored) {}
            }
            return TypedActionResult.success(stack, world.isClient());
        }
    }

    // ---- Reflection bridge to the existing 0.22 RPG progress system ----
    private static int getLevel(ServerPlayerEntity p) {
        try {
            Object progress = progress(p);
            return field(progress, "level").getInt(progress);
        } catch (Throwable ignored) {
            return Math.max(1, p.experienceLevel);
        }
    }

    private static int getStat(ServerPlayerEntity p, String stat) {
        try {
            Object progress = progress(p);
            String f = statField(stat);
            if (f == null) return 0;
            return field(progress, f).getInt(progress);
        } catch (Throwable ignored) { return 0; }
    }

    private static int addStat(ServerPlayerEntity p, String stat, int amount) {
        try {
            Object progress = progress(p);
            String fName = statField(stat);
            if (fName == null) return Integer.MIN_VALUE;
            Field f = field(progress, fName);
            int value = Math.max(0, Math.min(10000, f.getInt(progress) + amount));
            f.setInt(progress, value);
            markDirtyAndApply(p);
            return value;
        } catch (Throwable ignored) { return Integer.MIN_VALUE; }
    }

    private static String statField(String stat) {
        return switch (stat.toLowerCase()) {
            case "strength", "str", "힘" -> "strength";
            case "vitality", "vit", "체력", "생명" -> "vitality";
            case "defense", "def", "방어" -> "defense";
            case "agility", "agi", "민첩" -> "agility";
            case "attackspeed", "attack_speed", "공격속도" -> "attackSpeed";
            case "movespeed", "move_speed", "이동속도" -> "moveSpeed";
            case "magic", "magicpower", "마력" -> "magicPower";
            case "magicdefense", "마법방어" -> "magicDefense";
            case "critchance" -> "critChance";
            case "critdamage" -> "critDamage";
            case "regen", "regeneration" -> "regeneration";
            case "stamina" -> "stamina";
            default -> null;
        };
    }

    private static Object progress(ServerPlayerEntity p) throws Exception {
        Class<?> service = Class.forName("dev.crowncinder.progress.ProgressService");
        return service.getMethod("get", ServerPlayerEntity.class).invoke(null, p);
    }

    private static Field field(Object target, String name) throws Exception {
        Field f = target.getClass().getField(name);
        f.setAccessible(true);
        return f;
    }

    private static void refresh(ServerPlayerEntity p) {
        try {
            Class<?> service = Class.forName("dev.crowncinder.progress.ProgressService");
            Method apply = service.getMethod("apply", ServerPlayerEntity.class);
            apply.invoke(null, p);
        } catch (Throwable ignored) {}
    }

    private static void markDirtyAndApply(ServerPlayerEntity p) throws Exception {
        Class<?> service = Class.forName("dev.crowncinder.progress.ProgressService");
        Object store = service.getMethod("store", ServerPlayerEntity.class).invoke(null, p);
        store.getClass().getMethod("markDirty").invoke(store);
        refresh(p);
    }
}
