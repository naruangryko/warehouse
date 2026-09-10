package dev.crowncinder.entity;

import dev.crowncinder.CrownCinder;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.BiomeKeys;

public final class ModEntities {
    public static final EntityType<GoblinEntity> GOBLIN = Registry.register(Registries.ENTITY_TYPE,
        CrownCinder.id("goblin"), FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, GoblinEntity::new)
            .dimensions(EntityDimensions.fixed(0.6f, 1.95f)).trackRangeBlocks(64).build());
    public static void init() {
        FabricDefaultAttributeRegistry.register(GOBLIN, ZombieEntity.createZombieAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, CrownCinder.CONFIG.goblinHealth)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, CrownCinder.CONFIG.goblinDamage)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, CrownCinder.CONFIG.goblinSpeed)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24)
            .add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS, 0));
        SpawnRestriction.register(GOBLIN, SpawnRestriction.Location.ON_GROUND,
            Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        if (CrownCinder.CONFIG.goblinSpawnWeight > 0)
            BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.FOREST, BiomeKeys.TAIGA),
                SpawnGroup.MONSTER, GOBLIN, CrownCinder.CONFIG.goblinSpawnWeight, 1, 3);
    }
}
