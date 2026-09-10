package dev.crowncinder.gametest;

import dev.crowncinder.CrownCinder;
import dev.crowncinder.entity.ModEntities;
import dev.crowncinder.item.ModItems;
import dev.crowncinder.progress.ProgressStore;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import java.util.UUID;

/** Runs inside a real Minecraft test server; never packaged in the install JAR. */
public final class CrownGameTests implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void registryAndGoblinLoad(TestContext context) {
        context.assertTrue(CrownCinder.CONFIG != null, "Common mod entrypoint did not load");
        context.assertTrue(Registries.ITEM.get(CrownCinder.id("longsword")) == ModItems.LONGSWORD,
            "Longsword is not registered");
        context.assertTrue(Registries.ITEM.get(CrownCinder.id("greatsword")) == ModItems.GREATSWORD,
            "Greatsword is not registered");
        context.assertTrue(Registries.ITEM.get(CrownCinder.id("twinblade")) == ModItems.TWINBLADE,
            "Twinblade is not registered");
        context.assertTrue(new ItemStack(ModItems.LONGSWORD).isDamageable(), "Sword must have durability");
        var goblin = context.spawnEntity(ModEntities.GOBLIN, 2, 2, 2);
        context.assertTrue(goblin.isAlive(), "Goblin failed to spawn");
        context.assertTrue(goblin.getMaxHealth() == CrownCinder.CONFIG.goblinHealth,
            "Goblin default attributes were not applied");
        goblin.discard();
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void playerSaveRoundTrip(TestContext context) {
        var store = new ProgressStore();
        var alice = UUID.fromString("842773fb-a197-4994-935f-000000000001");
        var bob = UUID.fromString("842773fb-a197-4994-935f-000000000002");
        var p = store.player(alice);
        p.gainXp(CrownCinder.CONFIG.xpBase, CrownCinder.CONFIG.xpBase);
        p.allocate("vitality");
        p.acceptQuest(); p.goblinKilled();
        var copy = ProgressStore.read(store.writeNbt(new NbtCompound()));
        var restored = copy.player(alice);
        context.assertTrue(restored.level == 2 && restored.vitality == 1 && restored.points == 1,
            "Progress or allocated points were lost during NBT round trip");
        context.assertTrue(restored.questActive && restored.questKills == 1,
            "Quest state was lost during NBT round trip");
        context.assertTrue(copy.player(bob).level == 1 && copy.player(bob).questKills == 0,
            "Different players share progress");
        context.complete();
    }
}
