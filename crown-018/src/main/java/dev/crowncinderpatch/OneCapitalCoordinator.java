package dev.crowncinderpatch;

import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/** Coordinates one surface capital per nation and owns the safe world spawn for 0.20. */
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

    public static BlockPos anchor(ServerWorld world) {
        KingdomWorldState state = KingdomWorldState.get(world);
        if (!state.anchorSet) {
            BlockPos vanilla = world.getSpawnPos();
            state.anchorX = vanilla.getX();
            state.anchorZ = vanilla.getZ();
            state.anchorSet = true;
            state.markDirty();
        }
        return new BlockPos(state.anchorX, 0, state.anchorZ);
    }

    /** 0.20 rebuilds once even over a 0.19 world so old buried NPCs are migrated to the surface. */
    public static boolean ensure020(ServerWorld world) {
        KingdomWorldState state = KingdomWorldState.get(world);
        anchor(world);
        if (state.built020) {
            syncWorldSpawn(world);
            return false;
        }
        rebuildAll(world);
        return true;
    }

    public static int rebuildAll(ServerWorld world) {
        BlockPos base = anchor(world);
        for (Site site : SITES) {
            MedievalKingdoms.Nation nation = MedievalKingdoms.nation(site.id());
            if (nation != null) {
                BlockPos rough = base.add(site.dx(), 0, site.dz());
                MedievalKingdoms.rebuild(world, rough, nation);
                placeSignature(world, rough);
            }
        }
        int npcs = NpcBootstrap020.respawnAll(world);
        KingdomWorldState state = KingdomWorldState.get(world);
        state.built020 = true;
        state.markDirty();
        syncWorldSpawn(world);
        return npcs;
    }

    public static int rebuildOne(ServerWorld world, String id) {
        Site site = site(id);
        MedievalKingdoms.Nation nation = MedievalKingdoms.nation(id);
        if (site == null || nation == null) return -1;
        BlockPos base = anchor(world);
        BlockPos rough = base.add(site.dx(), 0, site.dz());
        MedievalKingdoms.rebuild(world, rough, nation);
        placeSignature(world, rough);
        int npcs = NpcBootstrap020.respawnOne(world, id);
        if ("central_empire".equals(id)) syncWorldSpawn(world);
        return npcs;
    }

    /** Safe open plaza inside the central capital. */
    public static BlockPos safeCentralSpawn(ServerWorld world) {
        BlockPos base = anchor(world);
        int x = base.getX();
        int z = base.getZ() + 32;
        world.getChunk(x >> 4, z >> 4);
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        y = Math.max(world.getBottomY() + 8, Math.min(world.getTopY() - 4, y));
        return new BlockPos(x, y, z);
    }

    public static void syncWorldSpawn(ServerWorld world) {
        BlockPos p = safeCentralSpawn(world);
        world.setSpawnPos(p, 180.0f);
    }

    public static void placePlayerAtSafeSpawn(ServerPlayerEntity player, boolean forceFirstSpawn) {
        ServerWorld world = player.getServerWorld();
        BlockPos safe = safeCentralSpawn(world);
        boolean first = !player.getCommandTags().contains("crown020_spawned");
        boolean tooLow = player.getY() <= world.getBottomY() + 12;
        boolean trapped = !world.getBlockState(player.getBlockPos()).isAir() || !world.getBlockState(player.getBlockPos().up()).isAir();
        if (forceFirstSpawn || first || tooLow || trapped) {
            player.teleport(world, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, 180.0f, 0.0f);
            player.addCommandTag("crown020_spawned");
        }
    }

    public static boolean has020Marker(ServerWorld world) {
        KingdomWorldState state = KingdomWorldState.get(world);
        if (state.built020) return true;
        BlockPos base = anchor(world);
        for (int y = world.getBottomY() + 4; y < world.getTopY() - 2; y++) {
            BlockPos p = new BlockPos(base.getX(), y, base.getZ());
            if (world.getBlockState(p).isOf(Blocks.LODESTONE)
                && world.getBlockState(p.east()).isOf(Blocks.EMERALD_BLOCK)
                && world.getBlockState(p.west()).isOf(Blocks.AMETHYST_BLOCK)) return true;
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
