package dev.crowncindersafe;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
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
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 0.26 systems layered over Safe025 without duplicating world/NPC systems. */
public final class Safe026 implements ModInitializer {
    public static final String MOD_ID = "crowncinder023safe";
    public static final Item NATURE_ROD = new NatureRod(new Item.Settings().maxCount(1));
    public static final Item ARCHMAGE_STAFF = new ArchmageStaff(new Item.Settings().maxCount(1).fireproof());
    public static final Item ARCHMAGE_CLOAK = new ArchmageCloak(new Item.Settings().maxCount(1).fireproof());

    private static final String GRAND_SPELL_KEY = "CrownGrandSpell";
    private static final String NATURE_SPELL_KEY = "CrownNatureSpell";
    private static final Map<UUID, Long> LAST_MAGIC_USE = new ConcurrentHashMap<>();
    private static int ticks = 0;

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, id("nature_rod"), NATURE_ROD);
        Registry.register(Registries.ITEM, id("archmage_staff"), ARCHMAGE_STAFF);
        Registry.register(Registries.ITEM, id("archmage_cloak"), ARCHMAGE_CLOAK);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(NATURE_ROD);
            entries.add(ARCHMAGE_STAFF);
            entries.add(ARCHMAGE_CLOAK);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.getWorld().isClient()) return;
            float roll = entity.getRandom().nextFloat();
            if (entity.getType() == EntityType.WITCH && roll < 0.12f) {
                entity.dropStack(new ItemStack(NATURE_ROD));
            } else if (entity.getType() == EntityType.EVOKER && roll < 0.08f) {
                entity.dropStack(new ItemStack(ARCHMAGE_STAFF));
            } else if (entity.getType() == EntityType.WITHER && roll < 0.25f) {
                entity.dropStack(new ItemStack(ARCHMAGE_CLOAK));
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(this::serverTick);
    }

    private static Identifier id(String path) { return new Identifier(MOD_ID, path); }

    /** Called by mixin whenever Safe025 actually spends mana. */
    public static void markMagicUsed(ServerPlayerEntity p) {
        if (p != null) LAST_MAGIC_USE.put(p.getUuid(), p.getServerWorld().getTime());
    }

    private void serverTick(MinecraftServer server) {
        ticks++;
        // Resource work is deliberately throttled: no scans every tick.
        if (ticks % 20 == 0) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                try {
                    long now = p.getServerWorld().getTime();
                    long last = LAST_MAGIC_USE.getOrDefault(p.getUuid(), now - 1200L);
                    if (now - last >= 1200L) {
                        double mana = getResource(p, "mana");
                        double max = invokeDouble(p, "maxMana");
                        if (mana < max) {
                            double regen = job(p).equals("mage") ? 3.0 : 2.0;
                            setResource(p, "mana", Math.min(max, mana + regen));
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }
        if (ticks % 60 == 0) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                try {
                    double stamina = getResource(p, "stamina");
                    double max = invokeDouble(p, "maxStamina");
                    if (stamina < max) setResource(p, "stamina", Math.min(max, stamina + 1.0));
                } catch (Throwable ignored) {}
            }
        }
    }

    private static boolean mageAllowed(ServerPlayerEntity p) {
        try {
            Method m = Safe025.class.getDeclaredMethod("canUseMagic", ServerPlayerEntity.class);
            m.setAccessible(true);
            return (boolean)m.invoke(null, p);
        } catch (Throwable ignored) { return false; }
    }

    private static String job(ServerPlayerEntity p) {
        try {
            Object d = playerData(p);
            Field f = d.getClass().getDeclaredField("job"); f.setAccessible(true);
            return String.valueOf(f.get(d));
        } catch (Throwable ignored) { return "none"; }
    }

    private static Object playerData(ServerPlayerEntity p) throws Exception {
        Method m = Safe025.class.getDeclaredMethod("data", ServerPlayerEntity.class);
        m.setAccessible(true);
        return m.invoke(null, p);
    }

    private static double getResource(ServerPlayerEntity p, String field) throws Exception {
        Object d = playerData(p);
        Field f = d.getClass().getDeclaredField(field); f.setAccessible(true);
        return f.getDouble(d);
    }

    private static void setResource(ServerPlayerEntity p, String field, double value) throws Exception {
        Object d = playerData(p);
        Field f = d.getClass().getDeclaredField(field); f.setAccessible(true);
        f.setDouble(d, Math.max(0, value));
        Method state = Safe025.class.getDeclaredMethod("state", ServerPlayerEntity.class);
        state.setAccessible(true);
        Object persistent = state.invoke(null, p);
        persistent.getClass().getMethod("markDirty").invoke(persistent);
    }

    private static double invokeDouble(ServerPlayerEntity p, String method) throws Exception {
        Method m = Safe025.class.getDeclaredMethod(method, ServerPlayerEntity.class);
        m.setAccessible(true);
        return ((Number)m.invoke(null, p)).doubleValue();
    }

    private static int stat(ServerPlayerEntity p, String name) {
        try {
            Method m = Safe025.class.getDeclaredMethod("getStat", ServerPlayerEntity.class, String.class);
            m.setAccessible(true);
            return ((Number)m.invoke(null, p, name)).intValue();
        } catch (Throwable ignored) { return 0; }
    }

    private static boolean consumeMana(ServerPlayerEntity p, double amount) {
        try {
            Method m = Safe025.class.getDeclaredMethod("consumeMana", ServerPlayerEntity.class, double.class);
            m.setAccessible(true);
            boolean ok = (boolean)m.invoke(null, p, amount);
            if (ok) markMagicUsed(p);
            return ok;
        } catch (Throwable ignored) { return false; }
    }

    private static boolean fullArchmageSet(ServerPlayerEntity p) {
        return job(p).equals("mage") && p.getMainHandStack().isOf(ARCHMAGE_STAFF) && p.getEquippedStack(EquipmentSlot.CHEST).isOf(ARCHMAGE_CLOAK);
    }

    private static int getSelected(ItemStack stack, String key, int max) {
        NbtCompound n = stack.getNbt();
        int v = n != null && n.contains(key) ? n.getInt(key) : 1;
        return v < 1 || v > max ? 1 : v;
    }
    private static void setSelected(ItemStack stack, String key, int value) { stack.getOrCreateNbt().putInt(key, value); }

    private static final class ArchmageCloak extends ArmorItem {
        ArchmageCloak(Settings s) { super(ArmorMaterials.NETHERITE, Type.CHESTPLATE, s); }
        @Override public boolean hasGlint(ItemStack stack) { return true; }
    }

    private static final class ArchmageStaff extends Item {
        ArchmageStaff(Settings s) { super(s); }
        @Override public boolean hasGlint(ItemStack stack) { return true; }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (world.isClient || !(user instanceof ServerPlayerEntity p)) return TypedActionResult.success(stack, world.isClient());
            if (!job(p).equals("mage")) {
                p.sendMessage(Text.literal("§c대마법사의 스태프는 마법사만 사용할 수 있습니다."), true);
                return TypedActionResult.fail(stack);
            }
            int normalUnlocked = Math.max(1, Math.min(10, getLevel(p) / 10));
            int max = fullArchmageSet(p) ? 11 : normalUnlocked;
            int selected = getSelected(stack, GRAND_SPELL_KEY, max);
            if (p.isSneaking()) {
                selected = selected >= max ? 1 : selected + 1;
                setSelected(stack, GRAND_SPELL_KEY, selected);
                p.sendMessage(Text.literal("§d[대마법 선택] §f" + grandSpellName(selected) + " §7(MP " + grandManaCost(selected) + ")"), true);
                p.getServerWorld().playSound(null, p.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.1f, 0.75f + selected * 0.025f);
                return TypedActionResult.success(stack, false);
            }
            int cost = grandManaCost(selected);
            if (!consumeMana(p, cost)) {
                p.sendMessage(Text.literal("§c마력이 부족합니다. 필요 MP: " + cost), true);
                return TypedActionResult.fail(stack);
            }
            if (selected == 11) castHolyFlame(p); else castGrandSpell(p, selected, fullArchmageSet(p));
            return TypedActionResult.success(stack, false);
        }
    }

    private static final class NatureRod extends Item {
        NatureRod(Settings s) { super(s); }
        @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (world.isClient || !(user instanceof ServerPlayerEntity p)) return TypedActionResult.success(stack, world.isClient());
            if (!mageAllowed(p)) {
                p.sendMessage(Text.literal("§c자연 마법은 마법사 또는 신의 축복을 받은 자만 사용할 수 있습니다."), true);
                return TypedActionResult.fail(stack);
            }
            int selected = getSelected(stack, NATURE_SPELL_KEY, 3);
            if (p.isSneaking()) {
                selected = selected >= 3 ? 1 : selected + 1;
                setSelected(stack, NATURE_SPELL_KEY, selected);
                p.sendMessage(Text.literal("§a[자연 마법] §f" + natureName(selected) + " §7(MP " + natureCost(selected) + ")"), true);
                return TypedActionResult.success(stack, false);
            }
            int cost = natureCost(selected);
            if (!consumeMana(p, cost)) {
                p.sendMessage(Text.literal("§c마력이 부족합니다. 필요 MP: " + cost), true);
                return TypedActionResult.fail(stack);
            }
            castNature(p, selected);
            return TypedActionResult.success(stack, false);
        }
    }

    private static int getLevel(ServerPlayerEntity p) {
        try {
            Method m = Safe025.class.getDeclaredMethod("getLevel", ServerPlayerEntity.class);
            m.setAccessible(true);
            return ((Number)m.invoke(null, p)).intValue();
        } catch (Throwable ignored) { return Math.max(1, p.experienceLevel); }
    }

    private static String grandSpellName(int s) {
        if (s == 11) return "§6성염";
        String[] n = {"", "마력탄", "화염창", "빙결창", "마력 폭발", "천둥의 창", "대화염 폭풍", "빙설 폭풍", "성광의 파동", "심연의 구체", "대마도사의 대재앙"};
        return n[Math.max(1, Math.min(10, s))];
    }
    private static int grandManaCost(int s) {
        if (s == 11) return 500;
        int[] c = {0,10,16,22,30,38,50,62,76,94,120};
        return c[Math.max(1, Math.min(10, s))];
    }
    private static double baseDamage(int s) {
        double[] d = {0,8,13,18,24,31,39,48,58,70,88};
        return d[Math.max(1, Math.min(10, s))];
    }

    private static void castGrandSpell(ServerPlayerEntity p, int spell, boolean fullSet) {
        ServerWorld w = p.getServerWorld();
        double mult = fullSet ? 1.65 : 1.35;
        double damage = (baseDamage(spell) + stat(p,"mana") * (0.32 + spell * 0.075)) * mult;
        Vec3d hitPos = rayImpact(p, Math.min(46, 22 + spell * 2.4));
        double radius = spell < 4 ? 1.0 : Math.min(7.0, 2.3 + spell * 0.48);
        Box area = new Box(hitPos.x-radius, hitPos.y-radius, hitPos.z-radius, hitPos.x+radius, hitPos.y+radius, hitPos.z+radius);
        List<HostileEntity> targets = w.getEntitiesByClass(HostileEntity.class, area, e -> e.isAlive());
        for (HostileEntity e : targets) {
            float dealt = (float)(damage * Math.max(0.55, 1.0 - e.getPos().distanceTo(hitPos)/(radius*2.0)));
            e.damage(p.getDamageSources().playerAttack(p), dealt);
            if (spell==2||spell==6) e.setOnFireFor(spell==6?8:4);
            if (spell==3||spell==7) e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, spell==7?2:1));
        }
        int particles = Math.min(140, 35 + spell * 10);
        w.spawnParticles(spell==3||spell==7 ? ParticleTypes.SNOWFLAKE : spell==5 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD,
                hitPos.x, hitPos.y, hitPos.z, particles, radius*.65, radius*.5, radius*.65, .08);
        if (spell>=5) w.spawnParticles(ParticleTypes.EXPLOSION, hitPos.x, hitPos.y, hitPos.z, Math.min(8,spell), .7,.7,.7,.02);
        w.playSound(null, BlockPos.ofFloored(hitPos), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.3f, .85f);
        p.getItemCooldownManager().set(ARCHMAGE_STAFF, 20 + spell*2);
        p.sendMessage(Text.literal("§d"+grandSpellName(spell)+" §7| 강화 위력 "+String.format("%.1f",damage)), true);
    }

    private static void castHolyFlame(ServerPlayerEntity p) {
        if (!fullArchmageSet(p)) {
            p.sendMessage(Text.literal("§c성염은 대마법사의 망토와 스태프를 함께 사용해야 합니다."), true);
            return;
        }
        ServerWorld w = p.getServerWorld();
        Vec3d impact = rayImpact(p, 42.0);
        double radius = 11.0;
        for (HostileEntity e : w.getEntitiesByClass(HostileEntity.class,
                new Box(impact.x-radius,impact.y-radius,impact.z-radius,impact.x+radius,impact.y+radius,impact.z+radius), e->e.isAlive())) {
            double dist = e.getPos().distanceTo(impact);
            if (dist <= radius) {
                float damage = (float)Math.max(175.0, 350.0 * (1.0 - dist/(radius*2.0)));
                e.damage(p.getDamageSources().playerAttack(p), damage);
                e.setOnFireFor(12);
            }
        }
        w.createExplosion(p, impact.x, impact.y, impact.z, 6.0f, World.ExplosionSourceType.MOB);
        w.spawnParticles(ParticleTypes.FLAME, impact.x,impact.y,impact.z, 220, 5.0,3.0,5.0,.12);
        w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, impact.x,impact.y,impact.z, 160, 4.0,2.5,4.0,.09);
        w.spawnParticles(ParticleTypes.END_ROD, impact.x,impact.y+1,impact.z, 100, 3.5,4.0,3.5,.08);
        w.playSound(null, BlockPos.ofFloored(impact), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2.0f, .55f);
        w.playSound(null, BlockPos.ofFloored(impact), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.6f, .7f);
        p.getItemCooldownManager().set(ARCHMAGE_STAFF, 160);
        p.sendMessage(Text.literal("§6✦ 전설의 대마법 · 성염 ✦ §fMP -500 · 기본 피해 350"), true);
    }

    private static String natureName(int s) { return s==1?"물의 탄환":s==2?"바람 칼날":"낙뢰"; }
    private static int natureCost(int s) { return s==1?12:s==2?14:18; }
    private static void castNature(ServerPlayerEntity p, int spell) {
        ServerWorld w = p.getServerWorld();
        Vec3d impact = rayImpact(p, 24.0);
        double radius = spell==2?3.5:1.8;
        double dmg = (spell==1?12:spell==2?10:22) + stat(p,"mana")*.22;
        List<HostileEntity> targets = w.getEntitiesByClass(HostileEntity.class,
                new Box(impact.x-radius,impact.y-radius,impact.z-radius,impact.x+radius,impact.y+radius,impact.z+radius), e->e.isAlive());
        for (HostileEntity e:targets) {
            e.damage(p.getDamageSources().playerAttack(p),(float)dmg);
            if (spell==1) e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,60,0));
            if (spell==2) {
                Vec3d push=e.getPos().subtract(p.getPos()).normalize().multiply(.9);
                e.addVelocity(push.x,.25,push.z);
            }
            if (spell==3) e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING,60,0));
        }
        if (spell==1) w.spawnParticles(ParticleTypes.SPLASH,impact.x,impact.y,impact.z,55,1.1,1.1,1.1,.08);
        else if (spell==2) w.spawnParticles(ParticleTypes.CLOUD,impact.x,impact.y,impact.z,70,1.7,.8,1.7,.12);
        else w.spawnParticles(ParticleTypes.ELECTRIC_SPARK,impact.x,impact.y,impact.z,85,1.0,2.0,1.0,.15);
        w.playSound(null,BlockPos.ofFloored(impact), spell==3?SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER:SoundEvents.ENTITY_EVOKER_CAST_SPELL,SoundCategory.PLAYERS,.8f,1.25f);
        p.getItemCooldownManager().set(NATURE_ROD, spell==3?24:16);
        p.sendMessage(Text.literal("§a"+natureName(spell)+" §7| MP -"+natureCost(spell)),true);
    }

    private static Vec3d rayImpact(ServerPlayerEntity p, double range) {
        ServerWorld w=p.getServerWorld(); Vec3d start=p.getEyePos(),dir=p.getRotationVec(1).normalize(); Vec3d last=start.add(dir.multiply(range));
        for(double d=.8;d<=range;d+=.65){Vec3d at=start.add(dir.multiply(d));last=at;Box b=new Box(at.x-.7,at.y-.7,at.z-.7,at.x+.7,at.y+.7,at.z+.7);if(!w.getEntitiesByClass(HostileEntity.class,b,e->e.isAlive()&&p.canSee(e)).isEmpty())return at;}
        return last;
    }
}
