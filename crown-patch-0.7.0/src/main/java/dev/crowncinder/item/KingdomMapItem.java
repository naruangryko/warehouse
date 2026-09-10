package dev.crowncinder.item;

import dev.crowncinder.world.NationWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class KingdomMapItem extends Item {
    public KingdomMapItem(){ super(new Item.Settings().maxCount(1)); }
    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand){
        ItemStack stack=user.getStackInHand(hand);
        if(!world.isClient && user instanceof ServerPlayerEntity sp) NationWorld.showMap(sp);
        return TypedActionResult.success(stack, world.isClient);
    }
}
