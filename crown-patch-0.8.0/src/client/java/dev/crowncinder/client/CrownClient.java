package dev.crowncinder.client;

import dev.crowncinder.CrownCinder;
import dev.crowncinder.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;

public final class CrownClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        register(ModEntities.GOBLIN,"goblin.png"); register(ModEntities.ORC,"orc.png"); register(ModEntities.TROLL,"troll.png"); register(ModEntities.WEREWOLF,"werewolf.png");
        register(ModEntities.OGRE,"ogre.png"); register(ModEntities.MINOTAUR,"minotaur.png"); register(ModEntities.DARK_KNIGHT,"dark_knight.png");
        register(ModEntities.CROWN_GUARD,"dark_knight.png");
    }
    private static <T extends ZombieEntity> void register(net.minecraft.entity.EntityType<T> type,String texture){
        EntityRendererRegistry.register(type, context -> new ZombieEntityRenderer(context) {
            @Override public Identifier getTexture(ZombieEntity entity) { return CrownCinder.id("textures/entity/"+texture); }
        });
    }
}
