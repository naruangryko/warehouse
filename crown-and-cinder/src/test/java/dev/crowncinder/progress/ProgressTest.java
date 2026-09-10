package dev.crowncinder.progress;

public final class ProgressTest {
    private static int checks;
    private static void check(boolean value, String name) {
        checks++;
        if (!value) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        Progress p = new Progress();
        check(p.gainXp(39, 40) == 0 && p.xp == 39, "below threshold");
        check(p.gainXp(1, 40) == 1 && p.level == 2 && p.points == 2, "exact threshold");
        check(p.gainXp(200, 40) == 2 && p.level == 4 && p.points == 6, "multiple levels");
        check(!p.allocate("invalid") && p.points == 6, "invalid stat rejected");
        check(p.allocate("strength") && p.strength == 1 && p.points == 5, "spend point");
        Progress q = new Progress();
        check(!q.allocate("strength"), "no points rejected");
        q.goblinKilled(); check(q.questKills == 0, "no credit before accept");
        check(q.acceptQuest() && !q.acceptQuest(), "one active quest");
        check(!q.claimQuest(), "early claim rejected");
        for (int i = 0; i < 20; i++) q.goblinKilled();
        check(q.questKills == 5 && q.claimQuest(), "quest capped and claim succeeds");
        check(!q.claimQuest() && !q.acceptQuest(), "no duplicate reward");
        p.xp = 100; p.die(0.1); check(p.xp == 90 && p.level == 4, "death keeps level");
        p.gainXp(Integer.MAX_VALUE, 1);
        check(p.level == 50 && p.xp == 0, "level cap and overflow");
        check(p.gainXp(-100, 40) == 0 && p.xp == 0, "negative xp rejected");
        p.level = -999; p.strength = 999; p.vitality = 999; p.agility = -999; p.points = 999;
        p.xp = Integer.MAX_VALUE; p.sanitize(40);
        check(p.level == 1 && p.strength == 0 && p.vitality == 0 && p.agility == 0 && p.points == 0 && p.xp == 39,
            "corrupt state bounded");
        System.out.println("PASS: " + checks + " core progression checks");
    }
}
