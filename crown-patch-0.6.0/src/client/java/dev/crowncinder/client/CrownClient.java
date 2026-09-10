package dev.crowncinder.client;
import dev.crowncinder.CrownCinder;
import dev.crowncinder.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;
public final class CrownClient implements ClientModInitializer{
 @Override public void onInitializeClient(){register(ModEntities.GOBLIN);register(ModEntities.ORC);register(ModEntities.TROLL);register(ModEntities.WEREWOLF);}
 private static <T extends ZombieEntity> void register(net.minecraft.entity.EntityType<T> type){EntityRendererRegistry.register(type,context->new ZombieEntityRenderer(context){@Override public Identifier getTexture(ZombieEntity entity){return CrownCinder.id("textures/entity/goblin.png");}});}
}
