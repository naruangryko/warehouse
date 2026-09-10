package dev.crowncinder.world;
import dev.crowncinder.entity.ModEntities;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
public final class RpgDungeon {
 private RpgDungeon(){}
 public static void build(ServerPlayerEntity player){ServerWorld w=player.getServerWorld();BlockPos o=player.getBlockPos().add(0,-1,14);
  for(int x=-8;x<=8;x++)for(int z=-11;z<=11;z++){w.setBlockState(o.add(x,0,z),Blocks.DEEPSLATE_BRICKS.getDefaultState(),3);for(int y=1;y<=6;y++)w.setBlockState(o.add(x,y,z),(Math.abs(x)==8||Math.abs(z)==11)?Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState():Blocks.AIR.getDefaultState(),3);w.setBlockState(o.add(x,7,z),Blocks.DEEPSLATE_TILES.getDefaultState(),3);}for(int y=1;y<=3;y++){w.setBlockState(o.add(0,y,-11),Blocks.AIR.getDefaultState(),3);w.setBlockState(o.add(1,y,-11),Blocks.AIR.getDefaultState(),3);}w.setBlockState(o.add(0,1,9),Blocks.CHEST.getDefaultState(),3);
  spawn(w,ModEntities.GOBLIN,o.add(-4,1,-4));spawn(w,ModEntities.GOBLIN,o.add(4,1,-4));spawn(w,ModEntities.ORC,o.add(-4,1,2));spawn(w,ModEntities.WEREWOLF,o.add(4,1,2));spawn(w,ModEntities.TROLL,o.add(0,1,7));player.sendMessage(Text.literal("§5[던전] §f폐허 지하요새 생성 완료. 마지막 방의 트롤을 조심하세요."),false);}
 private static <T extends MobEntity> void spawn(ServerWorld w,EntityType<T> t,BlockPos p){T e=t.create(w);if(e==null)return;e.refreshPositionAndAngles(p.getX()+.5,p.getY(),p.getZ()+.5,0,0);e.setPersistent();w.spawnEntity(e);}
}
