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
        Balance b=new Balance();b.validate();
        Skill custom=Skill.register("test:path_skill");rejects(()->Skill.register("test:path_skill"),"duplicate skill rejected");
        Progress extension=new Progress();extension.grant(custom,50,b);check(Progress.parse(extension.json()).track(custom).xp==50,"extension skill persistence");
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
        Progress p=new Progress();p.grant(Skill.strength,360,b);check(p.track(Skill.strength).level==3,"three consecutive levels");near(p.track(Skill.strength).xp,0,"level remainder");
        p=new Progress();p.track(Skill.strength).level=9;p.track(Skill.strength).xp=275;p.grant(Skill.strength,20,b);check(p.track(Skill.strength).level==10,"cross soft cap");near(p.track(Skill.strength).xp,12,"split award at cap");
        Progress small=new Progress(), large=new Progress();small.track(Skill.strength).level=10;large.track(Skill.strength).level=10;
        for(int i=0;i<1000;i++)small.grant(Skill.strength,0.5,b);large.grant(Skill.strength,500,b);
        check(small.track(Skill.strength).level==large.track(Skill.strength).level,"award grouping invariant");near(small.track(Skill.strength).xp,large.track(Skill.strength).xp,"fractional precision");
        small.track(Skill.strength).level=1000;double x=small.track(Skill.strength).xp;small.grant(Skill.strength,0.001,b);check(small.track(Skill.strength).xp>x,"positive progress far beyond cap");
        p=new Progress();long[] prices={100,150,225,338,507};
        for(int i=0;i<prices.length;i++){check(p.price(b)==prices[i],"global price "+i);check(p.buy(i%2==0?Skill.strength:Skill.building,10000,b),"purchase");}
        near(p.cap(Skill.strength,b),25,"own cap purchases");near(p.cap(Skill.building,b),20,"other cap purchases");
        long before=p.purchases;check(!p.buy(Skill.defense,0,b),"insufficient funds");check(p.purchases==before,"failed purchase atomicity");
        p=new Progress();var a=b.abilities.getFirst();p.track(a.skill()).level=a.level();check(!p.unlock(a.id(),b),"level alone insufficient");
        p.track(a.skill()).level=0;p.counters.put(a.counter(),a.amount());check(!p.unlock(a.id(),b),"condition alone insufficient");p.track(a.skill()).level=a.level();check(p.unlock(a.id(),b),"both conditions");check(!p.unlock(a.id(),b),"no duplicate unlock");
        var child=b.abilities.get(1);Progress q=new Progress();q.track(child.skill()).level=20;q.counters.put(child.counter(),10000.0);check(!q.unlock(child.id(),b),"parent required");
        check(!p.event("unknown",1,1,b),"unknown source");double old=p.track(Skill.constitution).xp;p.event("damage",4,4,b);near(p.track(Skill.constitution).xp,old+4,"actual damage practice");
        p=new Progress();near(p.spend(40,b),40,"paid stamina");near(p.fatigue,0.02,"fatigue only on paid amount");near(p.maxStamina(b),98,"reduced maximum");
        p.rest(10,false,false,true,b);near(p.stamina,98,"regeneration limited by fatigue");p.rest(1,true,false,true,b);near(p.fatigue,0.01,"gradual sleep");p.rest(1,false,true,true,b);near(p.fatigue,0.008,"gradual campfire");
        p.stamina=1;near(p.spend(20,b),1,"cannot overspend");double f=p.fatigue;p.spend(20,b);near(p.fatigue,f,"no fatigue for unpaid request");
        b.fatigue.put("enabled",0.0);near(p.maxStamina(b),100,"fatigue disabled");b.stamina.put("enabled",0.0);near(p.spend(20,b),0,"stamina disabled");near(p.fatigue,f,"disable preserves history");
        b=new Balance();p=new Progress();p.buy(Skill.defense,1000,b);p.fatigue=0.2;p.stamina=30;p.discoveries.add("minecraft:test/1");Progress restored=Progress.parse(p.json());check(restored.json().equals(p.json()),"save/load round trip incl fatigue, purchases, discoveries");
        rejects(()->Progress.parse("{\"schema\":2}"),"future schema rejected rather than erased");
        Progress offline=new Progress();offline.grant(Skill.building,50,b);String saved=offline.json();Balance changed=new Balance();changed.progression.put("levelBase",200.0);offline=Progress.parse(saved);offline.reconcile(changed);near(offline.track(Skill.building).xp,100,"offline threshold migration retains fraction");
        Balance invalid=new Balance();var one=invalid.abilities.getFirst();invalid.abilities.set(0,new Balance.Ability(one.id(),one.skill(),one.level(),one.counter(),one.amount(),one.id(),one.effect(),one.value()));rejects(invalid::validate,"cycle rejected");
        invalid=new Balance();invalid.fatigue.put("maximum",1.0);rejects(invalid::validate,"zero maximum prohibited");
        Path dir=Files.createTempDirectory("core-config-test");try {
            Balance loaded=Balance.load(dir);check(loaded.abilities.size()==14,"default config generated");
            Files.writeString(dir.resolve("progression.json"),"{}");boolean failed=false;try{Balance.load(dir);}catch(java.io.IOException e){failed=true;}check(failed,"invalid reload rejected");
        } finally {try(var paths=Files.walk(dir)){for(Path file:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
        System.out.println("Core model checks passed: "+checks);
    }
}
