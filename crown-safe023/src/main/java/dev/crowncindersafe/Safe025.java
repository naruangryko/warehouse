package dev.crowncindersafe;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.ClickType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Crown & Cinder 0.25 safe RPG layer. */
public final class Safe025 implements ModInitializer {
    public static final String MOD_ID = "crowncinder023safe";
    public static final Item ARCANE_STAFF = new ArcaneStaff(new Item.Settings().maxCount(1));
    public static final Item SPELLBOOK = new SpellBook(new Item.Settings().maxCount(16));
    public static final Item DIVINE_BLESSING = new DivineBlessing(new Item.Settings().maxCount(1));
    public static final Item MANA_POTION = new ManaPotion(new Item.Settings().maxCount(16));

    private static final String SELECTED_SPELL_KEY = "CrownSelectedSpell";
    private static final List<String> STAT_NAMES = List.of(
        "attack", "health", "defense", "critchance", "critdamage", "speed", "stamina", "mana"
    );
    private static int serverTicks = 0;
    private static int worldTicks = 0;

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, id("arcane_staff"), ARCANE_STAFF);
        Registry.register(Registries.ITEM, id("spellbook"), SPELLBOOK);
        Registry.register(Registries.ITEM, id("divine_blessing"), DIVINE_BLESSING);
        Registry.register(Registries.ITEM, id("mana_potion"), MANA_POTION);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> {
            ServerPlayerEntity p = handler.getPlayer();
            try {
                PlayerData data = data(p);
                normalizeResources(p, data, true);
                if (data.job.equals("none")) {
                    p.sendMessage(Text.literal("§6[Crown & Cinder] §f직업을 선택하세요: §b기사§f 또는 §d마법사"), false);
                    openClassMenu(p);
                }
                syncMilestones(p);
            } catch (Throwable ignored) {}
        }));

        ServerTickEvents.END_SERVER_TICK.register(this::serverTick);
        ServerTickEvents.END_WORLD_TICK.register(this::worldTick);
        AttackEntityCallback.EVENT.register(this::onAttack);
        UseItemCallback.EVENT.register(this::onUseItem);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var rpg = CommandManager.literal("rpg");

            rpg.then(CommandManager.literal("stats")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    openStatsMenu(p);
                    return 1;
                })
                .then(CommandManager.literal("up")
                    .then(CommandManager.argument("stat", StringArgumentType.word())
                        .suggests((ctx, builder) -> CommandSource.suggestMatching(STAT_NAMES, builder))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 1000))
                            .executes(ctx -> {
                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                String stat = canonicalStat(StringArgumentType.getString(ctx, "stat"));
                                int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                if (stat == null) {
                                    ctx.getSource().sendError(Text.literal("사용 가능: attack, health, defense, critchance, critdamage, speed, stamina, mana"));
                                    return 0;
                                }
                                int done = 0;
                                for (int i = 0; i < amount; i++) if (spendPoint(p, stat, false)) done++; else break;
                                if (done <= 0) return 0;
                                ctx.getSource().sendFeedback(() -> Text.literal("§a" + stat + " §f+" + done + "회 강화"), false);
                                return 1;
                            })))));

            rpg.then(CommandManager.literal("class")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    PlayerData d = data(p);
                    if (d.job.equals("none")) openClassMenu(p);
                    else p.sendMessage(Text.literal("§6[직업] §f" + jobName(d.job) + (d.blessed ? " §d· 신의 축복 활성" : "")), false);
                    return 1;
                }));

            rpg.then(CommandManager.literal("resources")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    showResources(p, false);
                    return 1;
                }));

            rpg.then(CommandManager.literal("magic")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    int unlocked = magicTier(p);
                    PlayerData d = data(p);
                    p.sendMessage(Text.literal("§d[마법] §f" + jobName(d.job) + " · 사용 가능 " + unlocked + "/10 · MP " + (int)d.mana + "/" + maxMana(p)), false);
                    return 1;
                })
                .then(CommandManager.literal("book").requires(s -> s.hasPermissionLevel(2)).executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    p.giveItemStack(new ItemStack(SPELLBOOK));
                    ItemStack staff = new ItemStack(ARCANE_STAFF);
                    setSelectedSpell(staff, 1);
                    p.giveItemStack(staff);
                    return 1;
                })));

            rpg.then(CommandManager.literal("blessing").requires(s -> s.hasPermissionLevel(2)).executes(ctx -> {
                ctx.getSource().getPlayer().giveItemStack(new ItemStack(DIVINE_BLESSING));
                return 1;
            }));
            rpg.then(CommandManager.literal("mana_potion").requires(s -> s.hasPermissionLevel(2)).executes(ctx -> {
                ctx.getSource().getPlayer().giveItemStack(new ItemStack(MANA_POTION, 3));
                return 1;
            }));

            dispatcher.register(rpg);
        });
    }

    private static Identifier id(String path) { return new Identifier(MOD_ID, path); }

    private void serverTick(MinecraftServer server) {
        serverTicks++;
        if (serverTicks % 20 == 0) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                try {
                    PlayerData d = data(p);
                    normalizeResources(p, d, false);
                    d.mana = Math.min(maxMana(p), d.mana + manaRegen(p));
                    d.stamina = Math.min(maxStamina(p), d.stamina + staminaRegen(p));
                    state(p).markDirty();
                    syncMilestones(p);
                } catch (Throwable ignored) {}
            }
        }
        if (serverTicks % 40 == 0) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                try { showResources(p, true); } catch (Throwable ignored) {}
            }
        }
    }

    private static void showResources(ServerPlayerEntity p, boolean actionbar) {
        PlayerData d = data(p);
        String msg = "§bMP " + (int)d.mana + "/" + maxMana(p) + " §8| §eST " + (int)d.stamina + "/" + maxStamina(p) + " §8| §6" + jobName(d.job);
        p.sendMessage(Text.literal(msg), actionbar);
    }

    private static void normalizeResources(ServerPlayerEntity p, PlayerData d, boolean fillIfZero) {
        int mm = maxMana(p);
        int ms = maxStamina(p);
        if (fillIfZero && d.mana <= 0) d.mana = mm;
        if (fillIfZero && d.stamina <= 0) d.stamina = ms;
        d.mana = Math.max(0, Math.min(mm, d.mana));
        d.stamina = Math.max(0, Math.min(ms, d.stamina));
        state(p).markDirty();
    }

    private static int maxMana(ServerPlayerEntity p) {
        PlayerData d = data(p);
        int mp = Math.max(0, getStat(p, "mana"));
        int base = d.job.equals("mage") ? 150 : 80;
        int scale = d.job.equals("mage") ? 10 : 7;
        return base + mp * scale;
    }

    private static int maxStamina(ServerPlayerEntity p) {
        PlayerData d = data(p);
        int st = Math.max(0, getStat(p, "stamina"));
        int base = d.job.equals("knight") ? 150 : 100;
        int scale = d.job.equals("knight") ? 9 : 7;
        return base + st * scale;
    }

    private static double manaRegen(ServerPlayerEntity p) {
        PlayerData d = data(p);
        return Math.max(2.0, maxMana(p) * (d.job.equals("mage") ? 0.045 : 0.025));
    }

    private static double staminaRegen(ServerPlayerEntity p) {
        PlayerData d = data(p);
        return Math.max(4.0, maxStamina(p) * (d.job.equals("knight") ? 0.065 : 0.045));
    }

    private ActionResult onAttack(PlayerEntity player, World world, Hand hand, Entity entity, net.minecraft.util.hit.EntityHitResult hit) {
        if (world.isClient || !(player instanceof ServerPlayerEntity p)) return ActionResult.PASS;
        ItemStack held = p.getStackInHand(hand);
        if (!(held.getItem() instanceof SwordItem)) return ActionResult.PASS;
        try {
            PlayerData d = data(p);
            if (d.job.equals("mage") && !d.blessed) {
                p.sendMessage(Text.literal("§c마법사는 검을 사용할 수 없습니다. '신의 축복'이 필요합니다."), true);
                return ActionResult.FAIL;
            }
            if (!d.job.equals("knight") && !d.blessed) return ActionResult.PASS;
            if (!consumeStamina(p, 5)) {
                p.sendMessage(Text.literal("§e스태미나가 부족합니다."), true);
                return ActionResult.FAIL;
            }
            if (entity instanceof net.minecraft.entity.LivingEntity living && rollCrit(p)) {
                double base = Math.max(1.0, p.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                float extra = (float)(base * (critMultiplier(p) - 1.0));
                living.damage(p.getDamageSources().playerAttack(p), extra);
                p.getServerWorld().spawnParticles(ParticleTypes.CRIT, living.getX(), living.getBodyY(0.55), living.getZ(), 18, 0.45, 0.45, 0.45, 0.15);
                p.sendMessage(Text.literal("§6✦ 치명타!"), true);
            }
        } catch (Throwable ignored) {}
        return ActionResult.PASS;
    }

    private TypedActionResult<ItemStack> onUseItem(PlayerEntity player, World world, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient || !(player instanceof ServerPlayerEntity p) || !(stack.getItem() instanceof SwordItem)) return TypedActionResult.pass(stack);
        try {
            PlayerData d = data(p);
            if (d.job.equals("mage") && !d.blessed) return TypedActionResult.fail(stack);
            if (p.isSneaking() && (d.job.equals("knight") || d.blessed)) {
                if (!consumeStamina(p, 24)) {
                    p.sendMessage(Text.literal("§e기사 기술 사용에 필요한 스태미나가 부족합니다. (24)"), true);
                    return TypedActionResult.fail(stack);
                }
                double base = Math.max(4.0, p.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                Box area = p.getBoundingBox().expand(4.5);
                for (HostileEntity e : p.getServerWorld().getEntitiesByClass(HostileEntity.class, area, x -> x.isAlive())) {
                    if (e.squaredDistanceTo(p) <= 20.25) e.damage(p.getDamageSources().playerAttack(p), (float)(base * 1.8));
                }
                p.getServerWorld().spawnParticles(ParticleTypes.SWEEP_ATTACK, p.getX(), p.getBodyY(0.6), p.getZ(), 24, 2.2, 0.7, 2.2, 0.02);
                p.getServerWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.3f, 0.75f);
                p.getItemCooldownManager().set(stack.getItem(), 35);
                p.sendMessage(Text.literal("§6[기사 기술] §f왕국의 휩쓸기! §7(ST -24)"), true);
                return TypedActionResult.success(stack, false);
            }
        } catch (Throwable ignored) {}
        return TypedActionResult.pass(stack);
    }

    private static boolean consumeStamina(ServerPlayerEntity p, double amount) {
        PlayerData d = data(p);
        if (d.stamina + 0.0001 < amount) return false;
        d.stamina -= amount;
        state(p).markDirty();
        return true;
    }

    private static boolean consumeMana(ServerPlayerEntity p, double amount) {
        PlayerData d = data(p);
        if (d.mana + 0.0001 < amount) return false;
        d.mana -= amount;
        state(p).markDirty();
        return true;
    }

    private static boolean canUseMagic(ServerPlayerEntity p) {
        PlayerData d = data(p);
        return d.job.equals("mage") || d.blessed;
    }

    private static int magicTier(ServerPlayerEntity p) {
        if (!canUseMagic(p)) return 0;
        int fromLevel = Math.min(10, Math.max(0, getLevel(p) / 10));
        int fromBook = p.getCommandTags().contains("ccsafe_magic_book") ? 1 : 0;
        return Math.max(fromLevel, fromBook);
    }

    private static int manaCost(int spell) {
        return switch (spell) {
            case 1 -> 10; case 2 -> 16; case 3 -> 22; case 4 -> 30; case 5 -> 38;
            case 6 -> 50; case 7 -> 62; case 8 -> 76; case 9 -> 94; case 10 -> 120;
            default -> 10;
        };
    }

    private static boolean rollCrit(ServerPlayerEntity p) {
        double chance = Math.min(75.0, 5.0 + Math.max(0, getStat(p, "critchance")) * 0.45);
        if (data(p).job.equals("mage")) chance += 5.0;
        return p.getRandom().nextDouble() * 100.0 < Math.min(80.0, chance);
    }

    private static double critMultiplier(ServerPlayerEntity p) {
        double mult = 1.50 + Math.max(0, getStat(p, "critdamage")) * 0.012;
        if (data(p).job.equals("mage")) mult += 0.20;
        return Math.min(4.0, mult);
    }

    private static void openClassMenu(ServerPlayerEntity p) {
        p.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, player) -> new ClassMenu(syncId, inv, p), Text.literal("직업 선택 - 한 번 선택하면 고정")));
    }

    private static void openStatsMenu(ServerPlayerEntity p) {
        p.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, player) -> new StatsMenu(syncId, inv, p), Text.literal("Crown & Cinder 스탯")));
    }

    private static ItemStack named(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.setCustomName(Text.literal(name));
        return stack;
    }

    private abstract static class LockedMenu extends ScreenHandler {
        protected final SimpleInventory menu;
        protected final ServerPlayerEntity player;
        protected LockedMenu(ScreenHandlerType<?> type, int syncId, PlayerInventory inv, ServerPlayerEntity player, int rows) {
            super(type, syncId);
            this.player = player;
            this.menu = new SimpleInventory(rows * 9);
            for (int r = 0; r < rows; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(menu, c + r * 9, 8 + c * 18, 18 + r * 18));
            int baseY = 31 + rows * 18;
            for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, baseY + r * 18));
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, baseY + 58));
        }
        @Override public boolean canUse(PlayerEntity player) { return true; }
        @Override public ItemStack quickMove(PlayerEntity player, int index) { return ItemStack.EMPTY; }
    }

    private static final class ClassMenu extends LockedMenu {
        ClassMenu(int syncId, PlayerInventory inv, ServerPlayerEntity p) {
            super(ScreenHandlerType.GENERIC_9X1, syncId, inv, p, 1);
            menu.setStack(2, named(Items.IRON_SWORD, "§b기사 §7- 공격력·체력 특화"));
            menu.setStack(6, named(Items.BLAZE_ROD, "§d마법사 §7- 마력·치확·치피 특화"));
        }
        @Override public void onSlotClick(int slotIndex, int button, ClickType actionType, PlayerEntity clicker) {
            if (slotIndex == 2) chooseJob(player, "knight");
            else if (slotIndex == 6) chooseJob(player, "mage");
            else if (slotIndex >= 0 && slotIndex < 9) return;
            else super.onSlotClick(slotIndex, button, actionType, clicker);
        }
    }

    private static final class StatsMenu extends LockedMenu {
        StatsMenu(int syncId, PlayerInventory inv, ServerPlayerEntity p) {
            super(ScreenHandlerType.GENERIC_9X3, syncId, inv, p, 3);
            refresh();
        }
        private void refresh() {
            menu.clear();
            menu.setStack(4, named(Items.EXPERIENCE_BOTTLE, "§e남은 포인트: " + getPoints(player)));
            menu.setStack(10, named(Items.IRON_SWORD, "§c공격력 §f" + getStat(player,"attack") + " §7[클릭 +1]"));
            menu.setStack(11, named(Items.GOLDEN_APPLE, "§a체력 §f" + getStat(player,"health") + " §7[클릭 +1]"));
            menu.setStack(12, named(Items.SHIELD, "§9방어력 §f" + getStat(player,"defense") + " §7[클릭 +1]"));
            menu.setStack(13, named(Items.ARROW, "§e치명타 확률 §f" + getStat(player,"critchance") + " §7[클릭 +1]"));
            menu.setStack(14, named(Items.NETHERITE_AXE, "§6치명타 피해 §f" + getStat(player,"critdamage") + " §7[클릭 +1]"));
            menu.setStack(15, named(Items.FEATHER, "§f속도 §f" + getStat(player,"speed") + " §7[클릭 +1]"));
            menu.setStack(16, named(Items.RABBIT_FOOT, "§2스태미나 §f" + getStat(player,"stamina") + " §7[클릭 +1]"));
            menu.setStack(22, named(Items.AMETHYST_SHARD, "§d마력 §f" + getStat(player,"mana") + " §7[클릭 +1]"));
        }
        @Override public void onSlotClick(int slotIndex, int button, ClickType actionType, PlayerEntity clicker) {
            String stat = switch (slotIndex) {
                case 10 -> "attack"; case 11 -> "health"; case 12 -> "defense"; case 13 -> "critchance";
                case 14 -> "critdamage"; case 15 -> "speed"; case 16 -> "stamina"; case 22 -> "mana"; default -> null;
            };
            if (stat != null) {
                spendPoint(player, stat, true);
                refresh();
                sendContentUpdates();
                return;
            }
            if (slotIndex >= 0 && slotIndex < 27) return;
            super.onSlotClick(slotIndex, button, actionType, clicker);
        }
    }

    private static void chooseJob(ServerPlayerEntity p, String job) {
        PlayerData d = data(p);
        if (!d.job.equals("none")) {
            p.sendMessage(Text.literal("§c직업은 이미 " + jobName(d.job) + "로 선택되어 있습니다."), false);
            p.closeHandledScreen();
            return;
        }
        d.job = job;
        if (job.equals("knight")) {
            addRawStat(p, "attack", 6); addRawStat(p, "health", 6); addRawStat(p, "defense", 2);
            ItemStack sword = Registries.ITEM.containsId(new Identifier("crowncinder","knight_blade")) ? new ItemStack(Registries.ITEM.get(new Identifier("crowncinder","knight_blade"))) : new ItemStack(Items.IRON_SWORD);
            p.giveItemStack(sword);
        } else {
            addRawStat(p, "mana", 6); addRawStat(p, "critchance", 6); addRawStat(p, "critdamage", 10);
            ItemStack staff = new ItemStack(ARCANE_STAFF); setSelectedSpell(staff, 1); p.giveItemStack(staff);
            p.giveItemStack(new ItemStack(SPELLBOOK));
        }
        normalizeResources(p, d, true);
        state(p).markDirty();
        refresh(p);
        p.closeHandledScreen();
        p.sendMessage(Text.literal("§6[직업 선택] §f" + jobName(job) + "로 전직했습니다."), false);
    }

    private static boolean spendPoint(ServerPlayerEntity p, String stat, boolean feedback) {
        try {
            Object progress = progress(p);
            Field points = field(progress, "points");
            int pt = points.getInt(progress);
            if (pt <= 0) {
                if (feedback) p.sendMessage(Text.literal("§c남은 스탯 포인트가 없습니다."), true);
                return false;
            }
            int amount = specialtyAmount(p, stat);
            applyStatIncrement(progress, stat, amount);
            points.setInt(progress, pt - 1);
            dirtyAndApply(p);
            normalizeResources(p, data(p), false);
            if (feedback) p.sendMessage(Text.literal("§a" + statDisplay(stat) + " +" + amount + " §7(포인트 -1)"), true);
            return true;
        } catch (Throwable t) {
            if (feedback) p.sendMessage(Text.literal("§c스탯 강화에 실패했습니다."), true);
            return false;
        }
    }

    private static int specialtyAmount(ServerPlayerEntity p, String stat) {
        String job = data(p).job;
        if (job.equals("knight") && (stat.equals("attack") || stat.equals("health"))) return 2;
        if (job.equals("mage") && (stat.equals("mana") || stat.equals("critchance") || stat.equals("critdamage"))) return 2;
        return 1;
    }

    private static void applyStatIncrement(Object progress, String stat, int amount) throws Exception {
        if (stat.equals("speed")) {
            Field move = field(progress, "moveSpeed"); move.setInt(progress, Math.min(10000, move.getInt(progress) + amount));
            Field atk = field(progress, "attackSpeed"); atk.setInt(progress, Math.min(10000, atk.getInt(progress) + amount));
            return;
        }
        String f = statField(stat);
        if (f == null) throw new IllegalArgumentException(stat);
        Field field = field(progress, f);
        field.setInt(progress, Math.min(10000, field.getInt(progress) + amount));
    }

    private static void addRawStat(ServerPlayerEntity p, String stat, int amount) {
        try { Object pr = progress(p); applyStatIncrement(pr, stat, amount); dirtyAndApply(p); } catch (Throwable ignored) {}
    }

    private static int getPoints(ServerPlayerEntity p) {
        try { Object pr = progress(p); return field(pr, "points").getInt(pr); } catch (Throwable ignored) { return 0; }
    }

    private static int getStat(ServerPlayerEntity p, String stat) {
        try {
            Object pr = progress(p);
            if (stat.equals("speed")) return field(pr,"moveSpeed").getInt(pr);
            String f = statField(stat);
            if (f == null) return 0;
            return field(pr, f).getInt(pr);
        } catch (Throwable ignored) { return 0; }
    }

    private static String canonicalStat(String stat) {
        return switch (stat.toLowerCase()) {
            case "attack", "atk", "공격", "공격력", "strength", "힘" -> "attack";
            case "health", "hp", "체력", "vitality" -> "health";
            case "defense", "def", "방어", "방어력" -> "defense";
            case "critchance", "crit", "치확", "치명타확률" -> "critchance";
            case "critdamage", "치피", "치명타피해" -> "critdamage";
            case "speed", "속도", "movespeed", "attackspeed" -> "speed";
            case "stamina", "스태미나" -> "stamina";
            case "mana", "magic", "마력" -> "mana";
            default -> null;
        };
    }

    private static String statField(String stat) {
        String c = canonicalStat(stat);
        if (c == null) return null;
        return switch (c) {
            case "attack" -> "strength"; case "health" -> "vitality"; case "defense" -> "defense";
            case "critchance" -> "critChance"; case "critdamage" -> "critDamage";
            case "stamina" -> "stamina"; case "mana" -> "magicPower"; default -> null;
        };
    }

    private static String statDisplay(String stat) {
        return switch (stat) {
            case "attack" -> "공격력"; case "health" -> "체력"; case "defense" -> "방어력";
            case "critchance" -> "치명타 확률"; case "critdamage" -> "치명타 피해"; case "speed" -> "속도";
            case "stamina" -> "스태미나"; case "mana" -> "마력"; default -> stat;
        };
    }

    private static void syncMilestones(ServerPlayerEntity p) {
        PlayerData d = data(p);
        if (d.job.equals("none")) return;
        int max = Math.min(100, (getLevel(p) / 10) * 10);
        for (int m = 10; m <= max; m += 10) {
            String tag = "ccsafe025_growth_" + m;
            if (p.getCommandTags().contains(tag)) continue;
            p.addCommandTag(tag);
            if (d.job.equals("knight")) {
                addRawStat(p,"attack",3); addRawStat(p,"health",3); addRawStat(p,"defense",1); addRawStat(p,"stamina",1);
                p.sendMessage(Text.literal("§6[기사 성장] §fLv."+m+": 공격 +3 · 체력 +3 · 방어 +1 · 스태미나 +1"), false);
            } else {
                addRawStat(p,"mana",3); addRawStat(p,"critchance",2); addRawStat(p,"critdamage",3); addRawStat(p,"health",1);
                p.sendMessage(Text.literal("§d[마법사 성장] §fLv."+m+": 마력 +3 · 치확 +2 · 치피 +3 · 체력 +1"), false);
            }
        }
    }

    private static final class ArcaneStaff extends Item {
        ArcaneStaff(Settings s) { super(s); }
        @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (world.isClient || !(user instanceof ServerPlayerEntity p)) return TypedActionResult.success(stack, world.isClient());
            try {
                if (!canUseMagic(p)) {
                    p.sendMessage(Text.literal("§c기사는 마법을 사용할 수 없습니다. '신의 축복'이 필요합니다."), true);
                    return TypedActionResult.fail(stack);
                }
                int unlocked = magicTier(p);
                if (unlocked <= 0) {
                    p.sendMessage(Text.literal("§dLv.10을 달성하거나 마법서를 사용해 마법을 배우세요."), false);
                    return TypedActionResult.fail(stack);
                }
                int selected = selectedSpell(stack, unlocked);
                if (p.isSneaking()) {
                    int next = selected >= unlocked ? 1 : selected + 1;
                    setSelectedSpell(stack,next);
                    p.sendMessage(Text.literal("§d[마법 선택] §f"+spellName(next)+" §7(MP "+manaCost(next)+")"), true);
                    p.getServerWorld().playSound(null,p.getBlockPos(),SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,SoundCategory.PLAYERS,0.9f,1.0f+next*0.04f);
                    return TypedActionResult.success(stack,false);
                }
                int cost = manaCost(selected);
                if (!consumeMana(p,cost)) {
                    p.sendMessage(Text.literal("§c마력이 부족합니다. 필요 MP: "+cost), true);
                    return TypedActionResult.fail(stack);
                }
                castSpell(p,selected);
                return TypedActionResult.success(stack,false);
            } catch (Throwable t) {
                p.sendMessage(Text.literal("§c마법 오류를 안전하게 차단했습니다."), false);
                return TypedActionResult.fail(stack);
            }
        }
    }

    private static final class SpellBook extends Item {
        SpellBook(Settings s) { super(s); }
        @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack=user.getStackInHand(hand);
            if (!world.isClient && user instanceof ServerPlayerEntity p) {
                if (!canUseMagic(p)) { p.sendMessage(Text.literal("§c기사는 마법서를 이해할 수 없습니다. 신의 축복이 필요합니다."), false); return TypedActionResult.fail(stack); }
                p.addCommandTag("ccsafe_magic_book");
                p.sendMessage(Text.literal("§d[마법서] §f기본 마법 '마력탄'을 습득했습니다."), false);
                if (!p.getAbilities().creativeMode) stack.decrement(1);
            }
            return TypedActionResult.success(stack,world.isClient());
        }
    }

    private static final class DivineBlessing extends Item {
        DivineBlessing(Settings s) { super(s); }
        @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack=user.getStackInHand(hand);
            if (!world.isClient && user instanceof ServerPlayerEntity p) {
                PlayerData d=data(p); d.blessed=true; state(p).markDirty();
                p.sendMessage(Text.literal("§d✦ 신의 축복 ✦ §f검과 마법의 제한이 해제되었습니다."), false);
                p.getServerWorld().spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,p.getX(),p.getBodyY(0.6),p.getZ(),100,1.2,1.5,1.2,0.15);
                p.getServerWorld().playSound(null,p.getBlockPos(),SoundEvents.ITEM_TOTEM_USE,SoundCategory.PLAYERS,1.2f,1.0f);
                if (!p.getAbilities().creativeMode) stack.decrement(1);
            }
            return TypedActionResult.success(stack,world.isClient());
        }
    }

    private static final class ManaPotion extends Item {
        ManaPotion(Settings s) { super(s); }
        @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack=user.getStackInHand(hand);
            if (!world.isClient && user instanceof ServerPlayerEntity p) {
                PlayerData d=data(p); int before=(int)d.mana; d.mana=Math.min(maxMana(p),d.mana+120); state(p).markDirty();
                p.sendMessage(Text.literal("§b[마력 회복] §f"+before+" → "+(int)d.mana+" / "+maxMana(p)), true);
                p.getServerWorld().spawnParticles(ParticleTypes.WITCH,p.getX(),p.getBodyY(0.6),p.getZ(),28,0.5,0.7,0.5,0.06);
                p.getServerWorld().playSound(null,p.getBlockPos(),SoundEvents.ENTITY_WITCH_DRINK,SoundCategory.PLAYERS,0.9f,1.2f);
                if (!p.getAbilities().creativeMode) stack.decrement(1);
            }
            return TypedActionResult.success(stack,world.isClient());
        }
    }

    private void worldTick(ServerWorld world) {
        if (!world.getRegistryKey().equals(World.OVERWORLD)) return;
        if (++worldTicks % 240 != 0 || world.getDifficulty()==Difficulty.PEACEFUL) return;
        long t=world.getTimeOfDay()%24000L; boolean night=t>=13000L&&t<=23000L;
        for(ServerPlayerEntity p:world.getPlayers()) tryNaturalSpawn(world,p,night);
    }

    private static void tryNaturalSpawn(ServerWorld world, ServerPlayerEntity player, boolean night) {
        try {
            if (!world.getEntitiesByClass(VillagerEntity.class,player.getBoundingBox().expand(96),v->v.isAlive()).isEmpty()) return;
            if (world.getEntitiesByClass(HostileEntity.class,player.getBoundingBox().expand(48),e->e.isAlive()).size()>=18||world.random.nextInt(3)!=0) return;
            int dx=20+world.random.nextInt(21),dz=20+world.random.nextInt(21); if(world.random.nextBoolean())dx=-dx;if(world.random.nextBoolean())dz=-dz;
            int x=player.getBlockX()+dx,z=player.getBlockZ()+dz,y=world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x,z); BlockPos pos=new BlockPos(x,y,z);
            String mob=regionMonster(world,pos); boolean special=mob!=null; if(!night&&!special)return;
            if(mob==null){String[]pool={"goblin","orc","skeleton_knight","werewolf"};mob=pool[world.random.nextInt(pool.length)];}
            if(!special&&world.getLightLevel(pos)>7)return; if(!world.getBlockState(pos).isAir()||!world.getBlockState(pos.up()).isAir()||world.getBlockState(pos.down()).isAir())return;
            Identifier id=new Identifier("crowncinder",mob); if(!Registries.ENTITY_TYPE.containsId(id))return; EntityType<?> type=Registries.ENTITY_TYPE.get(id); Entity e=type.create(world); if(!(e instanceof MobEntity m))return;
            m.refreshPositionAndAngles(x+.5,y,z+.5,world.random.nextFloat()*360,0); if(world.isSpaceEmpty(m))world.spawnEntity(m);
        } catch(Throwable ignored){}
    }

    private static String regionMonster(ServerWorld w,BlockPos p){var b=w.getBiome(p);if(b.matchesKey(BiomeKeys.DESERT)||b.matchesKey(BiomeKeys.BADLANDS))return"fire_elemental";if(b.matchesKey(BiomeKeys.DARK_FOREST))return w.random.nextBoolean()?"dark_knight":"skeleton_knight";if(b.matchesKey(BiomeKeys.TAIGA)||b.matchesKey(BiomeKeys.OLD_GROWTH_PINE_TAIGA))return w.random.nextBoolean()?"troll":"werewolf";if(b.matchesKey(BiomeKeys.SAVANNA)||b.matchesKey(BiomeKeys.WINDSWEPT_HILLS))return w.random.nextBoolean()?"ogre":"minotaur";if(b.matchesKey(BiomeKeys.FOREST))return w.random.nextBoolean()?"goblin":"werewolf";if(b.matchesKey(BiomeKeys.PLAINS))return w.random.nextBoolean()?"goblin":"orc";return null;}

    private static int selectedSpell(ItemStack s,int unlocked){NbtCompound n=s.getNbt();int x=n!=null&&n.contains(SELECTED_SPELL_KEY)?n.getInt(SELECTED_SPELL_KEY):1;return x<1||x>Math.max(1,unlocked)?1:x;}
    private static void setSelectedSpell(ItemStack s,int spell){s.getOrCreateNbt().putInt(SELECTED_SPELL_KEY,Math.max(1,Math.min(10,spell)));}
    private static String spellName(int s){return switch(s){case 1->"마력탄";case 2->"화염창";case 3->"빙결창";case 4->"마력 폭발";case 5->"천둥의 창";case 6->"대화염 폭풍";case 7->"빙설 폭풍";case 8->"성광의 파동";case 9->"심연의 구체";case 10->"대마도사의 대재앙";default->"미습득";};}
    private static double spellBaseDamage(int s){return switch(s){case 1->8;case 2->13;case 3->18;case 4->24;case 5->31;case 6->39;case 7->48;case 8->58;case 9->70;case 10->88;default->5;};}
    private static double spellScale(int s){return .30+s*.07;} private static double spellRange(int s){return Math.min(44,20+s*2.4);} private static double spellRadius(int s){return switch(s){case 4->2.5;case 5->2.8;case 6->3.8;case 7->4.2;case 8->4.8;case 9->5.4;case 10->6.5;default->.8;};}
    private static ParticleEffect spellParticle(int s){return switch(s){case 2,6->ParticleTypes.SOUL_FIRE_FLAME;case 3,7->ParticleTypes.SNOWFLAKE;case 5->ParticleTypes.ELECTRIC_SPARK;case 8->ParticleTypes.END_ROD;case 9->ParticleTypes.DRAGON_BREATH;case 10->ParticleTypes.REVERSE_PORTAL;default->ParticleTypes.ENCHANT;};}

    private static void castSpell(ServerPlayerEntity p,int spell){
        ServerWorld w=p.getServerWorld(); double dmg=spellBaseDamage(spell)+Math.max(0,getStat(p,"mana"))*spellScale(spell); boolean crit=rollCrit(p);if(crit)dmg*=critMultiplier(p);
        double range=spellRange(spell),radius=spellRadius(spell);ParticleEffect fx=spellParticle(spell);Vec3d start=p.getEyePos(),dir=p.getRotationVec(1).normalize(),impact=start.add(dir.multiply(range));HostileEntity hit=null;
        for(double d=.8;d<=range&&hit==null;d+=.55){Vec3d at=start.add(dir.multiply(d));impact=at;w.spawnParticles(fx,at.x,at.y,at.z,Math.min(10,3+spell),.08,.08,.08,.02);List<HostileEntity> es=w.getEntitiesByClass(HostileEntity.class,new Box(at.x-.8,at.y-.8,at.z-.8,at.x+.8,at.y+.8,at.z+.8),e->e.isAlive()&&p.canSee(e));if(!es.isEmpty())hit=es.get(0);}
        if(hit!=null){impact=hit.getPos().add(0,hit.getHeight()*.5,0);if(spell<=3)damageSpell(p,hit,(float)dmg,spell);else for(HostileEntity e:w.getEntitiesByClass(HostileEntity.class,new Box(impact.x-radius,impact.y-radius,impact.z-radius,impact.x+radius,impact.y+radius,impact.z+radius),e->e.isAlive()))damageSpell(p,e,(float)Math.max(dmg*.55,dmg*(1-e.getPos().distanceTo(impact)/(radius*2))),spell);}
        double spread=.5+spell*.12;w.spawnParticles(fx,impact.x,impact.y,impact.z,35+spell*16,spread,spread,spread,.06);if(spell>=4)w.spawnParticles(ParticleTypes.EXPLOSION,impact.x,impact.y,impact.z,Math.min(12,spell),spread*.5,spread*.5,spread*.5,.02);if(spell>=7)w.spawnParticles(ParticleTypes.FLASH,impact.x,impact.y,impact.z,2,.2,.2,.2,0);if(spell==10)w.spawnParticles(ParticleTypes.DRAGON_BREATH,impact.x,impact.y,impact.z,180,3,3,3,.1);
        w.playSound(null,p.getBlockPos(),SoundEvents.ENTITY_EVOKER_CAST_SPELL,SoundCategory.PLAYERS,1,1);p.getItemCooldownManager().set(ARCANE_STAFF,18+spell*2);p.sendMessage(Text.literal((crit?"§6✦ CRIT §r":"")+"§b"+spellName(spell)+" §7MP -"+manaCost(spell)+" | 위력 "+String.format("%.1f",dmg)),true);
    }
    private static void damageSpell(ServerPlayerEntity p,HostileEntity t,float d,int s){t.damage(p.getDamageSources().playerAttack(p),d);if(s==2||s==6)t.setOnFireFor(s==6?8:4);if(s==3||s==7)t.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,s==7?140:80,s==7?2:1));if(s==5)t.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,100,1));if(s==9)t.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER,80,1));}

    private static int getLevel(ServerPlayerEntity p){try{Object pr=progress(p);return field(pr,"level").getInt(pr);}catch(Throwable e){return Math.max(1,p.experienceLevel);}}
    private static Object progress(ServerPlayerEntity p)throws Exception{Class<?>s=Class.forName("dev.crowncinder.progress.ProgressService");return s.getMethod("get",ServerPlayerEntity.class).invoke(null,p);}
    private static Field field(Object o,String n)throws Exception{Field f=o.getClass().getField(n);f.setAccessible(true);return f;}
    private static void dirtyAndApply(ServerPlayerEntity p)throws Exception{Class<?>s=Class.forName("dev.crowncinder.progress.ProgressService");Object st=s.getMethod("store",ServerPlayerEntity.class).invoke(null,p);st.getClass().getMethod("markDirty").invoke(st);try{s.getMethod("apply",ServerPlayerEntity.class).invoke(null,p);}catch(Throwable ignored){}}
    private static void refresh(ServerPlayerEntity p){try{Class<?>s=Class.forName("dev.crowncinder.progress.ProgressService");s.getMethod("apply",ServerPlayerEntity.class).invoke(null,p);}catch(Throwable ignored){}}

    private static PlayerState state(ServerPlayerEntity p){return state(p.getServerWorld());}
    private static PlayerState state(ServerWorld w){return w.getPersistentStateManager().getOrCreate(PlayerState::fromNbt,PlayerState::new,"crown_safe025_players");}
    private static PlayerData data(ServerPlayerEntity p){return state(p).players.computeIfAbsent(p.getUuid(),u->new PlayerData());}
    private static String jobName(String j){return j.equals("knight")?"기사":j.equals("mage")?"마법사":"미선택";}

    private static final class PlayerData { String job="none"; boolean blessed=false; double mana=0,stamina=0; }
    private static final class PlayerState extends PersistentState {
        final Map<UUID,PlayerData> players=new HashMap<>();
        static PlayerState fromNbt(NbtCompound n){PlayerState s=new PlayerState();NbtList list=n.getList("players",NbtElement.COMPOUND_TYPE);for(int i=0;i<list.size();i++){NbtCompound e=list.getCompound(i);try{UUID u=UUID.fromString(e.getString("uuid"));PlayerData d=new PlayerData();d.job=e.getString("job");if(d.job.isEmpty())d.job="none";d.blessed=e.getBoolean("blessed");d.mana=e.getDouble("mana");d.stamina=e.getDouble("stamina");s.players.put(u,d);}catch(Throwable ignored){}}return s;}
        @Override public NbtCompound writeNbt(NbtCompound n){NbtList list=new NbtList();for(var en:players.entrySet()){NbtCompound e=new NbtCompound();e.putString("uuid",en.getKey().toString());PlayerData d=en.getValue();e.putString("job",d.job);e.putBoolean("blessed",d.blessed);e.putDouble("mana",d.mana);e.putDouble("stamina",d.stamina);list.add(e);}n.put("players",list);return n;}
    }
}
