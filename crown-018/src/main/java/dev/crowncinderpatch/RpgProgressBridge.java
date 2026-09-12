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
            if (progress == null) throw new IllegalStateException("No Crown progress object");
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
            if (progress == null) throw new IllegalStateException("No Crown progress object");
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
            Object progress = progress(player);
            return field(progress, "level").getInt(progress);
        } catch (Throwable ignored) {
            return player.experienceLevel;
        }
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
        player.sendMessage(Text.literal("§6[Crown & Cinder] §f" + what + (crownIntegrated ? " §7(RPG 레벨 반영)" : " §7(바닐라 경험치 대체)")), false);
    }
}
