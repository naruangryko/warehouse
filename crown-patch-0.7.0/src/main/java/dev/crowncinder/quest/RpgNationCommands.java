package dev.crowncinder.quest;
import dev.crowncinder.world.NationWorld;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
public final class RpgNationCommands{private RpgNationCommands(){} public static void init(){CommandRegistrationCallback.EVENT.register((d,a,e)->d.register(CommandManager.literal("rpg").then(CommandManager.literal("map").executes(c->{NationWorld.giveMap(c.getSource().getPlayerOrThrow());NationWorld.showMap(c.getSource().getPlayerOrThrow());return 1;})).then(CommandManager.literal("kingdoms").then(CommandManager.literal("build").executes(c->{NationWorld.buildAll(c.getSource().getPlayerOrThrow());return 1;})))));}}
