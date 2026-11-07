package com.faust.core.progression;
/** Deterministic skill bonuses and visual tier selection. */
public final class SkillEffects {
    public static double health(Progress p,Balance b){return p.track(Skill.constitution).level*b.skills.get("healthPerLevel");}
    public static double luck(Progress p,Balance b){return Math.min(b.skills.get("luckMaximum"),p.track(Skill.intelligence).level*b.skills.get("luckPerLevel"));}
    public static double reach(Progress p,Balance b){return Math.floor(p.track(Skill.building).level/b.skills.get("reachEveryLevels"))*b.skills.get("reachPerStep");}
    public static double breaking(Progress p,Balance b){return Math.min(b.skills.get("breakingMaximum"),p.track(Skill.building).level*b.skills.get("breakingPerLevel"))+p.effect("breaking",b);}
    public static int icon(int level,Balance b){return level>=b.skills.get("iconTier3")?24:level>=b.skills.get("iconTier2")?16:level>=b.skills.get("iconTier1")?8:0;}
}
