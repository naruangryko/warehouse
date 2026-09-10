package dev.crowncinder.progress;

/** Persistent RPG rules. Kept Minecraft-free so rules remain testable. */
public final class Progress {
    public static final int MAX_LEVEL = 100;
    public static final int QUEST_TARGET = 5;
    public int level = 1, xp, points;
    public int strength, vitality, defense, agility, attackSpeed, moveSpeed;
    public int magicPower, magicDefense, critChance, critDamage, regeneration, stamina;
    public String job = "wanderer";
    public String adventurerRank = "NONE";
    public int adventurerPoints;
    public int repAurelia, repEldoria, repKharum, repSylvan, repVarkhan;
    public boolean questActive, questClaimed, hud = true;
    public int questKills;

    public int requiredXp(int base) {
        double curve = Math.pow(level, 1.35);
        return Math.max(1, (int)Math.min(1_000_000, Math.max(1, base) * curve));
    }
    public int gainXp(int amount, int base) {
        if (amount <= 0 || level >= MAX_LEVEL) return 0;
        long total = (long)xp + amount; int before = level;
        while (level < MAX_LEVEL && total >= requiredXp(base)) {
            total -= requiredXp(base); level++; points += 3;
        }
        xp = level == MAX_LEVEL ? 0 : (int)total;
        return level - before;
    }
    public boolean allocate(String stat) {
        if (points <= 0) return false;
        switch (stat) {
            case "strength" -> strength++;
            case "vitality" -> vitality++;
            case "defense" -> defense++;
            case "agility" -> agility++;
            case "attackspeed" -> attackSpeed++;
            case "movespeed" -> moveSpeed++;
            case "magic" -> magicPower++;
            case "magicdefense" -> magicDefense++;
            case "critchance" -> critChance++;
            case "critdamage" -> critDamage++;
            case "regen" -> regeneration++;
            case "stamina" -> stamina++;
            default -> { return false; }
        }
        points--; return true;
    }
    public boolean chooseJob(String value) {
        if (!"wanderer".equals(job)) return false;
        switch (value) {
            case "warrior", "knight", "archer", "mage", "paladin", "assassin", "lancer", "spellblade" -> { job=value; return true; }
            default -> { return false; }
        }
    }
    public boolean joinAdventurers() {
        if (!"NONE".equals(adventurerRank)) return false;
        adventurerRank = "4"; return true;
    }
    public boolean promoteAdventurer() {
        int need;
        switch (adventurerRank) {
            case "4" -> need=100;
            case "3" -> need=250;
            case "2" -> need=500;
            case "1" -> need=1000;
            default -> { return false; }
        }
        if (adventurerPoints < need) return false;
        adventurerPoints -= need;
        adventurerRank = switch (adventurerRank) { case "4"->"3"; case "3"->"2"; case "2"->"1"; default->"SPECIAL"; };
        return true;
    }
    public int reputation(String nation) {
        return switch(nation) { case "aurelia"->repAurelia; case "eldoria"->repEldoria; case "kharum"->repKharum; case "sylvan"->repSylvan; case "varkhan"->repVarkhan; default->0; };
    }
    public void addReputation(String nation, int amount) {
        switch(nation) {
            case "aurelia" -> repAurelia=clampRep(repAurelia+amount);
            case "eldoria" -> repEldoria=clampRep(repEldoria+amount);
            case "kharum" -> repKharum=clampRep(repKharum+amount);
            case "sylvan" -> repSylvan=clampRep(repSylvan+amount);
            case "varkhan" -> repVarkhan=clampRep(repVarkhan+amount);
        }
    }
    private static int clampRep(int v){ return Math.max(-100, Math.min(100, v)); }
    public boolean acceptQuest(){ if(questActive||questClaimed)return false; questActive=true; questKills=0; return true; }
    public void goblinKilled(){ if(questActive)questKills=Math.min(QUEST_TARGET,questKills+1); }
    public boolean claimQuest(){ if(!questActive||questClaimed||questKills<QUEST_TARGET)return false; questActive=false; questClaimed=true; adventurerPoints+=60; addReputation("aurelia",5); return true; }
    public void die(double loss){ xp-=(int)Math.floor(xp*Math.max(0,Math.min(1,loss))); }
    public void sanitize(int base) {
        level=Math.max(1,Math.min(MAX_LEVEL,level)); xp=level==MAX_LEVEL?0:Math.max(0,Math.min(requiredXp(base)-1,xp));
        strength=Math.max(0,strength); vitality=Math.max(0,vitality); defense=Math.max(0,defense); agility=Math.max(0,agility);
        attackSpeed=Math.max(0,attackSpeed); moveSpeed=Math.max(0,moveSpeed); magicPower=Math.max(0,magicPower); magicDefense=Math.max(0,magicDefense);
        critChance=Math.max(0,critChance); critDamage=Math.max(0,critDamage); regeneration=Math.max(0,regeneration); stamina=Math.max(0,stamina);
        int spent=strength+vitality+defense+agility+attackSpeed+moveSpeed+magicPower+magicDefense+critChance+critDamage+regeneration+stamina;
        int budget=(level-1)*3; points=Math.max(0,Math.min(Math.max(0,budget-spent),points));
        questKills=Math.max(0,Math.min(QUEST_TARGET,questKills)); if(questClaimed)questActive=false;
        adventurerPoints=Math.max(0,adventurerPoints);
    }
}
