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
    public static Item LONGSWORD, GREATSWORD, TWINBLADE, RAPIER, SABER, KATANA, SPEAR, WARHAMMER, BATTLEAXE, DAGGER, CLAYMORE, RUNE_BLADE, KNIGHT_BLADE;
    public static Item GOBLIN_EGG, KINGDOM_MAP;
    public static CoinItem COPPER_COIN, SILVER_COIN, GOLD_COIN;
    public static Item GUILD_LEDGER;
    private static Item register(String name, Item item) { return Registry.register(Registries.ITEM, CrownCinder.id(name), item); }
    public static void init() {
        LONGSWORD = register("longsword", new RpgSword(3, -2.4f, 300, 1, false));
        GREATSWORD = register("greatsword", new RpgSword(6, -3.1f, 450, CrownCinder.CONFIG.greatswordLevel, true));
        TWINBLADE = register("twinblade", new RpgSword(1, -1.8f, 240, CrownCinder.CONFIG.twinbladeLevel, false));
        RAPIER = register("rapier", new RpgSword(2, -1.7f, 260, 4, false));
        SABER = register("saber", new RpgSword(3, -2.0f, 320, 7, false));
        KATANA = register("katana", new RpgSword(4, -2.1f, 380, 12, false));
        SPEAR = register("spear", new RpgSword(4, -2.5f, 420, 10, false));
        WARHAMMER = register("warhammer", new RpgSword(7, -3.3f, 520, 20, true));
        BATTLEAXE = register("battleaxe", new RpgSword(6, -3.0f, 480, 16, true));
        DAGGER = register("dagger", new RpgSword(1, -1.2f, 210, 3, false));
        CLAYMORE = register("claymore", new RpgSword(7, -3.2f, 560, 24, true));
        RUNE_BLADE = register("rune_blade", new RpgSword(5, -2.0f, 620, 32, false));
        KNIGHT_BLADE = register("knight_blade", new RpgSword(5, -2.3f, 700, 28, false));
        COPPER_COIN = (CoinItem)register("copper_coin", new CoinItem(1,"동화"));
        SILVER_COIN = (CoinItem)register("silver_coin", new CoinItem(100,"은화"));
        GOLD_COIN = (CoinItem)register("gold_coin", new CoinItem(10000,"금화"));
        GUILD_LEDGER = register("guild_ledger", new GuildLedgerItem());
        KINGDOM_MAP = register("kingdom_map", new KingdomMapItem());
        GOBLIN_EGG = register("goblin_spawn_egg", new SpawnEggItem(ModEntities.GOBLIN, 0x476f35, 0x4b3024, new Item.Settings()));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(e -> {
            e.add(LONGSWORD); e.add(GREATSWORD); e.add(TWINBLADE); e.add(RAPIER); e.add(SABER); e.add(KATANA); e.add(SPEAR);
            e.add(WARHAMMER); e.add(BATTLEAXE); e.add(DAGGER); e.add(CLAYMORE); e.add(RUNE_BLADE); e.add(KNIGHT_BLADE);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(e -> { e.add(COPPER_COIN); e.add(SILVER_COIN); e.add(GOLD_COIN); e.add(GUILD_LEDGER); e.add(KINGDOM_MAP); });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(e -> e.add(GOBLIN_EGG));
    }
}
