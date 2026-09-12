package dev.crowncinderpatch;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Surface-safe medieval capital builder. It intentionally does not touch any Crown & Cinder monster class,
 * spawn rule or monster texture. Run through /rpg kingdom rebuild... after backing up a world.
 */
public final class MedievalKingdoms {
    private MedievalKingdoms() {}

    public record Nation(String id, String name, int dx, int dz, Palette palette) {}
    public record Palette(Block wall, Block trim, Block roof, Block wood, Block accent, Block road) {}

    private static final Nation[] NATIONS = new Nation[] {
        new Nation("central_empire", "중앙 제국", 0, 0,
            new Palette(Blocks.STONE_BRICKS, Blocks.DEEPSLATE_BRICKS, Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.GOLD_BLOCK, Blocks.POLISHED_ANDESITE)),
        new Nation("aurelia", "오렐리아 왕국", 900, 0,
            new Palette(Blocks.STONE_BRICKS, Blocks.SMOOTH_STONE, Blocks.OAK_PLANKS, Blocks.BIRCH_PLANKS, Blocks.YELLOW_TERRACOTTA, Blocks.GRAVEL)),
        new Nation("eldoria", "엘도리아 마도왕국", -900, 0,
            new Palette(Blocks.DEEPSLATE_BRICKS, Blocks.POLISHED_DEEPSLATE, Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.AMETHYST_BLOCK, Blocks.POLISHED_DEEPSLATE)),
        new Nation("kharum", "카룸 산악국", 0, 900,
            new Palette(Blocks.COBBLED_DEEPSLATE, Blocks.DEEPSLATE_TILES, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.COPPER_BLOCK, Blocks.COBBLESTONE)),
        new Nation("sylvan", "실반 연맹", 0, -900,
            new Palette(Blocks.MOSSY_STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.GREEN_TERRACOTTA, Blocks.COARSE_DIRT)),
        new Nation("varkhan", "바르칸 제국", 650, 650,
            new Palette(Blocks.DEEPSLATE_BRICKS, Blocks.NETHER_BRICKS, Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.RED_TERRACOTTA, Blocks.BLACKSTONE))
    };

    public static Nation nation(String id) {
        for (Nation n : NATIONS) if (n.id.equals(id)) return n;
        return null;
    }

    public static String nationList() {
        StringBuilder b = new StringBuilder();
        for (Nation n : NATIONS) {
            if (!b.isEmpty()) b.append(", ");
            b.append(n.id);
        }
        return b.toString();
    }

    public static void rebuildAll(ServerWorld world) {
        BlockPos spawn = world.getSpawnPos();
        for (Nation n : NATIONS) rebuild(world, spawn.add(n.dx, 0, n.dz), n);
    }

    public static void rebuild(ServerWorld world, BlockPos roughCenter, Nation nation) {
        int y = sampleBuildHeight(world, roughCenter.getX(), roughCenter.getZ());
        BlockPos c = new BlockPos(roughCenter.getX(), y, roughCenter.getZ());
        prepareTerrace(world, c, nation.palette);
        buildRoads(world, c, nation.palette);
        buildOuterWall(world, c, nation.palette);
        buildGatehouse(world, c, nation.palette);
        buildKeep(world, c.add(0, 0, -8), nation.palette, nation.name);
        buildBarracks(world, c.add(27, 0, -10), nation.palette);
        buildChapel(world, c.add(-27, 0, -10), nation.palette);
        buildMarket(world, c.add(0, 0, 22), nation.palette);
        buildStables(world, c.add(28, 0, 22), nation.palette);
        buildHouses(world, c, nation.palette);
        spawnFantasyCitizens(world, c, nation);
        placeMarker(world, c, nation.palette);
    }

    /** Samples an outer ring instead of the old castle footprint so existing/buried buildings do not corrupt Y. */
    public static int sampleBuildHeight(ServerWorld world, int cx, int cz) {
        List<Integer> samples = new ArrayList<>();
        int[] radii = {62, 70, 78};
        for (int r : radii) {
            for (int i = -r; i <= r; i += 14) {
                samples.add(surfaceY(world, cx + i, cz - r));
                samples.add(surfaceY(world, cx + i, cz + r));
                samples.add(surfaceY(world, cx - r, cz + i));
                samples.add(surfaceY(world, cx + r, cz + i));
            }
        }
        samples.removeIf(v -> v <= world.getBottomY() + 2);
        if (samples.isEmpty()) return Math.max(world.getSeaLevel(), world.getBottomY() + 8);
        Collections.sort(samples);
        int median = samples.get(samples.size() / 2);
        return Math.max(world.getBottomY() + 6, Math.min(world.getTopY() - 45, median));
    }

