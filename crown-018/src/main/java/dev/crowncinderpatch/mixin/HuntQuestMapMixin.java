package dev.crowncinderpatch.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "dev.crowncinder.quest.HuntCommands", remap = false)
public abstract class HuntQuestMapMixin {
    @Inject(method = "accept", at = @At("RETURN"), remap = false)
    private static void crown021$routeMap(ServerPlayerEntity player, String region, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValue() == null || cir.getReturnValue() != 1) return;
        BlockPos target = hint(player, region);
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            Identifier id = Registries.ITEM.getId(stack.getItem());
            if (!"crowncinder".equals(id.getNamespace()) || !"hunt_map".equals(id.getPath())) continue;
            if (!region.equals(stack.getOrCreateNbt().getString("hunt_region"))) continue;
            stack.getOrCreateNbt().putInt("quest_target_x", target.getX());
            stack.getOrCreateNbt().putInt("quest_target_z", target.getZ());
            stack.setCustomName(Text.literal("§6퀘스트 목적지 지도 §f- " + korean(region)));
            player.sendMessage(Text.literal("§e[퀘스트 지도] §f목적지 안내: X " + target.getX() + " / Z " + target.getZ() + " · 지도를 우클릭하면 안내를 다시 볼 수 있습니다."), false);
            break;
        }
    }

    private static BlockPos hint(ServerPlayerEntity player, String region) {
        try {
            Class<?> c = Class.forName("dev.crowncinder.world.HuntRegions");
            Method m = c.getMethod("hint", net.minecraft.server.world.ServerWorld.class, String.class);
            Object out = m.invoke(null, player.getServerWorld(), region);
            if (out instanceof BlockPos p) return p;
        } catch (Throwable ignored) {}
        BlockPos s = player.getServerWorld().getSpawnPos();
        return switch (region) {
            case "greenwood" -> s.add(850, 0, 0);
            case "frostwood" -> s.add(0, 0, -850);
            case "ashen_waste" -> s.add(-850, 0, 0);
            case "shadowwood" -> s.add(0, 0, 850);
            case "highlands" -> s.add(850, 0, 850);
            default -> s;
        };
    }

    private static String korean(String id) {
        return switch (id) {
            case "greenwood" -> "녹음의 사냥터";
            case "frostwood" -> "서리숲";
            case "ashen_waste" -> "잿빛 황무지";
            case "shadowwood" -> "그림자 숲";
            case "highlands" -> "거인 고원";
            default -> id;
        };
    }
}
