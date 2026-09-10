package dev.crowncinder;

import dev.crowncinder.config.RpgConfig;
import dev.crowncinder.entity.*;
import dev.crowncinder.entity.ModEntities;
import dev.crowncinder.item.ModItems;
import dev.crowncinder.item.RpgSword;
import dev.crowncinder.progress.Progress;
import dev.crowncinder.progress.ProgressService;
import dev.crowncinder.quest.RpgCommands;
import dev.crowncinder.quest.RpgCombatCommands;
import dev.crowncinder.quest.RpgNationCommands;
import dev.crowncinder.world.RpgNpcInteractions;
import dev.crowncinder.world.RpgServiceInteractions;
import dev.crowncinder.world.CentralCastle;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
        ModEntities.init(); ModItems.init(); RpgCommands.init(); RpgCombatCommands.init(); RpgNationCommands.init(); RpgNpcInteractions.init(); RpgServiceInteractions.init();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (server.getOverworld() != null) CentralCastle.ensure(server.getOverworld());
        });
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
            if (entity instanceof HostileEntity && !(entity instanceof CrownGuardEntity) && source.getAttacker() instanceof ServerPlayerEntity killer
                    && !killer.isSpectator() && !killer.isCreative()) {
                int xp = entity instanceof MinotaurEntity ? 90 : entity instanceof OgreEntity ? 60 : entity instanceof DarkKnightEntity ? 50 : entity instanceof TrollEntity ? 45 : entity instanceof WerewolfEntity ? 30 : entity instanceof OrcEntity ? 22 : entity instanceof GoblinEntity ? CONFIG.goblinXp : CONFIG.otherMonsterXp;
                ProgressService.award(killer, xp);
                Progress prog = ProgressService.get(killer);
                if (entity instanceof MinotaurEntity) entity.dropStack(new net.minecraft.item.ItemStack(ModItems.GOLD_COIN, 1));
                else if (entity instanceof OgreEntity) entity.dropStack(new net.minecraft.item.ItemStack(ModItems.SILVER_COIN, 4));
                else if (entity instanceof DarkKnightEntity) entity.dropStack(new net.minecraft.item.ItemStack(ModItems.SILVER_COIN, 3));
                else if (entity instanceof TrollEntity) entity.dropStack(new net.minecraft.item.ItemStack(ModItems.SILVER_COIN, 2));
                else { int coins = entity instanceof WerewolfEntity ? 35 : entity instanceof OrcEntity ? 20 : entity instanceof GoblinEntity ? 6 : 2; entity.dropStack(new net.minecraft.item.ItemStack(ModItems.COPPER_COIN, coins)); }
                if (entity instanceof GoblinEntity) prog.goblinKilled();
                ProgressService.store(killer).markDirty();
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0) return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ProgressService.apply(player);
                if (server.getTicks() % 100 == 0) ProgressService.regen(player);
                Progress p = ProgressService.get(player);
                if (p.hud && player.isAlive() && server.getTicks() % CONFIG.hudIntervalTicks == 0)
                    player.sendMessage(Text.translatable("hud.crowncinder.progress", p.level, p.xp,
                        p.requiredXp(CONFIG.xpBase), p.points, p.questActive ? p.questKills : 0), true);
            }
        });
        LOGGER.info("Crown & Cinder initialized: central mega castle, fantasy villages and verified unique weapon models 0.10.0");
    }
}
