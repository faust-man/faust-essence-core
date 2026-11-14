package com.faust.core.progression;
import com.faust.essence.api.Skill;
import com.faust.essence.api.BonusEffects;


import java.nio.file.*;
import java.util.*;

/** Dependency-light regression suite, executed by :common:modelTest and build. */
public final class ProgressTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void near(double actual,double expected,String message){check(Math.abs(actual-expected)<1e-7,message+": "+actual+" != "+expected);}
    private static void rejects(Runnable task,String message){try{task.run();}catch(IllegalArgumentException e){checks++;return;}throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        ExperienceSkillsTest.run();RequestedRequirementsTest.run();ForestSkillsTest.run();BalanceRevisionTest.run();CounterPlanTest.run();PanelUpdateTest.run();CommandTest.run();AbilityToggleTest.run();NonToggleableTest.run();PracticeTest.run();Revision14Test.run();Revision141Test.run();StaminaSpritesTest.run();TreeTest.run();ProgressionRevisionTest.run();FatigueGraceTest.run();FatigueCapacityTest.run();com.faust.core.runtime.CakeFoodTest.run();
        Balance b=new Balance();b.validate();
        Progress lockedStats=new Progress();lockedStats.track(CoreSkills.constitution).level=20;near(SkillEffects.health(lockedStats,b),0,"health locked before learned stat");near(lockedStats.baseStamina(b),20,"stamina capacity locked before learned stat");lockedStats.unlocked.add("faustcore:training_constitution_health");near(SkillEffects.health(lockedStats,b),20,"health node unlocks only health");near(lockedStats.baseStamina(b),20,"health node does not unlock stamina");lockedStats.unlocked.add("faustcore:training_constitution_stamina");near(lockedStats.baseStamina(b),40,"separate stamina node");
        check(!SkillEffects.damageCounts(1.99,b),"less than a heart ignored");check(SkillEffects.damageCounts(2,b),"one actual heart counts");
        Progress shieldUser=new Progress();shieldUser.stamina=5;shieldUser.unlocked.add("faustcore:second_wind");shieldUser.recover(2,false,false,true,true,b);near(shieldUser.stamina,5,"raised shield blocks all recovery including Second Wind");shieldUser.recover(1,false,false,true,false,b);check(shieldUser.stamina>5,"release shield restores recovery");
        shieldUser.fatigue=5;shieldUser.sweetFood("minecraft:cookie",b);near(shieldUser.fatigue,4,"cookie fatigue value");shieldUser.sweetFood("minecraft:honey_bottle",b);near(shieldUser.fatigue,2,"honey fatigue value");shieldUser.sweetFood("minecraft:stone",b);near(shieldUser.fatigue,2,"unlisted food gives no reduction");
        near(b.stamina.get("maceCost"),2,"mace cost two");near(b.stamina.get("shieldCost"),2,"shield base cost one");shieldUser.unlocked.add("faustcore:shield_efficiency");near(b.stamina.get("shieldCost")-shieldUser.effect("shield_cost_reduction",b),1,"shield ability halves base cost");near(b.stamina.get("sprintJumpCost")+14*b.stamina.get("jumpArmorCost"),1.2,"armored sprint jump cost");
        Progress generic=new Progress();generic.track(CoreSkills.dexterity).level=5;
        var rule=new Balance.StatBonus();rule.id="dex_health";rule.skill=CoreSkills.dexterity;rule.attribute="minecraft:generic.max_health";rule.perLevel=1;b.statBonuses.add(rule);b.validate();near(SkillEffects.health(generic,b),0,"stat locked before ability");generic.unlocked.add(rule.requiresAbility);near(SkillEffects.health(generic,b),5,"one-line custom Dexterity HP rule");b.statBonuses.remove(rule);
        for(var bonus:b.statBonuses)generic.unlocked.add(bonus.requiresAbility);
        near(SkillEffects.attribute(generic,b,"minecraft:generic.movement_speed",false),0,"Dexterity does not speed up walking");near(SkillEffects.attribute(generic,b,"minecraft:generic.movement_speed",true),0.05,"Dexterity sprint speed");near(SkillEffects.attribute(generic,b,"minecraft:generic.attack_speed",false),0.05,"Dexterity attack speed");
        check(b.abilities.stream().noneMatch(a->a.id().equals("faustcore:tool_handling")),"original building perk removed");

        Progress stats=new Progress();for(var bonus:b.statBonuses)stats.unlocked.add(bonus.requiresAbility);stats.track(CoreSkills.constitution).level=20;near(20+SkillEffects.health(stats,b),40,"constitution 20 gives 40 HP");
        stats.track(CoreSkills.intelligence).level=10;near(SkillEffects.luck(stats,b),0.1,"intelligence Luck scaling");stats.track(CoreSkills.intelligence).level=100;near(SkillEffects.luck(stats,b),0.2,"Luck capped at plus 0.2");
        stats.track(CoreSkills.building).level=9;near(SkillEffects.reach(stats,b),0,"reach before threshold");stats.track(CoreSkills.building).level=10;near(SkillEffects.reach(stats,b),1,"ten building gives one reach");stats.track(CoreSkills.building).level=20;near(SkillEffects.reach(stats,b),2,"twenty building gives two reach");near(SkillEffects.breaking(stats,b),0.2,"one percent breaking speed per level");
        for(int[] pair:new int[][]{{0,0},{4,0},{5,8},{9,8},{10,16},{19,16},{20,24}})check(SkillEffects.icon(pair[0],b)==pair[1],"icon tier boundary "+pair[0]);
        BonusEffects.register("test:bonus");rejects(()->BonusEffects.register("test:bonus"),"duplicate bonus rejected");
        com.faust.essence.api.EssenceApi.registerPath("test:path",player->true);
        rejects(()->com.faust.essence.api.EssenceApi.registerPath("test:path",player->true),"duplicate path rejected");
        com.faust.essence.api.EssenceApi.beforeProgress(grant->0.5);
        near(com.faust.essence.api.EssenceApi.before(new com.faust.essence.api.EssenceApi.Grant(null,"test",2,1)),0.5,"API before-progress multiplier");
        com.faust.essence.api.EssenceApi.beforeProgress(grant->0);
        near(com.faust.essence.api.EssenceApi.before(new com.faust.essence.api.EssenceApi.Grant(null,"test",2,1)),0,"API cancellation");
        java.util.concurrent.atomic.AtomicInteger notified=new java.util.concurrent.atomic.AtomicInteger();
        com.faust.essence.api.EssenceApi.afterProgress(change->notified.incrementAndGet());
        com.faust.essence.api.EssenceApi.after(new com.faust.essence.api.EssenceApi.Change(java.util.UUID.randomUUID(),"test",null,null));
        check(notified.get()==1,"API after-progress notification");
        Progress p=new Progress();near(p.multiplier(CoreSkills.strength,b),1.5,"zero level growth reflects distance to cap");
        p.track(CoreSkills.strength).level=2;near(p.multiplier(CoreSkills.strength,b),1,"last step before cap is 100 percent");
        p.track(CoreSkills.strength).level=3;p.track(CoreSkills.strength).limitOverride=9;near(p.multiplier(CoreSkills.strength,b),2,"cap triple still limited to 200 percent");
        p=new Progress();p.track(CoreSkills.strength).level=4;p.track(CoreSkills.strength).xp=175;p.grant(CoreSkills.strength,40,b);check(p.track(CoreSkills.strength).level==5,"cross soft cap");near(p.track(CoreSkills.strength).xp,(40-20)*0.13,"split award at accelerated boundary");
        Progress small=new Progress(),large=new Progress();
        for(int i=0;i<1000;i++)small.grant(CoreSkills.strength,0.5,b);large.grant(CoreSkills.strength,500,b);
        check(small.track(CoreSkills.strength).level==large.track(CoreSkills.strength).level,"grouping invariant below and above cap");near(small.track(CoreSkills.strength).xp,large.track(CoreSkills.strength).xp,"fractional precision");
        small.track(CoreSkills.strength).level=1000;small.track(CoreSkills.strength).xp=0;small.track(CoreSkills.strength).threshold=small.needed(CoreSkills.strength,b);small.grant(CoreSkills.strength,0.001,b);check(small.track(CoreSkills.strength).xp>0,"growth above cap remains positive");
        p=new Progress();long[] prices={1,2,3,4,5};
        for(int i=0;i<prices.length;i++){check(p.price(CoreSkills.strength,b)==prices[i],"shared purchase count plus one "+i);check(p.buy(CoreSkills.strength,10000,b),"purchase +1");}
        near(p.cap(CoreSkills.strength,b),8,"five purchases add five");near(p.cap(CoreSkills.building,b),3,"other cap unchanged");near(p.price(CoreSkills.building,b),6,"shared sum affects different branch");
        for(int i=0;i<30;i++)p.buy(CoreSkills.strength,10000,b);near(p.cap(CoreSkills.strength,b),20,"cap hard maximum 20");
        long before=p.purchases;check(!p.buy(CoreSkills.strength,10000,b),"cannot buy beyond 20");check(p.purchases==before,"capped purchase atomicity");check(!p.buy(CoreSkills.defense,0,b),"insufficient vanilla levels");
        Progress mixed=new Progress();near(mixed.price(CoreSkills.strength,b),1,"first selected cap price");mixed.buy(CoreSkills.strength,5,b);near(mixed.price(CoreSkills.building,b),2,"global increase across branches");mixed.buy(CoreSkills.building,6,b);near(mixed.price(CoreSkills.strength,b),3,"return to purchased branch");
        Skill custom=Skill.register("test:path_skill");rejects(()->Skill.register("test:path_skill"),"duplicate skill rejected");
        Progress extension=new Progress();extension.grant(custom,5,b);check(Progress.parse(extension.json()).track(custom).xp==7.5,"extension skill persistence");
        p=new Progress();var a=b.abilities.stream().filter(n->n.id().equals("faustcore:armor_familiarity")).findFirst().orElseThrow();p.track(a.skill()).level=a.level();check(!p.unlock(a.id(),b),"level alone insufficient");
        p.track(a.skill()).level=0;for(var c:a.actionConditions())p.counters.put(c.counter(),c.amount());check(!p.unlock(a.id(),b),"condition alone insufficient");p.track(a.skill()).level=a.level();check(p.unlock(a.id(),b),"both conditions");check(!p.unlock(a.id(),b),"no duplicate unlock");
        var child=b.abilities.stream().filter(n->n.id().equals("faustcore:armor_mastery")).findFirst().orElseThrow();Progress q=new Progress();q.track(child.skill()).level=20;q.counters.put(child.counter(),10000.0);check(!q.unlock(child.id(),b),"parent required");
        check(!p.event("unknown",1,1,b),"unknown source");double old=p.track(CoreSkills.constitution).xp;p.event("damage",4,4,b);near(p.track(CoreSkills.constitution).xp,old+8*p.multiplier(CoreSkills.constitution,b),"actual damage practice");
        p=new Progress();near(p.stamina,20,"base stamina 20");near(p.spend(8,b),8,"paid stamina");near(p.fatigue,0.2,"fatigue in absolute units");near(p.maxStamina(b),19.8,"fatigue occupies right of bar");
        p.rest(10,false,false,true,b);near(p.stamina,19.8,"regen limited by fatigue");p.rest(1,true,false,true,b);near(p.fatigue,0,"gradual sleep units");p.rest(1,false,true,true,b);near(p.fatigue,0,"campfire retains fractional time until one unit");
        p.stamina=1;near(p.spend(20,b),1,"cannot overspend");double f=p.fatigue;p.spend(20,b);near(p.fatigue,f,"no fatigue for unpaid request");
        b.fatigue.put("enabled",0.0);near(p.maxStamina(b),20,"fatigue disabled");b.stamina.put("enabled",0.0);near(p.spend(20,b),0,"stamina disabled");near(p.fatigue,f,"disable preserves history");
        b=new Balance();Progress fire=new Progress();fire.fatigue=5;fire.rest(24,false,true,false,b);near(fire.fatigue,5,"campfire unit interval not complete");fire.rest(1,false,true,false,b);near(fire.fatigue,4,"campfire removes one unit per 25 seconds");
        fire.environmentalFatigue(1,true,false,b);near(fire.fatigue,4.02,"soul campfire adds fatigue");fire.environmentalFatigue(1,false,true,b);near(fire.fatigue,4.07,"soul fire adds fatigue");
        Progress runner=new Progress();runner.unlocked.add("faustcore:second_wind");for(int n=0;n<2000;n++){runner.spend(b.stamina.get("runCost")/20,b);runner.secondWind(0.05,b);}check(!runner.exhausted,"second wind sustains unarmored sprint");
        Progress capped=new Progress();for(var bonus:b.statBonuses)capped.unlocked.add(bonus.requiresAbility);capped.track(CoreSkills.building).level=100;near(SkillEffects.reach(capped,b),3,"building reach capped at plus three");capped.track(CoreSkills.defense).level=6;near(SkillEffects.attribute(capped,b,"minecraft:generic.armor",false),3,"one armor per two defense levels");
        var extraReach=new Balance.StatBonus();extraReach.id="extra_building_reach";extraReach.skill=CoreSkills.building;extraReach.attribute="minecraft:player.block_interaction_range";extraReach.perLevel=1;b.statBonuses.add(extraReach);near(SkillEffects.reach(capped,b),3,"global reach cap includes multiple rules");b.statBonuses.remove(extraReach);
        Progress hitter=new Progress();near(hitter.spend(b.stamina.get("attackCost"),b),1,"one stamina per attack");near(hitter.stamina,19,"attack deduction");
        Balance lockBalance=new Balance();Progress locked=new Progress();locked.spend(100,lockBalance);check(locked.exhausted,"zero stamina enters exhaustion");
        locked.rest(1,false,false,true,lockBalance);check(locked.stamina>0&&locked.exhausted,"partial recovery below ten remains exhausted");
        Progress lockedCopy=Progress.parse(locked.json());check(lockedCopy.exhausted,"exhaustion persists after reload");
        locked.rest(20,false,false,true,lockBalance);check(!locked.exhausted,"full available recovery unlocks sprint");near(locked.stamina,locked.maxStamina(lockBalance),"unlock at available maximum with fatigue");
        near((14-2)*lockBalance.stamina.get("armorCost"),3,"armor surcharge halved");
        b=new Balance();p=new Progress();p.buy(CoreSkills.defense,1000,b);p.fatigue=4;p.stamina=10;p.discoveries.add("minecraft:test/1");Progress restored=Progress.parse(p.json());check(restored.json().equals(p.json()),"save/load round trip");
        Progress migrated=Progress.parse("{\"schema\":1,\"stamina\":50,\"fatigue\":0.2}");near(migrated.stamina,10,"migrate v1 stamina");near(migrated.fatigue,4,"migrate fraction to units");check(migrated.schema==3,"new schema");
        rejects(()->Progress.parse("{\"schema\":4}"),"future schema rejected");
        Progress offline=new Progress();offline.grant(CoreSkills.building,5,b);String saved=offline.json();Balance changed=new Balance();changed.progression.put("levelBase",200.0);offline=Progress.parse(saved);offline.reconcile(changed);near(offline.track(CoreSkills.building).xp,15,"offline threshold migration retains fraction");
        Balance invalid=new Balance();var one=invalid.abilities.getFirst();invalid.abilities.set(0,new Balance.Ability(one.id(),one.skill(),one.level(),one.counter(),one.amount(),one.id(),one.effect(),one.value()));rejects(invalid::validate,"cycle rejected");
        invalid=new Balance();invalid.fatigue.put("maximum",21.0);rejects(invalid::validate,"zero maximum prohibited");
        Path dir=Files.createTempDirectory("core-config-test");try {
            Balance loaded=Balance.load(dir);check(loaded.abilities.size()>=10,"default config generated");
            String original=Files.readString(Balance.sectionFile(dir,"stamina"));boolean invalidEdit=false;
            try{Balance.saveGroup(dir,"stamina","{}");}catch(IllegalArgumentException e){invalidEdit=true;}check(invalidEdit,"config editor validates entire candidate");check(Files.readString(Balance.sectionFile(dir,"stamina")).equals(original),"invalid edit preserves disk config");
            var edited=Balance.JSON.toJsonTree(loaded.stamina).getAsJsonObject();edited.addProperty("regeneration",6);Balance.saveGroup(dir,"stamina",edited.toString());near(Balance.load(dir).stamina.get("regeneration"),6,"config editor saves valid group");
            rejects(()->loaded.edited("../server.properties","{}"),"editor rejects path traversal/unknown sections");
            Files.writeString(dir.resolve("progression.json"),"{}");boolean failed=false;try{Balance.load(dir);}catch(java.io.IOException e){failed=true;}check(failed,"invalid reload rejected");
            var legacy=Balance.JSON.toJsonTree(new Balance().progression).getAsJsonObject();legacy.remove("maximumCap");legacy.remove("costDivisor");legacy.addProperty("purchaseBase",100);legacy.addProperty("purchaseGrowth",1.5);legacy.addProperty("capStep",5);
            Files.writeString(dir.resolve("progression.json"),Balance.JSON.toJson(legacy));
            var legacyStamina=Balance.JSON.toJsonTree(new Balance().stamina).getAsJsonObject();legacyStamina.remove("exhaustionLock");legacyStamina.addProperty("armorCost",0.5);legacyStamina.addProperty("base",100);legacyStamina.addProperty("constitutionPerLevel",1);legacyStamina.addProperty("regeneration",20);Files.writeString(Balance.sectionFile(dir,"stamina"),Balance.JSON.toJson(legacyStamina));
            var legacyFatigue=Balance.JSON.toJsonTree(new Balance().fatigue).getAsJsonObject();legacyFatigue.remove("minimum");legacyFatigue.addProperty("maximum",0.8);legacyFatigue.addProperty("sleepPerSecond",0.01);legacyFatigue.addProperty("campfirePerSecond",0.002);Files.writeString(Balance.sectionFile(dir,"fatigue"),Balance.JSON.toJson(legacyFatigue));
            Balance migratedConfig=Balance.load(dir);near(migratedConfig.stamina.get("base"),20,"legacy config stamina migration");near(migratedConfig.fatigue.get("maximum"),16,"legacy fatigue units migration");near(migratedConfig.progression.get("maximumCap"),20,"legacy cap maximum");near(migratedConfig.stamina.get("armorCost"),0.25,"legacy armor cost migrates to current default");check(Files.exists(dir.resolve("migration-v1/progression.json")),"configuration backup retained");near(Balance.load(dir).stamina.get("base"),20,"config migration idempotent");
        } finally {try(var paths=Files.walk(dir)){for(Path file:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
        ExtensionAdapterTest.run();
        System.out.println("Core model checks passed: "+checks);
    }
}
