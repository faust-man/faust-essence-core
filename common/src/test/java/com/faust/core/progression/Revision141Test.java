package com.faust.core.progression;
import com.faust.essence.api.Skill;


import com.faust.stamina.client.StaminaSprites;
import java.nio.file.*;
import java.util.*;

/** Regression coverage for displayed zero, persistent exhaustion and the patch migration. */
public final class Revision141Test {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void near(double actual,double expected,String message){check(Math.abs(actual-expected)<1e-8,message+": "+actual+" != "+expected);}
    public static void run() throws Exception {
        Balance b=new Balance();Progress p=new Progress();b.validate();
        p.stamina=0.5;check(!p.actionsBlocked(b),"fractional reserve permits final action");near(p.spend(1,b),0.5,"final action consumes remaining reserve");check(p.exhausted&&p.actionsBlocked(b),"empty reserve enters persistent exhaustion");
        p.unlocked.add("faustcore:second_wind");p.recover(1,false,false,false,false,b);check(p.exhausted,"Second Wind cannot immediately cancel exhaustion");
        p.stamina=9.99;p.reconcile(b);check(p.actionsBlocked(b),"positive stamina still blocks sprint below recovery threshold");
        check(Progress.parse(p.json()).exhausted,"exhaustion flag survives client snapshot serialization");p.stamina=10;p.reconcile(b);check(!p.actionsBlocked(b),"recovery threshold releases sprint");
        p=new Progress();p.spend(8,b);near(p.fatigue,0.2,"fatigue from spending is halved");p.fatigue=0;p.environmentalFatigue(10,true,true,b);near(p.fatigue,0.7,"both soul sources accumulate half as fast");
        p.sleptNight(b);near(p.fatigue,-5,"sleep reserve not halved");p.fatigue=1;p.rest(1,true,false,false,b);near(p.fatigue,0.8,"sleep relief speed unchanged");
        p=new Progress();p.track(CoreSkills.strength).level=5;near(p.multiplier(CoreSkills.strength,b),0.13,"growth two levels beyond cap is thirteen percent");p.track(CoreSkills.strength).level=100;near(p.multiplier(CoreSkills.strength,b),0.01,"growth far beyond cap is one percent");p.track(CoreSkills.strength).level=2;near(p.multiplier(CoreSkills.strength,b),1,"below potential acceleration preserved");
        for(var rule:b.statBonuses)check(!(rule.skill==CoreSkills.building&&rule.attribute.equals("minecraft:player.entity_interaction_range")),"building cannot extend entity reach by default");
        check(StaminaSprites.color(0,true)==0xFFFF4040,"filled exhausted segment explicitly red");check(StaminaSprites.color(1,true)==0xFFFF4040,"empty exhausted outline explicitly red");
        Path dir=Files.createTempDirectory("core-141");try{
            Balance.load(dir);Files.delete(dir.resolve(".format-1.4.1"));
            var st=Balance.JSON.toJsonTree(b.stamina).getAsJsonObject();st.remove("exhaustionTrigger");st.addProperty("exhaustionLock",0);Files.writeString(Balance.sectionFile(dir,"stamina"),Balance.JSON.toJson(st));
            var f=Balance.JSON.toJsonTree(b.fatigue).getAsJsonObject();f.remove("accumulationMultiplier");f.addProperty("perSpentFraction",0.1);Files.writeString(Balance.sectionFile(dir,"fatigue"),Balance.JSON.toJson(f));
            var growth=Balance.JSON.toJsonTree(b.progression).getAsJsonObject();growth.remove("overCapGrowth");Files.writeString(dir.resolve("progression.json"),Balance.JSON.toJson(growth));
            var rules=new ArrayList<>(b.statBonuses);var old=new Balance.StatBonus();old.id="building_entity_reach";old.skill=CoreSkills.building;old.attribute="minecraft:player.entity_interaction_range";old.perLevel=1;old.requiresAbility="faustcore:training_building_reach";rules.add(old);Files.writeString(dir.resolve("statBonuses.json"),Balance.JSON.toJson(rules));
            Balance migrated=Balance.load(dir);near(migrated.stamina.get("exhaustionLock"),1,"patch enables exhaustion in old configs");near(migrated.fatigue.get("accumulationMultiplier"),0.5,"patch halves accumulation");near(migrated.fatigue.get("perSpentFraction"),0.1,"custom base rate preserved");near(migrated.progression.get("overCapGrowth"),0.33,"patch adds fixed over-cap growth");check(migrated.statBonuses.stream().noneMatch(r->r.skill==CoreSkills.building&&r.attribute.equals("minecraft:player.entity_interaction_range")),"migration removes old entity reach rule");check(Files.exists(dir.resolve("migration-v1.4.1/stamina.json")),"patch backup exists");
            var edited=Balance.JSON.toJsonTree(migrated.stamina).getAsJsonObject();edited.addProperty("exhaustionLock",0);Balance.saveGroup(dir,"stamina",edited.toString());near(Balance.load(dir).stamina.get("exhaustionLock"),0,"later explicit disabling is preserved");
        }finally{try(var paths=Files.walk(dir)){for(Path file:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
        System.out.println("Core 1.4.1 checks passed: "+checks);
    }
}
