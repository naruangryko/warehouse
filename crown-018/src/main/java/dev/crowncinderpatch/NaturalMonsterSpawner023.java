package dev.crowncinderpatch;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.BiomeKeys;

import java.util.List;

/**
 * Supplemental natural spawner for Crown & Cinder monsters.
 * At night monsters can appear around players; matching biomes act as special monster regions even during daytime.
 */
public final class NaturalMonsterSpawner023 {
    private NaturalMonsterSpawner023() {}
    private static int ticks;

    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (++ticks % 200 != 0) return;
            if (world.getDifficulty() == Difficulty.PEACEFUL) return;
            long dayTime = world.getTimeOfDay() % 24000L;
            boolean night = dayTime >= 13000L && dayTime <= 23000L;
            for (ServerPlayerEntity player : world.getPlayers()) trySpawn(world, player, night);
        });
    }

    private static void trySpawn(ServerWorld world, ServerPlayerEntity player, boolean night) {
        if (nearCapital(world, player.getBlockPos())) return;
        Box nearby = player.getBoundingBox().expand(48.0);
        int hostileCount = world.getEntitiesByClass(HostileEntity.class, nearby, e -> e.isAlive()).size();
        if (hostileCount >= 22) return;

        int dx = 20 + world.random.nextInt(21);
        int dz = 20 + world.random.nextInt(21);
        if (world.random.nextBoolean()) dx = -dx;
        if (world.random.nextBoolean()) dz = -dz;
        int x = player.getBlockX() + dx;
        int z = player.getBlockZ() + dz;
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        world.getChunk(x >> 4, z >> 4);

        String id = regionMonster(world, pos);
        boolean specialRegion = id != null;
        if (!night && !specialRegion) return;
        if (id == null) {
            String[] nightPool = {"goblin", "orc", "skeleton_knight", "werewolf"};
            id = nightPool[world.random.nextInt(nightPool.length)];
        }
        if (!specialRegion && world.getLightLevel(pos) > 7) return;
        if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) return;
        if (world.getBlockState(pos.down()).isAir()) return;

        EntityType<?> type = Registries.ENTITY_TYPE.get(new Identifier("crowncinder", id));
        if (type == EntityType.PIG && !Registries.ENTITY_TYPE.getId(type).getNamespace().equals("crowncinder")) return;
        Entity entity = type.create(world);
        if (!(entity instanceof MobEntity mob)) return;
        mob.refreshPositionAndAngles(x + 0.5, y, z + 0.5, world.random.nextFloat() * 360f, 0f);
        mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.NATURAL, null, null);
        world.spawnEntity(mob);
    }

    private static String regionMonster(ServerWorld world, BlockPos pos) {
        var biome = world.getBiome(pos);
        if (biome.matchesKey(BiomeKeys.DESERT) || biome.matchesKey(BiomeKeys.BADLANDS)) return "fire_elemental";
        if (biome.matchesKey(BiomeKeys.DARK_FOREST)) return world.random.nextBoolean() ? "dark_knight" : "skeleton_knight";
        if (biome.matchesKey(BiomeKeys.TAIGA) || biome.matchesKey(BiomeKeys.OLD_GROWTH_PINE_TAIGA)) return world.random.nextBoolean() ? "troll" : "werewolf";
        if (biome.matchesKey(BiomeKeys.SAVANNA) || biome.matchesKey(BiomeKeys.WINDSWEPT_HILLS)) return world.random.nextBoolean() ? "ogre" : "minotaur";
        if (biome.matchesKey(BiomeKeys.FOREST)) return world.random.nextBoolean() ? "goblin" : "werewolf";
        if (biome.matchesKey(BiomeKeys.PLAINS)) return world.random.nextBoolean() ? "goblin" : "orc";
        return null;
    }

    private static boolean nearCapital(ServerWorld world, BlockPos pos) {
        BlockPos a = OneCapitalCoordinator.anchor(world);
        int[][] sites = {{0,0},{920,40},{300,-900},{-760,-560},{-780,590},{310,920}};
        for (int[] s : sites) {
            long dx = pos.getX() - (a.getX() + s[0]);
            long dz = pos.getZ() - (a.getZ() + s[1]);
            if (dx * dx + dz * dz < 120L * 120L) return true;
        }
        return false;
    }
}
