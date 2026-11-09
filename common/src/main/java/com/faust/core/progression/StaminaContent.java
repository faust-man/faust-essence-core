package com.faust.core.progression;

import java.util.*;

/** Applies the optional module boundary to an in-memory configuration only. */
public final class StaminaContent {
    private StaminaContent(){}
    private static final Set<String> EFFECTS=Set.of("stamina","capacity","second_wind","regeneration","exhaustion_threshold_reduction","bow_cost_reduction","crossbow_cost_reduction","mace_cost_reduction","shield_cost_reduction");
    private static final Set<String> IDS=Set.of("faustcore:training_constitution_stamina","faustcore:armor_familiarity","faustcore:armor_mastery","faustcore:second_wind","faustcore:resilient_recovery","faustcore:bow_efficiency","faustcore:bow_mastery","faustcore:crossbow_efficiency","faustcore:mace_efficiency","faustcore:shield_efficiency");
    public static void withoutProvider(Balance b){
        if(StaminaServices.present())return;
        b.stamina.put("enabled",0.0);b.fatigue.put("enabled",0.0);
        Set<String> removed=new HashSet<>(IDS);
        for(var a:b.abilities)if(EFFECTS.contains(a.effect()))removed.add(a.id());
        b.abilities.removeIf(a->removed.contains(a.id()));
        b.statBonuses.removeIf(s->s.attribute.equals("faustcore:stamina")||removed.contains(s.requiresAbility));
        for(int i=0;i<b.abilities.size();i++)b.abilities.set(i,detach(b.abilities.get(i),removed));
        b.trees=b.trees.stream().map(t->new SkillTrees.Tree(t.skill(),t.nodes().stream().filter(n->!removed.contains(n.ability())).toList())).toList();
    }
    public static Balance.Ability detach(Balance.Ability a,Set<String> removed){
        var all=a.requiresAll().stream().filter(id->!removed.contains(id)).toList();
        var any=a.requiresAny().stream().filter(id->!removed.contains(id)).toList();
        var excludes=a.excludes().stream().filter(id->!removed.contains(id)).toList();
        return new Balance.Ability(a.id(),a.skill(),a.level(),a.counter(),a.amount(),removed.contains(a.parent())?"":a.parent(),a.effect(),a.value(),all,any,excludes,a.characteristics(),a.conditions(),a.conditionMode(),a.conditionsRequired(),Math.min(a.requiresCount(),any.size()),a.toggleable());
    }
}
