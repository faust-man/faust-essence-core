package com.faust.core.progression;
import com.faust.essence.api.Skill;


import java.util.*;

/** Persistent player data and deterministic balance calculations, independent of Minecraft. */
public final class Progress {
    public int schema=3;
    public Double initialCap;
    public Map<Skill,Track> tracks=new LinkedHashMap<>();
    public Map<String,Double> counters=new HashMap<>();
    public Map<String,Map<String,Double>> practice=new HashMap<>();
    private transient CounterPlan counterPlan;
    public Set<String> unlocked=CounterPlan.learnedSet(Set.of()), discoveries=new HashSet<>();
    public Set<String> disabled=new HashSet<>();
    public long purchases, revision;
    public double stamina=20, fatigue;
    public double externalStaminaBonus;
    public boolean exhausted,soulFireBurn;
    public boolean naturalRegeneration=true;
    public double campfireRest;
    public String path="";
    public static final class Track { public int level; public double xp, threshold; public Integer limitOverride; public long purchases; }
    public Progress() {CoreSkills.initialize();for(Skill s:Skill.values()) tracks.put(s,new Track());}
    public Track track(Skill skill) {return tracks.computeIfAbsent(skill,k->new Track());}
    /** Sum of characteristic levels, including registered addon characteristics. */
    public long totalLevel(){return tracks.values().stream().mapToLong(t->t.level).sum();}
    /** Administrative full unlock; existing higher levels and XP are retained. */
    public void max(Balance b){
        max(b,null,20);
    }
    /** Raises tracks and grants only abilities within the requested characteristic levels. */
    public void max(Balance b,Skill selected,int level){
        if(level<0)throw new IllegalArgumentException("Invalid level");
        for(var skill:Skill.values())if(selected==null||selected==skill){var t=track(skill);t.level=Math.max(level,t.level);t.limitOverride=Math.max(level,(int)cap(skill,b));reconcile(skill,b);}
        for(var a:b.abilities)if((selected==null||a.skill()==selected)&&a.skillRequirements().stream().allMatch(r->r.level()<=level&&track(r.skill()).level>=r.level())){unlocked.add(a.id());disabled.remove(a.id());}
        revision++;
    }
    private double rule(Skill skill,Balance b,String key){var custom=b.characteristicRules.stream().filter(r->r.skill()==skill).findFirst().orElse(null);if(custom==null)return b.progression.get(key);return switch(key){case "initialCap"->custom.initialCap();case "maximumCap"->custom.maximumCap();case "capStep"->custom.capStep();case "levelBase"->custom.levelBase();case "levelSlope"->custom.levelSlope();case "maximumGrowth"->custom.maximumGrowth();default->b.progression.get(key);};}
    public double needed(Skill skill,Balance b) {return rule(skill,b,"levelBase")+rule(skill,b,"levelSlope")*track(skill).level;}
    public double cap(Skill skill,Balance b) {return Math.min(rule(skill,b,"maximumCap"),track(skill).limitOverride!=null?track(skill).limitOverride:(initialCap==null||b.characteristicRules.stream().anyMatch(r->r.skill()==skill)?rule(skill,b,"initialCap"):initialCap)+rule(skill,b,"capStep")*track(skill).purchases);}
    public double multiplier(Skill skill,Balance b) {
        double level=track(skill).level,cap=cap(skill,b);
        return level<cap?Math.min(rule(skill,b,"maximumGrowth"),1+0.25*Math.max(0,cap-level-1)):Math.max(1,Math.ceil(100*Math.pow(2,-(level-cap+1))))/100;
    }
    public long price(Skill skill,Balance b) {
        String group=costGroup(skill);
        double count=tracks.entrySet().stream().filter(e->costGroup(e.getKey()).equals(group)).mapToDouble(e->e.getValue().purchases).sum();
        double value=Math.ceil(b.limitCosts.getOrDefault(group+":base",1.0)+count*b.limitCosts.getOrDefault(group+":step",1.0));
        return value>=Long.MAX_VALUE?Long.MAX_VALUE:Math.max(0,(long)value);
    }
    public static String costGroup(Skill skill){int colon=skill.name().indexOf(':');return colon<0?"core":skill.name().substring(0,colon);}

