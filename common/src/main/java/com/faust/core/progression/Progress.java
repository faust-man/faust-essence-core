package com.faust.core.progression;

import java.util.*;

/** Persistent player data and deterministic balance calculations, independent of Minecraft. */
public final class Progress {
    public int schema=2;
    public Map<Skill,Track> tracks=new LinkedHashMap<>();
    public Map<String,Double> counters=new HashMap<>();
    public Set<String> unlocked=new HashSet<>(), discoveries=new HashSet<>();
    public long purchases, revision;
    public double stamina=20, fatigue;
    public boolean exhausted,soulFireBurn;
    public double campfireRest;
    public String path="";
    public static final class Track { public int level; public double xp, threshold; public Integer limitOverride; public long purchases; }
    public Progress() {for(Skill s:Skill.values()) tracks.put(s,new Track());}
    public Track track(Skill skill) {return tracks.computeIfAbsent(skill,k->new Track());}
    public double needed(Skill skill,Balance b) {return b.progression.get("levelBase")+b.progression.get("levelSlope")*track(skill).level;}
    public double cap(Skill skill,Balance b) {return Math.min(b.progression.get("maximumCap"),track(skill).limitOverride!=null?track(skill).limitOverride:b.progression.get("initialCap")+b.progression.get("capStep")*track(skill).purchases);}
    public double multiplier(Skill skill,Balance b) {
        double level=track(skill).level,cap=cap(skill,b);
        return level<cap?Math.min(b.progression.get("maximumGrowth"),cap/Math.max(1,level)):b.progression.get("overCapGrowth");
    }
    public long price(Skill skill,Balance b) {
        double sum=Arrays.stream(Skill.values()).mapToDouble(s->cap(s,b)).sum();
        double value=Math.ceil(cap(skill,b)+sum/b.progression.get("costDivisor"));
        return value>=Long.MAX_VALUE?Long.MAX_VALUE:(long)value;
    }
    public boolean buy(Skill skill,long funds,Balance b) {
        if(cap(skill,b)>=b.progression.get("maximumCap")||price(skill,b)>funds||purchases==Long.MAX_VALUE||track(skill).purchases==Long.MAX_VALUE)return false;
        if(track(skill).limitOverride!=null)track(skill).limitOverride++;track(skill).purchases++;purchases++;revision++;return true;
    }
    public void grant(Skill skill,double raw,Balance b) {
        if(!Double.isFinite(raw)||raw<=0) return;
        Track t=track(skill);
        reconcile(skill,b);
        while(raw>0 && t.level<Integer.MAX_VALUE) {
            double m=multiplier(skill,b), required=needed(skill,b)-t.xp, consumed=required/m;
            if(raw<consumed) {t.xp+=raw*m;raw=0;} else {raw-=consumed;t.level++;t.xp=0;}
        }
        t.threshold=needed(skill,b);revision++;
    }
    public void reconcile(Skill skill,Balance b) {
        Track t=track(skill);double next=needed(skill,b);
        if(t.threshold>0&&t.threshold!=next)t.xp=Math.min(1,t.xp/t.threshold)*next;
        t.threshold=next;
    }
    private void normalizeFatigue(){if(Math.abs(fatigue)<1e-9)fatigue=0;}
    public void reconcile(Balance b) {normalizeFatigue();fatigue=Math.max(-b.fatigue.get("sleepReserve"),fatigue);for(Skill skill:List.copyOf(tracks.keySet()))reconcile(skill,b);stamina=Math.min(stamina,maxStamina(b));updateExhaustion(b);}
    public boolean event(String event,double units,double count,Balance b) {
        var rule=b.actions.get(event);
        if(rule==null||!Double.isFinite(units)||units<=0||!Double.isFinite(count)||count<0) return false;
        counters.merge(rule.counter(),count,Double::sum); grant(rule.skill(),units*rule.xp()*b.progression.get("trainingMultiplier"),b); revision++;return true;
    }
    public double requirementCount(Balance.Ability a){return Math.max(0,Math.min(a.amount(),counters.getOrDefault(a.counter(),0.0)));}
    public double requirementProgress(Balance.Ability a){return a.amount()<=0?1:requirementCount(a)/a.amount();}
    public boolean excluded(Balance.Ability a,Balance b){return a.excludes().stream().anyMatch(unlocked::contains)||b.abilities.stream().anyMatch(other->unlocked.contains(other.id())&&other.excludes().contains(a.id()));}
    public boolean prerequisites(Balance.Ability a){return unlocked.containsAll(a.all())&&(a.requiresAny().isEmpty()||a.requiresAny().stream().anyMatch(unlocked::contains));}
    public boolean canUnlock(Balance.Ability a,Balance b){return !unlocked.contains(a.id())&&!excluded(a,b)&&track(a.skill()).level>=a.level()&&counters.getOrDefault(a.counter(),0.0)>=a.amount()&&prerequisites(a);}
    public Set<String> revoke(String id,Balance b){
        Set<String> removed=new LinkedHashSet<>();if(unlocked.remove(id))removed.add(id);boolean changed;
        do{changed=false;for(var a:b.abilities)if(unlocked.contains(a.id())&&a.dependencies().stream().anyMatch(removed::contains)&&!prerequisites(a)){unlocked.remove(a.id());removed.add(a.id());changed=true;}}while(changed);
        revision++;return removed;
    }
    public boolean unlock(String id,Balance b) {
        var a=b.abilities.stream().filter(x->x.id().equals(id)).findFirst().orElse(null);
        if(a==null||!canUnlock(a,b)) return false;
        unlocked.add(id);revision++;return true;
    }
    public double effect(String effect,Balance b) {return b.abilities.stream().filter(a->unlocked.contains(a.id())&&a.effect().equals(effect)).mapToDouble(Balance.Ability::value).sum();}
    public double baseStamina(Balance b) {return b.stamina.get("base")+SkillEffects.attribute(this,b,"faustcore:stamina",false)+effect("stamina",b);}
    public double maxStamina(Balance b) {return Math.max(b.fatigue.get("minimum"),baseStamina(b)-(b.fatigue.get("enabled")>0?Math.max(0,Math.min(fatigue,b.fatigue.get("maximum"))):0));}
    public double capacity(Balance b) {return b.armor.get("baseCapacity")+track(Skill.defense).level*b.armor.get("capacityPerLevel")+effect("capacity",b);}
    public double spend(double requested,Balance b) {
        if(b.stamina.get("enabled")==0||!Double.isFinite(requested)||requested<=0) return 0;
        double paid=Math.min(stamina,requested);stamina-=paid;
        if(b.fatigue.get("enabled")>0) fatigue=Math.min(b.fatigue.get("maximum"),fatigue+b.fatigue.get("perSpentFraction")*paid*b.fatigue.get("accumulationMultiplier"));
        normalizeFatigue();stamina=Math.min(stamina,maxStamina(b));updateExhaustion(b); return paid;
    }
    public void rest(double dt,boolean sleeping,boolean campfire,boolean regenerate,Balance b) {rest(dt,sleeping,campfire,regenerate,1,b);}
    private void rest(double dt,boolean sleeping,boolean campfire,boolean regenerate,double recoveryScale,Balance b) {
        if(b.stamina.get("enabled")==0) return;
        if(b.fatigue.get("enabled")>0){
            if(sleeping&&fatigue>0)fatigue=Math.max(0,fatigue-dt*b.fatigue.get("sleepPerSecond"));
            else if(campfire){campfireRest+=dt*b.fatigue.get("campfirePerSecond");double whole=Math.floor(campfireRest+1e-9);campfireRest-=whole;if(fatigue>0)fatigue=Math.max(0,fatigue-whole);}
        }
        if(regenerate) stamina=Math.min(maxStamina(b),stamina+dt*recoveryScale*(b.stamina.get("regeneration")+effect("regeneration",b)));
        else stamina=Math.min(stamina,maxStamina(b));
        updateExhaustion(b);
    }
    public void environmentalFatigue(double dt,boolean soulCampfire,boolean soulFire,Balance b){
        if(b.stamina.get("enabled")==0||b.fatigue.get("enabled")==0)return;
        fatigue=Math.min(b.fatigue.get("maximum"),fatigue+dt*b.fatigue.get("accumulationMultiplier")*((soulCampfire?b.fatigue.get("soulCampfirePerSecond"):0)+(soulFire?b.fatigue.get("soulFirePerSecond"):0)));normalizeFatigue();stamina=Math.min(stamina,maxStamina(b));updateExhaustion(b);
    }
    public void recover(double dt,boolean sleep,boolean campfire,boolean delayReady,boolean shielding,Balance b){recover(dt,sleep,campfire,delayReady,shielding,false,false,b);}
    public void recover(double dt,boolean sleep,boolean campfire,boolean delayReady,boolean shielding,boolean flying,boolean airVisible,Balance b){
        double scale=airVisible?b.stamina.get("airRecoveryMultiplier"):1;
        if(!shielding&&!flying)secondWind(dt*scale,b);
        // Fatigue relief still uses real elapsed time, only stamina regeneration is slowed.
        rest(dt,sleep,campfire,delayReady&&!shielding&&!flying,scale,b);
    }
    public double exhaustionThreshold(Balance b){return Math.min(maxStamina(b),b.stamina.get("exhaustionRecovery")*Math.max(0,1-effect("exhaustion_threshold_reduction",b)));}
    public boolean empty(Balance b){return stamina<=1e-9||stamina<b.stamina.get("exhaustionTrigger");}
    public boolean actionsBlocked(Balance b){return b.stamina.get("enabled")>0&&(exhausted||empty(b));}
    public double actionCost(String action,Balance b){
        double cost=b.stamina.get(action+"Cost");
        if(action.equals("bow")||action.equals("crossbow")||action.equals("mace"))cost-=effect(action+"_cost_reduction",b);
        if(action.equals("mace"))cost=Math.max(b.stamina.get("attackCost"),cost);
        return Math.max(0,cost);
    }
    public void sleptNight(Balance b){if(b.fatigue.get("enabled")>0&&b.fatigue.get("resetAfterNight")>0)fatigue=-b.fatigue.get("sleepReserve");}

