package dev.crowncinder.progress;

import dev.crowncinder.CrownCinder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Stored in overworld data/crowncinder_players.dat; shared across dimensions. */
public final class ProgressStore extends PersistentState {
    private final Map<UUID, Progress> players = new HashMap<>();
    public static ProgressStore get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
            ProgressStore::read, ProgressStore::new, "crowncinder_players");
    }
    public Progress player(UUID id) {
        return players.computeIfAbsent(id, key -> { markDirty(); return new Progress(); });
    }
    public static ProgressStore read(NbtCompound root) {
        ProgressStore store = new ProgressStore();
        NbtCompound entries = root.getCompound("players");
        for (String key : entries.getKeys()) {
            try {
                UUID id = UUID.fromString(key);
                NbtCompound n = entries.getCompound(key);
                Progress p = new Progress();
                p.level = n.getInt("level"); p.xp = n.getInt("xp");
                p.points = n.getInt("points"); p.strength = n.getInt("strength");
                p.vitality = n.getInt("vitality"); p.agility = n.getInt("agility");
                p.questActive = n.getBoolean("questActive"); p.questKills = n.getInt("questKills");
                p.questClaimed = n.getBoolean("questClaimed");
                p.hud = !n.contains("hud") || n.getBoolean("hud");
                p.sanitize(CrownCinder.CONFIG.xpBase);
                store.players.put(id, p);
            } catch (IllegalArgumentException e) {
                CrownCinder.LOGGER.warn("Skipping malformed player UUID in RPG save: {}", key);
            }
        }
        return store;
    }
    @Override public NbtCompound writeNbt(NbtCompound root) {
        NbtCompound entries = new NbtCompound();
        players.forEach((id, p) -> {
            NbtCompound n = new NbtCompound();
            n.putInt("level", p.level); n.putInt("xp", p.xp); n.putInt("points", p.points);
            n.putInt("strength", p.strength); n.putInt("vitality", p.vitality); n.putInt("agility", p.agility);
            n.putBoolean("questActive", p.questActive); n.putInt("questKills", p.questKills);
            n.putBoolean("questClaimed", p.questClaimed); n.putBoolean("hud", p.hud);
            entries.put(id.toString(), n);
        });
        root.putInt("schemaVersion", 1);
        root.put("players", entries);
        return root;
    }
}
