package dev.crowncinderpatch;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

/** Emperor-given quest that awards the existing swordsman_proof item after 12 hostile kills. */
public final class KnightProofQuest022 {
    private KnightProofQuest022() {}

    public static void init() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!isEmperor(entity)) return ActionResult.PASS;

            KnightProofQuestState022 state = KnightProofQuestState022.get(serverPlayer.getServerWorld());
            KnightProofQuestState022.Progress progress = state.progress(serverPlayer.getUuid());

            if (progress.stage == 0) {
                progress.stage = 1;
                progress.kills = 0;
                state.markDirty();
                serverPlayer.sendMessage(Text.literal("§6[황제] §f그대의 실력을 증명하라. 적대 몬스터 12마리를 처치하고 다시 나를 찾아오게."), false);
                serverPlayer.sendMessage(Text.literal("§e퀘스트 시작: §f기사의 증명 §7(0/12)"), false);
                return ActionResult.SUCCESS;
            }

            if (progress.stage == 1) {
                if (progress.kills < KnightProofQuestState022.REQUIRED_KILLS) {
                    serverPlayer.sendMessage(Text.literal("§6[황제] §f아직 충분하지 않다. 현재 진척도: §e" + progress.kills + "/" + KnightProofQuestState022.REQUIRED_KILLS), false);
                    return ActionResult.SUCCESS;
                }

                Item proof = Registries.ITEM.getOrEmpty(new Identifier("crowncinder", "swordsman_proof")).orElse(Items.PAPER);
                ItemStack reward = new ItemStack(proof);
                if (proof == Items.PAPER) reward.setCustomName(Text.literal("§6기사의 증명"));
                serverPlayer.giveItemStack(reward);
                progress.stage = 2;
                state.markDirty();
                serverPlayer.sendMessage(Text.literal("§6[황제] §f훌륭하군. 이제 그대를 제국의 기사로 인정하겠다."), false);
                serverPlayer.sendMessage(Text.literal("§a퀘스트 완료: §f기사의 증명 획득"), false);
                return ActionResult.SUCCESS;
            }

            serverPlayer.sendMessage(Text.literal("§6[황제] §f그대는 이미 기사의 자격을 증명했다."), false);
            return ActionResult.SUCCESS;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof HostileEntity)) return;
            if (!(damageSource.getAttacker() instanceof ServerPlayerEntity player)) return;
            KnightProofQuestState022 state = KnightProofQuestState022.get(player.getServerWorld());
            KnightProofQuestState022.Progress progress = state.progress(player.getUuid());
            if (progress.stage != 1 || progress.kills >= KnightProofQuestState022.REQUIRED_KILLS) return;
            progress.kills++;
            state.markDirty();
            player.sendMessage(Text.literal("§e[기사의 증명] §f적대 몬스터 처치 §a" + progress.kills + "/" + KnightProofQuestState022.REQUIRED_KILLS), true);
            if (progress.kills >= KnightProofQuestState022.REQUIRED_KILLS) {
                player.sendMessage(Text.literal("§6황제에게 돌아가 보고하면 '기사의 증명'을 받을 수 있습니다."), false);
            }
        });
    }

    private static boolean isEmperor(Entity entity) {
        if (!entity.hasCustomName()) return false;
        String name = entity.getCustomName().getString();
        return name.contains("황제") && !name.contains("수호기사");
    }
}
