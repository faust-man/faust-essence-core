package com.faust.core.progression;

import java.util.*;

/** Persistent player data and deterministic balance calculations, independent of Minecraft. */
public final class Progress {
    public int schema=1;
    public Map<Skill,Track> tracks=new LinkedHashMap<>();
    public Map<String,Double> counters=new HashMap<>();
    public Set<String> unlocked=new HashSet<>(), discoveries=new HashSet<>();
    public long purchases, revision;
    public double stamina=100, fatigue;
    public String path="";
    public static final class Track { public int level; public double xp, threshold; public long purchases; }
    public Progress() {for(Skill s:Skill.values()) tracks.put(s,new Track());}
    public Track track(Skill skill) {return tracks.computeIfAbsent(skill,k->new Track());}
    public double needed(Skill skill,Balance b) {return b.progression.get("levelBase")+b.progression.get("levelSlope")*track(skill).level;}
    public double cap(Skill skill,Balance b) {return b.progression.get("initialCap")+b.progression.get("capStep")*track(skill).purchases;}
    public double multiplier(Skill skill,Balance b) {return track(skill).level<cap(skill,b)?1:1/(1+b.progression.get("slowdown")*(track(skill).level-cap(skill,b)+1));}
    public long price(Balance b) {
        double value=Math.ceil(b.progression.get("purchaseBase")*Math.pow(b.progression.get("purchaseGrowth"),purchases));
        return value>=Long.MAX_VALUE?Long.MAX_VALUE:(long)value;
    }
    public boolean buy(Skill skill,long funds,Balance b) {
        if(price(b)>funds||purchases==Long.MAX_VALUE||track(skill).purchases==Long.MAX_VALUE) return false;
        track(skill).purchases++; purchases++; revision++; return true;
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
    public void reconcile(Balance b) {for(Skill skill:List.copyOf(tracks.keySet()))reconcile(skill,b);stamina=Math.min(stamina,maxStamina(b));}
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
    public double maxStamina(Balance b) {return baseStamina(b)*(1-(b.fatigue.get("enabled")>0?Math.min(fatigue,b.fatigue.get("maximum")):0));}
    public double capacity(Balance b) {return b.armor.get("baseCapacity")+track(Skill.defense).level*b.armor.get("capacityPerLevel")+effect("capacity",b);}
    public double spend(double requested,Balance b) {
        if(b.stamina.get("enabled")==0||!Double.isFinite(requested)||requested<=0) return 0;
        double paid=Math.min(stamina,requested);stamina-=paid;
        if(b.fatigue.get("enabled")>0) fatigue=Math.min(b.fatigue.get("maximum"),fatigue+b.fatigue.get("perSpentFraction")*paid/baseStamina(b));
        stamina=Math.min(stamina,maxStamina(b)); return paid;
    }
    public void rest(double dt,boolean sleeping,boolean campfire,boolean regenerate,Balance b) {
        if(b.stamina.get("enabled")==0) return;
        if(b.fatigue.get("enabled")>0) fatigue=Math.max(0,fatigue-dt*(sleeping?b.fatigue.get("sleepPerSecond"):campfire?b.fatigue.get("campfirePerSecond"):0));
        if(regenerate) stamina=Math.min(maxStamina(b),stamina+dt*(b.stamina.get("regeneration")+effect("regeneration",b)));
        else stamina=Math.min(stamina,maxStamina(b));
    }
    public String json() {return Balance.JSON.toJson(this);}
    public static Progress parse(String json) {
        Progress p=Balance.JSON.fromJson(json,Progress.class);
        if(p==null||p.schema!=1||p.tracks==null||p.counters==null||p.unlocked==null||p.discoveries==null||p.path==null||p.purchases<0||!Double.isFinite(p.stamina)||p.stamina<0||!Double.isFinite(p.fatigue)||p.fatigue<0||p.fatigue>=1) throw new IllegalArgumentException("Invalid saved core data");
        for(Skill s:Skill.values()) {Track t=p.track(s);if(t.level<0||!Double.isFinite(t.xp)||t.xp<0||t.purchases<0) throw new IllegalArgumentException("Invalid skill data");}
        return p;
    }
}
