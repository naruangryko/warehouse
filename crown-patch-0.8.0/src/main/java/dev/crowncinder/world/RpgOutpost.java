package dev.crowncinder.world;

import dev.crowncinder.entity.CrownGuardEntity;
import dev.crowncinder.entity.ModEntities;
import dev.crowncinder.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;

public final class RpgOutpost {
    private RpgOutpost() {}
    public static void build(ServerPlayerEntity player,String nation){ BlockPos o=player.getBlockPos().add(0,-1,10); buildAt(player.getServerWorld(),o,nation); player.sendMessage(Text.literal("§6"+nationName(nation)+" 초기 거점 생성 완료."),false); }
    public static void buildAt(ServerWorld w,BlockPos o,String nation){
        Palette p=palette(nation);
        for(int x=-24;x<=24;x++)for(int z=-17;z<=17;z++){set(w,o.add(x,0,z),(x==0||z==0||Math.abs(x)==8)?p.road:p.floor);for(int y=1;y<=8;y++)set(w,o.add(x,y,z),Blocks.AIR);}
        building(w,o.add(-16,1,0),9,11,p,"castle");
        building(w,o.add(0,1,0),9,11,p,"guild");
        building(w,o.add(16,1,0),9,11,p,"market");
        building(w,o.add(-8,1,11),7,7,p,"smithy");
        building(w,o.add(8,1,11),7,7,p,"inn");
        building(w,o.add(-8,1,-11),7,7,p,"house");
        building(w,o.add(8,1,-11),7,7,p,"house");

        spawnMerchant(w,o.add(16,1,1),nation); spawnClerk(w,o.add(0,1,1),nation); spawnSteward(w,o.add(17,1,1),nation);
        villager(w,o.add(-7,1,12),"§6[대장간] §f"+nationName(nation)+" 대장장이",VillagerProfession.WEAPONSMITH,"blacksmith",nation,false);
        villager(w,o.add(8,1,12),"§d[여관] §f"+nationName(nation)+" 여관주인",VillagerProfession.BUTCHER,"innkeeper",nation,false);

        combatNpc(w,o.add(-18,1,-3),"§f[검사] "+nationName(nation)+" 순찰검사","swordsman",nation,ModItems.LONGSWORD,false);
        combatNpc(w,o.add(-14,1,-3),"§c[기사단장] "+nationName(nation)+" 단장","captain",nation,ModItems.KNIGHT_BLADE,true);
        combatNpc(w,o.add(1,1,-3),"§e[용병] 자유 용병","mercenary",nation,ModItems.BATTLEAXE,false);
        combatNpc(w,o.add(-1,1,-3),"§e[용병] 창병 용병","mercenary",nation,ModItems.SPEAR,false);
        combatNpc(w,o.add(-20,1,5),"§7[기사] 성문 경비기사","guard",nation,ModItems.CLAYMORE,false);
        combatNpc(w,o.add(-12,1,5),"§7[기사] 성채 순찰기사","guard",nation,ModItems.SABER,false);
    }
    private static void building(ServerWorld w,BlockPos c,int sx,int sz,Palette p,String kind){
        int hx=sx/2,hz=sz/2;
        for(int x=-hx;x<=hx;x++)for(int z=-hz;z<=hz;z++){set(w,c.add(x,0,z),p.road);for(int y=1;y<=4;y++){boolean wall=x==-hx||x==hx||z==-hz||z==hz;set(w,c.add(x,y,z),wall?p.wall:Blocks.AIR);}set(w,c.add(x,5,z),p.roof);}
        set(w,c.add(0,1,-hz),Blocks.AIR);set(w,c.add(0,2,-hz),Blocks.AIR);
        if("castle".equals(kind)){set(w,c.add(0,1,2),p.accent);set(w,c.add(0,2,2),p.accent);}
        if("guild".equals(kind)){set(w,c.add(0,1,2),Blocks.BARREL);set(w,c.add(1,1,2),Blocks.CRAFTING_TABLE);}
        if("market".equals(kind)){set(w,c.add(0,1,2),Blocks.CHEST);set(w,c.add(1,1,2),Blocks.SMITHING_TABLE);}
        if("smithy".equals(kind)){set(w,c.add(0,1,1),Blocks.ANVIL);set(w,c.add(1,1,1),Blocks.SMITHING_TABLE);set(w,c.add(-1,1,1),Blocks.BLAST_FURNACE);}
        if("inn".equals(kind)){set(w,c.add(-1,1,1),Blocks.RED_BED);set(w,c.add(1,1,1),Blocks.RED_BED);set(w,c.add(0,1,2),Blocks.BARREL);}
        if("house".equals(kind)){set(w,c.add(0,1,1),Blocks.WHITE_BED);set(w,c.add(1,1,1),Blocks.CHEST);}
    }
    private static void spawnMerchant(ServerWorld w,BlockPos pos,String nation){ VillagerEntity v=villager(w,pos,"§6[상점] §f"+nationName(nation)+" 무기상",VillagerProfession.WEAPONSMITH,"merchant",nation,false); if(v==null)return;
        v.getOffers().add(new TradeOffer(new ItemStack(ModItems.COPPER_COIN,40),new ItemStack(Items.BREAD,4),999,0,0));
        v.getOffers().add(new TradeOffer(new ItemStack(ModItems.SILVER_COIN,4),new ItemStack(ModItems.LONGSWORD),999,0,0));
        v.getOffers().add(new TradeOffer(new ItemStack(ModItems.SILVER_COIN,6),new ItemStack(ModItems.RAPIER),999,0,0));
        v.getOffers().add(new TradeOffer(new ItemStack(ModItems.SILVER_COIN,8),new ItemStack(ModItems.KATANA),999,0,0));
        v.getOffers().add(new TradeOffer(new ItemStack(ModItems.SILVER_COIN,12),new ItemStack(ModItems.KNIGHT_BLADE),999,0,0));
        v.getOffers().add(new TradeOffer(new ItemStack(ModItems.SILVER_COIN,14),new ItemStack(ModItems.RUNE_BLADE),999,0,0));
    }
    private static void spawnClerk(ServerWorld w,BlockPos pos,String nation){villager(w,pos,"§b[용병단 길드] §f접수원",VillagerProfession.LIBRARIAN,"guild",nation,false);}
    private static void spawnSteward(ServerWorld w,BlockPos pos,String nation){villager(w,pos,"§a[국가] §f"+nationName(nation)+" 서기관",VillagerProfession.CARTOGRAPHER,"steward",nation,false);}
    private static void combatNpc(ServerWorld w,BlockPos pos,String name,String role,String nation,Item weapon,boolean commander){
        CrownGuardEntity g=ModEntities.CROWN_GUARD.create(w); if(g==null)return; g.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);g.setCustomName(Text.literal(name));g.setCustomNameVisible(true);g.addCommandTag("crown_npc");g.addCommandTag("crown_role_"+role);g.addCommandTag("crown_nation_"+nation);g.equipRole(new ItemStack(weapon),commander);w.spawnEntity(g);
    }
    private static VillagerEntity villager(ServerWorld w,BlockPos pos,String name,VillagerProfession prof,String role,String nation,boolean moving){ VillagerEntity v=EntityType.VILLAGER.create(w); if(v==null)return null; v.setVillagerData(new VillagerData(VillagerType.PLAINS,prof,5));v.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);v.setCustomName(Text.literal(name));v.setCustomNameVisible(true);v.setPersistent();v.setAiDisabled(!moving);v.addCommandTag("crown_npc");v.addCommandTag("crown_role_"+role);v.addCommandTag("crown_nation_"+nation);w.spawnEntity(v);return v; }
    private static void set(ServerWorld w,BlockPos p,Block b){w.setBlockState(p,b.getDefaultState(),3);}
    private static Palette palette(String n){return switch(n){case "eldoria"->new Palette(Blocks.POLISHED_DEEPSLATE,Blocks.CALCITE,Blocks.DEEPSLATE_BRICKS,Blocks.AMETHYST_BLOCK,Blocks.DARK_OAK_SLAB);case "kharum"->new Palette(Blocks.COBBLESTONE,Blocks.STONE_BRICKS,Blocks.COBBLED_DEEPSLATE,Blocks.IRON_BLOCK,Blocks.STONE_SLAB);case "sylvan"->new Palette(Blocks.MOSS_BLOCK,Blocks.OAK_PLANKS,Blocks.OAK_LOG,Blocks.EMERALD_BLOCK,Blocks.OAK_SLAB);case "varkhan"->new Palette(Blocks.POLISHED_BLACKSTONE,Blocks.DEEPSLATE_TILES,Blocks.BLACKSTONE,Blocks.REDSTONE_BLOCK,Blocks.NETHER_BRICK_SLAB);default->new Palette(Blocks.STONE_BRICKS,Blocks.SMOOTH_STONE,Blocks.STONE_BRICKS,Blocks.GOLD_BLOCK,Blocks.SMOOTH_STONE_SLAB);};}
    private record Palette(Block floor,Block road,Block wall,Block accent,Block roof){}
    public static String nationName(String n){return switch(n){case "aurelia"->"오렐리아 왕국";case "eldoria"->"엘도리아 마도왕국";case "kharum"->"카룸 산악국";case "sylvan"->"실반 연맹";case "varkhan"->"바르칸 제국";default->"무소속";};}
}
