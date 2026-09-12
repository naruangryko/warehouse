package dev.crowncinderpatch;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;

import java.util.ArrayList;
import java.util.List;

/** Reliable post-build NPC population pass for 0.20. */
public final class NpcBootstrap020 {
    private NpcBootstrap020() {}

    private record Site(String id, String name, int dx, int dz) {}
    private static final Site[] SITES = {
        new Site("central_empire", "중앙 제국", 0, 0),
        new Site("aurelia", "오렐리아 왕국", 920, 40),
        new Site("eldoria", "엘도리아 마도왕국", 300, -900),
        new Site("kharum", "카룸 산악국", -760, -560),
        new Site("sylvan", "실반 연맹", -780, 590),
        new Site("varkhan", "바르칸 제국", 310, 920)
    };

    private record Role(String name, VillagerProfession profession, int x, int z, int gear) {}

    private static final Role[] ROLES = {
        new Role("국왕", VillagerProfession.CARTOGRAPHER, 0, -8, 3),
        new Role("왕실 시종", VillagerProfession.LIBRARIAN, -4, -5, 3),
        new Role("궁정 서기관", VillagerProfession.LIBRARIAN, 4, -5, 3),
        new Role("재무관", VillagerProfession.CARTOGRAPHER, -5, -2, 3),
        new Role("왕실 사제", VillagerProfession.CLERIC, -25, -12, 3),
        new Role("왕실 요리사", VillagerProfession.BUTCHER, 2, -12, 0),
        new Role("왕궁 하인", VillagerProfession.FARMER, -2, -4, 0),

        new Role("기사단장", VillagerProfession.WEAPONSMITH, -54, -22, 2),
        new Role("기사 부단장", VillagerProfession.ARMORER, -51, -22, 2),
        new Role("왕실 기사", VillagerProfession.WEAPONSMITH, -57, -18, 2),
        new Role("왕실 기사", VillagerProfession.ARMORER, -51, -18, 2),
        new Role("기사 교관", VillagerProfession.WEAPONSMITH, -57, -26, 2),
        new Role("기사 보급관", VillagerProfession.TOOLSMITH, -51, -26, 2),
        new Role("기사", VillagerProfession.WEAPONSMITH, -59, -16, 2),
        new Role("기사", VillagerProfession.ARMORER, -49, -16, 2),

        new Role("용병단장", VillagerProfession.WEAPONSMITH, 54, -22, 1),
        new Role("베테랑 용병", VillagerProfession.LEATHERWORKER, 51, -22, 1),
        new Role("베테랑 용병", VillagerProfession.FLETCHER, 57, -18, 1),
        new Role("정찰 용병", VillagerProfession.FLETCHER, 51, -18, 1),
        new Role("용병 접수원", VillagerProfession.CARTOGRAPHER, 57, -26, 0),
        new Role("현상금 담당관", VillagerProfession.LIBRARIAN, 51, -26, 0),
        new Role("용병", VillagerProfession.LEATHERWORKER, 59, -16, 1),
        new Role("용병", VillagerProfession.FLETCHER, 49, -16, 1),

        new Role("여관주인", VillagerProfession.BUTCHER, -54, 20, 0),
        new Role("주방장", VillagerProfession.FARMER, -51, 20, 0),
        new Role("음유시인", VillagerProfession.LIBRARIAN, -57, 23, 0),
        new Role("대장장이", VillagerProfession.WEAPONSMITH, 54, 20, 2),
        new Role("갑옷 장인", VillagerProfession.ARMORER, 51, 20, 2),
        new Role("도구 장인", VillagerProfession.TOOLSMITH, 57, 20, 1),

        new Role("시장 상인", VillagerProfession.CARTOGRAPHER, -10, 22, 0),
        new Role("약초상", VillagerProfession.CLERIC, 0, 22, 0),
        new Role("빵집 주인", VillagerProfession.FARMER, 10, 22, 0),
        new Role("직물 상인", VillagerProfession.SHEPHERD, -15, 27, 0),
        new Role("지도 제작자", VillagerProfession.CARTOGRAPHER, 15, 27, 0),
        new Role("가죽 장인", VillagerProfession.LEATHERWORKER, 20, 22, 0),

        new Role("농부", VillagerProfession.FARMER, -54, 45, 0),
        new Role("농부", VillagerProfession.FARMER, -36, 54, 0),
        new Role("석공", VillagerProfession.MASON, -15, 54, 0),
        new Role("어부", VillagerProfession.FISHERMAN, 15, 54, 0),
        new Role("마구간지기", VillagerProfession.LEATHERWORKER, 36, 54, 0),
        new Role("마을 촌장", VillagerProfession.CARTOGRAPHER, 54, 45, 3),
        new Role("주민", VillagerProfession.NONE, -54, 5, 0),
        new Role("주민", VillagerProfession.NONE, 54, 5, 0),
        new Role("주민", VillagerProfession.NONE, -54, -45, 0),
        new Role("주민", VillagerProfession.NONE, -36, -54, 0),
        new Role("주민", VillagerProfession.NONE, 36, -54, 0),
        new Role("주민", VillagerProfession.NONE, 54, -45, 0),
        new Role("주민", VillagerProfession.NONE, -15, -54, 0),
        new Role("주민", VillagerProfession.NONE, 15, -54, 0)
    };

