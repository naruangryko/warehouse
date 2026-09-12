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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
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

/** Crash-safe feature layer for Crown & Cinder 0.22. */
public final class Safe023 implements ModInitializer {
    public static final String MOD_ID = "crowncinder023safe";
    public static final Item ARCANE_STAFF = new ArcaneStaff(new Item.Settings().maxCount(1));
    public static final Item SPELLBOOK = new SpellBook(new Item.Settings().maxCount(16));

    private static final List<String> STAT_NAMES = List.of(
        "strength", "vitality", "defense", "agility", "attackspeed", "movespeed",
        "magic", "magicdefense", "critchance", "critdamage", "regeneration", "stamina"
    );
    private static final String SELECTED_SPELL_KEY = "CrownSelectedSpell";
    private static int serverTicks = 0;
    private static int worldTicks = 0;

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "arcane_staff"), ARCANE_STAFF);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "spellbook"), SPELLBOOK);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> {
            ServerPlayerEntity p = handler.getPlayer();
            try {
                if (!p.getCommandTags().contains("ccsafe_staff_granted")) {
                    p.addCommandTag("ccsafe_staff_granted");
                    ItemStack staff = new ItemStack(ARCANE_STAFF);
                    setSelectedSpell(staff, 1);
                    p.giveItemStack(staff);
                    p.sendMessage(Text.literal("§b[Crown & Cinder] §f마법 스태프 지급! §e쉬프트+우클릭§f으로 마법 선택, §b우클릭§f으로 발동합니다."), false);
                }
                syncMilestones(p);
            } catch (Throwable ignored) {}
        }));

        ServerTickEvents.END_SERVER_TICK.register(this::serverTick);
        ServerTickEvents.END_WORLD_TICK.register(this::worldTick);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var rpg = CommandManager.literal("rpg");

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
                    int unlocked = magicTier(p);
                    p.sendMessage(Text.literal("§d[마법] §fLv." + getLevel(p) + " · 사용 가능 " + unlocked + "/10"), false);
                    if (unlocked > 0) {
                        StringBuilder b = new StringBuilder("§7");
                        for (int i = 1; i <= unlocked; i++) {
                            if (i > 1) b.append(" §8/ §7");
                            b.append(i).append(".").append(spellName(i));
                        }
                        p.sendMessage(Text.literal(b.toString()), false);
                    }
                    return 1;
                })
                .then(CommandManager.literal("book").requires(s -> s.hasPermissionLevel(2)).executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    p.giveItemStack(new ItemStack(SPELLBOOK));
                    ItemStack staff = new ItemStack(ARCANE_STAFF);
                    setSelectedSpell(staff, 1);
                    p.giveItemStack(staff);
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

    private static void syncMilestones(ServerPlayerEntity p) {
        int level = Math.max(1, getLevel(p));
        int max = Math.min(100, (level / 10) * 10);
        for (int milestone = 10; milestone <= max; milestone += 10) {
            String tag = "ccsafe_growth_" + milestone;
            if (p.getCommandTags().contains(tag)) continue;
            p.addCommandTag(tag);
            int s = addStat(p, "strength", 2);
            int d = addStat(p, "defense", 2);
            int v = addStat(p, "vitality", 3);
            refresh(p);
            if (s != Integer.MIN_VALUE && d != Integer.MIN_VALUE && v != Integer.MIN_VALUE) {
                p.sendMessage(Text.literal("§6[10레벨 성장] §fLv." + milestone + " 보너스: 힘 +2 · 방어 +2 · 생명력 +3"), false);
            }
            p.sendMessage(Text.literal("§d[마법 습득] §f" + spellName(milestone / 10) + " 해금"), false);
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
        if (!world.getEntitiesByClass(VillagerEntity.class, player.getBoundingBox().expand(96.0), v -> v.isAlive()).isEmpty()) return;
        int hostile = world.getEntitiesByClass(HostileEntity.class, player.getBoundingBox().expand(48.0), e -> e.isAlive()).size();
        if (hostile >= 18 || world.random.nextInt(3) != 0) return;

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
        if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir() || world.getBlockState(pos.down()).isAir()) return;

        Identifier id = new Identifier("crowncinder", mobName);
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
        int fromBook = p.getCommandTags().contains("ccsafe_magic_book") ? 1 : 0;
        return Math.max(fromLevel, fromBook);
    }

    private static String spellName(int spell) {
        return switch (spell) {
            case 1 -> "마력탄";
            case 2 -> "화염창";
            case 3 -> "빙결창";
            case 4 -> "마력 폭발";
            case 5 -> "천둥의 창";
            case 6 -> "대화염 폭풍";
            case 7 -> "빙설 폭풍";
            case 8 -> "성광의 파동";
            case 9 -> "심연의 구체";
            case 10 -> "대마도사의 대재앙";
            default -> "미습득";
        };
    }

    private static double spellBaseDamage(int spell) {
        return switch (spell) {
            case 1 -> 8.0;
            case 2 -> 13.0;
            case 3 -> 18.0;
            case 4 -> 24.0;
            case 5 -> 31.0;
            case 6 -> 39.0;
            case 7 -> 48.0;
            case 8 -> 58.0;
            case 9 -> 70.0;
            case 10 -> 88.0;
            default -> 5.0;
        };
    }

    private static double spellMagicScale(int spell) { return 0.30 + spell * 0.07; }
    private static double spellRange(int spell) { return Math.min(44.0, 20.0 + spell * 2.4); }
    private static double spellRadius(int spell) {
        return switch (spell) {
            case 4 -> 2.5;
            case 5 -> 2.8;
            case 6 -> 3.8;
            case 7 -> 4.2;
            case 8 -> 4.8;
            case 9 -> 5.4;
            case 10 -> 6.5;
            default -> 0.8;
        };
    }

    private static ParticleEffect spellParticle(int spell) {
        return switch (spell) {
            case 2, 6 -> ParticleTypes.SOUL_FIRE_FLAME;
            case 3, 7 -> ParticleTypes.SNOWFLAKE;
            case 5 -> ParticleTypes.ELECTRIC_SPARK;
            case 8 -> ParticleTypes.END_ROD;
            case 9 -> ParticleTypes.DRAGON_BREATH;
            case 10 -> ParticleTypes.REVERSE_PORTAL;
            default -> ParticleTypes.ENCHANT;
        };
    }

    private static int selectedSpell(ItemStack stack, int unlocked) {
        int max = Math.max(1, unlocked);
        NbtCompound nbt = stack.getNbt();
        int selected = nbt != null && nbt.contains(SELECTED_SPELL_KEY) ? nbt.getInt(SELECTED_SPELL_KEY) : 1;
        if (selected < 1 || selected > max) selected = 1;
        return selected;
    }

    private static void setSelectedSpell(ItemStack stack, int spell) {
        stack.getOrCreateNbt().putInt(SELECTED_SPELL_KEY, Math.max(1, Math.min(10, spell)));
    }

    private static final class ArcaneStaff extends Item {
        ArcaneStaff(Settings settings) { super(settings); }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (world.isClient || !(user instanceof ServerPlayerEntity p)) return TypedActionResult.success(stack, world.isClient());
            try {
                int unlocked = magicTier(p);
                if (unlocked <= 0) {
                    p.sendMessage(Text.literal("§d[마법] §fLv.10을 달성하거나 마법서를 먼저 사용하세요."), false);
                    return TypedActionResult.fail(stack);
                }

                int selected = selectedSpell(stack, unlocked);
                if (p.isSneaking()) {
                    int next = selected >= unlocked ? 1 : selected + 1;
                    setSelectedSpell(stack, next);
                    p.sendMessage(Text.literal("§d[마법 선택] §f" + next + ". §b" + spellName(next)), true);
                    p.getServerWorld().playSound(null, p.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.8f, 1.0f + next * 0.04f);
                    p.getItemCooldownManager().set(this, 6);
                    return TypedActionResult.success(stack, false);
                }

                castSpell(p, stack, selected);
                return TypedActionResult.success(stack, false);
            } catch (Throwable t) {
                p.sendMessage(Text.literal("§c마법 사용 중 오류를 안전하게 차단했습니다."), false);
                return TypedActionResult.fail(stack);
            }
        }
    }

    private static void castSpell(ServerPlayerEntity p, ItemStack staff, int spell) {
        ServerWorld sw = p.getServerWorld();
        int magicPower = Math.max(0, getStat(p, "magic"));
        double damage = spellBaseDamage(spell) + magicPower * spellMagicScale(spell);
        double range = spellRange(spell);
        double radius = spellRadius(spell);
        ParticleEffect fx = spellParticle(spell);

        Vec3d start = p.getEyePos();
        Vec3d dir = p.getRotationVec(1.0f).normalize();
        LivingEntity hit = null;
        Vec3d impact = start.add(dir.multiply(range));
        int trailCount = Math.min(9, 2 + spell / 2);

        for (double d = 0.8; d <= range && hit == null; d += 0.55) {
            Vec3d at = start.add(dir.multiply(d));
            impact = at;
            sw.spawnParticles(fx, at.x, at.y, at.z, trailCount, 0.06 + spell * 0.01, 0.06 + spell * 0.01, 0.06 + spell * 0.01, 0.01);
            if (spell >= 6 && ((int)(d * 10)) % 11 == 0) {
                sw.spawnParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 3, 0.12, 0.12, 0.12, 0.01);
            }
            Box box = new Box(at.x - 0.8, at.y - 0.8, at.z - 0.8, at.x + 0.8, at.y + 0.8, at.z + 0.8);
            List<HostileEntity> enemies = sw.getEntitiesByClass(HostileEntity.class, box, e -> e.isAlive() && p.canSee(e));
            if (!enemies.isEmpty()) hit = enemies.get(0);
        }

        if (hit != null) {
            impact = hit.getPos().add(0, hit.getHeight() * 0.5, 0);
            if (spell <= 3) {
                damageAndDebuff(p, hit, (float)damage, spell);
            } else {
                Box area = new Box(impact.x - radius, impact.y - radius, impact.z - radius, impact.x + radius, impact.y + radius, impact.z + radius);
                List<HostileEntity> targets = sw.getEntitiesByClass(HostileEntity.class, area, e -> e.isAlive());
                for (HostileEntity target : targets) {
                    double distance = Math.max(0.0, target.getPos().distanceTo(impact));
                    float scaled = (float)Math.max(damage * 0.55, damage * (1.0 - distance / (radius * 2.0)));
                    damageAndDebuff(p, target, scaled, spell);
                }
            }
        }

        int impactCount = 28 + spell * 14;
        double spread = 0.45 + spell * 0.10;
        sw.spawnParticles(fx, impact.x, impact.y, impact.z, impactCount, spread, spread, spread, 0.05 + spell * 0.005);
        if (spell >= 4) sw.spawnParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, Math.min(10, spell), spread * 0.5, spread * 0.5, spread * 0.5, 0.02);
        if (spell >= 7) sw.spawnParticles(ParticleTypes.FLASH, impact.x, impact.y, impact.z, 2, 0.2, 0.2, 0.2, 0.0);
        if (spell == 10) {
            sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, impact.y, impact.z, 2, 0.4, 0.4, 0.4, 0.0);
            sw.spawnParticles(ParticleTypes.DRAGON_BREATH, impact.x, impact.y, impact.z, 160, 2.8, 2.8, 2.8, 0.09);
        }

        sw.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.9f + spell * 0.04f, 0.95f + spell * 0.025f);
        if (spell >= 5) sw.playSound(null, BlockPos.ofFloored(impact), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.7f + spell * 0.05f, 1.2f - spell * 0.025f);

        int cooldown = 18 + spell * 2;
        p.getItemCooldownManager().set(ARCANE_STAFF, cooldown);
        p.sendMessage(Text.literal("§b" + spellName(spell) + " §7| 위력 " + String.format("%.1f", damage) + " | 범위 " + String.format("%.1f", radius)), true);
    }

    private static void damageAndDebuff(ServerPlayerEntity p, HostileEntity target, float damage, int spell) {
        target.damage(p.getDamageSources().playerAttack(p), damage);
        if (spell == 2 || spell == 6) target.setOnFireFor(spell == 6 ? 8 : 4);
        if (spell == 3 || spell == 7) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, spell == 7 ? 140 : 80, spell == 7 ? 2 : 1));
        if (spell == 5) target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1));
        if (spell == 9) target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 80, 1));
        if (spell == 10) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 2));
            target.setOnFireFor(6);
        }
    }

    private static final class SpellBook extends Item {
        SpellBook(Settings settings) { super(settings); }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (!world.isClient && user instanceof ServerPlayerEntity p) {
                try {
                    p.addCommandTag("ccsafe_magic_book");
                    p.sendMessage(Text.literal("§d[마법서] §f기본 마법 '마력탄'을 배웠습니다. 스태프를 들고 §e쉬프트+우클릭§f으로 마법을 선택하세요."), false);
                    if (!p.getAbilities().creativeMode) stack.decrement(1);
                } catch (Throwable ignored) {}
            }
            return TypedActionResult.success(stack, world.isClient());
        }
    }

    private static int getLevel(ServerPlayerEntity p) {
        try {
            Class<?> service = Class.forName("dev.crowncinder.progress.ProgressService");
            Object progress = service.getMethod("get", ServerPlayerEntity.class).invoke(null, p);
            return field(progress, "level").getInt(progress);
        } catch (Throwable ignored) { return Math.max(1, p.experienceLevel); }
    }

    private static int getStat(ServerPlayerEntity p, String stat) {
        try {
            Object progress = progress(p);
            String fieldName = statField(stat);
            if (fieldName == null) return 0;
            return field(progress, fieldName).getInt(progress);
        } catch (Throwable ignored) { return 0; }
    }

    private static int addStat(ServerPlayerEntity p, String stat, int amount) {
        try {
            Object progress = progress(p);
            String fieldName = statField(stat);
            if (fieldName == null) return Integer.MIN_VALUE;
            Field f = field(progress, fieldName);
            int value = Math.max(0, Math.min(10000, f.getInt(progress) + amount));
            f.setInt(progress, value);
            dirtyAndApply(p);
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

    private static void dirtyAndApply(ServerPlayerEntity p) throws Exception {
        Class<?> service = Class.forName("dev.crowncinder.progress.ProgressService");
        Object store = service.getMethod("store", ServerPlayerEntity.class).invoke(null, p);
        store.getClass().getMethod("markDirty").invoke(store);
        try { service.getMethod("apply", ServerPlayerEntity.class).invoke(null, p); } catch (Throwable ignored) {}
    }

    private static void refresh(ServerPlayerEntity p) {
        try {
            Class<?> service = Class.forName("dev.crowncinder.progress.ProgressService");
            service.getMethod("apply", ServerPlayerEntity.class).invoke(null, p);
        } catch (Throwable ignored) {}
    }
}