    public void secondWind(double dt,Balance b){if(b.stamina.get("enabled")>0&&effect("second_wind",b)>0){stamina=Math.min(maxStamina(b),stamina+dt*b.stamina.get("runCost")*b.stamina.get("secondWindMultiplier"));updateExhaustion(b);}}
    public void sweetFood(String item,Balance b){if(b.stamina.get("enabled")>0&&b.fatigue.get("enabled")>0&&fatigue>0)fatigue=Math.max(0,fatigue-b.sweetFoods.getOrDefault(item,0.0));}
    public void updateExhaustion(Balance b) {
        if(b.stamina.get("exhaustionLock")==0){exhausted=false;return;}
        if(empty(b))exhausted=true;
        else if(stamina>=exhaustionThreshold(b)-1e-9)exhausted=false;
    }
    public String json() {return Balance.JSON.toJson(this);}
    public static Progress parse(String json) {return parse(json,new Balance());}
    public static Progress parse(String json,Balance config) {
        Progress p=Balance.JSON.fromJson(json,Progress.class);
        if(p==null||(p.schema!=1&&p.schema!=2)||p.tracks==null||p.counters==null||p.unlocked==null||p.discoveries==null||p.path==null||p.purchases<0||!Double.isFinite(p.stamina)||p.stamina<0||!Double.isFinite(p.fatigue)||p.fatigue< -1e9) throw new IllegalArgumentException("Invalid saved core data");
        for(Skill s:Skill.values()) {Track t=p.track(s);if(t.level<0||!Double.isFinite(t.xp)||t.xp<0||t.purchases<0) throw new IllegalArgumentException("Invalid skill data");}
        if(p.schema==1) {
            double newBase=p.baseStamina(config);
            double oldBase=newBase/0.2;
            p.stamina=p.stamina*newBase/oldBase;p.fatigue*=newBase;
            // v1 bought +5; retain achieved cap, clamped at the new maximum.
            for(var t:p.tracks.values())t.purchases=Math.min(10,t.purchases>2?10:t.purchases*5);
            p.schema=2;
        }
        return p;
    }
}