    public boolean buy(Skill skill,long funds,Balance b) {
        if(cap(skill,b)>=rule(skill,b,"maximumCap")||price(skill,b)>funds||purchases==Long.MAX_VALUE||track(skill).purchases==Long.MAX_VALUE)return false;
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
    public void normalizeFatigue(){if(Math.abs(fatigue)<1e-9)fatigue=0;}
    public void reconcile(Balance b) {reconcileAbilities(b);if(StaminaServices.present()){normalizeFatigue();fatigue=Math.max(-b.fatigue.get("sleepReserve"),Math.min(fatigue,maxFatigue(b)));}for(Skill skill:List.copyOf(tracks.keySet()))reconcile(skill,b);if(StaminaServices.present())stamina=Math.min(stamina,maxStamina(b));updateExhaustion(b);}
    public boolean event(String event,double units,double count,Balance b) {
        var rule=b.actions.get(event);
        if(rule==null||!Double.isFinite(units)||units<=0||!Double.isFinite(count)||count<0) return false;
        recordCounter(rule.counter(),count,b); grant(rule.skill(),units*rule.xp()*b.progression.get("trainingMultiplier"),b); return true;
    }
    private CounterPlan counterPlan(Balance b){if(counterPlan==null||!counterPlan.valid(this,b))counterPlan=new CounterPlan(this,b);return counterPlan;}
    private static final Set<String> LIFETIME_MILESTONES=Set.of("regeneration_2_received","strength_2_received","speed_2_received","resistance_received","turtle_helmet_worn","faust_epic_fire_resistance_lava","faust_epic_poison_regeneration","faust_epic_resistance_4_damage");
    public boolean recordMilestone(String key,double value,Balance b){if(!Double.isFinite(value)||value<0)return false;if(value<=counters.getOrDefault(key,0.0))return false;setCounter(key,value,b);return true;}
    public boolean wantsCounter(String key,Balance b){return counterPlan(b).wants(this,key);}
    public boolean recordCounter(String key,double amount,Balance b){return Double.isFinite(amount)&&amount>0&&counterPlan(b).record(this,key,amount,false);}
    public boolean recordMaximum(String key,double value,Balance b){if(LIFETIME_MILESTONES.contains(key))return recordMilestone(key,value,b);return Double.isFinite(value)&&value>0&&counterPlan(b).record(this,key,value,true);}
    /** Explicit administrative/fixture override; gameplay should use recordCounter. */
    public void setCounter(String key,double value,Balance b){if(!Double.isFinite(value)||value<0)throw new IllegalArgumentException("Invalid counter");counterPlan(b);counters.put(key,value);for(var a:b.abilities)if(prerequisites(a))for(var c:a.actionConditions())if(c.counter().equals(key))practice.get(a.id()).put(key,Math.min(value,c.amount()));counterPlan=null;revision++;}
    public double requirementCount(Balance.Ability a){return conditionCount(a,new Balance.Condition(a.counter(),a.amount()));}
    public double conditionCount(Balance.Condition c){return Math.max(0,Math.min(c.amount(),counters.getOrDefault(c.counter(),0.0)));}
    public double conditionCount(Balance.Ability a,Balance.Condition c){if(!prerequisites(a))return 0;if(LIFETIME_MILESTONES.contains(c.counter()))return conditionCount(c);var values=practice.get(a.id());return Math.max(0,Math.min(c.amount(),(values==null?counters:values).getOrDefault(c.counter(),0.0)));}
    public long completedConditions(Balance.Ability a){return a.actionConditions().stream().filter(c->conditionCount(a,c)>=c.amount()).count();}
    public double requirementProgress(Balance.Ability a){if(!prerequisites(a))return 0;return a.actionConditions().stream().mapToDouble(c->c.amount()<=0?(a.skillRequirements().stream().allMatch(r->track(r.skill()).level>=r.level())?1:0):conditionCount(a,c)/c.amount()).boxed().sorted(java.util.Comparator.reverseOrder()).limit(a.requiredConditions()).mapToDouble(Double::doubleValue).sum()/a.requiredConditions();}
    public boolean excluded(Balance.Ability a,Balance b){return a.excludes().stream().anyMatch(unlocked::contains)||b.abilities.stream().anyMatch(other->unlocked.contains(other.id())&&other.excludes().contains(a.id()));}
    public boolean prerequisites(Balance.Ability a){return unlocked.containsAll(a.all())&&(a.requiresAny().stream().filter(unlocked::contains).count()>=a.requiredParents());}
    public boolean canUnlock(Balance.Ability a,Balance b){return !unlocked.contains(a.id())&&!excluded(a,b)&&a.skillRequirements().stream().allMatch(r->track(r.skill()).level>=r.level())&&completedConditions(a)>=a.requiredConditions()&&prerequisites(a);}
    public Set<String> revoke(String id,Balance b){
        Set<String> removed=new LinkedHashSet<>();if(unlocked.remove(id))removed.add(id);boolean changed;
        do{changed=false;for(var a:b.abilities)if(unlocked.contains(a.id())&&a.dependencies().stream().anyMatch(removed::contains)&&!prerequisites(a)){unlocked.remove(a.id());removed.add(a.id());changed=true;}}while(changed);
        disabled.removeAll(removed);revision++;return removed;
    }
    public boolean unlock(String id,Balance b) {
        var a=b.abilities.stream().filter(x->x.id().equals(id)).findFirst().orElse(null);
        if(a==null||!canUnlock(a,b)) return false;
        counterPlan(b);unlocked.add(id);disabled.remove(id);revision++;return true;
    }
    /** Learned status controls requirements; enabled status controls only effects. */
    public boolean enabled(String id){return unlocked.contains(id)&&!disabled.contains(id);}
    public boolean enabled(String id,Balance b){return unlocked.contains(id)&&(enabled(id)||b.abilities.stream().anyMatch(a->a.id().equals(id)&&!a.toggleable()));}
    public void reconcileAbilities(Balance b){
        if(disabled.removeIf(id->b.abilities.stream().anyMatch(a->a.id().equals(id)&&!a.toggleable())))revision++;
    }
    public boolean toggle(String id,Balance b){
        if(!unlocked.contains(id)||b.abilities.stream().noneMatch(a->a.id().equals(id)&&a.toggleable()))return false;
        if(!disabled.remove(id))disabled.add(id);
        revision++;return true;
    }
    public double effect(String effect,Balance b) {return b.abilities.stream().filter(a->enabled(a.id(),b)&&a.effect().equals(effect)).mapToDouble(Balance.Ability::value).sum();}
    public double baseStamina(Balance b) {return StaminaServices.mechanics().baseStamina(this, b)+(StaminaServices.present()&&Double.isFinite(externalStaminaBonus)?externalStaminaBonus:0);}
    public double maxFatigue(Balance b) {return StaminaServices.mechanics().maxFatigue(this, b);}
    public double maxStamina(Balance b) {return StaminaServices.mechanics().maxStamina(this, b);}
    public double capacity(Balance b) {return StaminaServices.mechanics().capacity(this, b);}
    public double spend(double requested,Balance b) {return StaminaServices.mechanics().spend(this, requested, b);}
    public double spend(double requested,Balance b,long worldAgeTicks) {return StaminaServices.mechanics().spend(this, requested, b, worldAgeTicks);}
    public void rest(double dt,boolean sleeping,boolean campfire,boolean regenerate,Balance b) {StaminaServices.mechanics().rest(this, dt, sleeping, campfire, regenerate, b);}
    public void rest(double dt,boolean sleeping,boolean campfire,boolean regenerate,double recoveryScale,Balance b) {StaminaServices.mechanics().rest(this, dt, sleeping, campfire, regenerate, recoveryScale, b);}
    public void environmentalFatigue(double dt,boolean soulCampfire,boolean soulFire,Balance b) {StaminaServices.mechanics().environmentalFatigue(this, dt, soulCampfire, soulFire, b);}
    public void environmentalFatigue(double dt,boolean soulCampfire,boolean soulFire,Balance b,long worldAgeTicks) {StaminaServices.mechanics().environmentalFatigue(this, dt, soulCampfire, soulFire, b, worldAgeTicks);}
    public void recover(double dt,boolean sleep,boolean campfire,boolean delayReady,boolean shielding,Balance b) {StaminaServices.mechanics().recover(this, dt, sleep, campfire, delayReady, shielding, b);}
    public void recover(double dt,boolean sleep,boolean campfire,boolean delayReady,boolean shielding,boolean flying,boolean airVisible,Balance b) {StaminaServices.mechanics().recover(this, dt, sleep, campfire, delayReady, shielding, flying, airVisible, b);}
    public void recover(double dt,boolean sleep,boolean campfire,boolean delayReady,boolean shielding,boolean flying,boolean airVisible,boolean fastSwimming,Balance b) {StaminaServices.mechanics().recover(this, dt, sleep, campfire, delayReady, shielding, flying, airVisible, fastSwimming, b);}
    public double exhaustionThreshold(Balance b) {return StaminaServices.mechanics().exhaustionThreshold(this, b);}
    public boolean empty(Balance b) {return StaminaServices.mechanics().empty(this, b);}
    public boolean actionsBlocked(Balance b) {return StaminaServices.mechanics().actionsBlocked(this, b);}
    public double actionCost(String action,Balance b) {return StaminaServices.mechanics().actionCost(this, action, b);}
    public void sleptNight(Balance b) {StaminaServices.mechanics().sleptNight(this, b);}
    public void secondWind(double dt,Balance b) {StaminaServices.mechanics().secondWind(this, dt, b);}
    public void sweetFood(String item,Balance b) {StaminaServices.mechanics().sweetFood(this, item, b);}
    public void sweetFood(String item,double count,Balance b) {StaminaServices.mechanics().sweetFood(this, item, count, b);}
    public void updateExhaustion(Balance b) {StaminaServices.mechanics().updateExhaustion(this, b);}
    public String json() {return Balance.JSON.toJson(this);}
    public static Progress parse(String json) {return parse(json,new Balance());}
    public static Progress parse(String json,Balance config) {
        Progress p=Balance.JSON.fromJson(json,Progress.class);
        if(p==null||(p.schema!=1&&p.schema!=2&&p.schema!=3)||p.tracks==null||p.counters==null||p.unlocked==null||p.disabled==null||p.disabled.contains(null)||p.discoveries==null||p.path==null||p.purchases<0||!Double.isFinite(p.stamina)||p.stamina<0||!Double.isFinite(p.fatigue)||p.fatigue< -1e9) throw new IllegalArgumentException("Invalid saved core data");
        for(Skill s:Skill.values()) {Track t=p.track(s);if(t.level<0||!Double.isFinite(t.xp)||t.xp<0||t.purchases<0) throw new IllegalArgumentException("Invalid skill data");}
        if(p.schema==1&&StaminaServices.present()) {
            double newBase=p.baseStamina(config);
            double oldBase=newBase/0.2;
            p.stamina=p.stamina*newBase/oldBase;p.fatigue*=newBase;
            // v1 bought +5; retain achieved cap, clamped at the new maximum.
            for(var t:p.tracks.values())t.purchases=Math.min(10,t.purchases>2?10:t.purchases*5);
            p.schema=2;
        }
        if(p.schema<3){p.initialCap=config.progression.getOrDefault("legacyInitialCap",5.0);p.schema=3;}
        if(p.initialCap!=null&&(!Double.isFinite(p.initialCap)||p.initialCap<0))throw new IllegalArgumentException("Invalid initial cap");
        if(p.practice==null)throw new IllegalArgumentException("Invalid practice data");
        for(var values:p.practice.values()){if(values==null||values.values().stream().anyMatch(v->v==null||!Double.isFinite(v)||v<0))throw new IllegalArgumentException("Invalid practice counter");}
        if(!com.google.gson.JsonParser.parseString(json).getAsJsonObject().has("practice")){
            for(var a:config.abilities){var values=new HashMap<String,Double>();if(p.prerequisites(a))for(var c:a.actionConditions())values.put(c.counter(),Math.min(c.amount(),p.counters.getOrDefault(c.counter(),0.0)));p.practice.put(a.id(),values);}
        }
        p.unlocked=CounterPlan.learnedSet(p.unlocked);p.disabled.retainAll(p.unlocked);p.reconcileAbilities(config);
        return p;
    }
}
