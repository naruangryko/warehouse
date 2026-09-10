package dev.crowncinder.progress;

import dev.crowncinder.CrownCinder;
import net.minecraft.entity.attribute.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.UUID;

public final class ProgressService {
    private static final UUID STR=UUID.fromString("795a754d-4d46-443f-94ab-100000000001");
    private static final UUID VIT=UUID.fromString("795a754d-4d46-443f-94ab-100000000002");
    private static final UUID MOVE=UUID.fromString("795a754d-4d46-443f-94ab-100000000003");
    private static final UUID DEF=UUID.fromString("795a754d-4d46-443f-94ab-100000000004");
    private static final UUID ASPD=UUID.fromString("795a754d-4d46-443f-94ab-100000000005");
    public static ProgressStore store(ServerPlayerEntity p){return ProgressStore.get(p.getServer());}
    public static Progress get(ServerPlayerEntity p){return store(p).player(p.getUuid());}
    public static void award(ServerPlayerEntity p,int raw){ Progress r=get(p); int lv=r.gainXp((int)(raw*CrownCinder.CONFIG.xpMultiplier),CrownCinder.CONFIG.xpBase); store(p).markDirty(); if(lv>0)p.sendMessage(Text.translatable("message.crowncinder.level",r.level,r.points),false); }
    private static void mod(ServerPlayerEntity p,EntityAttribute a,UUID id,double amount){ EntityAttributeInstance i=p.getAttributeInstance(a); if(i==null)return; EntityAttributeModifier o=i.getModifier(id); if(o!=null&&o.getValue()==amount)return; i.removeModifier(id); if(amount!=0)i.addTemporaryModifier(new EntityAttributeModifier(id,"crowncinder",amount,EntityAttributeModifier.Operation.ADDITION)); }
    public static void apply(ServerPlayerEntity p){
        Progress r=get(p);
        double jobDamage = switch(r.job){case "warrior"->1.5; case "knight","paladin","lancer","spellblade"->1.0; case "assassin"->0.75; default->0;};
        double jobHealth = switch(r.job){case "knight","paladin"->4; case "warrior","lancer"->2; default->0;};
        mod(p,EntityAttributes.GENERIC_ATTACK_DAMAGE,STR,r.strength*CrownCinder.CONFIG.damagePerStrength+jobDamage);
        mod(p,EntityAttributes.GENERIC_MAX_HEALTH,VIT,r.vitality*CrownCinder.CONFIG.healthPerVitality+jobHealth);
        mod(p,EntityAttributes.GENERIC_ARMOR,DEF,r.defense*0.35+("knight".equals(r.job)?2:0));
        mod(p,EntityAttributes.GENERIC_ATTACK_SPEED,ASPD,r.attackSpeed*0.025+r.agility*0.01);
        mod(p,EntityAttributes.GENERIC_MOVEMENT_SPEED,MOVE,Math.min(0.16,r.moveSpeed*0.002+r.agility*CrownCinder.CONFIG.speedPerAgility));
        if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
    }
    public static void regen(ServerPlayerEntity p){ Progress r=get(p); if(r.regeneration>0&&p.isAlive()&&p.getHealth()<p.getMaxHealth()) p.heal(Math.min(1.5f,(float)r.regeneration*0.03f)); }
}
