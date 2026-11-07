package com.faust.core.progression;

import java.nio.file.*;
import java.util.*;

/** Dependency-light regression suite, executed by :common:modelTest and build. */
public final class ProgressTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void near(double actual,double expected,String message){check(Math.abs(actual-expected)<1e-7,message+": "+actual+" != "+expected);}
    private static void rejects(Runnable task,String message){try{task.run();}catch(IllegalArgumentException e){checks++;return;}throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        CommandTest.run();
        Balance b=new Balance();b.validate();
        Progress stats=new Progress();stats.track(Skill.constitution).level=20;near(20+SkillEffects.health(stats,b),40,"constitution 20 gives 40 HP");
        stats.track(Skill.intelligence).level=10;near(SkillEffects.luck(stats,b),0.1,"intelligence Luck scaling");stats.track(Skill.intelligence).level=100;near(SkillEffects.luck(stats,b),0.2,"Luck capped at plus 0.2");
        stats.track(Skill.building).level=9;near(SkillEffects.reach(stats,b),0,"reach before threshold");stats.track(Skill.building).level=10;near(SkillEffects.reach(stats,b),1,"ten building gives one reach");stats.track(Skill.building).level=20;near(SkillEffects.reach(stats,b),2,"twenty building gives two reach");near(SkillEffects.breaking(stats,b),0.2,"one percent breaking speed per level");
        for(int[] pair:new int[][]{{0,0},{4,0},{5,8},{9,8},{10,16},{19,16},{20,24}})check(SkillEffects.icon(pair[0],b)==pair[1],"icon tier boundary "+pair[0]);
        BonusEffects.register("test:bonus");rejects(()->BonusEffects.register("test:bonus"),"duplicate bonus rejected");
        com.faust.core.api.CoreApi.registerPath("test:path",player->true);
        rejects(()->com.faust.core.api.CoreApi.registerPath("test:path",player->true),"duplicate path rejected");
        com.faust.core.api.CoreApi.beforeProgress(grant->0.5);
        near(com.faust.core.api.CoreApi.before(new com.faust.core.api.CoreApi.Grant(null,"test",2,1)),0.5,"API before-progress multiplier");
        com.faust.core.api.CoreApi.beforeProgress(grant->0);
        near(com.faust.core.api.CoreApi.before(new com.faust.core.api.CoreApi.Grant(null,"test",2,1)),0,"API cancellation");
        java.util.concurrent.atomic.AtomicInteger notified=new java.util.concurrent.atomic.AtomicInteger();
        com.faust.core.api.CoreApi.afterProgress(change->notified.incrementAndGet());
        com.faust.core.api.CoreApi.after(new com.faust.core.api.CoreApi.Change(java.util.UUID.randomUUID(),"test","before","after"));
        check(notified.get()==1,"API after-progress notification");
        Progress p=new Progress();near(p.multiplier(Skill.strength,b),2,"zero level growth limited to 200 percent");
        p.track(Skill.strength).level=5;near(p.multiplier(Skill.strength,b),2,"cap double -> 200 percent");
        p.track(Skill.strength).level=3;p.track(Skill.strength).limitOverride=9;near(p.multiplier(Skill.strength,b),2,"cap triple still limited to 200 percent");
        p=new Progress();p.track(Skill.strength).level=9;p.track(Skill.strength).xp=275;p.grant(Skill.strength,20,b);check(p.track(Skill.strength).level==10,"cross soft cap");near(p.track(Skill.strength).xp,(20-4.5)*0.8,"split award at accelerated boundary");
        Progress small=new Progress(),large=new Progress();
        for(int i=0;i<1000;i++)small.grant(Skill.strength,0.5,b);large.grant(Skill.strength,500,b);
        check(small.track(Skill.strength).level==large.track(Skill.strength).level,"grouping invariant below and above cap");near(small.track(Skill.strength).xp,large.track(Skill.strength).xp,"fractional precision");
        small.track(Skill.strength).level=1000;small.track(Skill.strength).xp=0;small.track(Skill.strength).threshold=small.needed(Skill.strength,b);small.grant(Skill.strength,0.001,b);check(small.track(Skill.strength).xp>0,"growth above cap remains positive");
        p=new Progress();long[] prices={22,24,25,26,27};
        for(int i=0;i<prices.length;i++){check(p.price(Skill.strength,b)==prices[i],"n + total caps / 5 rounded up "+i);check(p.buy(Skill.strength,10000,b),"purchase +1");}
        near(p.cap(Skill.strength,b),15,"five purchases add five");near(p.cap(Skill.building,b),10,"other cap unchanged");near(p.price(Skill.building,b),23,"shared sum affects different branch");
        for(int i=0;i<30;i++)p.buy(Skill.strength,10000,b);near(p.cap(Skill.strength,b),20,"cap hard maximum 20");
        long before=p.purchases;check(!p.buy(Skill.strength,10000,b),"cannot buy beyond 20");check(p.purchases==before,"capped purchase atomicity");check(!p.buy(Skill.defense,0,b),"insufficient vanilla levels");
        Progress mixed=new Progress();near(mixed.price(Skill.strength,b),22,"first selected cap price");mixed.buy(Skill.strength,22,b);near(mixed.price(Skill.building,b),23,"global increase across branches");mixed.buy(Skill.building,23,b);near(mixed.price(Skill.strength,b),24,"return to purchased branch");
        Skill custom=Skill.register("test:path_skill");rejects(()->Skill.register("test:path_skill"),"duplicate skill rejected");
        Progress extension=new Progress();extension.grant(custom,5,b);check(Progress.parse(extension.json()).track(custom).xp==10,"extension skill persistence");
        p=new Progress();var a=b.abilities.getFirst();p.track(a.skill()).level=a.level();check(!p.unlock(a.id(),b),"level alone insufficient");
        p.track(a.skill()).level=0;p.counters.put(a.counter(),a.amount());check(!p.unlock(a.id(),b),"condition alone insufficient");p.track(a.skill()).level=a.level();check(p.unlock(a.id(),b),"both conditions");check(!p.unlock(a.id(),b),"no duplicate unlock");
        var child=b.abilities.get(1);Progress q=new Progress();q.track(child.skill()).level=20;q.counters.put(child.counter(),10000.0);check(!q.unlock(child.id(),b),"parent required");
        check(!p.event("unknown",1,1,b),"unknown source");double old=p.track(Skill.constitution).xp;p.event("damage",4,4,b);near(p.track(Skill.constitution).xp,old+4*p.multiplier(Skill.constitution,b),"actual damage practice");
        p=new Progress();near(p.stamina,20,"base stamina 20");near(p.spend(8,b),8,"paid stamina");near(p.fatigue,0.4,"fatigue in absolute units");near(p.maxStamina(b),19.6,"fatigue occupies right of bar");
        p.rest(10,false,false,true,b);near(p.stamina,19.6,"regen limited by fatigue");p.rest(1,true,false,true,b);near(p.fatigue,0.2,"gradual sleep units");p.rest(1,false,true,true,b);near(p.fatigue,0.16,"campfire units");
        p.stamina=1;near(p.spend(20,b),1,"cannot overspend");double f=p.fatigue;p.spend(20,b);near(p.fatigue,f,"no fatigue for unpaid request");
        b.fatigue.put("enabled",0.0);near(p.maxStamina(b),20,"fatigue disabled");b.stamina.put("enabled",0.0);near(p.spend(20,b),0,"stamina disabled");near(p.fatigue,f,"disable preserves history");
        Balance lockBalance=new Balance();Progress locked=new Progress();locked.spend(100,lockBalance);check(locked.exhausted,"zero stamina locks sprint");
        locked.rest(1,false,false,true,lockBalance);check(locked.stamina>0&&locked.exhausted,"partial recovery cannot unlock sprint");
        Progress lockedCopy=Progress.parse(locked.json());check(lockedCopy.exhausted,"sprint lock persists over save/load");
        locked.rest(20,false,false,true,lockBalance);check(!locked.exhausted,"full available recovery unlocks sprint");near(locked.stamina,locked.maxStamina(lockBalance),"unlock at available maximum with fatigue");
        near((14-2)*lockBalance.stamina.get("armorCost"),6,"netherite zero defense costs six stamina per second");
        b=new Balance();p=new Progress();p.buy(Skill.defense,1000,b);p.fatigue=4;p.stamina=10;p.discoveries.add("minecraft:test/1");Progress restored=Progress.parse(p.json());check(restored.json().equals(p.json()),"save/load round trip");
        Progress migrated=Progress.parse("{\"schema\":1,\"stamina\":50,\"fatigue\":0.2}");near(migrated.stamina,10,"migrate v1 stamina");near(migrated.fatigue,4,"migrate fraction to units");check(migrated.schema==2,"new schema");
        rejects(()->Progress.parse("{\"schema\":3}"),"future schema rejected");
        Progress offline=new Progress();offline.grant(Skill.building,5,b);String saved=offline.json();Balance changed=new Balance();changed.progression.put("levelBase",200.0);offline=Progress.parse(saved);offline.reconcile(changed);near(offline.track(Skill.building).xp,20,"offline threshold migration retains fraction");
        Balance invalid=new Balance();var one=invalid.abilities.getFirst();invalid.abilities.set(0,new Balance.Ability(one.id(),one.skill(),one.level(),one.counter(),one.amount(),one.id(),one.effect(),one.value()));rejects(invalid::validate,"cycle rejected");
        invalid=new Balance();invalid.fatigue.put("maximum",21.0);rejects(invalid::validate,"zero maximum prohibited");
        Path dir=Files.createTempDirectory("core-config-test");try {
            Balance loaded=Balance.load(dir);check(loaded.abilities.size()==14,"default config generated");
            String original=Files.readString(dir.resolve("stamina.json"));boolean invalidEdit=false;
            try{Balance.saveGroup(dir,"stamina","{}");}catch(IllegalArgumentException e){invalidEdit=true;}check(invalidEdit,"config editor validates entire candidate");check(Files.readString(dir.resolve("stamina.json")).equals(original),"invalid edit preserves disk config");
            var edited=Balance.JSON.toJsonTree(loaded.stamina).getAsJsonObject();edited.addProperty("regeneration",6);Balance.saveGroup(dir,"stamina",edited.toString());near(Balance.load(dir).stamina.get("regeneration"),6,"config editor saves valid group");
            rejects(()->loaded.edited("../server.properties","{}"),"editor rejects path traversal/unknown sections");
            Files.writeString(dir.resolve("progression.json"),"{}");boolean failed=false;try{Balance.load(dir);}catch(java.io.IOException e){failed=true;}check(failed,"invalid reload rejected");
            var legacy=Balance.JSON.toJsonTree(new Balance().progression).getAsJsonObject();legacy.remove("maximumCap");legacy.remove("costDivisor");legacy.addProperty("purchaseBase",100);legacy.addProperty("purchaseGrowth",1.5);legacy.addProperty("capStep",5);
            Files.writeString(dir.resolve("progression.json"),Balance.JSON.toJson(legacy));
            var legacyStamina=Balance.JSON.toJsonTree(new Balance().stamina).getAsJsonObject();legacyStamina.remove("exhaustionLock");legacyStamina.addProperty("armorCost",0.5);legacyStamina.addProperty("base",100);legacyStamina.addProperty("constitutionPerLevel",1);legacyStamina.addProperty("regeneration",20);Files.writeString(dir.resolve("stamina.json"),Balance.JSON.toJson(legacyStamina));
            var legacyFatigue=Balance.JSON.toJsonTree(new Balance().fatigue).getAsJsonObject();legacyFatigue.remove("minimum");legacyFatigue.addProperty("maximum",0.8);legacyFatigue.addProperty("sleepPerSecond",0.01);legacyFatigue.addProperty("campfirePerSecond",0.002);Files.writeString(dir.resolve("fatigue.json"),Balance.JSON.toJson(legacyFatigue));
            Balance migratedConfig=Balance.load(dir);near(migratedConfig.stamina.get("base"),20,"legacy config stamina migration");near(migratedConfig.fatigue.get("maximum"),16,"legacy fatigue units migration");near(migratedConfig.progression.get("maximumCap"),20,"legacy cap maximum");near(migratedConfig.stamina.get("armorCost"),0.5,"migrate default armor cost increase");check(Files.exists(dir.resolve("migration-v1/progression.json")),"configuration backup retained");near(Balance.load(dir).stamina.get("base"),20,"config migration idempotent");
        } finally {try(var paths=Files.walk(dir)){for(Path file:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
        System.out.println("Core model checks passed: "+checks);
    }
}
