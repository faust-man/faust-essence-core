package com.faust.stamina;
import com.faust.core.progression.*;

/** Resource rules extracted from the original core model; saved IDs and units are unchanged. */
public final class StaminaModel implements StaminaMechanics {
    @Override public void prepare(Balance b) {
        Balance defaults=new Balance();
        java.util.Set<String> resource=new java.util.HashSet<>();
        for(var a:defaults.abilities)if(java.util.Set.of("stamina","capacity","second_wind","regeneration","exhaustion_threshold_reduction","bow_cost_reduction","crossbow_cost_reduction","mace_cost_reduction","shield_cost_reduction").contains(a.effect())||a.id().equals("faustcore:training_constitution_stamina")){
            resource.add(a.id());if(b.abilities.stream().noneMatch(old->old.id().equals(a.id())))b.abilities.add(a);
        }
        for(int i=0;i<b.abilities.size();i++){
            var a=b.abilities.get(i);
            if(a.id().equals("faustcore:training_constitution_health")&&a.parent().isEmpty()){
                var json=Balance.JSON.toJsonTree(a).getAsJsonObject();json.addProperty("parent","faustcore:training_constitution_stamina");b.abilities.set(i,Balance.JSON.fromJson(json,Balance.Ability.class));
            }
        }
        for(var stat:defaults.statBonuses)if(stat.attribute.equals("faustcore:stamina")&&b.statBonuses.stream().noneMatch(old->old.id.equals(stat.id)))b.statBonuses.add(stat);
        var layouts=new java.util.ArrayList<SkillTrees.Tree>();
        for(var tree:b.trees){
            var nodes=new java.util.ArrayList<>(tree.nodes());
            defaults.trees.stream().filter(t->t.skill()==tree.skill()).flatMap(t->t.nodes().stream()).filter(n->resource.contains(n.ability())&&nodes.stream().noneMatch(old->old.ability().equals(n.ability()))).forEach(nodes::add);
            layouts.add(new SkillTrees.Tree(tree.skill(),nodes));
        }
        b.trees=layouts;
    }

    @Override public void tick(net.minecraft.server.level.ServerPlayer player,Progress p,com.faust.core.runtime.CoreRuntime.Session s,Balance b) {StaminaRuntime.tick(player,p,s,b);}

    @Override public void defaults(Balance b,int stage) {
        switch(stage) {
            case 1 -> {b.ability("second_wind",Skill.constitution,5,"damage_taken",50,"","second_wind",1);}
            case 2 -> {b.ability("armor_familiarity",Skill.defense,5,"armor_metres",500,"","capacity",2);}
            case 3 -> {b.ability("armor_mastery",Skill.defense,15,"armor_metres",2000,"armor_familiarity","capacity",4);}
            case 4 -> {b.stat("constitution_stamina",Skill.constitution,"max_health","add",1,1,1e9,false);b.statBonuses.getLast().attribute="faustcore:stamina";}
            case 5 -> {b.ability("shield_efficiency",Skill.defense,8,"blocks",60,"shield_care","shield_cost_reduction",0.5);}
            case 6 -> {b.ability("resilient_recovery",Skill.constitution,10,"constitution_run_metres",5000,"second_wind","exhaustion_threshold_reduction",0.5);}
            case 7 -> {b.ability("bow_efficiency",Skill.dexterity,5,"bow_shots",30,"","bow_cost_reduction",1);}
            case 8 -> {b.ability("bow_mastery",Skill.dexterity,10,"bow_shots",150,"bow_efficiency","bow_cost_reduction",1);}
            case 9 -> {b.ability("crossbow_efficiency",Skill.strength,5,"crossbow_shots",30,"","crossbow_cost_reduction",1);}
            case 10 -> {b.ability("mace_efficiency",Skill.strength,10,"mace_hits",50,"training_strength_damage","mace_cost_reduction",1);}
            default -> {}
        }
    }

