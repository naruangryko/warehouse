package dev.crowncinder;

import dev.crowncinder.config.RpgConfig;
import dev.crowncinder.entity.GoblinEntity;
import dev.crowncinder.entity.ModEntities;
import dev.crowncinder.item.ModItems;
import dev.crowncinder.item.RpgSword;
import dev.crowncinder.progress.Progress;
import dev.crowncinder.progress.ProgressService;
import dev.crowncinder.quest.RpgCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CrownCinder implements ModInitializer {
    public static final String ID = "crowncinder";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static RpgConfig CONFIG;
    public static Identifier id(String path) { return new Identifier(ID, path); }
    @Override public void onInitialize() {
        CONFIG = RpgConfig.load(LOGGER);
        ModEntities.init(); ModItems.init(); RpgCommands.init();
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!player.isSpectator() && player instanceof ServerPlayerEntity serverPlayer &&
                player.getMainHandStack().getItem() instanceof RpgSword sword && !sword.permitted(serverPlayer))
                return ActionResult.FAIL;
            return ActionResult.PASS;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity dead) {
                ProgressService.get(dead).die(CONFIG.deathXpLoss);
                ProgressService.store(dead).markDirty();
            }
            if (entity instanceof HostileEntity && source.getAttacker() instanceof ServerPlayerEntity killer
                    && !killer.isSpectator() && !killer.isCreative()) {
                ProgressService.award(killer, entity instanceof GoblinEntity ? CONFIG.goblinXp : CONFIG.otherMonsterXp);
                if (entity instanceof GoblinEntity) {
                    ProgressService.get(killer).goblinKilled(); ProgressService.store(killer).markDirty();
                }
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0) return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ProgressService.apply(player);
                Progress p = ProgressService.get(player);
                if (p.hud && player.isAlive() && server.getTicks() % CONFIG.hudIntervalTicks == 0)
                    player.sendMessage(Text.translatable("hud.crowncinder.progress", p.level, p.xp,
                        p.requiredXp(CONFIG.xpBase), p.points, p.questActive ? p.questKills : 0), true);
            }
        });
        LOGGER.info("Crown & Cinder initialized: MVP 0.1.0");
    }
}
