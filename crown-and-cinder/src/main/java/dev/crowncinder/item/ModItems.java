package dev.crowncinder.item;

import dev.crowncinder.CrownCinder;
import dev.crowncinder.entity.ModEntities;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {
    public static Item LONGSWORD;
    public static Item GREATSWORD;
    public static Item TWINBLADE;
    public static Item GOBLIN_EGG;
    private static Item register(String name, Item item) { return Registry.register(Registries.ITEM, CrownCinder.id(name), item); }
    public static void init() {
        LONGSWORD = register("longsword", new RpgSword(3, -2.4f, 300, 1, false));
        GREATSWORD = register("greatsword", new RpgSword(6, -3.1f, 450, CrownCinder.CONFIG.greatswordLevel, true));
        TWINBLADE = register("twinblade", new RpgSword(1, -1.8f, 240, CrownCinder.CONFIG.twinbladeLevel, false));
        GOBLIN_EGG = register("goblin_spawn_egg", new SpawnEggItem(ModEntities.GOBLIN, 0x476f35, 0x4b3024, new Item.Settings()));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(e -> { e.add(LONGSWORD); e.add(GREATSWORD); e.add(TWINBLADE); });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(e -> e.add(GOBLIN_EGG));
    }
}
