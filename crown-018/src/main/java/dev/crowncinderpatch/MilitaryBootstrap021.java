package dev.crowncinderpatch;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

/** 0.21 military population: 20 inner-wall knights, 40 outer-wall knights, plus royal guards per capital. */
public final class MilitaryBootstrap021 {
    private MilitaryBootstrap021() {}

    public static int populate(ServerWorld world, BlockPos rough, MedievalKingdoms.Nation nation) {
        int y = MedievalKingdoms.sampleBuildHeight(world, rough.getX(), rough.getZ());
        BlockPos c = new BlockPos(rough.getX(), y, rough.getZ());
        clear(world, c);
        int count = 0;

        // Eight elite guards around the throne/keep.
        int[][] royal = {{-4,-14},{4,-14},{-7,-10},{7,-10},{-7,-4},{7,-4},{-4,0},{4,0}};
        for (int i = 0; i < royal.length; i++) {
            count += spawn(world, c.add(royal[i][0], 0, royal[i][1]), nation,
                "central_empire".equals(nation.id()) ? "황실 수호기사" : "왕실 수호기사",
                "crown_guard", "knight_blade", 3);
        }

        // 20 inside the first wall, mixed sword/spear/bow.
        count += ring(world, c, nation, 34, 20, "내성 기사단", 0.0);
        // 40 inside the second wall.
        count += ring(world, c, nation, 56, 40, "외성 기사단", Math.PI / 40.0);
        return count;
    }

    private static int ring(ServerWorld world, BlockPos c, MedievalKingdoms.Nation nation, int radius, int amount, String title, double offset) {
        int made = 0;
        for (int i = 0; i < amount; i++) {
            double a = (Math.PI * 2.0 * i / amount) + offset;
            int x = c.getX() + (int)Math.round(Math.cos(a) * radius);
            int z = c.getZ() + (int)Math.round(Math.sin(a) * radius);
            int kind = i % 3;
            String entity = kind == 0 ? "crown_guard" : (kind == 1 ? "imperial_spear_knight" : "imperial_archer");
            String weapon = kind == 0 ? "knight_blade" : (kind == 1 ? "spear" : "bow");
            String role = title + (kind == 0 ? " · 검" : kind == 1 ? " · 창" : " · 활");
            made += spawn(world, new BlockPos(x, c.getY(), z), nation, role, entity, weapon, kind);
        }
        return made;
    }

    private static int spawn(ServerWorld world, BlockPos approx, MedievalKingdoms.Nation nation, String role, String entityId, String weaponId, int gear) {
        world.getChunk(approx.getX() >> 4, approx.getZ() >> 4);
        BlockPos safe = safe(world, approx.getX(), approx.getY() + 2, approx.getZ());
        if (safe == null) safe = approx.up(2);
        var typeOpt = Registries.ENTITY_TYPE.getOrEmpty(new Identifier("crowncinder", entityId));
        if (typeOpt.isEmpty()) return 0;
        Entity e = typeOpt.get().create(world);
        if (!(e instanceof LivingEntity living)) return 0;
        living.refreshPositionAndAngles(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, world.random.nextFloat() * 360f, 0f);
        living.setCustomName(Text.literal("§9[" + nation.name() + "] §f" + role));
        living.setCustomNameVisible(false);
        living.addCommandTag("crown021_military");
        living.addCommandTag("crown021_" + nation.id());
        Item weapon = Registries.ITEM.getOrEmpty(new Identifier("crowncinder", weaponId)).orElse(weaponId.equals("bow") ? Items.BOW : Items.IRON_SWORD);
        living.equipStack(EquipmentSlot.MAINHAND, new ItemStack(weapon));
        if (gear == 3) living.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        else if (gear == 2) living.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        else living.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        if (living instanceof MobEntity mob) mob.setPersistent();
        return world.spawnEntity(living) ? 1 : 0;
    }

    private static BlockPos safe(ServerWorld world, int x, int y0, int z) {
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int r = 0; r <= 3; r++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) continue;
            int xx = x + dx, zz = z + dz;
            for (int y = y0 - 2; y <= y0 + 8; y++) {
                boolean floor = !world.getBlockState(p.set(xx, y - 1, zz)).isAir() && world.getBlockState(p).getFluidState().isEmpty();
                boolean body = world.getBlockState(p.set(xx, y, zz)).isAir();
                boolean head = world.getBlockState(p.set(xx, y + 1, zz)).isAir();
                if (floor && body && head) return new BlockPos(xx, y, zz);
            }
        }
        return null;
    }

    private static void clear(ServerWorld world, BlockPos c) {
        Box box = new Box(c.add(-75, -10, -75), c.add(75, 35, 75));
        List<Entity> remove = new ArrayList<>(world.getOtherEntities(null, box, e -> e.getCommandTags().contains("crown021_military")));
        for (Entity e : remove) e.discard();
    }
}
