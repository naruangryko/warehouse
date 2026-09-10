package dev.crowncinder.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public final class RpgConfig {
    public int xpBase = 40;
    public double xpMultiplier = 1.0;
    public int goblinXp = 18;
    public int otherMonsterXp = 5;
    public int questXp = 100;
    public int questEmeralds = 3;
    public double deathXpLoss = 0.1;
    public double damagePerStrength = 0.25;
    public double healthPerVitality = 1.0;
    public double speedPerAgility = 0.003;
    public int goblinSpawnWeight = 12;
    public double goblinHealth = 16;
    public double goblinDamage = 3;
    public double goblinSpeed = 0.27;
    public int greatswordLevel = 3;
    public int twinbladeLevel = 2;
    public double cleaveDamage = 4;
    public int cleaveCooldownTicks = 100;
    public double cleaveRadius = 3;
    public int hudIntervalTicks = 40;

    public static RpgConfig load(Logger logger) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("crowncinder.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        RpgConfig config = new RpgConfig();
        try {
            if (Files.exists(path)) {
                RpgConfig parsed = gson.fromJson(Files.readString(path), RpgConfig.class);
                if (parsed != null) config = parsed;
            } else {
                Files.createDirectories(path.getParent());
                Files.writeString(path, gson.toJson(config), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            logger.warn("Cannot read configuration; using defaults and preserving original file", e);
        }
        config.validate();
        return config;
    }

    private static double clamp(double n, double min, double max, double fallback) {
        return Double.isFinite(n) ? Math.max(min, Math.min(max, n)) : fallback;
    }
    private void validate() {
        xpBase = (int) clamp(xpBase, 1, 100000, 40);
        xpMultiplier = clamp(xpMultiplier, 0, 100, 1);
        goblinXp = (int) clamp(goblinXp, 0, 100000, 18);
        otherMonsterXp = (int) clamp(otherMonsterXp, 0, 100000, 5);
        questXp = (int) clamp(questXp, 0, 100000, 100);
        questEmeralds = (int) clamp(questEmeralds, 0, 64, 3);
        deathXpLoss = clamp(deathXpLoss, 0, 1, 0.1);
        damagePerStrength = clamp(damagePerStrength, 0, 5, 0.25);
        healthPerVitality = clamp(healthPerVitality, 0, 10, 1);
        speedPerAgility = clamp(speedPerAgility, 0, 0.01, 0.003);
        goblinSpawnWeight = (int) clamp(goblinSpawnWeight, 0, 100, 12);
        goblinHealth = clamp(goblinHealth, 1, 1000, 16);
        goblinDamage = clamp(goblinDamage, 0, 100, 3);
        goblinSpeed = clamp(goblinSpeed, 0.05, 0.6, 0.27);
        greatswordLevel = (int) clamp(greatswordLevel, 1, 50, 3);
        twinbladeLevel = (int) clamp(twinbladeLevel, 1, 50, 2);
        cleaveDamage = clamp(cleaveDamage, 0, 100, 4);
        cleaveRadius = clamp(cleaveRadius, 1, 5, 3);
        cleaveCooldownTicks = (int) clamp(cleaveCooldownTicks, 20, 1200, 100);
        hudIntervalTicks = ((int) clamp(hudIntervalTicks, 20, 200, 40) / 20) * 20;
    }
}
