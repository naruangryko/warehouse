package dev.crowncinder.world;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Adds a visually distinct civilian village district around each national stronghold. */
public final class FantasyVillage {
    private FantasyVillage() {}

    public static void build(ServerWorld w, BlockPos o, String nation) {
        switch (nation) {
            case "eldoria" -> eldoria(w, o);
            case "kharum" -> kharum(w, o);
            case "sylvan" -> sylvan(w, o);
            case "varkhan" -> varkhan(w, o);
            default -> aurelia(w, o);
        }
    }

    private static void aurelia(ServerWorld w, BlockPos o) {
        avenue(w, o, Blocks.STONE_BRICKS, Blocks.SMOOTH_STONE);
        for (int[] p : new int[][]{{-34,-12},{-34,8},{34,-12},{34,8},{-19,27},{19,27}})
            townhouse(w, o.add(p[0],1,p[1]), Blocks.BIRCH_PLANKS, Blocks.BLUE_WOOL, Blocks.QUARTZ_BLOCK);
        farm(w, o.add(-34,1,27), Blocks.WHEAT); farm(w, o.add(34,1,27), Blocks.WHEAT);
        plaza(w, o.add(0,1,30), Blocks.QUARTZ_BLOCK, Blocks.GOLD_BLOCK);
    }

    private static void eldoria(ServerWorld w, BlockPos o) {
        avenue(w, o, Blocks.POLISHED_DEEPSLATE, Blocks.CALCITE);
        for (int[] p : new int[][]{{-36,-8},{-27,22},{-8,31},{15,30},{34,13},{34,-14}})
            arcaneHouse(w, o.add(p[0],1,p[1]));
        crystalGarden(w, o.add(-34,1,31));
        crystalGarden(w, o.add(32,1,31));
    }

    private static void kharum(ServerWorld w, BlockPos o) {
        avenue(w, o, Blocks.COBBLED_DEEPSLATE, Blocks.STONE_BRICKS);
        for (int[] p : new int[][]{{-36,-12},{-36,10},{-18,31},{5,31},{31,22},{36,-7}})
            bunker(w, o.add(p[0],1,p[1]));
        forgeCourt(w, o.add(31,1,-27));
        oreYard(w, o.add(-31,1,-28));
    }

    private static void sylvan(ServerWorld w, BlockPos o) {
        avenue(w, o, Blocks.MOSS_BLOCK, Blocks.OAK_PLANKS);
        for (int[] p : new int[][]{{-36,-9},{-31,18},{-15,32},{12,33},{34,18},{37,-7}})
            woodlandHome(w, o.add(p[0],1,p[1]));
        grove(w, o.add(-34,1,31)); grove(w, o.add(34,1,31));
    }

    private static void varkhan(ServerWorld w, BlockPos o) {
        avenue(w, o, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.DEEPSLATE_TILES);
        for (int[] p : new int[][]{{-36,-13},{-36,4},{-36,21},{36,-13},{36,4},{36,21}})
            barracks(w, o.add(p[0],1,p[1]));
        watchPost(w, o.add(-22,1,33)); watchPost(w, o.add(22,1,33));
    }

    private static void avenue(ServerWorld w, BlockPos o, Block ground, Block road) {
        for (int x = -42; x <= 42; x++) for (int z = -34; z <= 36; z++) {
            if (Math.abs(x) > 27 || z > 21) set(w, o.add(x,1,z), ground);
        }
        for (int x = -42; x <= 42; x++) for (int z = -2; z <= 2; z++) set(w,o.add(x,2,z),road);
        for (int z = 20; z <= 36; z++) for (int x = -2; x <= 2; x++) set(w,o.add(x,2,z),road);
    }

    private static void townhouse(ServerWorld w, BlockPos c, Block wall, Block roof, Block trim) {
        clearBox(w,c,6,6,5); foundation(w,c,6,5,Blocks.STONE_BRICKS);
        walls(w,c,6,5,5,wall); fill(w,c,-4,6,-3,4,6,3,roof); fill(w,c,-1,1,-3,1,3,-3,Blocks.AIR);
        for(int y=2;y<=4;y++){set(w,c.add(-4,y,0),trim);set(w,c.add(4,y,0),trim);} set(w,c.add(0,2,3),Blocks.GLASS_PANE);
    }
    private static void arcaneHouse(ServerWorld w, BlockPos c) {
        clearBox(w,c,6,6,7); foundation(w,c,5,5,Blocks.POLISHED_DEEPSLATE); walls(w,c,5,5,6,Blocks.CALCITE);
        for(int y=7;y<=10;y++) fill(w,c,-2,y,-2,2,y,2,Blocks.DEEPSLATE_TILES);
        set(w,c.add(0,11,0),Blocks.AMETHYST_CLUSTER); fill(w,c,-5,7,-5,5,7,5,Blocks.PURPLE_WOOL); fill(w,c,-1,1,-5,1,3,-5,Blocks.AIR);
        set(w,c.add(3,3,5),Blocks.END_ROD); set(w,c.add(-3,3,5),Blocks.END_ROD);
    }
    private static void bunker(ServerWorld w, BlockPos c) {
        clearBox(w,c,6,6,6); foundation(w,c,6,5,Blocks.COBBLED_DEEPSLATE); walls(w,c,6,5,5,Blocks.STONE_BRICKS);
        fill(w,c,-6,6,-5,6,7,5,Blocks.COBBLED_DEEPSLATE); fill(w,c,-1,1,-5,1,3,-5,Blocks.AIR);
        set(w,c.add(3,2,-5),Blocks.IRON_BARS); set(w,c.add(-3,2,-5),Blocks.IRON_BARS);
    }
    private static void woodlandHome(ServerWorld w, BlockPos c) {
        clearBox(w,c,6,6,7); foundation(w,c,5,5,Blocks.OAK_PLANKS);
        for(int y=1;y<=6;y++) {set(w,c.add(-5,y,-5),Blocks.OAK_LOG);set(w,c.add(5,y,-5),Blocks.OAK_LOG);set(w,c.add(-5,y,5),Blocks.OAK_LOG);set(w,c.add(5,y,5),Blocks.OAK_LOG);}
        walls(w,c,5,5,5,Blocks.SPRUCE_PLANKS); fill(w,c,-6,6,-6,6,6,6,Blocks.OAK_LEAVES); fill(w,c,-1,1,-5,1,3,-5,Blocks.AIR);
    }
    private static void barracks(ServerWorld w, BlockPos c) {
        clearBox(w,c,6,8,6); foundation(w,c,6,5,Blocks.POLISHED_BLACKSTONE_BRICKS); walls(w,c,6,5,6,Blocks.DEEPSLATE_BRICKS);
        fill(w,c,-6,7,-5,6,7,5,Blocks.RED_WOOL); fill(w,c,-1,1,-5,1,3,-5,Blocks.AIR);
        for(int x=-4;x<=4;x+=4)set(w,c.add(x,2,-5),Blocks.IRON_BARS);
    }

