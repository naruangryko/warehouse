package dev.crowncinder.entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.world.World;
public final class WerewolfEntity extends ZombieEntity {
    public WerewolfEntity(EntityType<? extends ZombieEntity> type, World world){super(type,world);}
    @Override protected boolean burnsInDaylight(){return false;}
    @Override protected boolean canConvertInWater(){return false;}
}
