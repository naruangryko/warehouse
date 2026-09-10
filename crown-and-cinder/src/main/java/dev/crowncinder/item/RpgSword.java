package dev.crowncinder.item;

import dev.crowncinder.CrownCinder;
import dev.crowncinder.progress.ProgressService;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import java.util.List;

public final class RpgSword extends SwordItem {
    private final int requiredLevel;
    private final boolean cleave;
    public RpgSword(int attack, float speed, int durability, int requiredLevel, boolean cleave) {
        super(ToolMaterials.IRON, attack, speed, new Settings().maxDamage(durability));
        this.requiredLevel = requiredLevel;
        this.cleave = cleave;
    }
    public boolean permitted(ServerPlayerEntity player) {
        if (player.isCreative() || ProgressService.get(player).level >= requiredLevel) return true;
        player.sendMessage(Text.translatable("message.crowncinder.level_required", requiredLevel), true);
        return false;
    }
    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!cleave) return TypedActionResult.pass(stack);
        if (world.isClient) return TypedActionResult.success(stack);
        if (!(user instanceof ServerPlayerEntity player) || !permitted(player)) return TypedActionResult.fail(stack);
        if (player.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
        double radius = CrownCinder.CONFIG.cleaveRadius;
        // Bounded local query; no friendly-fire and no attacks through walls.
        for (HostileEntity enemy : world.getEntitiesByClass(HostileEntity.class,
                player.getBoundingBox().expand(radius), e -> e.isAlive() && player.canSee(e)
                    && player.squaredDistanceTo(e) <= radius * radius)) {
            enemy.damage(world.getDamageSources().playerAttack(player), (float) CrownCinder.CONFIG.cleaveDamage);
        }
        player.getItemCooldownManager().set(this, CrownCinder.CONFIG.cleaveCooldownTicks);
        stack.damage(2, player, p -> p.sendToolBreakStatus(hand));
        player.swingHand(hand, true);
        return TypedActionResult.success(stack);
    }
    @Override public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.crowncinder.level", requiredLevel));
        if (cleave) tooltip.add(Text.translatable("tooltip.crowncinder.cleave"));
    }
}
