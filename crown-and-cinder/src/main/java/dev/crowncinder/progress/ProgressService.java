package dev.crowncinder.progress;

import dev.crowncinder.CrownCinder;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.UUID;

public final class ProgressService {
    private static final UUID STRENGTH = UUID.fromString("795a754d-4d46-443f-94ab-100000000001");
    private static final UUID VITALITY = UUID.fromString("795a754d-4d46-443f-94ab-100000000002");
    private static final UUID AGILITY = UUID.fromString("795a754d-4d46-443f-94ab-100000000003");
    public static ProgressStore store(ServerPlayerEntity player) { return ProgressStore.get(player.getServer()); }
    public static Progress get(ServerPlayerEntity player) { return store(player).player(player.getUuid()); }
    public static void award(ServerPlayerEntity player, int rawXp) {
        Progress p = get(player);
        int levels = p.gainXp((int) (rawXp * CrownCinder.CONFIG.xpMultiplier), CrownCinder.CONFIG.xpBase);
        store(player).markDirty();
        if (levels > 0) player.sendMessage(Text.translatable("message.crowncinder.level", p.level, p.points), false);
    }
    private static void modifier(ServerPlayerEntity player, EntityAttribute attribute, UUID id, double amount) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) return;
        EntityAttributeModifier old = instance.getModifier(id);
        if (old != null && old.getValue() == amount) return;
        instance.removeModifier(id);
        if (amount != 0) instance.addTemporaryModifier(new EntityAttributeModifier(id, "crowncinder", amount,
            EntityAttributeModifier.Operation.ADDITION));
    }
    public static void apply(ServerPlayerEntity player) {
        Progress p = get(player);
        modifier(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, STRENGTH, p.strength * CrownCinder.CONFIG.damagePerStrength);
        modifier(player, EntityAttributes.GENERIC_MAX_HEALTH, VITALITY, p.vitality * CrownCinder.CONFIG.healthPerVitality);
        modifier(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, AGILITY, Math.min(0.1, p.agility * CrownCinder.CONFIG.speedPerAgility));
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }
}
