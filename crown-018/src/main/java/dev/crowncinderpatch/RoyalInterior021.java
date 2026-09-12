package dev.crowncinderpatch;

import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Adds a furnished three-floor medieval palace interior after the capital shell is built. */
public final class RoyalInterior021 {
    private RoyalInterior021() {}

    public static void decorate(ServerWorld world, BlockPos rough, MedievalKingdoms.Nation nation) {
        int y = MedievalKingdoms.sampleBuildHeight(world, rough.getX(), rough.getZ());
        BlockPos keep = new BlockPos(rough.getX(), y, rough.getZ() - 8);

        // Main hall lighting and royal aisle (1F: floor y+1, standing y+2)
        carpet(world, keep, 0, 2, 8, 1);
        for (int z = -7; z <= 7; z += 7) chandelier(world, keep.add(0, 21, z));
        for (int z = -7; z <= 7; z += 4) {
            set(world, keep.add(-12, 2, z), Blocks.WALL_TORCH);
            set(world, keep.add(12, 2, z), Blocks.WALL_TORCH);
        }
        // Throne dais and audience furniture
        for (int x = -3; x <= 3; x++) for (int z = -10; z <= -7; z++) set(world, keep.add(x, 2, z), Blocks.RED_CARPET);
        set(world, keep.add(0, 2, -10), nation.palette().accent());
        set(world, keep.add(0, 3, -9), Blocks.DARK_OAK_STAIRS);
        set(world, keep.add(-1, 3, -9), Blocks.GOLD_BLOCK);
        set(world, keep.add(1, 3, -9), Blocks.GOLD_BLOCK);
        set(world, keep.add(0, 4, -10), Blocks.GOLD_BLOCK);
        for (int x : new int[]{-8, 8}) {
            table(world, keep.add(x, 2, -2));
            set(world, keep.add(x, 2, 4), Blocks.BARREL);
            set(world, keep.add(x, 2, 6), Blocks.ANVIL);
        }
        set(world, keep.add(-10, 2, 8), Blocks.SMITHING_TABLE);
        set(world, keep.add(10, 2, 8), Blocks.LECTERN);

        // 1F -> 2F staircase
        stairs(world, keep.add(-10, 2, 8), Direction.EAST, 7);
        // 2F -> 3F staircase
        stairs(world, keep.add(4, 9, 8), Direction.EAST, 7);

        // 2F council / library
        for (int z = -8; z <= 8; z += 4) {
            set(world, keep.add(-13, 9, z), Blocks.BOOKSHELF);
            set(world, keep.add(13, 9, z), Blocks.BOOKSHELF);
            set(world, keep.add(-13, 10, z), Blocks.BOOKSHELF);
            set(world, keep.add(13, 10, z), Blocks.BOOKSHELF);
        }
        for (int x = -6; x <= 6; x += 3) table(world, keep.add(x, 9, 0));
        set(world, keep.add(0, 9, -7), Blocks.LECTERN);
        set(world, keep.add(-6, 9, -7), Blocks.CARTOGRAPHY_TABLE);
        set(world, keep.add(6, 9, -7), Blocks.CRAFTING_TABLE);
        set(world, keep.add(-10, 9, 7), Blocks.CHEST);
        set(world, keep.add(10, 9, 7), Blocks.CHEST);
        lantern(world, keep.add(-6, 13, 0));
        lantern(world, keep.add(6, 13, 0));

        // 3F royal chamber / strategy room
        set(world, keep.add(-8, 16, -6), Blocks.RED_BED);
        set(world, keep.add(-7, 16, -6), Blocks.RED_BED);
        set(world, keep.add(-10, 16, -8), Blocks.CHEST);
        set(world, keep.add(-10, 16, -5), Blocks.ENDER_CHEST);
        set(world, keep.add(-4, 16, -8), Blocks.BOOKSHELF);
        set(world, keep.add(-3, 16, -8), Blocks.BOOKSHELF);
        set(world, keep.add(7, 16, -5), Blocks.CARTOGRAPHY_TABLE);
        set(world, keep.add(9, 16, -5), Blocks.LECTERN);
        table(world, keep.add(7, 16, 2));
        table(world, keep.add(10, 16, 2));
        set(world, keep.add(8, 16, 6), Blocks.BARREL);
        set(world, keep.add(10, 16, 6), Blocks.BARREL);
        for (int x = -10; x <= 10; x += 5) set(world, keep.add(x, 16, 9), Blocks.BLUE_CARPET);
        lantern(world, keep.add(-7, 20, 1));
        lantern(world, keep.add(7, 20, 1));

        // Medieval clutter and banners feeling using safe vanilla blocks.
        for (int x : new int[]{-11, 11}) {
            set(world, keep.add(x, 2, -8), Blocks.CAULDRON);
            set(world, keep.add(x, 9, 5), Blocks.FLOWER_POT);
            set(world, keep.add(x, 16, 5), Blocks.CANDLE);
        }
    }

    private static void carpet(ServerWorld w, BlockPos c, int x, int y, int zRadius, int xRadius) {
        for (int dx = -xRadius; dx <= xRadius; dx++) for (int z = -zRadius; z <= zRadius; z++) set(w, c.add(x + dx, y, z), Blocks.RED_CARPET);
    }

    private static void stairs(ServerWorld w, BlockPos start, Direction facing, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos p = start.add(i, i, 0);
            w.setBlockState(p, Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, facing), 2);
            set(w, p.down(), Blocks.SPRUCE_PLANKS);
            set(w, p.up(), Blocks.AIR);
        }
    }

    private static void table(ServerWorld w, BlockPos p) {
        set(w, p, Blocks.SPRUCE_FENCE);
        set(w, p.up(), Blocks.DARK_OAK_PRESSURE_PLATE);
    }

    private static void chandelier(ServerWorld w, BlockPos top) {
        set(w, top, Blocks.CHAIN);
        set(w, top.down(), Blocks.CHAIN);
        set(w, top.down(2), Blocks.LANTERN);
    }

    private static void lantern(ServerWorld w, BlockPos p) {
        set(w, p, Blocks.CHAIN);
        set(w, p.down(), Blocks.LANTERN);
    }

    private static void set(ServerWorld w, BlockPos p, net.minecraft.block.Block b) {
        w.setBlockState(p, b.getDefaultState(), 2);
    }
}