    private static int surfaceY(ServerWorld world, int x, int z) {
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        y = Math.max(y, world.getBottomY() + 1);
        BlockPos.Mutable pos = new BlockPos.Mutable(x, y, z);
        // If top is foliage/log, walk down. Water surface is intentionally accepted and becomes an island terrace.
        for (int i = 0; i < 18 && y > world.getBottomY() + 1; i++) {
            BlockState state = world.getBlockState(pos.set(x, y, z));
            if (!state.isAir() && !state.isIn(BlockTags.LEAVES) && !state.isIn(BlockTags.LOGS)) return y;
            y--;
        }
        return y;
    }

    private static void prepareTerrace(ServerWorld world, BlockPos c, Palette p) {
        final int r = 46;
        final int y = c.getY();
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                int wx = c.getX() + x, wz = c.getZ() + z;
                int old = surfaceY(world, wx, wz);
                // Remove old misplaced city/terrain above the new build plane inside the rebuild footprint.
                for (int yy = y + 1; yy <= Math.min(world.getTopY() - 2, y + 34); yy++) {
                    if (!world.getBlockState(m.set(wx, yy, wz)).isAir()) world.setBlockState(m, Blocks.AIR.getDefaultState(), 2);
                }
                // Cut high terrain down to the terrace plane.
                if (old > y) {
                    for (int yy = y + 1; yy <= Math.min(old, y + 34); yy++) world.setBlockState(m.set(wx, yy, wz), Blocks.AIR.getDefaultState(), 2);
                }
                // Fill valleys/water to the terrace plane. Stone lower core + soil cap.
                int floor = Math.max(world.getBottomY() + 1, Math.min(old, y));
                for (int yy = floor + 1; yy < y; yy++) {
                    Block fill = yy >= y - 3 ? Blocks.DIRT : p.wall;
                    world.setBlockState(m.set(wx, yy, wz), fill.getDefaultState(), 2);
                }
                world.setBlockState(m.set(wx, y, wz), (Math.abs(x) < 43 && Math.abs(z) < 43 ? Blocks.GRASS_BLOCK : p.wall).getDefaultState(), 2);
                // Deep ravine/water support pier so the capital visibly meets terrain instead of floating.
                if (old < y - 6 && ((x + r) % 4 == 0 || (z + r) % 4 == 0)) {
                    for (int yy = y - 1; yy >= Math.max(old, y - 36); yy--) {
                        BlockState s = world.getBlockState(m.set(wx, yy, wz));
                        if (!s.isAir() && s.getFluidState().isEmpty()) break;
                        world.setBlockState(m, p.wall.getDefaultState(), 2);
                    }
                }
            }
        }
    }

    private static void buildRoads(ServerWorld w, BlockPos c, Palette p) {
        for (int i = -40; i <= 40; i++) {
            for (int d = -2; d <= 2; d++) {
                set(w, c.add(d, 0, i), p.road);
                set(w, c.add(i, 0, d), p.road);
            }
        }
        for (int i = -36; i <= 36; i++) {
            set(w, c.add(i, 0, 18), p.road);
            set(w, c.add(i, 0, 19), p.road);
        }
        for (int x = -6; x <= 6; x++) for (int z = 15; z <= 29; z++) set(w, c.add(x, 0, z), p.trim);
    }

    private static void buildOuterWall(ServerWorld w, BlockPos c, Palette p) {
        int r = 44;
        for (int i = -r; i <= r; i++) {
            for (int h = 1; h <= 8; h++) {
                if (!(Math.abs(i) <= 4 && h <= 5)) set(w, c.add(i, h, r), p.wall);
                set(w, c.add(i, h, -r), p.wall);
                set(w, c.add(r, h, i), p.wall);
                set(w, c.add(-r, h, i), p.wall);
            }
            if (i % 2 == 0) {
                set(w, c.add(i, 9, r), p.trim); set(w, c.add(i, 9, -r), p.trim);
                set(w, c.add(r, 9, i), p.trim); set(w, c.add(-r, 9, i), p.trim);
            }
        }
        buildTower(w, c.add(-42, 0, -42), p, 13);
        buildTower(w, c.add(42, 0, -42), p, 13);
        buildTower(w, c.add(-42, 0, 42), p, 13);
        buildTower(w, c.add(42, 0, 42), p, 13);
        for (int x = -38; x <= 38; x += 8) {
            lanternPost(w, c.add(x, 1, -40), p);
            lanternPost(w, c.add(x, 1, 40), p);
        }
    }

    private static void buildTower(ServerWorld w, BlockPos c, Palette p, int height) {
        int r = 4;
        for (int y = 1; y <= height; y++) {
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                boolean edge = Math.abs(x) == r || Math.abs(z) == r;
                if (edge) set(w, c.add(x, y, z), (y % 5 == 0 ? p.trim : p.wall));
                else set(w, c.add(x, y, z), Blocks.AIR);
            }
        }
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            if (Math.abs(x) == r || Math.abs(z) == r || (Math.abs(x) <= 2 && Math.abs(z) <= 2)) set(w, c.add(x, height + 1, z), p.trim);
        }
        for (int x = -r; x <= r; x += 2) {
            set(w, c.add(x, height + 2, -r), p.wall); set(w, c.add(x, height + 2, r), p.wall);
        }
        for (int z = -r; z <= r; z += 2) {
            set(w, c.add(-r, height + 2, z), p.wall); set(w, c.add(r, height + 2, z), p.wall);
        }
    }

    private static void buildGatehouse(ServerWorld w, BlockPos c, Palette p) {
        BlockPos g = c.add(0, 0, 41);
        buildTower(w, g.add(-8, 0, 0), p, 12);
        buildTower(w, g.add(8, 0, 0), p, 12);
        for (int x = -4; x <= 4; x++) for (int y = 6; y <= 11; y++) set(w, g.add(x, y, 0), p.wall);
        for (int x = -3; x <= 3; x++) for (int y = 1; y <= 5; y++) set(w, g.add(x, y, 0), Blocks.AIR);
        for (int x = -4; x <= 4; x++) set(w, g.add(x, 12, 0), p.trim);
        set(w, g.add(0, 10, 1), p.accent);
    }

    private static void buildKeep(ServerWorld w, BlockPos c, Palette p, String nationName) {
        int rx = 15, rz = 12;
        for (int y = 1; y <= 22; y++) {
            for (int x = -rx; x <= rx; x++) for (int z = -rz; z <= rz; z++) {
                boolean edge = Math.abs(x) == rx || Math.abs(z) == rz;
                if (edge) {
                    boolean window = (y == 6 || y == 13 || y == 19) && ((Math.abs(x) % 5 == 0) || (Math.abs(z) % 5 == 0));
                    set(w, c.add(x, y, z), window ? Blocks.GLASS_PANE : (y % 7 == 0 ? p.trim : p.wall));
                } else {
                    set(w, c.add(x, y, z), Blocks.AIR);
                }
            }
        }
        for (int x = -rx + 1; x < rx; x++) for (int z = -rz + 1; z < rz; z++) {
            set(w, c.add(x, 1, z), p.wood);
            set(w, c.add(x, 8, z), p.wood);
            set(w, c.add(x, 15, z), p.wood);
        }
        // Three-floor throne keep with central stair-like stepped ascent.
        for (int i = 0; i < 12; i++) {
            set(w, c.add(-11 + i, 2 + i / 2, 7), p.trim);
            set(w, c.add(-11 + i, 2 + i / 2, 8), p.trim);
        }
        for (int x = -rx; x <= rx; x++) for (int z = -rz; z <= rz; z++) set(w, c.add(x, 23, z), p.roof);
        for (int x = -rx; x <= rx; x += 2) {
            set(w, c.add(x, 24, -rz), p.wall); set(w, c.add(x, 24, rz), p.wall);
        }
        for (int z = -rz; z <= rz; z += 2) {
            set(w, c.add(-rx, 24, z), p.wall); set(w, c.add(rx, 24, z), p.wall);
        }
        // Front portal and throne.
        for (int x = -2; x <= 2; x++) for (int y = 2; y <= 6; y++) set(w, c.add(x, y, rz), Blocks.AIR);
        for (int y = 2; y <= 6; y++) { set(w, c.add(-3, y, rz), p.trim); set(w, c.add(3, y, rz), p.trim); }
        set(w, c.add(0, 2, -8), p.accent);
        set(w, c.add(0, 3, -8), Blocks.OAK_STAIRS);
        set(w, c.add(0, 4, -9), p.accent);
        // Chandeliers.
        for (int z = -5; z <= 5; z += 10) {
            set(w, c.add(0, 20, z), Blocks.CHAIN);
            set(w, c.add(0, 19, z), Blocks.CHAIN);
            set(w, c.add(0, 18, z), Blocks.LANTERN);
        }
        // Treasury chests on second floor.
        treasure(w, c.add(-10, 9, -8));
        treasure(w, c.add(10, 9, -8));
    }

    private static void buildBarracks(ServerWorld w, BlockPos c, Palette p) {
        building(w, c, 11, 8, 7, p.wall, p.wood, p.roof);
        for (int i = -7; i <= 7; i += 4) {
            set(w, c.add(i, 2, -4), Blocks.WHITE_BED);
            set(w, c.add(i, 2, 4), Blocks.WHITE_BED);
        }
        set(w, c.add(0, 2, 0), Blocks.SMITHING_TABLE);
        set(w, c.add(2, 2, 0), Blocks.ANVIL);
    }

    private static void buildChapel(ServerWorld w, BlockPos c, Palette p) {
        building(w, c, 10, 7, 10, p.wall, p.trim, p.roof);
        for (int y = 11; y <= 18; y++) {
            int shrink = (y - 11) / 2;
            for (int x = -3 + shrink; x <= 3 - shrink; x++) set(w, c.add(x, y, 0), p.trim);
        }
        set(w, c.add(0, 6, -7), Blocks.GLASS_PANE);
        set(w, c.add(0, 3, 0), p.accent);
        set(w, c.add(0, 4, 0), Blocks.CANDLE);
    }

    private static void buildMarket(ServerWorld w, BlockPos c, Palette p) {
        for (int x = -15; x <= 15; x += 10) for (int z = -5; z <= 5; z += 10) {
            for (int dx = -3; dx <= 3; dx++) for (int dz = -2; dz <= 2; dz++) set(w, c.add(x + dx, 1, z + dz), Blocks.OAK_PLANKS);
            for (int dx : new int[]{-3, 3}) for (int dz : new int[]{-2, 2}) {
                set(w, c.add(x + dx, 2, z + dz), Blocks.OAK_FENCE);
                set(w, c.add(x + dx, 3, z + dz), Blocks.OAK_FENCE);
            }
            for (int dx = -3; dx <= 3; dx++) for (int dz = -2; dz <= 2; dz++) set(w, c.add(x + dx, 4, z + dz), ((dx + dz) & 1) == 0 ? Blocks.RED_WOOL : Blocks.WHITE_WOOL);
            set(w, c.add(x, 2, z), Blocks.BARREL);
        }
        set(w, c.add(0, 1, 0), p.accent);
        lanternPost(w, c.add(-18, 1, 0), p); lanternPost(w, c.add(18, 1, 0), p);
    }

    private static void buildStables(ServerWorld w, BlockPos c, Palette p) {
        building(w, c, 10, 7, 6, Blocks.COBBLESTONE, p.wood, p.roof);
        for (int z = -5; z <= 5; z += 5) {
            for (int x = -8; x <= 8; x++) set(w, c.add(x, 2, z), Blocks.OAK_FENCE);
            set(w, c.add(0, 2, z), Blocks.OAK_FENCE_GATE);
        }
        set(w, c.add(-7, 2, 0), Blocks.HAY_BLOCK);
        set(w, c.add(7, 2, 0), Blocks.HAY_BLOCK);
    }

    private static void buildHouses(ServerWorld w, BlockPos c, Palette p) {
        int[][] spots = {{-30,24},{-20,31},{20,31},{30,24},{-33,8},{33,8},{-33,-28},{33,-28}};
        for (int i = 0; i < spots.length; i++) {
            BlockPos h = c.add(spots[i][0], 0, spots[i][1]);
            building(w, h, 6, 5, 5 + (i % 2), Blocks.COBBLESTONE, p.wood, p.roof);
            set(w, h.add(0, 2, 0), i % 3 == 0 ? Blocks.CRAFTING_TABLE : (i % 3 == 1 ? Blocks.BARREL : Blocks.LOOM));
            set(w, h.add(3, 2, -4), Blocks.LANTERN);
        }
    }

    private static void building(ServerWorld w, BlockPos c, int rx, int rz, int height, Block lower, Block upper, Block roof) {
        for (int x = -rx; x <= rx; x++) for (int z = -rz; z <= rz; z++) set(w, c.add(x, 1, z), Blocks.SPRUCE_PLANKS);
        for (int y = 2; y <= height; y++) for (int x = -rx; x <= rx; x++) for (int z = -rz; z <= rz; z++) {
            boolean edge = Math.abs(x) == rx || Math.abs(z) == rz;
            if (edge) {
                boolean beam = x % 4 == 0 || z % 4 == 0 || y == 2 || y == height;
                boolean window = y == 4 && ((Math.abs(x) % 4 == 2) || (Math.abs(z) % 4 == 2));
                set(w, c.add(x, y, z), window ? Blocks.GLASS_PANE : (beam ? upper : lower));
            } else set(w, c.add(x, y, z), Blocks.AIR);
        }
        for (int x = -rx - 1; x <= rx + 1; x++) for (int z = -rz - 1; z <= rz + 1; z++) set(w, c.add(x, height + 1, z), roof);
        set(w, c.add(0, 2, rz), Blocks.AIR); set(w, c.add(0, 3, rz), Blocks.AIR);
    }

    private static void lanternPost(ServerWorld w, BlockPos base, Palette p) {
        set(w, base, p.trim);
        set(w, base.up(), Blocks.OAK_FENCE);
        set(w, base.up(2), Blocks.OAK_FENCE);
        set(w, base.up(3), Blocks.LANTERN);
    }

    private static void placeMarker(ServerWorld w, BlockPos c, Palette p) {
        set(w, c.add(0, 1, 38), Blocks.LODESTONE);
        set(w, c.add(-1, 1, 38), p.accent);
        set(w, c.add(1, 1, 38), p.accent);
    }

    private static void treasure(ServerWorld w, BlockPos pos) {
        set(w, pos, Blocks.CHEST);
        if (w.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            chest.setStack(0, new ItemStack(Items.EMERALD, 16));
            chest.setStack(1, new ItemStack(Items.GOLD_INGOT, 12));
            chest.setStack(2, new ItemStack(Items.IRON_INGOT, 24));
            chest.setStack(3, new ItemStack(Items.GOLDEN_APPLE, 2));
            chest.setStack(4, new ItemStack(Items.DIAMOND, 3));
            chest.markDirty();
        }
    }

    private static void spawnFantasyCitizens(ServerWorld world, BlockPos c, Nation n) {
        String[] roles = {"왕실 시종","대장장이","갑옷 장인","여관주인","상인","사제","서기관","농부","약초상","마구간지기","귀족","빵집 주인","석공","경비 보급관"};
        VillagerProfession[] jobs = {VillagerProfession.LIBRARIAN, VillagerProfession.WEAPONSMITH, VillagerProfession.ARMORER, VillagerProfession.BUTCHER,
            VillagerProfession.CARTOGRAPHER, VillagerProfession.CLERIC, VillagerProfession.LIBRARIAN, VillagerProfession.FARMER,
            VillagerProfession.CLERIC, VillagerProfession.LEATHERWORKER, VillagerProfession.CARTOGRAPHER, VillagerProfession.FARMER,
            VillagerProfession.MASON, VillagerProfession.TOOLSMITH};
        int[][] pos = {{-4,9},{4,9},{27,-10},{2,22},{-8,22},{-27,-10},{-5,-3},{-30,24},{-20,31},{28,22},{5,-10},{20,31},{33,8},{12,4}};
        for (int i = 0; i < roles.length; i++) {
            VillagerEntity v = EntityType.VILLAGER.create(world);
            if (v == null) continue;
            v.setVillagerData(v.getVillagerData().withType(VillagerType.PLAINS).withProfession(jobs[i]).withLevel(5));
            v.refreshPositionAndAngles(c.getX() + pos[i][0] + 0.5, c.getY() + 2, c.getZ() + pos[i][1] + 0.5, world.random.nextFloat() * 360f, 0f);
            v.setCustomName(Text.literal("§6[" + n.name + "] §f" + roles[i]));
            v.setCustomNameVisible(i <= 5);
            v.setPersistent();
            // Visible fantasy headgear using vanilla assets keeps E-key/inventory rendering stable.
            ItemStack head = switch (i % 5) {
                case 0 -> new ItemStack(Items.GOLDEN_HELMET);
                case 1 -> new ItemStack(Items.IRON_HELMET);
                case 2 -> new ItemStack(Items.CHAINMAIL_HELMET);
                case 3 -> new ItemStack(Items.LEATHER_HELMET);
                default -> ItemStack.EMPTY;
            };
            if (!head.isEmpty()) v.equipStack(EquipmentSlot.HEAD, head);
            world.spawnEntity(v);
        }
    }

    private static void set(ServerWorld w, BlockPos pos, Block block) {
        w.setBlockState(pos, block.getDefaultState(), 2);
    }
}
