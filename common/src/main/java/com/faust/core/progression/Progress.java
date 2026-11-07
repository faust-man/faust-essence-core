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
    public boolean exhausted;
    public String path="";
    public static final class Track { public int level; public double xp, threshold; public Integer limitOverride; public long purchases; }
    public Progress() {for(Skill s:Skill.values()) tracks.put(s,new Track());}
    public Track track(Skill skill) {return tracks.computeIfAbsent(skill,k->new Track());}
    public double needed(Skill skill,Balance b) {return b.progression.get("levelBase")+b.progression.get("levelSlope")*track(skill).level;}
    public double cap(Skill skill,Balance b) {return Math.min(b.progression.get("maximumCap"),track(skill).limitOverride!=null?track(skill).limitOverride:b.progression.get("initialCap")+b.progression.get("capStep")*track(skill).purchases);}
    public double multiplier(Skill skill,Balance b) {
        double level=track(skill).level,cap=cap(skill,b);
        return level<cap?Math.min(b.progression.get("maximumGrowth"),cap/Math.max(1,level)):1/(1+b.progression.get("slowdown")*(level-cap+1));
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
    public void reconcile(Balance b) {for(Skill skill:List.copyOf(tracks.keySet()))reconcile(skill,b);stamina=Math.min(stamina,maxStamina(b));updateExhaustion(b);}
    public boolean event(String event,double units,double count,Balance b) {
        var rule=b.actions.get(event);
        if(rule==null||!Double.isFinite(units)||units<=0||!Double.isFinite(count)||count<0) return false;
        counters.merge(rule.counter(),count,Double::sum); grant(rule.skill(),units*rule.xp(),b); revision++;return true;
    }
    public boolean canUnlock(Balance.Ability a) {
        return !unlocked.contains(a.id())&&track(a.skill()).level>=a.level()&&counters.getOrDefault(a.counter(),0.0)>=a.amount()&&(a.parent().isEmpty()||unlocked.contains(a.parent()));
    }
    public boolean unlock(String id,Balance b) {
        var a=b.abilities.stream().filter(x->x.id().equals(id)).findFirst().orElse(null);
        if(a==null||!canUnlock(a)) return false;
        unlocked.add(id);revision++;return true;
    }
    public double effect(String effect,Balance b) {return b.abilities.stream().filter(a->unlocked.contains(a.id())&&a.effect().equals(effect)).mapToDouble(Balance.Ability::value).sum();}
    public double baseStamina(Balance b) {return b.stamina.get("base")+track(Skill.constitution).level*b.stamina.get("constitutionPerLevel")+effect("stamina",b);}
    public double maxStamina(Balance b) {return Math.max(b.fatigue.get("minimum"),baseStamina(b)-(b.fatigue.get("enabled")>0?Math.min(fatigue,b.fatigue.get("maximum")):0));}
    public double capacity(Balance b) {return b.armor.get("baseCapacity")+track(Skill.defense).level*b.armor.get("capacityPerLevel")+effect("capacity",b);}
    public double spend(double requested,Balance b) {
        if(b.stamina.get("enabled")==0||!Double.isFinite(requested)||requested<=0) return 0;
        double paid=Math.min(stamina,requested);stamina-=paid;
        if(b.fatigue.get("enabled")>0) fatigue=Math.min(b.fatigue.get("maximum"),fatigue+b.fatigue.get("perSpentFraction")*paid);
        stamina=Math.min(stamina,maxStamina(b));updateExhaustion(b); return paid;
    }
    public void rest(double dt,boolean sleeping,boolean campfire,boolean regenerate,Balance b) {
        if(b.stamina.get("enabled")==0) return;
        if(b.fatigue.get("enabled")>0) fatigue=Math.max(0,fatigue-dt*(sleeping?b.fatigue.get("sleepPerSecond"):campfire?b.fatigue.get("campfirePerSecond"):0));
        if(regenerate) stamina=Math.min(maxStamina(b),stamina+dt*(b.stamina.get("regeneration")+effect("regeneration",b)));
        else stamina=Math.min(stamina,maxStamina(b));
        updateExhaustion(b);
    }
    public void updateExhaustion(Balance b) {
        if(b.stamina.get("exhaustionLock")==0){exhausted=false;return;}
        if(stamina<=1e-9)exhausted=true;
        else if(stamina>=maxStamina(b)-1e-9)exhausted=false;
    }
    public String json() {return Balance.JSON.toJson(this);}
    public static Progress parse(String json) {return parse(json,new Balance());}
    public static Progress parse(String json,Balance config) {
        Progress p=Balance.JSON.fromJson(json,Progress.class);
        if(p==null||(p.schema!=1&&p.schema!=2)||p.tracks==null||p.counters==null||p.unlocked==null||p.discoveries==null||p.path==null||p.purchases<0||!Double.isFinite(p.stamina)||p.stamina<0||!Double.isFinite(p.fatigue)||p.fatigue<0) throw new IllegalArgumentException("Invalid saved core data");
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
