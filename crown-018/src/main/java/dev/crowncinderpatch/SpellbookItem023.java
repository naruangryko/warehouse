package dev.crowncinderpatch;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Consumable book that unlocks the first spell before Lv.10. */
public final class SpellbookItem023 extends Item {
    public SpellbookItem023(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            if (player.getCommandTags().contains("crown023_spellbook_basic") || MagicSystem023.magicTier(player) >= 1) {
                player.sendMessage(Text.literal("§d[마법서] §f이미 기본 마법을 배웠습니다."), false);
                return TypedActionResult.fail(stack);
            }
            player.addCommandTag("crown023_spellbook_basic");
            player.sendMessage(Text.literal("§d[마법서] §f마력탄을 배웠습니다! 마법 스태프를 우클릭해 사용하세요."), false);
            if (!player.getAbilities().creativeMode) stack.decrement(1);
        }
        return TypedActionResult.success(stack, world.isClient());
    }
}
