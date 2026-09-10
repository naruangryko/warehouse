package dev.crowncinder.world;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.*;
public final class RpgServiceNpcs{private RpgServiceNpcs(){}public static void spawn(ServerPlayerEntity p){ServerWorld w=p.getServerWorld();BlockPos o=p.getBlockPos().add(0,0,4);villager(w,o.add(-2,0,0),"§d[여관] §f여관주인",VillagerProfession.BUTCHER,"inn");villager(w,o.add(2,0,0),"§7[대장장이] §f왕국 대장장이",VillagerProfession.TOOLSMITH,"smith");p.sendMessage(Text.literal("§6서비스 NPC 생성 완료: §f여관주인과 대장장이를 우클릭하세요."),false);}private static void villager(ServerWorld w,BlockPos pos,String name,VillagerProfession prof,String role){VillagerEntity v=EntityType.VILLAGER.create(w);if(v==null)return;v.setVillagerData(new VillagerData(VillagerType.PLAINS,prof,5));v.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);v.setCustomName(Text.literal(name));v.setCustomNameVisible(true);v.setPersistent();v.setAiDisabled(true);v.addCommandTag("crown_service_"+role);w.spawnEntity(v);}}
