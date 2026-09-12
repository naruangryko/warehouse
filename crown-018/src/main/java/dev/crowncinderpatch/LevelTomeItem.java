package dev.crowncinderpatch;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class LevelTomeItem extends Item {
    private final int levels;
    private final String display;

    public LevelTomeItem(Settings settings, int levels, String display) {
        super(settings);
        this.levels = levels;
        this.display = display;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            boolean integrated = RpgProgressBridge.addLevels(player, levels);
            int now = RpgProgressBridge.getLevel(player);
            player.sendMessage(Text.literal("§d" + display + "§f 사용! §a+" + levels + "레벨 §7(현재 Lv." + now + ")"), false);
            if (!integrated) {
                player.sendMessage(Text.literal("§7Crown & Cinder RPG 진행 데이터에 접근하지 못해 바닐라 경험치 레벨에 적용했습니다."), false);
            }
            if (!player.getAbilities().creativeMode) stack.decrement(1);
            player.getItemCooldownManager().set(this, 20);
        }
        return TypedActionResult.success(stack, world.isClient());
    }
}
