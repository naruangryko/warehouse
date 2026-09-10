package dev.crowncinder.quest;
import dev.crowncinder.entity.ModEntities;
import dev.crowncinder.world.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
public final class RpgCombatCommands{private RpgCombatCommands(){}public static void init(){CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->{var root=CommandManager.literal("rpg").then(CommandManager.literal("dungeon").executes(c->{RpgDungeon.build(c.getSource().getPlayerOrThrow());return 1;})).then(CommandManager.literal("raid").executes(c->{RpgRaid.start(c.getSource().getPlayerOrThrow());return 1;})).then(CommandManager.literal("services").executes(c->{RpgServiceNpcs.spawn(c.getSource().getPlayerOrThrow());return 1;}));var spawn=CommandManager.literal("spawn").then(CommandManager.literal("orc").executes(c->spawn(c.getSource().getPlayerOrThrow(),ModEntities.ORC,"오크"))).then(CommandManager.literal("troll").executes(c->spawn(c.getSource().getPlayerOrThrow(),ModEntities.TROLL,"트롤"))).then(CommandManager.literal("werewolf").executes(c->spawn(c.getSource().getPlayerOrThrow(),ModEntities.WEREWOLF,"늑대인간")));root.then(spawn);dispatcher.register(root);});}private static <T extends MobEntity> int spawn(ServerPlayerEntity p,EntityType<T> type,String name){ServerWorld w=p.getServerWorld();BlockPos pos=p.getBlockPos().add(0,0,4);T e=type.create(w);if(e==null)return 0;e.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);e.setPersistent();w.spawnEntity(e);p.sendMessage(Text.literal("§c"+name+"§f 소환 완료."),false);return 1;}}