    public static int respawnAll(ServerWorld world) {
        BlockPos anchor = OneCapitalCoordinator.anchor(world);
        int total = 0;
        for (Site site : SITES) {
            total += respawnAt(world, anchor.add(site.dx(), 0, site.dz()), site.id(), site.name());
        }
        return total;
    }

    public static int respawnOne(ServerWorld world, String id) {
        BlockPos anchor = OneCapitalCoordinator.anchor(world);
        for (Site site : SITES) {
            if (site.id().equals(id)) return respawnAt(world, anchor.add(site.dx(), 0, site.dz()), site.id(), site.name());
        }
        return 0;
    }

    private static int respawnAt(ServerWorld world, BlockPos rough, String id, String nationName) {
        int cityY = MedievalKingdoms.sampleBuildHeight(world, rough.getX(), rough.getZ());
        BlockPos center = new BlockPos(rough.getX(), cityY, rough.getZ());
        clearOldNpcs(world, center);

        int count = 0;
        for (int i = 0; i < ROLES.length; i++) {
            Role role = ROLES[i];
            int x = center.getX() + role.x();
            int z = center.getZ() + role.z();
            world.getChunk(x >> 4, z >> 4);
            BlockPos safe = findSafeFloor(world, x, center.getY() + 2, z);
            if (safe == null) continue;

            VillagerEntity v = EntityType.VILLAGER.create(world);
            if (v == null) continue;
            v.setVillagerData(v.getVillagerData().withType(VillagerType.PLAINS).withProfession(role.profession()).withLevel(5));
            v.refreshPositionAndAngles(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, world.random.nextFloat() * 360f, 0f);
            String title = (i == 0 && "central_empire".equals(id)) ? "황제" : role.name();
            v.setCustomName(Text.literal("§6[" + nationName + "] §f" + title));
            v.setCustomNameVisible(i < 23);
            v.setPersistent();
            v.addCommandTag("crown020_npc");
            v.addCommandTag("crown020_" + id);
            equip(v, role.gear());

            if (world.spawnEntity(v)) count++;
        }
        return count;
    }

    private static void equip(VillagerEntity v, int gear) {
        if (gear == 3) v.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        else if (gear == 2) v.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        else if (gear == 1) v.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
    }

    private static BlockPos findSafeFloor(ServerWorld world, int baseX, int preferredY, int baseZ) {
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int radius = 0; radius <= 5; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    int x = baseX + dx;
                    int z = baseZ + dz;
                    world.getChunk(x >> 4, z >> 4);
                    for (int y = preferredY - 1; y <= preferredY + 10; y++) {
                        if (y <= world.getBottomY() + 2 || y >= world.getTopY() - 2) continue;
                        boolean floor = !world.getBlockState(p.set(x, y - 1, z)).isAir()
                            && world.getBlockState(p).getFluidState().isEmpty();
                        boolean body = world.getBlockState(p.set(x, y, z)).isAir();
                        boolean head = world.getBlockState(p.set(x, y + 1, z)).isAir();
                        if (floor && body && head) return new BlockPos(x, y, z);
                    }
                }
            }
        }
        return null;
    }

    private static void clearOldNpcs(ServerWorld world, BlockPos center) {
        Box box = new Box(center.add(-78, -28, -78), center.add(78, 40, 78));
        List<Entity> remove = new ArrayList<>(world.getOtherEntities(null, box, e -> shouldClear(e)));
        for (Entity e : remove) e.discard();
    }

    private static boolean shouldClear(Entity e) {
        if (e instanceof PlayerEntity) return false;
        if (!(e instanceof VillagerEntity)) return false;
        for (String tag : e.getCommandTags()) {
            if (tag.startsWith("crown019_") || tag.startsWith("crown020_")) return true;
        }
        if (e.hasCustomName()) {
            String n = e.getCustomName().getString();
            return n.contains("황제") || n.contains("국왕") || n.contains("기사") || n.contains("용병")
                || n.contains("시종") || n.contains("상인") || n.contains("대장장이") || n.contains("주민")
                || n.contains("사제") || n.contains("서기관") || n.contains("농부") || n.contains("여관");
        }
        return false;
    }
}
