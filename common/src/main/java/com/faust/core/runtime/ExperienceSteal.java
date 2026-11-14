package com.faust.core.runtime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import com.faust.core.progression.*;
public final class ExperienceSteal {
 private ExperienceSteal(){}
 public static void hit(ServerPlayer victim,DamageSource source){
  if(!(source.getEntity() instanceof ServerPlayer attacker)||attacker==victim||source.getDirectEntity()!=attacker||source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)||!CoreRuntime.active(attacker)||!CoreRuntime.data(attacker).enabled("faustcore:experience_steal",CoreRuntime.balance))return;
  int from=ExperienceMath.points(victim.experienceLevel,victim.experienceProgress),to=ExperienceMath.points(attacker.experienceLevel,attacker.experienceProgress);
  int amount=ExperienceMath.stolen(from,CoreRuntime.data(attacker).track(CoreSkills.intelligence).level,to);if(amount<=0)return;
  victim.totalExperience=from;attacker.totalExperience=to;victim.giveExperiencePoints(-amount);attacker.giveExperiencePoints(amount);
 }
}
