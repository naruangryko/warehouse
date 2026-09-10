package dev.crowncinder.progress;

import dev.crowncinder.CrownCinder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import java.util.*;

public final class ProgressStore extends PersistentState {
    private final Map<UUID,Progress> players=new HashMap<>();
    public static ProgressStore get(MinecraftServer s){return s.getOverworld().getPersistentStateManager().getOrCreate(ProgressStore::read,ProgressStore::new,"crowncinder_players");}
    public Progress player(UUID id){return players.computeIfAbsent(id,k->{markDirty();return new Progress();});}
    public static ProgressStore read(NbtCompound root){ ProgressStore s=new ProgressStore(); NbtCompound es=root.getCompound("players"); for(String key:es.getKeys())try{ UUID id=UUID.fromString(key); NbtCompound n=es.getCompound(key); Progress p=new Progress();
        p.level=n.getInt("level"); p.xp=n.getInt("xp"); p.points=n.getInt("points"); p.strength=n.getInt("strength"); p.vitality=n.getInt("vitality"); p.defense=n.getInt("defense"); p.agility=n.getInt("agility"); p.attackSpeed=n.getInt("attackSpeed"); p.moveSpeed=n.getInt("moveSpeed"); p.magicPower=n.getInt("magicPower"); p.magicDefense=n.getInt("magicDefense"); p.critChance=n.getInt("critChance"); p.critDamage=n.getInt("critDamage"); p.regeneration=n.getInt("regeneration"); p.stamina=n.getInt("stamina");
        p.job=n.contains("job")?n.getString("job"):"wanderer"; p.adventurerRank=n.contains("adventurerRank")?n.getString("adventurerRank"):"NONE"; p.adventurerPoints=n.getInt("adventurerPoints");
        p.repAurelia=n.getInt("repAurelia");p.repEldoria=n.getInt("repEldoria");p.repKharum=n.getInt("repKharum");p.repSylvan=n.getInt("repSylvan");p.repVarkhan=n.getInt("repVarkhan");
        p.questActive=n.getBoolean("questActive");p.questKills=n.getInt("questKills");p.questClaimed=n.getBoolean("questClaimed");p.hud=!n.contains("hud")||n.getBoolean("hud");p.sanitize(CrownCinder.CONFIG.xpBase);s.players.put(id,p);
    }catch(IllegalArgumentException e){CrownCinder.LOGGER.warn("Skipping malformed player UUID in RPG save: {}",key);} return s; }
    @Override public NbtCompound writeNbt(NbtCompound root){NbtCompound es=new NbtCompound();players.forEach((id,p)->{NbtCompound n=new NbtCompound();
        n.putInt("level",p.level);n.putInt("xp",p.xp);n.putInt("points",p.points);n.putInt("strength",p.strength);n.putInt("vitality",p.vitality);n.putInt("defense",p.defense);n.putInt("agility",p.agility);n.putInt("attackSpeed",p.attackSpeed);n.putInt("moveSpeed",p.moveSpeed);n.putInt("magicPower",p.magicPower);n.putInt("magicDefense",p.magicDefense);n.putInt("critChance",p.critChance);n.putInt("critDamage",p.critDamage);n.putInt("regeneration",p.regeneration);n.putInt("stamina",p.stamina);n.putString("job",p.job);n.putString("adventurerRank",p.adventurerRank);n.putInt("adventurerPoints",p.adventurerPoints);n.putInt("repAurelia",p.repAurelia);n.putInt("repEldoria",p.repEldoria);n.putInt("repKharum",p.repKharum);n.putInt("repSylvan",p.repSylvan);n.putInt("repVarkhan",p.repVarkhan);n.putBoolean("questActive",p.questActive);n.putInt("questKills",p.questKills);n.putBoolean("questClaimed",p.questClaimed);n.putBoolean("hud",p.hud);es.put(id.toString(),n);});root.putInt("schemaVersion",2);root.put("players",es);return root;}
}
