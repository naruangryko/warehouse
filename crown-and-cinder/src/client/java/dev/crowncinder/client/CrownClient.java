package dev.crowncinder.client;

import dev.crowncinder.CrownCinder;
import dev.crowncinder.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;

/** This class never loads on a dedicated server. */
public final class CrownClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.GOBLIN, context -> new ZombieEntityRenderer(context) {
            @Override public Identifier getTexture(ZombieEntity entity) {
                return CrownCinder.id("textures/entity/goblin.png");
            }
        });
    }
}