    @Override public double baseStamina(Progress p, Balance b) {return b.stamina.get("base")+SkillEffects.attribute(p,b,"faustcore:stamina",false)+p.effect("stamina",b);}
    @Override public double maxFatigue(Progress p, Balance b) {double total=p.baseStamina(b),bonus=Math.max(0,total-b.stamina.get("base"));return Math.max(0,Math.min(total-b.fatigue.get("minimum"),b.fatigue.get("maximum")+bonus));}
    @Override public double maxStamina(Progress p, Balance b) {return Math.max(b.fatigue.get("minimum"),p.baseStamina(b)-(b.fatigue.get("enabled")>0?Math.max(0,Math.min(p.fatigue,p.maxFatigue(b))):0));}
    @Override public double capacity(Progress p, Balance b) {return b.armor.get("baseCapacity")+p.track(Skill.defense).level*b.armor.get("capacityPerLevel")+p.effect("capacity",b);}
    @Override public double spend(Progress p, double requested,Balance b) {return p.spend(requested,b,Long.MAX_VALUE);}
    @Override public double spend(Progress p, double requested,Balance b,long worldAgeTicks) {
        if(b.stamina.get("enabled")==0||!Double.isFinite(requested)||requested<=0) return 0;
        double paid=Math.min(p.stamina,requested);p.stamina-=paid;
        if(b.fatigue.get("enabled")>0&&FatigueRules.accumulates(worldAgeTicks,b)) p.fatigue=Math.min(p.maxFatigue(b),p.fatigue+b.fatigue.get("perSpentFraction")*paid*b.fatigue.get("accumulationMultiplier"));
        p.normalizeFatigue();p.stamina=Math.min(p.stamina,p.maxStamina(b));p.updateExhaustion(b); return paid;
    }
    @Override public void rest(Progress p, double dt,boolean sleeping,boolean campfire,boolean regenerate,Balance b) {p.rest(dt,sleeping,campfire,regenerate,1,b);}
    @Override public void rest(Progress p, double dt,boolean sleeping,boolean campfire,boolean regenerate,double recoveryScale,Balance b) {
        if(b.stamina.get("enabled")==0) return;
        if(b.fatigue.get("enabled")>0){
            if(sleeping&&p.fatigue>0)p.fatigue=Math.max(0,p.fatigue-dt*b.fatigue.get("sleepPerSecond"));
            else if(campfire){p.campfireRest+=dt*b.fatigue.get("campfirePerSecond");double whole=Math.floor(p.campfireRest+1e-9);p.campfireRest-=whole;if(p.fatigue>0)p.fatigue=Math.max(0,p.fatigue-whole);}
        }
        if(regenerate) p.stamina=Math.min(p.maxStamina(b),p.stamina+dt*recoveryScale*(b.stamina.get("regeneration")+p.effect("regeneration",b)));
        else p.stamina=Math.min(p.stamina,p.maxStamina(b));
        p.updateExhaustion(b);
    }
    @Override public void environmentalFatigue(Progress p, double dt,boolean soulCampfire,boolean soulFire,Balance b) {p.environmentalFatigue(dt,soulCampfire,soulFire,b,Long.MAX_VALUE);}
    @Override public void environmentalFatigue(Progress p, double dt,boolean soulCampfire,boolean soulFire,Balance b,long worldAgeTicks) {
        if(b.stamina.get("enabled")==0||b.fatigue.get("enabled")==0||!FatigueRules.accumulates(worldAgeTicks,b))return;
        p.fatigue=Math.min(p.maxFatigue(b),p.fatigue+dt*b.fatigue.get("accumulationMultiplier")*((soulCampfire?b.fatigue.get("soulCampfirePerSecond"):0)+(soulFire?b.fatigue.get("soulFirePerSecond"):0)));p.normalizeFatigue();p.stamina=Math.min(p.stamina,p.maxStamina(b));p.updateExhaustion(b);
    }
    @Override public void recover(Progress p, double dt,boolean sleep,boolean campfire,boolean delayReady,boolean shielding,Balance b) {p.recover(dt,sleep,campfire,delayReady,shielding,false,false,b);}
    @Override public void recover(Progress p, double dt,boolean sleep,boolean campfire,boolean delayReady,boolean shielding,boolean flying,boolean airVisible,Balance b) {
        p.recover(dt,sleep,campfire,delayReady,shielding,flying,airVisible,false,b);
    }
    @Override public void recover(Progress p, double dt,boolean sleep,boolean campfire,boolean delayReady,boolean shielding,boolean flying,boolean airVisible,boolean fastSwimming,Balance b) {
        double scale=airVisible?b.stamina.get("airRecoveryMultiplier"):1;
        if(!shielding&&!flying&&!fastSwimming)p.secondWind(dt*scale,b);
        // Fatigue relief still uses real elapsed time, only p.stamina regeneration is slowed.
        p.rest(dt,sleep,campfire,delayReady&&!shielding&&!flying&&!fastSwimming,scale,b);
    }
    @Override public double exhaustionThreshold(Progress p, Balance b) {return Math.min(p.maxStamina(b),b.stamina.get("exhaustionRecovery")*Math.max(0,1-p.effect("exhaustion_threshold_reduction",b)));}
    @Override public boolean empty(Progress p, Balance b) {return p.stamina<=1e-9;}
    @Override public boolean actionsBlocked(Progress p, Balance b) {return b.stamina.get("enabled")>0&&(p.exhausted||p.empty(b));}
    @Override public double actionCost(Progress p, String action,Balance b) {
        double cost=b.stamina.get(action+"Cost");
        if(action.equals("bow")||action.equals("crossbow")||action.equals("mace"))cost-=p.effect(action+"_cost_reduction",b);
        if(action.equals("mace"))cost=Math.max(b.stamina.get("attackCost"),cost);
        return Math.max(0,cost);
    }
    @Override public void sleptNight(Progress p, Balance b) {if(b.fatigue.get("enabled")>0&&b.fatigue.get("resetAfterNight")>0)p.fatigue=-b.fatigue.get("sleepReserve");}
    @Override public void secondWind(Progress p, double dt,Balance b) {if(b.stamina.get("enabled")>0&&p.effect("second_wind",b)>0){p.stamina=Math.min(p.maxStamina(b),p.stamina+dt*b.stamina.get("runCost")*b.stamina.get("secondWindMultiplier"));p.updateExhaustion(b);}}
    @Override public void sweetFood(Progress p, String item,Balance b) {p.sweetFood(item,1,b);}
    @Override public void sweetFood(Progress p, String item,double count,Balance b) {if(Double.isFinite(count)&&count>0&&b.stamina.get("enabled")>0&&b.fatigue.get("enabled")>0&&p.fatigue>0)p.fatigue=item.equals("minecraft:enchanted_golden_apple")?0:Math.max(0,p.fatigue-b.sweetFoods.getOrDefault(item,0.0)*count);}
    @Override public void updateExhaustion(Progress p, Balance b) {
        if(b.stamina.get("exhaustionLock")==0){p.exhausted=false;return;}
        if(p.empty(b))p.exhausted=true;
        else if(p.stamina>=p.exhaustionThreshold(b)-1e-9)p.exhausted=false;
    }
}
