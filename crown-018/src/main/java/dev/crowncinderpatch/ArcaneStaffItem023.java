package dev.crowncinderpatch;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

/** Simple visible hitscan spell staff. Highest learned spell is used automatically. */
public final class ArcaneStaffItem023 extends Item {
    public ArcaneStaffItem023(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack, true);
        if (!(user instanceof ServerPlayerEntity player)) return TypedActionResult.pass(stack);

        int tier = MagicSystem023.magicTier(player);
        if (tier <= 0) {
            player.sendMessage(Text.literal("§d[마법] §f아직 마법을 배우지 않았습니다. Lv.10 달성 또는 마법서를 사용하세요."), false);
            return TypedActionResult.fail(stack);
        }

        ServerWorld serverWorld = player.getServerWorld();
        double damage = 6.0 + tier * 2.2 + RpgProgressBridge.getMagicPower(player) * 0.35;
        double range = Math.min(38.0, 20.0 + tier * 1.5);
        ParticleEffect particle = MagicSystem023.particle(tier);
        Vec3d start = player.getEyePos();
        Vec3d dir = player.getRotationVec(1.0f).normalize();
        Set<Integer> hit = new HashSet<>();
        LivingEntity primary = null;

        for (double d = 0.8; d <= range; d += 0.65) {
            Vec3d p = start.add(dir.multiply(d));
            serverWorld.spawnParticles(particle, p.x, p.y, p.z, 2, 0.04, 0.04, 0.04, 0.005);
            Box box = new Box(p.x - 0.65, p.y - 0.65, p.z - 0.65, p.x + 0.65, p.y + 0.65, p.z + 0.65);
            for (LivingEntity target : serverWorld.getEntitiesByClass(LivingEntity.class, box,
                    e -> e.isAlive() && e != player && e instanceof HostileEntity)) {
                if (!hit.add(target.getId())) continue;
                if (!player.canSee(target)) continue;
                primary = target;
                target.damage(player.getDamageSources().playerAttack(player), (float)damage);
                applySpellEffect(target, tier);
                burst(serverWorld, target.getPos(), particle, tier);
                break;
            }
            if (primary != null) break;
        }

        int cooldown = Math.max(10, 30 - tier);
        player.getItemCooldownManager().set(this, cooldown);
        serverWorld.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                SoundCategory.PLAYERS, 0.8f, 1.0f + Math.min(0.6f, tier * 0.04f));
        player.sendMessage(Text.literal("§b" + MagicSystem023.spellName(tier) + " §7(" + String.format("%.1f", damage) + " 피해)"), true);
        return TypedActionResult.success(stack, false);
    }

    private static void applySpellEffect(LivingEntity target, int tier) {
        if (tier == 2 || tier == 6 || tier == 10) target.setOnFireFor(Math.min(8, 2 + tier / 2));
        if (tier == 3 || tier == 7) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60 + tier * 5, 1));
        if (tier == 5 || tier == 9) target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 50 + tier * 5, 0));
        if (tier >= 8) target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 100, 0));
    }

    private static void burst(ServerWorld world, Vec3d p, ParticleEffect particle, int tier) {
        world.spawnParticles(particle, p.x, p.y + 0.8, p.z, 16 + tier * 2, 0.5, 0.6, 0.5, 0.06);
        if (tier >= 4) world.spawnParticles(ParticleTypes.END_ROD, p.x, p.y + 0.8, p.z, 10, 0.35, 0.35, 0.35, 0.04);
    }
}
