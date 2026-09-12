package dev.crowncinderpatch;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player emperor quest progress for earning the Swordsman's/Knight's Proof. */
public final class KnightProofQuestState022 extends PersistentState {
    public static final int REQUIRED_KILLS = 12;

    public static final class Progress {
        public int stage; // 0 not accepted, 1 active, 2 completed
        public int kills;
    }

    private final Map<UUID, Progress> players = new HashMap<>();

    public static KnightProofQuestState022 get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
            KnightProofQuestState022::read,
            KnightProofQuestState022::new,
            "crowncinder_022_knight_proof_quest");
    }

    public Progress progress(UUID id) {
        return players.computeIfAbsent(id, k -> new Progress());
    }

    public static KnightProofQuestState022 read(NbtCompound nbt) {
        KnightProofQuestState022 state = new KnightProofQuestState022();
        NbtList list = nbt.getList("players", 10);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound e = list.getCompound(i);
            if (!e.containsUuid("uuid")) continue;
            Progress p = new Progress();
            p.stage = e.getInt("stage");
            p.kills = e.getInt("kills");
            state.players.put(e.getUuid("uuid"), p);
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, Progress> entry : players.entrySet()) {
            NbtCompound e = new NbtCompound();
            e.putUuid("uuid", entry.getKey());
            e.putInt("stage", entry.getValue().stage);
            e.putInt("kills", entry.getValue().kills);
            list.add(e);
        }
        nbt.put("players", list);
        return nbt;
    }
}
