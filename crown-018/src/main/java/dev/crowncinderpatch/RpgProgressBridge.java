package dev.crowncinderpatch;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Best-effort bridge into Crown & Cinder's existing RPG progress without compiling against its internals. */
public final class RpgProgressBridge {
    private RpgProgressBridge() {}

    public static boolean addXp(ServerPlayerEntity player, int amount) {
        if (amount <= 0) return false;
        try {
            Class<?> service = Class.forName("dev.crowncinder.progress.ProgressService");
            Method award = service.getMethod("award", ServerPlayerEntity.class, int.class);
            award.invoke(null, player, amount);
            apply(service, player);
            return true;
        } catch (Throwable ignored) {
            player.addExperience(amount);
            return false;
        }
    }

    public static boolean addLevels(ServerPlayerEntity player, int amount) {
        try {
            Object progress = progress(player);
            Field level = field(progress, "level");
            Field xp = field(progress, "xp");
            Field points = field(progress, "points");
            int before = level.getInt(progress);
            int after = Math.max(1, Math.min(100, before + amount));
            int gained = Math.max(0, after - before);
            level.setInt(progress, after);
            xp.setInt(progress, 0);
            if (gained > 0) points.setInt(progress, points.getInt(progress) + gained * 3);
            dirtyAndApply(player);
            return true;
        } catch (Throwable ignored) {
            player.addExperienceLevels(amount);
            return false;
        }
    }

    public static boolean setLevel(ServerPlayerEntity player, int target) {
        target = Math.max(1, Math.min(100, target));
        try {
            Object progress = progress(player);
            Field level = field(progress, "level");
            Field xp = field(progress, "xp");
            Field points = field(progress, "points");
            int before = level.getInt(progress);
            level.setInt(progress, target);
            xp.setInt(progress, 0);
            if (target > before) points.setInt(progress, points.getInt(progress) + (target - before) * 3);
            dirtyAndApply(player);
            return true;
        } catch (Throwable ignored) {
            int diff = target - player.experienceLevel;
            player.addExperienceLevels(diff);
            return false;
        }
    }

    public static int getLevel(ServerPlayerEntity player) {
        try {
            Object p = progress(player);
            return field(p, "level").getInt(p);
        } catch (Throwable ignored) {
            return player.experienceLevel;
        }
    }

    public static int addPoints(ServerPlayerEntity player, int amount) {
        try {
            Object p = progress(player);
            Field points = field(p, "points");
            int max = maxUnspentPoints(p);
            int value = Math.max(0, Math.min(max, points.getInt(p) + amount));
            points.setInt(p, value);
            dirtyAndApply(player);
            return value;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    public static int setPoints(ServerPlayerEntity player, int amount) {
        try {
            Object p = progress(player);
            int value = Math.max(0, Math.min(maxUnspentPoints(p), amount));
            field(p, "points").setInt(p, value);
            dirtyAndApply(player);
            return value;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    public static int maxPoints(ServerPlayerEntity player) {
        try {
            Object p = progress(player);
            int value = maxUnspentPoints(p);
            field(p, "points").setInt(p, value);
            dirtyAndApply(player);
            return value;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    public static int getPoints(ServerPlayerEntity player) {
        try {
            Object p = progress(player);
            return field(p, "points").getInt(p);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    public static int addStat(ServerPlayerEntity player, String stat, int amount) {
        try {
            Object p = progress(player);
            String fieldName = statField(stat);
            if (fieldName == null) return Integer.MIN_VALUE;
            Field f = field(p, fieldName);
            int value = Math.max(0, Math.min(10000, f.getInt(p) + amount));
            f.setInt(p, value);
            dirtyAndApply(player);
            return value;
        } catch (Throwable ignored) {
            return Integer.MIN_VALUE;
        }
    }

    public static int setStat(ServerPlayerEntity player, String stat, int amount) {
        try {
            Object p = progress(player);
            String fieldName = statField(stat);
            if (fieldName == null) return Integer.MIN_VALUE;
            int value = Math.max(0, Math.min(10000, amount));
            field(p, fieldName).setInt(p, value);
            dirtyAndApply(player);
            return value;
        } catch (Throwable ignored) {
            return Integer.MIN_VALUE;
        }
    }

    public static int getStrength(ServerPlayerEntity player) {
        try {
            Object p = progress(player);
            return field(p, "strength").getInt(p);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /** Re-applies Crown & Cinder's own configured modifiers after equipment swaps. */
    public static void refreshCombatStats(ServerPlayerEntity player) {
        try {
            Class<?> service = Class.forName("dev.crowncinder.progress.ProgressService");
            apply(service, player);
        } catch (Throwable ignored) {}
    }

    private static int maxUnspentPoints(Object p) throws Exception {
        int level = field(p, "level").getInt(p);
        int spent = 0;
        String[] stats = {"strength","vitality","defense","agility","attackSpeed","moveSpeed","magicPower","magicDefense","critChance","critDamage","regeneration","stamina"};
        for (String name : stats) spent += Math.max(0, field(p, name).getInt(p));
        return Math.max(0, Math.max(0, (level - 1) * 3) - spent);
    }

    private static String statField(String stat) {
        return switch (stat.toLowerCase()) {
            case "strength", "str", "힘" -> "strength";
            case "vitality", "vit", "체력" -> "vitality";
            case "defense", "def", "방어" -> "defense";
            case "agility", "agi", "민첩" -> "agility";
            case "attackspeed", "attack_speed" -> "attackSpeed";
            case "movespeed", "move_speed" -> "moveSpeed";
            case "magic", "magicpower" -> "magicPower";
            case "magicdefense" -> "magicDefense";
            case "critchance" -> "critChance";
            case "critdamage" -> "critDamage";
            case "regen", "regeneration" -> "regeneration";
            case "stamina" -> "stamina";
            default -> null;
        };
    }

    private static Object progress(ServerPlayerEntity player) throws Exception {
        Class<?> service = Class.forName("dev.crowncinder.progress.ProgressService");
        Method get = service.getMethod("get", ServerPlayerEntity.class);
        return get.invoke(null, player);
    }

    private static Field field(Object target, String name) throws Exception {
        Field f = target.getClass().getField(name);
        f.setAccessible(true);
        return f;
    }

    private static void dirtyAndApply(ServerPlayerEntity player) throws Exception {
        Class<?> service = Class.forName("dev.crowncinder.progress.ProgressService");
        Method store = service.getMethod("store", ServerPlayerEntity.class);
        Object storeObj = store.invoke(null, player);
        Method dirty = storeObj.getClass().getMethod("markDirty");
        dirty.invoke(storeObj);
        apply(service, player);
    }

    private static void apply(Class<?> service, ServerPlayerEntity player) {
        try {
            service.getMethod("apply", ServerPlayerEntity.class).invoke(null, player);
        } catch (Throwable ignored) {}
    }

    public static void feedback(ServerPlayerEntity player, String what, boolean crownIntegrated) {
        player.sendMessage(Text.literal("§6[Crown & Cinder] §f" + what + (crownIntegrated ? " §7(RPG 데이터 반영)" : " §c(RPG 데이터 연결 실패)")), false);
    }
}
