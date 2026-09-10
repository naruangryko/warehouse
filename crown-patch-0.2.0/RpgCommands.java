package dev.crowncinder.quest;

import dev.crowncinder.CrownCinder;
import dev.crowncinder.progress.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class RpgCommands {
    private static final String[] STATS={"strength","vitality","defense","agility","attackspeed","movespeed","magic","magicdefense","critchance","critdamage","regen","stamina"};
    private static final String[] JOBS={"warrior","knight","archer","mage","paladin","assassin","lancer","spellblade"};
    public static void init(){CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->{
        var root=CommandManager.literal("rpg").executes(c->status(c.getSource().getPlayerOrThrow()))
            .then(CommandManager.literal("stats").executes(c->status(c.getSource().getPlayerOrThrow())))
            .then(CommandManager.literal("nations").executes(c->nations(c.getSource().getPlayerOrThrow())))
            .then(CommandManager.literal("hud").executes(c->{var pl=c.getSource().getPlayerOrThrow();Progress p=ProgressService.get(pl);p.hud=!p.hud;ProgressService.store(pl).markDirty();pl.sendMessage(Text.literal("RPG HUD: "+(p.hud?"ON":"OFF")),false);return 1;}));
        var spend=CommandManager.literal("spend"); for(String stat:STATS) spend.then(CommandManager.literal(stat).executes(c->{var pl=c.getSource().getPlayerOrThrow(); if(!ProgressService.get(pl).allocate(stat)){pl.sendMessage(Text.literal("포인트가 없거나 능력치 이름이 올바르지 않습니다."),false);return 0;}ProgressService.store(pl).markDirty();ProgressService.apply(pl);return status(pl);})); root.then(spend);
        var job=CommandManager.literal("job").executes(c->{var pl=c.getSource().getPlayerOrThrow();pl.sendMessage(Text.literal("현재 직업: "+ProgressService.get(pl).job+" | /rpg job <직업> (최초 1회)"),false);return 1;});
        for(String j:JOBS) job.then(CommandManager.literal(j).executes(c->{var pl=c.getSource().getPlayerOrThrow();boolean ok=ProgressService.get(pl).chooseJob(j);if(ok){ProgressService.store(pl).markDirty();ProgressService.apply(pl);}pl.sendMessage(Text.literal(ok?"직업 선택 완료: "+j:"직업은 최초 1회만 선택할 수 있습니다."),false);return ok?1:0;}));root.then(job);
        root.then(CommandManager.literal("adventurer").executes(c->adv(c.getSource().getPlayerOrThrow()))
            .then(CommandManager.literal("join").executes(c->{var pl=c.getSource().getPlayerOrThrow();boolean ok=ProgressService.get(pl).joinAdventurers();ProgressService.store(pl).markDirty();pl.sendMessage(Text.literal(ok?"모험가 길드 등록 완료: 4급":"이미 모험가로 등록되어 있습니다."),false);return ok?1:0;}))
            .then(CommandManager.literal("promote").executes(c->{var pl=c.getSource().getPlayerOrThrow();boolean ok=ProgressService.get(pl).promoteAdventurer();ProgressService.store(pl).markDirty();pl.sendMessage(Text.literal(ok?"모험가 등급이 상승했습니다!":"승급 포인트가 부족하거나 최고 등급입니다."),false);return ok?1:0;})));
        root.then(CommandManager.literal("quest").executes(c->quest(c.getSource().getPlayerOrThrow()))
            .then(CommandManager.literal("accept").executes(c->{var pl=c.getSource().getPlayerOrThrow();boolean ok=ProgressService.get(pl).acceptQuest();ProgressService.store(pl).markDirty();pl.sendMessage(Text.literal(ok?"의뢰 수락: 가시숲 고블린 5마리 처치":"현재 받을 수 없는 의뢰입니다."),false);return ok?1:0;}))
            .then(CommandManager.literal("claim").executes(c->{var pl=c.getSource().getPlayerOrThrow();if(!ProgressService.get(pl).claimQuest()){pl.sendMessage(Text.literal("의뢰 목표를 아직 달성하지 못했습니다."),false);return 0;}ProgressService.store(pl).markDirty();ProgressService.award(pl,CrownCinder.CONFIG.questXp);ItemStack reward=new ItemStack(Items.EMERALD,CrownCinder.CONFIG.questEmeralds);if(!pl.getInventory().insertStack(reward))pl.dropItem(reward,false);pl.sendMessage(Text.literal("의뢰 완료! XP, 에메랄드, 오렐리아 평판 +5, 모험가 포인트 +60"),false);return 1;})));
        dispatcher.register(root);
    });}
    private static int status(ServerPlayerEntity pl){Progress p=ProgressService.get(pl);pl.sendMessage(Text.literal("§6[Crown & Cinder] §fLv."+p.level+" XP "+p.xp+"/"+p.requiredXp(CrownCinder.CONFIG.xpBase)+" | 직업 "+p.job+" | 모험가 "+p.adventurerRank+"급"),false);pl.sendMessage(Text.literal("§c힘 "+p.strength+" 체력 "+p.vitality+" 방어 "+p.defense+" §a민첩 "+p.agility+" 공속 "+p.attackSpeed+" 이속 "+p.moveSpeed),false);pl.sendMessage(Text.literal("§b마력 "+p.magicPower+" 마방 "+p.magicDefense+" §e치확 "+p.critChance+" 치피 "+p.critDamage+" 재생 "+p.regeneration+" 스태미나 "+p.stamina+" | 남은 포인트 "+p.points),false);pl.sendMessage(Text.literal("/rpg job | /rpg spend <stat> | /rpg adventurer | /rpg quest | /rpg nations"),false);return 1;}
    private static int adv(ServerPlayerEntity pl){Progress p=ProgressService.get(pl);pl.sendMessage(Text.literal("모험가 등급: "+p.adventurerRank+" | 승급 포인트: "+p.adventurerPoints+" | join / promote"),false);return 1;}
    private static int quest(ServerPlayerEntity pl){Progress p=ProgressService.get(pl);pl.sendMessage(Text.literal("가시숲 토벌: "+p.questKills+"/5 | "+(p.questClaimed?"완료":p.questActive?"진행 중":"/rpg quest accept")),false);return 1;}
    private static int nations(ServerPlayerEntity pl){Progress p=ProgressService.get(pl);pl.sendMessage(Text.literal("§6오렐리아 왕국§f(기사·석조) 평판 "+p.repAurelia+" | §9엘도리아§f(마법·학문) "+p.repEldoria),false);pl.sendMessage(Text.literal("§7카룸 산악국§f(광산·중갑) "+p.repKharum+" | §2실반 연맹§f(숲·궁술) "+p.repSylvan+" | §4바르칸 제국§f(군사·요새) "+p.repVarkhan),false);return 1;}
}
