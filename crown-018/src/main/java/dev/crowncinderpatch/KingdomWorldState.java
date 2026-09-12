package dev.crowncinderpatch;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

/** Stores immutable kingdom anchor and one-time versioned capital upgrade state. */
public final class KingdomWorldState extends PersistentState {
    public boolean anchorSet;
    public int anchorX;
    public int anchorZ;
    public boolean built020;
    public boolean npcsSeeded;
    public boolean built021;

    public static KingdomWorldState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(KingdomWorldState::read, KingdomWorldState::new, "crowncinder_020_world");
    }

    public static KingdomWorldState read(NbtCompound nbt) {
        KingdomWorldState s = new KingdomWorldState();
        s.anchorSet = nbt.getBoolean("anchorSet");
        s.anchorX = nbt.getInt("anchorX");
        s.anchorZ = nbt.getInt("anchorZ");
        s.built020 = nbt.getBoolean("built020");
        s.npcsSeeded = nbt.getBoolean("npcsSeeded");
        s.built021 = nbt.getBoolean("built021");
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("anchorSet", anchorSet);
        nbt.putInt("anchorX", anchorX);
        nbt.putInt("anchorZ", anchorZ);
        nbt.putBoolean("built020", built020);
        nbt.putBoolean("npcsSeeded", npcsSeeded);
        nbt.putBoolean("built021", built021);
        return nbt;
    }
}
