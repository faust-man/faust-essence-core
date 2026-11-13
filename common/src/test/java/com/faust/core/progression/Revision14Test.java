package com.faust.core.progression;

import java.nio.file.*;
import java.util.*;

/** Regression checks for exhaustion, breathing, ranged costs and configuration upgrades. */
public final class Revision14Test {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void near(double actual,double expected,String message){check(Math.abs(actual-expected)<1e-7,message+": "+actual+" != "+expected);}
    private static Balance.Ability node(Balance b,String id){return b.abilities.stream().filter(a->a.id().equals("faustcore:"+id)).findFirst().orElseThrow();}
    public static void run() throws Exception {
        Balance b=new Balance();Progress p=new Progress();b.validate();
        near(b.stamina.get("runCost"),0.1,"base sprint cost halved");near(b.stamina.get("armorCost"),0.25,"armor sprint cost halved");near(b.stamina.get("sprintJumpCost"),0.5,"sprint jump cost halved");near(b.stamina.get("jumpArmorCost"),0.05,"armor jump cost halved");
        near(p.cap(Skill.strength,b),5,"initial potential is five");near(p.price(Skill.strength,b),5,"purchase formula uses new initial caps");
        for(var a:b.abilities)if(a.effect().equals("stat_unlock")){check(a.level()>=3,"stat unlock requires trained level");check(a.amount()>=15,"stat unlock requires substantial practice");}
        var requirement=node(b,"training_strength_damage");p.counters.put("hits",1e6);near(p.requirementCount(requirement),100,"display counter capped at requirement");near(p.requirementProgress(requirement),1,"overcompletion bar stays within bounds");near(p.counters.get("hits"),1e6,"underlying counter remains available for later nodes");
        near(node(b,"shield_care").value(),0.25,"shield wear bonus reduced to ten percent");near(node(b,"shield_recovery").value(),0.25,"shield disable bonus reduced to fifteen percent");
        p.spend(20,b);check(p.exhausted&&p.actionsBlocked(b),"zero enters exhaustion");p.stamina=9.99;p.updateExhaustion(b);check(p.actionsBlocked(b),"exhausted below ten despite positive stamina");p.stamina=10;p.updateExhaustion(b);check(!p.actionsBlocked(b),"ten clears exhaustion without full refill");
        p.stamina=0;p.updateExhaustion(b);p.unlocked.add("faustcore:resilient_recovery");near(p.exhaustionThreshold(b),5,"recovery perk halves threshold");p.stamina=5;p.updateExhaustion(b);check(!p.exhausted,"perk exits at five");
        p=new Progress();p.fatigue=16;p.stamina=0;p.updateExhaustion(b);near(p.exhaustionThreshold(b),4,"threshold respects capacity reduced by fatigue");p.recover(10,false,false,true,false,false,true,b);near(p.stamina,4,"air recovery clamps exactly at reduced capacity");check(!p.exhausted,"no softlock at reduced capacity");
        p=new Progress();p.stamina=1;p.recover(1,false,false,true,false,false,false,b);near(p.stamina,5,"normal regeneration");p.stamina=1;p.recover(1,false,false,true,false,false,true,b);near(p.stamina,3,"bubbles halve regeneration");
        p.unlocked.add("faustcore:second_wind");p.stamina=1;p.recover(1,false,false,true,false,false,true,b);near(p.stamina,3.075,"bubbles halve Second Wind too");
        p.stamina=1;p.recover(10,false,false,true,false,true,false,b);near(p.stamina,1,"elytra suppresses all recovery including Second Wind");p.recover(10,false,false,true,true,false,false,b);near(p.stamina,1,"shield suppresses all recovery");
        p=new Progress();near(p.actionCost("bow",b),2,"bow costs two");near(p.actionCost("crossbow",b),1,"crossbow costs one");near(p.actionCost("mace",b),2,"mace costs two");
        var bow=node(b,"bow_efficiency");p.track(Skill.dexterity).level=4;p.counters.put("bow_shots",30.0);check(!p.canUnlock(bow,b),"archery requires level five");p.track(Skill.dexterity).level=5;check(p.unlock(bow.id(),b),"archery requires shots and level");near(p.actionCost("bow",b),1,"first archery tier costs one");
        p.track(Skill.dexterity).level=10;p.setCounter("bow_shots",150.0,b);check(p.unlock(node(b,"bow_mastery").id(),b),"archery second tier follows first");near(p.actionCost("bow",b),0,"master archery costs zero");
        p.stamina=0;p.updateExhaustion(b);check(p.actionsBlocked(b),"free shots still blocked during exhaustion");p.unlocked.add("faustcore:crossbow_efficiency");near(p.actionCost("crossbow",b),0,"strength perk makes crossbow free");p.unlocked.add("faustcore:mace_efficiency");near(p.actionCost("mace",b),b.stamina.get("attackCost"),"mace perk brings cost to regular attack");
        Progress runner=new Progress(),swimmer=new Progress();runner.event("constitution_run",10,10,b);swimmer.event("constitution_swim",10*b.skills.get("constitutionSwimMultiplier"),10,b);near(swimmer.track(Skill.constitution).xp,2*runner.track(Skill.constitution).xp,"swimming trains constitution twice as fast");
        check(Breathing.visible(true,300,300),"full air underwater renders bubbles");check(Breathing.visible(false,280,300),"depleted air renders bubbles outside water");check(!Breathing.visible(false,300,300),"no bubbles at full air outside water");check(!Breathing.visible(true,0,300),"no bubble sprite at zero air");check(!Breathing.visible(true,20,0),"zero max air cannot divide by zero");
        p=new Progress();p.fatigue=12;p.sleptNight(b);near(p.fatigue,-5,"night skip grants minus five fatigue");near(p.maxStamina(b),20,"reserve cannot inflate stamina capacity");check(Progress.parse(p.json()).fatigue==-5,"negative fatigue survives serialization");
        p.spend(20,b);near(p.fatigue,-4.5,"spending consumes reserve");p.fatigue=-0.025;p.stamina=20;p.spend(1,b);near(p.fatigue,0,"reserve reaches zero");near(p.maxStamina(b),20,"zero fatigue has full capacity");p.spend(1,b);near(p.fatigue,0.025,"reserve crosses zero to positive fatigue");near(p.maxStamina(b),19.975,"positive fatigue alone reduces capacity");
        Progress reserve=new Progress();reserve.sleptNight(b);for(int i=0;i<200;i++){reserve.stamina=20;reserve.spend(1,b);}check(reserve.fatigue==0,"repeated reserve spending normalizes zero exactly");near(reserve.maxStamina(b),20,"floating point residue never reserves a HUD segment");
        p.fatigue=-2;p.rest(1,true,false,true,b);near(p.fatigue,-2,"ordinary sleep recovery preserves existing negative reserve");p.sweetFood("minecraft:cookie",b);near(p.fatigue,-2,"sweet food never raises negative fatigue");p.recover(100,false,false,true,false,false,false,b);near(p.fatigue,-2,"stamina refill does not assign minus five");
        b.fatigue.put("resetAfterNight",0.0);p.fatigue=4;p.sleptNight(b);near(p.fatigue,4,"night reset can be disabled");
        b=new Balance();Path dir=Files.createTempDirectory("core-14-config");try {
            Balance.load(dir);Files.delete(dir.resolve(".format-1.4"));
            var progression=Balance.JSON.toJsonTree(b.progression).getAsJsonObject();progression.addProperty("initialCap",10);Files.writeString(dir.resolve("progression.json"),Balance.JSON.toJson(progression));
            var stamina=Balance.JSON.toJsonTree(b.stamina).getAsJsonObject();stamina.remove("swimCost");stamina.addProperty("runCost",0.2);stamina.addProperty("armorCost",0.5);stamina.addProperty("sprintJumpCost",1);stamina.addProperty("jumpArmorCost",0.1);stamina.addProperty("bowCost",3);Files.writeString(Balance.sectionFile(dir,"stamina"),Balance.JSON.toJson(stamina));
            var old=new ArrayList<>(b.abilities);var a=node(b,"training_strength_damage");old.removeIf(n->n.id().equals(a.id())||n.id().equals("faustcore:bow_mastery"));old.add(new Balance.Ability(a.id(),a.skill(),1,a.counter(),1,"",a.effect(),0));old.add(new Balance.Ability("test:custom",Skill.strength,7,"hits",42,"","damage",0.03));Files.writeString(dir.resolve("abilities.json"),Balance.JSON.toJson(old));
            Balance migrated=Balance.load(dir);near(migrated.progression.get("initialCap"),5,"migration reduces old default potential");near(migrated.stamina.get("runCost"),0.1,"migration halves old sprint rate");near(migrated.stamina.get("armorCost"),0.25,"migration halves old armor rate");near(migrated.stamina.get("swimCost"),0.4,"migration adds swim cost");near(migrated.stamina.get("bowCost"),3,"migration preserves custom costs");check(node(migrated,"training_strength_damage").level()==5,"migration hardens built-in requirements");check(migrated.abilities.stream().anyMatch(n->n.id().equals("test:custom")),"migration preserves custom node");check(node(migrated,"bow_mastery").level()==10,"migration adds ranged node");check(Files.exists(dir.resolve("migration-v1.4/progression.json")),"migration keeps backups");near(Balance.load(dir).stamina.get("bowCost"),3,"migration idempotent");
        }finally{try(var paths=Files.walk(dir)){for(Path file:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
        Path parents=Files.createTempDirectory("core-defense-parents");try {
            Balance.load(parents);Files.delete(parents.resolve(".defense-parents"));Files.delete(parents.resolve(".health-ten"));
            var saved=Balance.JSON.toJsonTree(b.abilities).getAsJsonArray();
            for(var entry:saved){var a=entry.getAsJsonObject();switch(a.get("id").getAsString()){
                case "faustcore:training_defense_armor" -> {a.addProperty("parent","");a.addProperty("value",0.123);}
                case "faustcore:shield_recovery" -> a.addProperty("parent","faustcore:shield_care");
                case "faustcore:training_constitution_health" -> a.addProperty("level",7);
            }}
            Files.writeString(parents.resolve("abilities.json"),Balance.JSON.toJson(saved));Balance upgraded=Balance.load(parents);
            check(node(upgraded,"training_defense_armor").parent().equals("faustcore:armor_familiarity"),"existing reinforced gains iron armor parent");
            check(node(upgraded,"shield_recovery").parent().equals("faustcore:shield_efficiency"),"existing shield recovery gains efficient block parent");
            near(node(upgraded,"training_defense_armor").value(),0.123,"dependency migration preserves custom effect value");
            check(node(upgraded,"training_constitution_health").level()==10,"existing health requires constitution10");
            check(Files.exists(parents.resolve("abilities-before-defense-parents.json")),"dependency migration backup");
            check(Balance.load(parents).abilities.equals(upgraded.abilities),"dependency migration idempotent");
        }finally{try(var paths=Files.walk(parents)){for(Path file:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
        System.out.println("Core 1.4 checks passed: "+checks);
    }
}
