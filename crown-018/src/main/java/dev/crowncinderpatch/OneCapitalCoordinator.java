package dev.crowncinderpatch;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Keeps 0.19 capitals at the same coordinates used by the original Crown & Cinder world generator.
 * A unique three-block signature distinguishes the new surface capital from legacy lodestone markers.
 */
public final class OneCapitalCoordinator {
    private OneCapitalCoordinator() {}

    private record Site(String id, int dx, int dz) {}

    private static final Site[] SITES = {
        new Site("central_empire", 0, 0),
        new Site("aurelia", 920, 40),
        new Site("eldoria", 300, -900),
        new Site("kharum", -760, -560),
        new Site("sylvan", -780, 590),
        new Site("varkhan", 310, 920)
    };

    public static void ensureFreshWorld(ServerWorld world) {
        if (world.getTime() > 6000L || has019Marker(world)) return;
        rebuildAll(world);
    }

    public static void rebuildAll(ServerWorld world) {
        BlockPos spawn = world.getSpawnPos();
        for (Site site : SITES) {
            MedievalKingdoms.Nation nation = MedievalKingdoms.nation(site.id());
            if (nation != null) {
                BlockPos rough = spawn.add(site.dx(), 0, site.dz());
                MedievalKingdoms.rebuild(world, rough, nation);
                placeSignature(world, rough);
            }
        }
    }

    public static boolean rebuildOne(ServerWorld world, String id) {
        Site site = site(id);
        MedievalKingdoms.Nation nation = MedievalKingdoms.nation(id);
        if (site == null || nation == null) return false;
        BlockPos rough = world.getSpawnPos().add(site.dx(), 0, site.dz());
        MedievalKingdoms.rebuild(world, rough, nation);
        placeSignature(world, rough);
        return true;
    }

    public static boolean has019Marker(ServerWorld world) {
        BlockPos spawn = world.getSpawnPos();
        for (int y = world.getBottomY() + 4; y < world.getTopY() - 2; y++) {
            BlockPos p = new BlockPos(spawn.getX(), y, spawn.getZ());
            if (world.getBlockState(p).isOf(Blocks.LODESTONE)
                && world.getBlockState(p.east()).isOf(Blocks.EMERALD_BLOCK)
                && world.getBlockState(p.west()).isOf(Blocks.AMETHYST_BLOCK)) {
                return true;
            }
        }
        return false;
    }

    private static void placeSignature(ServerWorld world, BlockPos rough) {
        int y = MedievalKingdoms.sampleBuildHeight(world, rough.getX(), rough.getZ());
        BlockPos p = new BlockPos(rough.getX(), y, rough.getZ());
        world.setBlockState(p, Blocks.LODESTONE.getDefaultState(), 2);
        world.setBlockState(p.east(), Blocks.EMERALD_BLOCK.getDefaultState(), 2);
        world.setBlockState(p.west(), Blocks.AMETHYST_BLOCK.getDefaultState(), 2);
    }

    private static Site site(String id) {
        for (Site site : SITES) if (site.id().equals(id)) return site;
        return null;
    }
}
