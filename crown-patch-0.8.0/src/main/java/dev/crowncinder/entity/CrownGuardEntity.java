package dev.crowncinder.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;

public final class CrownGuardEntity extends ZombieEntity {
    public CrownGuardEntity(EntityType<? extends ZombieEntity> type, World world) { super(type, world); }

    @Override protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.05, false));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.75));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this).setGroupRevenge(CrownGuardEntity.class));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, HostileEntity.class, 10, true, false,
            target -> !(target instanceof CrownGuardEntity)));
    }

    public void equipRole(ItemStack weapon, boolean commander) {
        this.equipStack(EquipmentSlot.MAINHAND, weapon);
        this.equipStack(EquipmentSlot.HEAD, new ItemStack(commander ? Items.DIAMOND_HELMET : Items.IRON_HELMET));
        this.equipStack(EquipmentSlot.CHEST, new ItemStack(commander ? Items.DIAMOND_CHESTPLATE : Items.IRON_CHESTPLATE));
        this.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        this.equipStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        for (EquipmentSlot slot : EquipmentSlot.values()) this.setEquipmentDropChance(slot, 0.0f);
        this.setCanPickUpLoot(false);
        this.setPersistent();
    }

    @Override public boolean canTarget(LivingEntity target) {
        if (target instanceof PlayerEntity || target instanceof CrownGuardEntity) return false;
        return super.canTarget(target);
    }
}
