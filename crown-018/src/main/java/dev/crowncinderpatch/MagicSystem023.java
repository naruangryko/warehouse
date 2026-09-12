package dev.crowncinderpatch;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Learns one spell every 10 RPG levels; a spellbook can unlock basic magic early. */
public final class MagicSystem023 {
    private MagicSystem023() {}
    private static int ticks;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % 40 != 0) return;
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) syncLearning(p);
        });
    }

    public static void giveStaffOnce(ServerPlayerEntity player, Item staff) {
        if (player.getCommandTags().contains("crown023_staff_given")) return;
        ItemStack stack = new ItemStack(staff);
        if (!player.giveItemStack(stack)) player.dropItem(stack, false);
        player.addCommandTag("crown023_staff_given");
        player.sendMessage(Text.literal("§d[마법] §f마법 스태프를 지급했습니다. Lv.10마다 새 마법을 배우거나 마법서를 사용할 수 있습니다."), false);
    }

    public static void syncLearning(ServerPlayerEntity player) {
        int level = Math.max(1, RpgProgressBridge.getLevel(player));
        int maxTier = Math.min(10, level / 10);
        for (int tier = 1; tier <= maxTier; tier++) {
            String tag = "crown023_spell_" + tier;
            if (!player.getCommandTags().contains(tag)) {
                player.addCommandTag(tag);
                player.sendMessage(Text.literal("§d[마법 습득] §fLv." + (tier * 10) + " 달성: §b" + spellName(tier)), false);
            }
        }
    }

    public static int magicTier(ServerPlayerEntity player) {
        syncLearning(player);
        int tier = Math.min(10, Math.max(0, RpgProgressBridge.getLevel(player) / 10));
        if (tier == 0 && player.getCommandTags().contains("crown023_spellbook_basic")) tier = 1;
        return tier;
    }

    public static String status(ServerPlayerEntity player) {
        int tier = magicTier(player);
        if (tier <= 0) return "배운 마법 없음 · Lv.10 또는 마법서 필요";
        return "Tier " + tier + " · " + spellName(tier) + " · 마력 " + RpgProgressBridge.getMagicPower(player);
    }

    public static String spellName(int tier) {
        return switch (Math.max(1, Math.min(10, tier))) {
            case 1 -> "마력탄";
            case 2 -> "화염탄";
            case 3 -> "서리창";
            case 4 -> "번개탄";
            case 5 -> "성광탄";
            case 6 -> "지옥화염";
            case 7 -> "빙결창";
            case 8 -> "폭풍창";
            case 9 -> "용의 숨결";
            default -> "대마도사의 마력포";
        };
    }

    public static ParticleEffect particle(int tier) {
        return switch (Math.max(1, Math.min(10, tier))) {
            case 2, 6 -> ParticleTypes.FLAME;
            case 3, 7 -> ParticleTypes.SNOWFLAKE;
            case 4, 8 -> ParticleTypes.ELECTRIC_SPARK;
            case 5 -> ParticleTypes.END_ROD;
            case 9 -> ParticleTypes.DRAGON_BREATH;
            case 10 -> ParticleTypes.WITCH;
            default -> ParticleTypes.ENCHANT;
        };
    }
}
