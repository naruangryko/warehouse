package dev.crowncindersafe;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Crown & Cinder 0.27 progression rewards and weak starter weapons. */
public final class Safe027 implements ModInitializer {
    public static final String MOD_ID = "crowncinder023safe";
    public static final Item IRON_LONGSWORD = new SwordItem(ToolMaterials.IRON, 1, -2.7f, new Item.Settings().maxCount(1));
    public static final Item NOVICE_STAFF = new NoviceStaff(new Item.Settings().maxCount(1));
    private static int ticks = 0;

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, id("iron_longsword"), IRON_LONGSWORD);
        Registry.register(Registries.ITEM, id("novice_staff"), NOVICE_STAFF);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> {
            ServerPlayerEntity p = handler.getPlayer();
            try {
                grantLegacyStarterIfNeeded(p);
                grantProgressionRewards(p);
            } catch (Throwable ignored) {}
        }));

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private static Identifier id(String path) { return new Identifier(MOD_ID, path); }

    private void tick(MinecraftServer server) {
        if (++ticks % 100 != 0) return; // only every 5 seconds
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            try {
                grantLegacyStarterIfNeeded(p);
                grantProgressionRewards(p);
            } catch (Throwable ignored) {}
        }
    }

    /** Called from StarterWeaponMixin027 immediately after the player selects a class. */
    public static void onJobChosen(ServerPlayerEntity p, String job) {
        if (p == null || job == null) return;
        if (job.equals("knight")) {
            removeOne(p, new Identifier("crowncinder", "knight_blade"));
            placeInWeaponSlot(p, new ItemStack(IRON_LONGSWORD));
            p.addCommandTag("ccsafe027_starter_given");
            p.sendMessage(Text.literal("§7[기본 장비] §f철제 장검을 무기칸에 지급했습니다. 성능은 낮지만 기사 수련용으로 적합합니다."), false);
        } else if (job.equals("mage")) {
            removeOne(p, new Identifier(MOD_ID, "arcane_staff"));
            removeOne(p, new Identifier(MOD_ID, "spellbook"));
            placeInWeaponSlot(p, new ItemStack(NOVICE_STAFF));
            p.addCommandTag("ccsafe027_starter_given");
            p.sendMessage(Text.literal("§7[기본 장비] §f초급자의 지팡이를 무기칸에 지급했습니다. 위력과 사거리가 낮습니다."), false);
        }
    }

    private static void grantLegacyStarterIfNeeded(ServerPlayerEntity p) {
        if (p.getCommandTags().contains("ccsafe027_starter_given")) return;
        String job = job(p);
        if (job.equals("none")) return;
        // Existing 0.25/0.26 characters receive the new starter once, but old earned gear is not deleted.
        if (job.equals("knight")) placeInWeaponSlot(p, new ItemStack(IRON_LONGSWORD));
        else if (job.equals("mage")) placeInWeaponSlot(p, new ItemStack(NOVICE_STAFF));
        p.addCommandTag("ccsafe027_starter_given");
        p.sendMessage(Text.literal("§6[0.27] §f새 기본 무기를 1번 무기칸에 지급했습니다."), false);
    }

    private static void grantProgressionRewards(ServerPlayerEntity p) {
        if (!job(p).equals("mage")) return;
        int level = level(p);
        if (level >= 10 && !p.getCommandTags().contains("ccsafe027_nature_rod")) {
            p.addCommandTag("ccsafe027_nature_rod");
            p.giveItemStack(new ItemStack(Safe026.NATURE_ROD));
            rewardFx(p, "§a[마법사 성장 보상] §fLv.10 달성: §a자연의 막대기§f를 획득했습니다.");
        }
        if (level >= 70 && !p.getCommandTags().contains("ccsafe027_archmage_cloak")) {
            p.addCommandTag("ccsafe027_archmage_cloak");
            p.giveItemStack(new ItemStack(Safe026.ARCHMAGE_CLOAK));
            rewardFx(p, "§d[대마법사 시험] §fLv.70 달성: §5대마법사의 망토§f를 인정받았습니다.");
        }
        if (level >= 80 && !p.getCommandTags().contains("ccsafe027_archmage_staff")) {
            p.addCommandTag("ccsafe027_archmage_staff");
            p.giveItemStack(new ItemStack(Safe026.ARCHMAGE_STAFF));
            rewardFx(p, "§6[대마법사 시험] §fLv.80 달성: §d대마법사의 스태프§f를 계승했습니다.");
        }
    }

    private static void rewardFx(ServerPlayerEntity p, String message) {
        p.sendMessage(Text.literal(message), false);
        ServerWorld w = p.getServerWorld();
        w.spawnParticles(ParticleTypes.END_ROD, p.getX(), p.getBodyY(0.6), p.getZ(), 45, .7, 1.0, .7, .05);
        w.playSound(null, p.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.05f);
    }

    public static void placeInWeaponSlot(ServerPlayerEntity p, ItemStack weapon) {
        var inv = p.getInventory();
        ItemStack old = inv.getStack(0).copy();
        inv.setStack(0, weapon);
        inv.selectedSlot = 0;
        if (!old.isEmpty()) {
            if (!inv.insertStack(old)) p.dropItem(old, false);
        }
        inv.markDirty();
    }

    private static void removeOne(ServerPlayerEntity p, Identifier id) {
        if (!Registries.ITEM.containsId(id)) return;
        Item target = Registries.ITEM.get(id);
        var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(target)) {
                s.decrement(1);
                if (s.isEmpty()) inv.setStack(i, ItemStack.EMPTY);
                return;
            }
        }
    }

    private static String job(ServerPlayerEntity p) {
        try {
            Method data = Safe025.class.getDeclaredMethod("data", ServerPlayerEntity.class);
            data.setAccessible(true);
            Object d = data.invoke(null, p);
            Field f = d.getClass().getDeclaredField("job");
            f.setAccessible(true);
            return String.valueOf(f.get(d));
        } catch (Throwable ignored) { return "none"; }
    }

    private static int level(ServerPlayerEntity p) {
        try {
            Method m = Safe025.class.getDeclaredMethod("getLevel", ServerPlayerEntity.class);
            m.setAccessible(true);
            return ((Number)m.invoke(null, p)).intValue();
        } catch (Throwable ignored) { return Math.max(1, p.experienceLevel); }
    }

    private static boolean canUseMagic(ServerPlayerEntity p) {
        try {
            Method m = Safe025.class.getDeclaredMethod("canUseMagic", ServerPlayerEntity.class);
            m.setAccessible(true);
            return (boolean)m.invoke(null, p);
        } catch (Throwable ignored) { return false; }
    }

    private static boolean consumeMana(ServerPlayerEntity p, double amount) {
        try {
            Method m = Safe025.class.getDeclaredMethod("consumeMana", ServerPlayerEntity.class, double.class);
            m.setAccessible(true);
            boolean ok = (boolean)m.invoke(null, p, amount);
            if (ok) Safe026.markMagicUsed(p);
            return ok;
        } catch (Throwable ignored) { return false; }
    }

    private static int manaStat(ServerPlayerEntity p) {
        try {
            Method m = Safe025.class.getDeclaredMethod("getStat", ServerPlayerEntity.class, String.class);
            m.setAccessible(true);
            return ((Number)m.invoke(null, p, "mana")).intValue();
        } catch (Throwable ignored) { return 0; }
    }

    private static final class NoviceStaff extends Item {
        NoviceStaff(Settings settings) { super(settings); }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (world.isClient || !(user instanceof ServerPlayerEntity p)) return TypedActionResult.success(stack, world.isClient());
            if (!canUseMagic(p)) {
                p.sendMessage(Text.literal("§c이 지팡이는 마법사 또는 신의 축복을 받은 자만 사용할 수 있습니다."), true);
                return TypedActionResult.fail(stack);
            }
            if (!consumeMana(p, 8)) {
                p.sendMessage(Text.literal("§c마력이 부족합니다. 필요 MP: 8"), true);
                return TypedActionResult.fail(stack);
            }

            ServerWorld w = p.getServerWorld();
            Vec3d start = p.getEyePos();
            Vec3d dir = p.getRotationVec(1.0f).normalize();
            HostileEntity target = null;
            Vec3d impact = start.add(dir.multiply(14));
            for (double d = .8; d <= 14; d += .7) {
                Vec3d at = start.add(dir.multiply(d));
                impact = at;
                if (((int)(d * 10)) % 14 == 0) w.spawnParticles(ParticleTypes.ENCHANT, at.x, at.y, at.z, 2, .05, .05, .05, .01);
                List<HostileEntity> list = w.getEntitiesByClass(HostileEntity.class,
                        new Box(at.x-.65, at.y-.65, at.z-.65, at.x+.65, at.y+.65, at.z+.65), e -> e.isAlive() && p.canSee(e));
                if (!list.isEmpty()) { target = list.get(0); break; }
            }
            double damage = 5.0 + Math.max(0, manaStat(p)) * .08;
            if (target != null) {
                impact = target.getPos().add(0, target.getHeight()*.5, 0);
                target.damage(p.getDamageSources().playerAttack(p), (float)damage);
            }
            w.spawnParticles(ParticleTypes.ENCHANT, impact.x, impact.y, impact.z, 18, .35, .35, .35, .04);
            w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, .7f, 1.5f);
            p.getItemCooldownManager().set(this, 26);
            p.sendMessage(Text.literal("§d초급 마력탄 §7| MP -8 | 위력 " + String.format("%.1f", damage)), true);
            return TypedActionResult.success(stack, false);
        }
    }
}
