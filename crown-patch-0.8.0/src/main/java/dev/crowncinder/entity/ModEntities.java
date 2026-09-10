package dev.crowncinder.entity;

import dev.crowncinder.CrownCinder;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.BiomeKeys;

public final class ModEntities {
    private static <T extends ZombieEntity> EntityType<T> type(String id, EntityType.EntityFactory<T> factory, float width, float height){
        return Registry.register(Registries.ENTITY_TYPE, CrownCinder.id(id), FabricEntityTypeBuilder.create(SpawnGroup.MONSTER,factory)
            .dimensions(EntityDimensions.fixed(width,height)).trackRangeBlocks(64).build());
    }
    public static final EntityType<GoblinEntity> GOBLIN=type("goblin",GoblinEntity::new,0.6f,1.95f);
    public static final EntityType<OrcEntity> ORC=type("orc",OrcEntity::new,0.7f,2.05f);
    public static final EntityType<TrollEntity> TROLL=type("troll",TrollEntity::new,0.9f,2.35f);
    public static final EntityType<WerewolfEntity> WEREWOLF=type("werewolf",WerewolfEntity::new,0.65f,2.0f);
    public static final EntityType<OgreEntity> OGRE=type("ogre",OgreEntity::new,1.0f,2.5f);
    public static final EntityType<MinotaurEntity> MINOTAUR=type("minotaur",MinotaurEntity::new,0.9f,2.4f);
    public static final EntityType<DarkKnightEntity> DARK_KNIGHT=type("dark_knight",DarkKnightEntity::new,0.65f,2.0f);
    public static final EntityType<CrownGuardEntity> CROWN_GUARD = Registry.register(Registries.ENTITY_TYPE, CrownCinder.id("crown_guard"), FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CrownGuardEntity::new).dimensions(EntityDimensions.fixed(0.6f,1.95f)).trackRangeBlocks(64).build());
    public static void init(){
        FabricDefaultAttributeRegistry.register(GOBLIN, attrs(CrownCinder.CONFIG.goblinHealth,CrownCinder.CONFIG.goblinDamage,CrownCinder.CONFIG.goblinSpeed,24));
        FabricDefaultAttributeRegistry.register(ORC, attrs(34,7,0.24,28));
        FabricDefaultAttributeRegistry.register(TROLL, attrs(70,11,0.18,30).add(EntityAttributes.GENERIC_ARMOR,5));
        FabricDefaultAttributeRegistry.register(WEREWOLF, attrs(42,8,0.34,32));
        FabricDefaultAttributeRegistry.register(OGRE, attrs(90,13,0.17,30).add(EntityAttributes.GENERIC_ARMOR,7));
        FabricDefaultAttributeRegistry.register(MINOTAUR, attrs(110,15,0.23,36).add(EntityAttributes.GENERIC_ARMOR,8));
        FabricDefaultAttributeRegistry.register(DARK_KNIGHT, attrs(58,10,0.26,34).add(EntityAttributes.GENERIC_ARMOR,10));
        FabricDefaultAttributeRegistry.register(CROWN_GUARD, attrs(46,8,0.28,32).add(EntityAttributes.GENERIC_ARMOR,8));
        restriction(GOBLIN); restriction(ORC); restriction(TROLL); restriction(WEREWOLF); restriction(OGRE); restriction(MINOTAUR); restriction(DARK_KNIGHT);
        if(CrownCinder.CONFIG.goblinSpawnWeight>0) BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.PLAINS,BiomeKeys.FOREST,BiomeKeys.TAIGA),SpawnGroup.MONSTER,GOBLIN,CrownCinder.CONFIG.goblinSpawnWeight,1,3);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.PLAINS,BiomeKeys.FOREST),SpawnGroup.MONSTER,ORC,18,1,2);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.TAIGA,BiomeKeys.OLD_GROWTH_PINE_TAIGA),SpawnGroup.MONSTER,TROLL,7,1,1);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.FOREST,BiomeKeys.DARK_FOREST,BiomeKeys.TAIGA),SpawnGroup.MONSTER,WEREWOLF,10,1,2);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.SAVANNA,BiomeKeys.PLAINS),SpawnGroup.MONSTER,OGRE,5,1,1);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.DARK_FOREST,BiomeKeys.WINDSWEPT_HILLS),SpawnGroup.MONSTER,DARK_KNIGHT,4,1,1);
    }
    private static net.minecraft.entity.attribute.DefaultAttributeContainer.Builder attrs(double hp,double dmg,double speed,double follow){
        return ZombieEntity.createZombieAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,hp).add(EntityAttributes.GENERIC_ATTACK_DAMAGE,dmg)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED,speed).add(EntityAttributes.GENERIC_FOLLOW_RANGE,follow).add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS,0);
    }
    private static <T extends HostileEntity> void restriction(EntityType<T> type){SpawnRestriction.register(type,SpawnRestriction.Location.ON_GROUND,Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,HostileEntity::canSpawnInDark);}
}
