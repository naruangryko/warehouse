package dev.crowncinder.world;
import dev.crowncinder.entity.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
public final class RpgRaid{private RpgRaid(){}public static void start(ServerPlayerEntity p){ServerWorld w=p.getServerWorld();BlockPos o=p.getBlockPos();for(int i=0;i<6;i++)spawn(w,ModEntities.GOBLIN,o.add((i%3-1)*4,0,10+(i/3)*4));for(int i=0;i<3;i++)spawn(w,ModEntities.ORC,o.add((i-1)*5,0,18));spawn(w,ModEntities.TROLL,o.add(0,0,24));p.sendMessage(Text.literal("§4[습격] §f고블린·오크 부대가 접근합니다. 후방에는 트롤이 있습니다!"),false);}private static <T extends MobEntity> void spawn(ServerWorld w,EntityType<T> t,BlockPos p){T e=t.create(w);if(e==null)return;e.refreshPositionAndAngles(p.getX()+.5,p.getY(),p.getZ()+.5,0,0);e.setPersistent();w.spawnEntity(e);}}
