package dev.crowncinderpatch;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Gives a one-time starter kit on the player's first Crown & Cinder 0.21 join. */
public final class StarterKit021 {
    private StarterKit021() {}

    public static void giveOnce(ServerPlayerEntity player) {
        if (player.getCommandTags().contains("crown021_starter")) return;
        give(player, customOr("knight_blade", Items.IRON_SWORD), 1);
        give(player, Items.SHIELD, 1);
        give(player, Items.BREAD, 16);
        give(player, Items.COOKED_BEEF, 8);
        give(player, Items.TORCH, 32);
        give(player, Items.COMPASS, 1);
        Item kingdomMap = Registries.ITEM.getOrEmpty(new Identifier("crowncinder", "kingdom_map")).orElse(null);
        if (kingdomMap != null) give(player, kingdomMap, 1);
        player.addCommandTag("crown021_starter");
        player.sendMessage(Text.literal("§6[Crown & Cinder] §f초보 모험가 장비를 지급했습니다. 검·방패·식량·횃불·나침반·왕국 지도를 확인하세요."), false);
    }

    private static Item customOr(String id, Item fallback) {
        return Registries.ITEM.getOrEmpty(new Identifier("crowncinder", id)).orElse(fallback);
    }

    private static void give(ServerPlayerEntity p, Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        if (!p.giveItemStack(stack)) p.dropItem(stack, false);
    }
}
