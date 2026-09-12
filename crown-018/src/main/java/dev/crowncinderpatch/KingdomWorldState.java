package dev.crowncinderpatch;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

/** Stores the immutable world anchor, surface-capital state, and automatic NPC population state. */
public final class KingdomWorldState extends PersistentState {
    public boolean anchorSet;
    public int anchorX;
    public int anchorZ;
    public boolean built020;
    public boolean npcsSeeded;

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
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("anchorSet", anchorSet);
        nbt.putInt("anchorX", anchorX);
        nbt.putInt("anchorZ", anchorZ);
        nbt.putBoolean("built020", built020);
        nbt.putBoolean("npcsSeeded", npcsSeeded);
        return nbt;
    }
}
