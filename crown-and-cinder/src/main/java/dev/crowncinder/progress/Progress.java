package dev.crowncinder.progress;

/** Pure game rules: no Minecraft dependency, so these can be tested offline. */
public final class Progress {
    public static final int MAX_LEVEL = 50;
    public static final int QUEST_TARGET = 5;
    public int level = 1;
    public int xp;
    public int points;
    public int strength;
    public int vitality;
    public int agility;
    public boolean questActive;
    public int questKills;
    public boolean questClaimed;
    public boolean hud = true;

    public int requiredXp(int base) {
        return Math.max(1, Math.min(100000, base)) * level;
    }

    public int gainXp(int amount, int base) {
        if (amount <= 0 || level >= MAX_LEVEL) return 0;
        long total = (long) xp + amount;
        int previous = level;
        while (level < MAX_LEVEL && total >= requiredXp(base)) {
            total -= requiredXp(base);
            level++;
            points += 2;
        }
        xp = level == MAX_LEVEL ? 0 : (int) total;
        return level - previous;
    }

    public boolean allocate(String stat) {
        if (points <= 0) return false;
        switch (stat) {
            case "strength" -> strength++;
            case "vitality" -> vitality++;
            case "agility" -> agility++;
            default -> { return false; }
        }
        points--;
        return true;
    }

    public boolean acceptQuest() {
        if (questActive || questClaimed) return false;
        questActive = true;
        questKills = 0;
        return true;
    }

    public void goblinKilled() {
        if (questActive) questKills = Math.min(QUEST_TARGET, questKills + 1);
    }

    public boolean claimQuest() {
        if (!questActive || questClaimed || questKills < QUEST_TARGET) return false;
        questActive = false;
        questClaimed = true;
        return true;
    }

    public void die(double loss) {
        xp -= (int) Math.floor(xp * Math.max(0, Math.min(1, loss)));
    }

    public void sanitize(int base) {
        level = Math.max(1, Math.min(MAX_LEVEL, level));
        xp = level == MAX_LEVEL ? 0 : Math.max(0, Math.min(requiredXp(base) - 1, xp));
        int budget = (level - 1) * 2;
        strength = Math.max(0, Math.min(budget, strength));
        vitality = Math.max(0, Math.min(budget - strength, vitality));
        agility = Math.max(0, Math.min(budget - strength - vitality, agility));
        points = Math.max(0, Math.min(budget - strength - vitality - agility, points));
        questKills = Math.max(0, Math.min(QUEST_TARGET, questKills));
        if (questClaimed) questActive = false;
    }
}
