package com.faust.core.progression;
/** Deterministic skill bonuses and visual tier selection. */
public final class SkillEffects {
    public static double attribute(Progress p,Balance b,String attribute,boolean sprint){
        double total=0,buildingReach=0;for(var r:b.statBonuses){if(!r.attribute.equals(attribute)||(r.sprintOnly&&!sprint))continue;double value=r.amount(p.track(r.skill).level);if(r.skill==Skill.building&&(attribute.equals("minecraft:player.block_interaction_range")||attribute.equals("minecraft:player.entity_interaction_range")))buildingReach+=value;else total+=value;}return total+Math.min(buildingReach,b.skills.get("buildingReachMaximum"));
    }
    public static double health(Progress p,Balance b){return attribute(p,b,"minecraft:generic.max_health",false);}
    public static double luck(Progress p,Balance b){return attribute(p,b,"minecraft:generic.luck",false);}
    public static double reach(Progress p,Balance b){return attribute(p,b,"minecraft:player.block_interaction_range",false);}
    public static double breaking(Progress p,Balance b){return Math.min(b.skills.get("breakingMaximum"),p.track(Skill.building).level*b.skills.get("breakingPerLevel"))+p.effect("breaking",b);}
    public static int icon(int level,Balance b){return level>=b.skills.get("iconTier3")?24:level>=b.skills.get("iconTier2")?16:level>=b.skills.get("iconTier1")?8:0;}
}
