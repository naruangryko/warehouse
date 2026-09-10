package dev.crowncinder.world;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Builds one large neutral royal castle around the world spawn. */
public final class CentralCastle {
    private CentralCastle() {}

    private static BlockPos origin(ServerWorld world) {
        return world.getSpawnPos().add(0, -1, 0);
    }

    public static boolean isBuilt(ServerWorld world) {
        BlockPos o = origin(world);
        return world.getBlockState(o.add(0, -2, 0)).isOf(Blocks.LODESTONE)
                && world.getBlockState(o.add(1, -2, 0)).isOf(Blocks.BEDROCK);
    }

    public static void ensure(ServerWorld world) {
        if (!isBuilt(world)) build(world, false);
    }

    public static void build(ServerWorld world, boolean force) {
        if (!force && isBuilt(world)) return;
        BlockPos o = origin(world);

        for (int x = -37; x <= 37; x++) {
            for (int z = -37; z <= 37; z++) {
                set(world, o.add(x, 0, z), (Math.abs(x) <= 4 || Math.abs(z) <= 4) ? Blocks.SMOOTH_STONE : Blocks.STONE_BRICKS);
                for (int y = 1; y <= 20; y++) set(world, o.add(x, y, z), Blocks.AIR);
            }
        }

        set(world, o.add(0, -2, 0), Blocks.LODESTONE);
        set(world, o.add(1, -2, 0), Blocks.BEDROCK);

        wallRect(world, o, -36, 1, -36, 36, 9, 36, Blocks.STONE_BRICKS);
        gatehouse(world, o.add(0, 0, -36));
        tower(world, o.add(-31, 0, -31), 6, 18, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.GOLD_BLOCK);
        tower(world, o.add(31, 0, -31), 6, 18, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.GOLD_BLOCK);
        tower(world, o.add(-31, 0, 31), 6, 18, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.GOLD_BLOCK);
        tower(world, o.add(31, 0, 31), 6, 18, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.GOLD_BLOCK);

        grandKeep(world, o.add(0, 0, 15));

        hall(world, o.add(-22, 0, 6), 17, 13, Blocks.STONE_BRICKS, Blocks.BLUE_WOOL);
        hall(world, o.add(22, 0, 6), 17, 13, Blocks.STONE_BRICKS, Blocks.RED_WOOL);
        hall(world, o.add(-22, 0, -17), 15, 11, Blocks.POLISHED_ANDESITE, Blocks.GRAY_WOOL);
        hall(world, o.add(22, 0, -17), 15, 11, Blocks.QUARTZ_BLOCK, Blocks.BLUE_WOOL);

        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
            if (x * x + z * z <= 25) set(world, o.add(x, 1, z), Blocks.QUARTZ_BLOCK);
        }
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            if (x * x + z * z <= 9) set(world, o.add(x, 2, z), Blocks.WATER);
        }
        set(world, o.add(0, 2, 0), Blocks.SEA_LANTERN);
        set(world, o.add(0, 3, 0), Blocks.QUARTZ_PILLAR);
        set(world, o.add(0, 4, 0), Blocks.GOLD_BLOCK);

        for (int z = -35; z <= 35; z++) for (int x = -3; x <= 3; x++) set(world, o.add(x, 1, z), Blocks.SMOOTH_STONE);
        for (int x = -35; x <= 35; x++) for (int z = -3; z <= 3; z++) set(world, o.add(x, 1, z), Blocks.SMOOTH_STONE);
    }

    private static void grandKeep(ServerWorld w, BlockPos c) {
        int hx = 17, hz = 12;
        for (int y = 1; y <= 11; y++) {
            for (int x = -hx; x <= hx; x++) for (int z = -hz; z <= hz; z++) {
                boolean edge = Math.abs(x) == hx || Math.abs(z) == hz;
                set(w, c.add(x, y, z), edge ? Blocks.STONE_BRICKS : Blocks.AIR);
            }
        }
        fill(w, c, -hx - 1, 12, -hz - 1, hx + 1, 12, hz + 1, Blocks.DEEPSLATE_TILES);
        fill(w, c, -3, 1, -hz, 3, 6, -hz, Blocks.AIR);
        tower(w, c.add(-13, 0, -9), 5, 16, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.GOLD_BLOCK);
        tower(w, c.add(13, 0, -9), 5, 16, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.GOLD_BLOCK);
        fill(w, c, -5, 1, 6, 5, 2, 10, Blocks.QUARTZ_BLOCK);
        fill(w, c, -2, 3, 8, 2, 6, 10, Blocks.BLUE_WOOL);
        set(w, c.add(0, 3, 7), Blocks.GOLD_BLOCK);
    }

    private static void gatehouse(ServerWorld w, BlockPos c) {
        fill(w, c, -10, 1, -2, 10, 12, 2, Blocks.STONE_BRICKS);
        fill(w, c, -4, 1, -3, 4, 7, 3, Blocks.AIR);
        for (int y = 1; y <= 7; y++) for (int x = -4; x <= 4; x += 2) set(w, c.add(x, y, 0), Blocks.IRON_BARS);
        tower(w, c.add(-9, 0, 0), 4, 15, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.GOLD_BLOCK);
        tower(w, c.add(9, 0, 0), 4, 15, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.GOLD_BLOCK);
        set(w, c.add(0, 13, 0), Blocks.GOLD_BLOCK);
    }

    private static void hall(ServerWorld w, BlockPos c, int sx, int sz, Block wall, Block roof) {
        int hx = sx / 2, hz = sz / 2;
        fill(w, c, -hx, 0, -hz, hx, 0, hz, Blocks.SMOOTH_STONE);
        for (int y = 1; y <= 6; y++) for (int x = -hx; x <= hx; x++) for (int z = -hz; z <= hz; z++) {
            boolean edge = Math.abs(x) == hx || Math.abs(z) == hz;
            set(w, c.add(x, y, z), edge ? wall : Blocks.AIR);
        }
        fill(w, c, -hx - 1, 7, -hz - 1, hx + 1, 7, hz + 1, roof);
        fill(w, c, -1, 1, -hz, 1, 3, -hz, Blocks.AIR);
    }

    private static void tower(ServerWorld w, BlockPos c, int r, int h, Block wall, Block roof, Block cap) {
        for (int y = 1; y <= h; y++) for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            boolean edge = Math.abs(x) == r || Math.abs(z) == r;
            set(w, c.add(x, y, z), edge ? wall : Blocks.AIR);
        }
        fill(w, c, -r - 1, h + 1, -r - 1, r + 1, h + 1, r + 1, roof);
        set(w, c.add(0, h + 2, 0), cap);
    }

    private static void wallRect(ServerWorld w, BlockPos o, int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
        for (int y = y1; y <= y2; y++) {
            fill(w, o, x1, y, z1, x2, y, z1, b); fill(w, o, x1, y, z2, x2, y, z2, b);
            fill(w, o, x1, y, z1, x1, y, z2, b); fill(w, o, x2, y, z1, x2, y, z2, b);
        }
    }

    private static void fill(ServerWorld w, BlockPos o, int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(w, o.add(x, y, z), b);
    }
    private static void set(ServerWorld w, BlockPos p, Block b) { w.setBlockState(p, b.getDefaultState(), 3); }
}
