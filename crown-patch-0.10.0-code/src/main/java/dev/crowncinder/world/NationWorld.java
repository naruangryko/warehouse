package dev.crowncinder.world;

import dev.crowncinder.item.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

public final class NationWorld {
    public static final String[] NATIONS={"aurelia","eldoria","kharum","sylvan","varkhan"};
    private NationWorld(){}
    public static BlockPos center(ServerWorld w,String n){
        BlockPos s=w.getSpawnPos(); int dx=0,dz=0;
        switch(n){
            case "aurelia" -> {dx=180;dz=0;}
            case "eldoria" -> {dx=55;dz=-170;}
            case "kharum" -> {dx=-145;dz=-105;}
            case "sylvan" -> {dx=-145;dz=105;}
            case "varkhan" -> {dx=55;dz=170;}
        }
        int x=s.getX()+dx,z=s.getZ()+dz; int y=w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
        return new BlockPos(x,y,z);
    }
    public static void buildAll(ServerPlayerEntity p){
        ServerWorld w=p.getServerWorld();
        for(String n:NATIONS) RpgOutpost.buildAt(w,center(w,n),n);
        giveMap(p);
        p.sendMessage(Text.literal("§6[왕국 개척] §f월드 스폰 주변에 5개 국가의 초기 거점을 생성했습니다. 왕국 지도를 우클릭해 위치를 확인하세요."),false);
    }
    public static void buildVillages(ServerPlayerEntity p){
        ServerWorld w=p.getServerWorld();
        for(String n:NATIONS) FantasyVillage.build(w,center(w,n),n);
        p.sendMessage(Text.literal("§6[마을 재건] §f5개 왕국의 민간 마을 구역을 새 판타지 디자인으로 다시 만들었습니다."),false);
    }
    public static void buildCentralCastle(ServerPlayerEntity p){
        CentralCastle.build(p.getServerWorld(), true);
        p.sendMessage(Text.literal("§6[중앙성] §f월드 스폰 중앙에 거대한 왕성을 재건했습니다."),false);
    }
    public static void giveMap(ServerPlayerEntity p){ ItemStack s=new ItemStack(ModItems.KINGDOM_MAP); if(!p.getInventory().insertStack(s))p.dropItem(s,false); }
    public static void showMap(ServerPlayerEntity p){
        ServerWorld w=p.getServerWorld(); BlockPos here=p.getBlockPos();
        p.sendMessage(Text.literal("§e==== Crown & Cinder 왕국 지도 ===="),false);
        for(String n:NATIONS){ BlockPos c=center(w,n); double d=Math.sqrt(here.getSquaredDistance(c)); String dir=direction(c.getX()-here.getX(),c.getZ()-here.getZ());
            p.sendMessage(Text.literal("§6"+RpgOutpost.nationName(n)+" §f| "+dir+" 약 "+Math.round(d)+"블록 | X "+c.getX()+" Z "+c.getZ()),false);
        }
        p.sendMessage(Text.literal("§7모든 국가 중심은 월드 스폰 기준 약 180블록 반경에 배치됩니다."),false);
    }
    private static String direction(int dx,int dz){
        String ns=dz< -20?"북":dz>20?"남":""; String ew=dx>20?"동":dx< -20?"서":""; String r=ns+ew; return r.isEmpty()?"현재 위치 근처":r;
    }
}