    private static void farm(ServerWorld w, BlockPos c, Block crop) {
        for(int x=-7;x<=7;x++)for(int z=-6;z<=6;z++){set(w,c.add(x,0,z),Blocks.FARMLAND);if(z==0)set(w,c.add(x,0,z),Blocks.WATER);else set(w,c.add(x,1,z),crop);}
    }
    private static void plaza(ServerWorld w, BlockPos c, Block floor, Block centerpiece) {
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)if(x*x+z*z<=64)set(w,c.add(x,0,z),floor);
        set(w,c.add(0,1,0),Blocks.QUARTZ_PILLAR);set(w,c.add(0,2,0),Blocks.QUARTZ_PILLAR);set(w,c.add(0,3,0),centerpiece);
    }
    private static void crystalGarden(ServerWorld w,BlockPos c){for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++)if(x*x+z*z<45)set(w,c.add(x,0,z),Blocks.CALCITE);for(int[]p:new int[][]{{0,0},{4,2},{-4,-2},{2,-4},{-2,4}}){set(w,c.add(p[0],1,p[1]),Blocks.AMETHYST_BLOCK);set(w,c.add(p[0],2,p[1]),Blocks.AMETHYST_CLUSTER);}}
    private static void forgeCourt(ServerWorld w,BlockPos c){fill(w,c,-7,0,-7,7,0,7,Blocks.STONE_BRICKS);set(w,c.add(0,1,0),Blocks.BLAST_FURNACE);set(w,c.add(2,1,0),Blocks.ANVIL);set(w,c.add(-2,1,0),Blocks.SMITHING_TABLE);for(int x=-6;x<=6;x+=4)set(w,c.add(x,1,5),Blocks.MAGMA_BLOCK);}
    private static void oreYard(ServerWorld w,BlockPos c){fill(w,c,-6,0,-6,6,0,6,Blocks.COBBLED_DEEPSLATE);for(int[]p:new int[][]{{-3,-2},{2,-3},{3,2},{-2,3}}){set(w,c.add(p[0],1,p[1]),Blocks.IRON_ORE);set(w,c.add(p[0],2,p[1]),Blocks.COAL_ORE);}}
    private static void grove(ServerWorld w,BlockPos c){for(int[]p:new int[][]{{-5,-4},{4,-5},{5,4},{-4,5},{0,0}})tree(w,c.add(p[0],0,p[1]));}
    private static void tree(ServerWorld w,BlockPos c){fill(w,c,0,0,0,0,5,0,Blocks.OAK_LOG);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=4;y<=7;y++)if(Math.abs(x)+Math.abs(z)<4)set(w,c.add(x,y,z),Blocks.OAK_LEAVES);}
    private static void watchPost(ServerWorld w,BlockPos c){fill(w,c,-2,0,-2,2,8,2,Blocks.BLACKSTONE);fill(w,c,-3,9,-3,3,9,3,Blocks.RED_WOOL);fill(w,c,-1,1,-2,1,3,-2,Blocks.AIR);}

    private static void foundation(ServerWorld w,BlockPos c,int hx,int hz,Block b){fill(w,c,-hx,0,-hz,hx,0,hz,b);}
    private static void walls(ServerWorld w,BlockPos c,int hx,int hz,int h,Block b){for(int y=1;y<=h;y++)for(int x=-hx;x<=hx;x++)for(int z=-hz;z<=hz;z++){boolean edge=Math.abs(x)==hx||Math.abs(z)==hz;set(w,c.add(x,y,z),edge?b:Blocks.AIR);}}
    private static void clearBox(ServerWorld w,BlockPos c,int hx,int hz,int h){for(int x=-hx;x<=hx;x++)for(int z=-hz;z<=hz;z++)for(int y=1;y<=h+4;y++)set(w,c.add(x,y,z),Blocks.AIR);}
    private static void fill(ServerWorld w,BlockPos o,int x1,int y1,int z1,int x2,int y2,int z2,Block b){for(int x=Math.min(x1,x2);x<=Math.max(x1,x2);x++)for(int y=Math.min(y1,y2);y<=Math.max(y1,y2);y++)for(int z=Math.min(z1,z2);z<=Math.max(z1,z2);z++)set(w,o.add(x,y,z),b);}
    private static void set(ServerWorld w,BlockPos p,Block b){w.setBlockState(p,b.getDefaultState(),3);}
}
