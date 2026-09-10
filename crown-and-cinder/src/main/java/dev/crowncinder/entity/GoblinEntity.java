package dev.crowncinder.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.world.World;

/** A green raider using Minecraft's built-in biped and melee AI, not another mod's assets. */
public final class GoblinEntity extends ZombieEntity {
    public GoblinEntity(EntityType<? extends ZombieEntity> type, World world) { super(type, world); }
    @Override protected boolean burnsInDaylight() { return false; }
    @Override protected boolean canConvertInWater() { return false; }
    @Override public boolean canPickUpLoot() { return false; }
}
