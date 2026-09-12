package dev.crowncinderpatch;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One-capital-per-nation builder for Crown & Cinder 0.19.
 *
 * Important design rule: a capital is generated once on one surface terrace. Before a rebuild, legacy
 * named NPCs are removed and the old underground hollow layer is sealed so rulers cannot remain trapped
 * under the new city.
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

    /** The central marker is the single source of truth: no second capital is generated above/below it. */
    public static boolean hasGenerated(ServerWorld world) {
        BlockPos spawn = world.getSpawnPos();
        for (int y = world.getBottomY() + 4; y < world.getTopY() - 2; y++) {
            if (world.getBlockState(new BlockPos(spawn.getX(), y, spawn.getZ())).isOf(Blocks.LODESTONE)) return true;
        }
        return false;
    }

    public static void ensureFreshWorld(ServerWorld world) {
        if (hasGenerated(world) || world.getTime() > 6000L) return;
        rebuildAll(world);
    }

    public static void rebuildAll(ServerWorld world) {
        BlockPos spawn = world.getSpawnPos();
        for (Nation n : NATIONS) rebuild(world, spawn.add(n.dx, 0, n.dz), n);
    }

    public static void rebuild(ServerWorld world, BlockPos roughCenter, Nation nation) {
        int y = sampleBuildHeight(world, roughCenter.getX(), roughCenter.getZ());
        BlockPos c = new BlockPos(roughCenter.getX(), y, roughCenter.getZ());

        clearLegacyCapitalEntities(world, c);
        prepareSingleSurface(world, c, nation.palette);

        buildRoads(world, c, nation.palette);
        buildInnerWall(world, c, nation.palette);
        buildInnerGatehouse(world, c, nation.palette);
        buildOuterCityWall(world, c, nation.palette);
        buildOuterGatehouse(world, c, nation.palette);

        buildKeep(world, c.add(0, 0, -8), nation.palette);
        buildChapel(world, c.add(-25, 0, -12), nation.palette);
        buildMarket(world, c.add(0, 0, 22), nation.palette);
        buildStables(world, c.add(27, 0, 18), nation.palette);
        buildGuildHall(world, c.add(-54, 0, -22), nation.palette, true);
        buildGuildHall(world, c.add(54, 0, -22), nation.palette, false);
        buildTavern(world, c.add(-54, 0, 20), nation.palette);
        buildSmithy(world, c.add(54, 0, 20), nation.palette);
        buildOuterVillage(world, c, nation.palette);

        spawnFantasyCitizens(world, c, nation);
        placeMarker(world, c);
    }

    /**
     * Samples untouched terrain well outside the old inner castle. This prevents an already-buried castle roof
     * from being mistaken for the natural ground height.
     */
    public static int sampleBuildHeight(ServerWorld world, int cx, int cz) {
        List<Integer> samples = new ArrayList<>();
        int[] radii = {82, 90, 98};
        for (int r : radii) {
            for (int i = -r; i <= r; i += 12) {
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
        return Math.max(world.getBottomY() + 8, Math.min(world.getTopY() - 48, median));
    }

    private static int surfaceY(ServerWorld world, int x, int z) {
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        y = Math.max(y, world.getBottomY() + 1);
        BlockPos.Mutable p = new BlockPos.Mutable(x, y, z);
        for (int i = 0; i < 28 && y > world.getBottomY() + 1; i++) {
            BlockState state = world.getBlockState(p.set(x, y, z));
            if (!state.isAir() && !state.isIn(BlockTags.LEAVES) && !state.isIn(BlockTags.LOGS)) return y;
            y--;
        }
        return y;
    }

    /**
     * Creates exactly one build plane. The old hollow layer directly beneath the inner city is replaced by a
     * solid foundation, while anything above the new plane is cleared before new buildings are placed.
     */
    private static void prepareSingleSurface(ServerWorld world, BlockPos c, Palette p) {
        final int r = 72;
        final int y = c.getY();
        BlockPos.Mutable m = new BlockPos.Mutable();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                int wx = c.getX() + x;
                int wz = c.getZ() + z;
                int old = surfaceY(world, wx, wz);
                int top = Math.min(world.getTopY() - 2, Math.max(old, y + 30));

                // Nothing from an old capital is allowed to remain above the new city plane.
                for (int yy = y + 1; yy <= top; yy++) {
                    if (!world.getBlockState(m.set(wx, yy, wz)).isAir()) {
                        world.setBlockState(m, Blocks.AIR.getDefaultState(), 2);
                    }
                }

                // Fill from the natural terrain up to the city plane.
                int natural = Math.max(world.getBottomY() + 1, Math.min(old, y));
                for (int yy = natural + 1; yy < y; yy++) {
                    Block fill = yy >= y - 3 ? Blocks.DIRT : p.wall;
                    world.setBlockState(m.set(wx, yy, wz), fill.getDefaultState(), 2);
                }

                // In the inner city, overwrite the old underground hollow castle layer. This removes the
                // below-ground duplicate that trapped rulers/NPCs in older versions.
                if (Math.abs(x) <= 46 && Math.abs(z) <= 46) {
                    int low = Math.max(world.getBottomY() + 1, y - 14);
                    for (int yy = low; yy < y - 3; yy++) {
                        world.setBlockState(m.set(wx, yy, wz), p.wall.getDefaultState(), 2);
                    }
                    for (int yy = Math.max(low, y - 3); yy < y; yy++) {
                        world.setBlockState(m.set(wx, yy, wz), Blocks.DIRT.getDefaultState(), 2);
                    }
                }

                world.setBlockState(m.set(wx, y, wz), Blocks.GRASS_BLOCK.getDefaultState(), 2);
            }
        }
    }

    private static void buildRoads(ServerWorld w, BlockPos c, Palette p) {
        for (int i = -66; i <= 66; i++) {
            for (int d = -2; d <= 2; d++) {
                set(w, c.add(d, 0, i), p.road);
                set(w, c.add(i, 0, d), p.road);
            }
        }
        for (int ring : new int[]{-52, 52}) {
            for (int i = -62; i <= 62; i++) {
                set(w, c.add(i, 0, ring), p.road);
                set(w, c.add(ring, 0, i), p.road);
            }
        }
        for (int x = -7; x <= 7; x++) for (int z = 14; z <= 30; z++) set(w, c.add(x, 0, z), p.trim);
    }

    private static void buildInnerWall(ServerWorld w, BlockPos c, Palette p) {
        buildSquareWall(w, c, p, 42, 8);
        buildTower(w, c.add(-40, 0, -40), p, 13);
        buildTower(w, c.add(40, 0, -40), p, 13);
        buildTower(w, c.add(-40, 0, 40), p, 13);
        buildTower(w, c.add(40, 0, 40), p, 13);
    }

    private static void buildOuterCityWall(ServerWorld w, BlockPos c, Palette p) {
        buildSquareWall(w, c, p, 68, 10);
        int t = 66;
        buildTower(w, c.add(-t, 0, -t), p, 16);
        buildTower(w, c.add(t, 0, -t), p, 16);
        buildTower(w, c.add(-t, 0, t), p, 16);
        buildTower(w, c.add(t, 0, t), p, 16);
        buildTower(w, c.add(0, 0, -67), p, 14);
        buildTower(w, c.add(-67, 0, 0), p, 14);
        buildTower(w, c.add(67, 0, 0), p, 14);
    }

    private static void buildSquareWall(ServerWorld w, BlockPos c, Palette p, int r, int hMax) {
        for (int i = -r; i <= r; i++) {
            for (int h = 1; h <= hMax; h++) {
                boolean southGate = Math.abs(i) <= 4 && h <= 6;
                if (!southGate) set(w, c.add(i, h, r), p.wall);
                set(w, c.add(i, h, -r), p.wall);
                set(w, c.add(r, h, i), p.wall);
                set(w, c.add(-r, h, i), p.wall);
            }
            if ((i & 1) == 0) {
                set(w, c.add(i, hMax + 1, r), p.trim);
                set(w, c.add(i, hMax + 1, -r), p.trim);
                set(w, c.add(r, hMax + 1, i), p.trim);
                set(w, c.add(-r, hMax + 1, i), p.trim);
            }
        }
    }

    private static void buildInnerGatehouse(ServerWorld w, BlockPos c, Palette p) {
        buildGatehouse(w, c.add(0, 0, 40), p, 11);
    }

    private static void buildOuterGatehouse(ServerWorld w, BlockPos c, Palette p) {
        buildGatehouse(w, c.add(0, 0, 66), p, 15);
    }

    private static void buildGatehouse(ServerWorld w, BlockPos g, Palette p, int height) {
        buildTower(w, g.add(-9, 0, 0), p, height);
        buildTower(w, g.add(9, 0, 0), p, height);
        for (int x = -4; x <= 4; x++) for (int y = 7; y <= height; y++) set(w, g.add(x, y, 0), p.wall);
        for (int x = -3; x <= 3; x++) for (int y = 1; y <= 6; y++) set(w, g.add(x, y, 0), Blocks.AIR);
        for (int x = -5; x <= 5; x++) set(w, g.add(x, height + 1, 0), p.trim);
        set(w, g.add(0, height - 2, 1), p.accent);
    }

    private static void buildTower(ServerWorld w, BlockPos c, Palette p, int height) {
        int r = 4;
        for (int y = 1; y <= height; y++) {
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                boolean edge = Math.abs(x) == r || Math.abs(z) == r;
                set(w, c.add(x, y, z), edge ? (y % 5 == 0 ? p.trim : p.wall) : Blocks.AIR);
            }
        }
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            if (Math.abs(x) == r || Math.abs(z) == r) set(w, c.add(x, height + 1, z), p.trim);
        }
        for (int x = -r; x <= r; x += 2) {
            set(w, c.add(x, height + 2, -r), p.wall);
            set(w, c.add(x, height + 2, r), p.wall);
        }
        for (int z = -r; z <= r; z += 2) {
            set(w, c.add(-r, height + 2, z), p.wall);
            set(w, c.add(r, height + 2, z), p.wall);
        }
    }

    private static void buildKeep(ServerWorld w, BlockPos c, Palette p) {
        building(w, c, 15, 12, 20, p.wall, p.trim, p.roof);
        for (int x = -13; x <= 13; x++) for (int z = -10; z <= 10; z++) {
            set(w, c.add(x, 8, z), p.wood);
            set(w, c.add(x, 15, z), p.wood);
        }
        for (int i = 0; i < 11; i++) {
            set(w, c.add(-10 + i, 2 + i / 2, 7), p.trim);
            set(w, c.add(-10 + i, 2 + i / 2, 8), p.trim);
        }
        // Throne area on the same, above-ground first floor.
        set(w, c.add(0, 2, -8), p.accent);
        set(w, c.add(0, 3, -8), Blocks.OAK_STAIRS);
        set(w, c.add(0, 4, -9), p.accent);
        set(w, c.add(0, 18, 0), Blocks.LANTERN);
        treasure(w, c.add(-10, 9, -8));
        treasure(w, c.add(10, 9, -8));
    }

    private static void buildChapel(ServerWorld w, BlockPos c, Palette p) {
        building(w, c, 10, 7, 10, p.wall, p.trim, p.roof);
        set(w, c.add(0, 3, 0), p.accent);
        set(w, c.add(0, 4, 0), Blocks.CANDLE);
    }

    private static void buildMarket(ServerWorld w, BlockPos c, Palette p) {
        for (int x = -15; x <= 15; x += 10) for (int z = -5; z <= 5; z += 10) {
            stall(w, c.add(x, 0, z), ((x + z) & 1) == 0 ? Blocks.RED_WOOL : Blocks.BLUE_WOOL);
        }
        lanternPost(w, c.add(-18, 1, 0), p);
        lanternPost(w, c.add(18, 1, 0), p);
    }

    private static void stall(ServerWorld w, BlockPos c, Block cloth) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -2; dz <= 2; dz++) set(w, c.add(dx, 1, dz), Blocks.OAK_PLANKS);
        for (int dx : new int[]{-3, 3}) for (int dz : new int[]{-2, 2}) {
            set(w, c.add(dx, 2, dz), Blocks.OAK_FENCE);
            set(w, c.add(dx, 3, dz), Blocks.OAK_FENCE);
        }
        for (int dx = -3; dx <= 3; dx++) for (int dz = -2; dz <= 2; dz++) set(w, c.add(dx, 4, dz), cloth);
        set(w, c.add(0, 2, 0), Blocks.BARREL);
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

    private static void buildGuildHall(ServerWorld w, BlockPos c, Palette p, boolean knights) {
        building(w, c, 10, 8, 8, p.wall, p.wood, p.roof);
        for (int x = -7; x <= 7; x += 4) {
            set(w, c.add(x, 2, -5), knights ? Blocks.BLUE_BED : Blocks.RED_BED);
            set(w, c.add(x, 2, 5), knights ? Blocks.BLUE_BED : Blocks.RED_BED);
        }
        set(w, c.add(0, 2, 0), knights ? Blocks.SMITHING_TABLE : Blocks.CARTOGRAPHY_TABLE);
        set(w, c.add(2, 2, 0), Blocks.ANVIL);
        set(w, c.add(-2, 2, 0), Blocks.BARREL);
        set(w, c.add(0, 6, 0), knights ? Blocks.BLUE_BANNER : Blocks.RED_BANNER);
    }

    private static void buildTavern(ServerWorld w, BlockPos c, Palette p) {
        building(w, c, 9, 7, 7, Blocks.COBBLESTONE, p.wood, p.roof);
        for (int x = -5; x <= 5; x += 5) {
            set(w, c.add(x, 2, 0), Blocks.OAK_FENCE);
            set(w, c.add(x, 3, 0), Blocks.OAK_PRESSURE_PLATE);
        }
        set(w, c.add(0, 2, -4), Blocks.BARREL);
        set(w, c.add(2, 2, -4), Blocks.SMOKER);
        set(w, c.add(-2, 2, -4), Blocks.BREWING_STAND);
    }

    private static void buildSmithy(ServerWorld w, BlockPos c, Palette p) {
        building(w, c, 9, 7, 7, Blocks.STONE_BRICKS, p.wood, p.roof);
        set(w, c.add(-3, 2, 0), Blocks.BLAST_FURNACE);
        set(w, c.add(0, 2, 0), Blocks.ANVIL);
        set(w, c.add(3, 2, 0), Blocks.SMITHING_TABLE);
        set(w, c.add(0, 2, -4), Blocks.LAVA_CAULDRON);
    }

    private static void buildOuterVillage(ServerWorld w, BlockPos c, Palette p) {
        int[][] spots = {
            {-54,45},{-36,54},{-15,54},{15,54},{36,54},{54,45},
            {-54,5},{54,5},{-54,-45},{-36,-54},{-15,-54},{15,-54},{36,-54},{54,-45}
        };
        for (int i = 0; i < spots.length; i++) {
            BlockPos h = c.add(spots[i][0], 0, spots[i][1]);
            building(w, h, 6, 5, 5 + (i % 2), Blocks.COBBLESTONE, p.wood, p.roof);
            Block work = switch (i % 6) {
                case 0 -> Blocks.LOOM;
                case 1 -> Blocks.BARREL;
                case 2 -> Blocks.FLETCHING_TABLE;
                case 3 -> Blocks.COMPOSTER;
                case 4 -> Blocks.CRAFTING_TABLE;
                default -> Blocks.STONECUTTER;
            };
            set(w, h.add(0, 2, 0), work);
            set(w, h.add(3, 2, -3), Blocks.LANTERN);
            set(w, h.add(-2, 2, 2), i % 2 == 0 ? Blocks.WHITE_BED : Blocks.BROWN_BED);
        }
    }

    /** Building floor is always at cY+1; NPC helper below verifies this floor before spawning. */
    private static void building(ServerWorld w, BlockPos c, int rx, int rz, int height, Block lower, Block upper, Block roof) {
        for (int x = -rx; x <= rx; x++) for (int z = -rz; z <= rz; z++) set(w, c.add(x, 1, z), Blocks.SPRUCE_PLANKS);
        for (int y = 2; y <= height; y++) for (int x = -rx; x <= rx; x++) for (int z = -rz; z <= rz; z++) {
            boolean edge = Math.abs(x) == rx || Math.abs(z) == rz;
            if (edge) {
                boolean beam = x % 4 == 0 || z % 4 == 0 || y == 2 || y == height;
                boolean window = y == 4 && ((Math.abs(x) % 4 == 2) || (Math.abs(z) % 4 == 2));
                set(w, c.add(x, y, z), window ? Blocks.GLASS_PANE : (beam ? upper : lower));
            } else {
                set(w, c.add(x, y, z), Blocks.AIR);
            }
        }
        for (int x = -rx - 1; x <= rx + 1; x++) for (int z = -rz - 1; z <= rz + 1; z++) set(w, c.add(x, height + 1, z), roof);
        set(w, c.add(0, 2, rz), Blocks.AIR);
        set(w, c.add(0, 3, rz), Blocks.AIR);
    }

    private static void spawnFantasyCitizens(ServerWorld world, BlockPos c, Nation n) {
        String ruler = "central_empire".equals(n.id) ? "황제" : "국왕";
        String[] roles = {
            ruler,"왕실 시종","궁정 서기관","재무관","왕실 사제","왕실 요리사","왕궁 하인",
            "기사단장","기사 부단장","왕실 기사","왕실 기사","기사 교관","기사 보급관",
            "용병단장","베테랑 용병","베테랑 용병","정찰 용병","용병 접수원","현상금 담당관",
            "여관주인","주방장","음유시인","대장장이","갑옷 장인","도구 장인",
            "시장 상인","약초상","빵집 주인","직물 상인","지도 제작자","가죽 장인",
            "농부","농부","석공","어부","마구간지기","마을 촌장",
            "주민","주민","주민","주민","주민","주민","주민","주민"
        };
        int[][] pos = {
            {0,-8},{-4,-5},{4,-5},{-5,-2},{-25,-12},{2,-12},{-2,-4},
            {-54,-22},{-51,-22},{-57,-18},{-51,-18},{-57,-26},{-51,-26},
            {54,-22},{51,-22},{57,-18},{51,-18},{57,-26},{51,-26},
            {-54,20},{-51,20},{-57,23},{54,20},{51,20},{57,20},
            {-10,22},{0,22},{10,22},{-15,27},{15,27},{20,22},
            {-54,45},{-36,54},{-15,54},{15,54},{36,54},{54,45},
            {-54,5},{54,5},{-54,-45},{-36,-54},{36,-54},{54,-45},{-15,-54},{15,-54}
        };
        VillagerProfession[] jobs = {
            VillagerProfession.CARTOGRAPHER, VillagerProfession.LIBRARIAN, VillagerProfession.LIBRARIAN, VillagerProfession.CARTOGRAPHER, VillagerProfession.CLERIC, VillagerProfession.BUTCHER, VillagerProfession.FARMER,
            VillagerProfession.WEAPONSMITH, VillagerProfession.ARMORER, VillagerProfession.WEAPONSMITH, VillagerProfession.ARMORER, VillagerProfession.WEAPONSMITH, VillagerProfession.TOOLSMITH,
            VillagerProfession.WEAPONSMITH, VillagerProfession.LEATHERWORKER, VillagerProfession.FLETCHER, VillagerProfession.FLETCHER, VillagerProfession.CARTOGRAPHER, VillagerProfession.LIBRARIAN,
            VillagerProfession.BUTCHER, VillagerProfession.FARMER, VillagerProfession.LIBRARIAN, VillagerProfession.WEAPONSMITH, VillagerProfession.ARMORER, VillagerProfession.TOOLSMITH,
            VillagerProfession.CARTOGRAPHER, VillagerProfession.CLERIC, VillagerProfession.FARMER, VillagerProfession.SHEPHERD, VillagerProfession.CARTOGRAPHER, VillagerProfession.LEATHERWORKER,
            VillagerProfession.FARMER, VillagerProfession.FARMER, VillagerProfession.MASON, VillagerProfession.FISHERMAN, VillagerProfession.LEATHERWORKER, VillagerProfession.CARTOGRAPHER,
            VillagerProfession.NONE, VillagerProfession.NONE, VillagerProfession.NONE, VillagerProfession.NONE, VillagerProfession.NONE, VillagerProfession.NONE, VillagerProfession.NONE, VillagerProfession.NONE
        };

        for (int i = 0; i < roles.length; i++) {
            VillagerEntity v = EntityType.VILLAGER.create(world);
            if (v == null) continue;
            v.setVillagerData(v.getVillagerData().withType(VillagerType.PLAINS).withProfession(jobs[i]).withLevel(5));

            int x = c.getX() + pos[i][0];
            int z = c.getZ() + pos[i][1];
            int sy = safeSpawnY(world, x, z, c.getY() + 2);
            v.refreshPositionAndAngles(x + 0.5, sy, z + 0.5, world.random.nextFloat() * 360f, 0f);
            v.setCustomName(Text.literal("§6[" + n.name + "] §f" + roles[i]));
            v.setCustomNameVisible(i < 19);
            v.setPersistent();
            v.addCommandTag("crown019_npc");
            v.addCommandTag("crown019_" + n.id);

            if (i == 0) {
                v.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            } else if (i >= 7 && i <= 18) {
                v.equipStack(EquipmentSlot.HEAD, new ItemStack(i <= 12 ? Items.IRON_HELMET : Items.CHAINMAIL_HELMET));
            } else if (i < 7) {
                v.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            } else if (i % 4 == 0) {
                v.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            }
            world.spawnEntity(v);
        }
    }

    /** Finds a real floor with two air blocks above it, so no NPC is ever spawned into the buried old city. */
    private static int safeSpawnY(ServerWorld world, int x, int z, int preferredY) {
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int y = preferredY; y <= preferredY + 12; y++) {
            BlockState below = world.getBlockState(p.set(x, y - 1, z));
            BlockState here = world.getBlockState(p.set(x, y, z));
            BlockState head = world.getBlockState(p.set(x, y + 1, z));
            if (!below.isAir() && below.getFluidState().isEmpty() && here.isAir() && head.isAir()) return y;
        }
        return preferredY;
    }

    /**
     * Removes both old patch villagers and older named Crown & Cinder capital mobs. Ordinary unnamed monsters and
     * players are left alone. This is what prevents the old emperor/knights from surviving under the new palace.
     */
    private static void clearLegacyCapitalEntities(ServerWorld world, BlockPos c) {
        Box area = new Box(c.add(-82, -42, -82), c.add(82, 45, 82));
        List<Entity> remove = world.getOtherEntities(null, area, e -> shouldRemoveLegacyNpc(e));
        for (Entity e : remove) e.discard();
    }

    private static boolean shouldRemoveLegacyNpc(Entity e) {
        if (e instanceof PlayerEntity) return false;
        for (String tag : e.getCommandTags()) {
            if (tag.startsWith("crown_" ) || tag.startsWith("crown018_") || tag.startsWith("crown019_")) return true;
        }
        if (e instanceof VillagerEntity) return true;
        if (e instanceof MobEntity && e.hasCustomName()) {
            String name = e.getCustomName().getString();
            return name.contains("황제") || name.contains("국왕") || name.contains("기사") || name.contains("궁수")
                || name.contains("창병") || name.contains("시종") || name.contains("상인") || name.contains("대장장이")
                || name.contains("용병") || name.contains("사제") || name.contains("서기관");
        }
        return false;
    }

    private static void lanternPost(ServerWorld w, BlockPos base, Palette p) {
        set(w, base, p.trim);
        set(w, base.up(), Blocks.OAK_FENCE);
        set(w, base.up(2), Blocks.OAK_FENCE);
        set(w, base.up(3), Blocks.LANTERN);
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

    private static void placeMarker(ServerWorld w, BlockPos c) {
        set(w, c, Blocks.LODESTONE);
    }

    private static void set(ServerWorld w, BlockPos pos, Block block) {
        w.setBlockState(pos, block.getDefaultState(), 2);
    }
}
