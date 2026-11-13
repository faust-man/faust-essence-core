package com.faust.core.progression;
import com.faust.essence.api.Skill;
/** Core owns the six base characteristics; the API owns only their registration mechanism. */
public final class CoreSkills {
 private CoreSkills(){}
 public static final Skill strength=Skill.register("strength"),constitution=Skill.register("constitution"),dexterity=Skill.register("dexterity"),defense=Skill.register("defense"),intelligence=Skill.register("intelligence"),building=Skill.register("building");
 static {for(String id:java.util.List.of("fall_damage_reduction","enchanting_power","potion_duration","exhaustion_threshold_reduction","bow_cost_reduction","crossbow_cost_reduction","mace_cost_reduction","stat_unlock","shield_cost_reduction","damage","second_wind","regeneration","stamina","speed","capacity","shield_durability","shield_recovery","breaking"))com.faust.essence.api.BonusEffects.register(id);}
 public static void initialize(){}
}
