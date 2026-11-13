package com.faust.core.progression;

import java.util.*;
import java.nio.file.*;

public final class CounterPlanTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static Balance.Ability node(String id,String parent,double count){return new Balance.Ability(id,Skill.strength,0,"test_counter",count,parent,"stamina",1);}
    public static void run() throws Exception {
        Balance b=new Balance();var root=node("test:root","",3);var child=node("test:child",root.id(),2);var leaf=node("test:leaf",child.id(),1);
        b.abilities.addAll(List.of(root,child,leaf));Progress p=new Progress();
        var learnedReference=p.unlocked;check(p.recordCounter("test_counter",50,b),"root counted");check(p.unlocked==learnedReference,"public learned-set references remain stable");
        check(p.requirementCount(root)==3&&p.requirementCount(child)==0,"saturated root and blocked child");
        long revision=p.revision;for(int i=0;i<1000;i++)check(!p.recordCounter("test_counter",1,b),"completed parent cannot accumulate for locked child");check(p.revision==revision,"no revision churn after saturation");
        check(p.unlock(root.id(),b),"learn completed root");check(p.requirementCount(child)==0,"child never inherits root actions");
        check(p.recordCounter("test_counter",1,b)&&p.requirementCount(child)==1,"child starts after parent");
        p.recordCounter("test_counter",1,b);check(p.requirementCount(child)==2&&!p.wantsCounter("test_counter",b),"child saturated before purchase");
        check(p.unlock(child.id(),b)&&p.requirementCount(leaf)==0,"grandchild starts empty");p.recordCounter("test_counter",1,b);check(p.requirementCount(leaf)==1,"grandchild has own progress");
        p=Progress.parse(p.json(),b);check(!p.wantsCounter("test_counter",b),"completion persists through reload");
        p.revoke(root.id(),b);check(p.requirementCount(child)==0&&!p.recordCounter("test_counter",1,b),"revoked prerequisite suspends counters");p.unlock(root.id(),b);check(p.requirementCount(child)==2,"already earned child achievement retained");
        Progress q=new Progress();q.wantsCounter("test_counter",b);q.unlocked.add(root.id());check(q.recordCounter("test_counter",1,b)&&q.requirementCount(child)==1,"direct public set mutation invalidates cache");q.unlocked.remove(root.id());q.unlocked.add("test:same_size");q.recordCounter("test_counter",1,b);check(q.requirementCount(child)==0&&q.practice.get(child.id()).get("test_counter")==1,"same-size learned-set replacement cannot leave stale eligibility");
        q=new Progress();q.unlocked.add(root.id());q.recordCounter("test_counter",2,b);var replacement=node(child.id(),root.id(),4);b.abilities.set(b.abilities.indexOf(child),replacement);b.validate();check(q.wantsCounter("test_counter",b),"higher config threshold resumes tracking");q.recordCounter("test_counter",2,b);check(q.requirementCount(replacement)==4&&!q.wantsCounter("test_counter",b),"new threshold saturates");
        var either=new Balance.Ability("test:either",Skill.strength,0,"a",1,"","stamina",1,List.of(),List.of(),List.of(),List.of(),List.of(new Balance.Condition("a",1),new Balance.Condition("b",1),new Balance.Condition("c",1)),"at_least",2,0);
        b.abilities.add(either);q=new Progress();q.recordCounter("a",1,b);q.recordCounter("b",1,b);check(!q.wantsCounter("c",b)&&!q.recordCounter("c",1,b),"two-of-three stops all unused conditions");
        var maximum=new Balance.Ability("test:maximum",Skill.strength,0,"maximum",10,"","stamina",1);b.abilities.add(maximum);q.recordMaximum("maximum",5,b);q.recordMaximum("maximum",4,b);check(q.requirementCount(maximum)==5,"maximum cannot add smaller events");q.recordMaximum("maximum",500,b);check(q.requirementCount(maximum)==10&&!q.recordMaximum("maximum",501,b),"maximum stops at threshold");
        var xp=new Progress();var rule=b.actions.get("run");double before=xp.track(rule.skill()).xp;xp.max(b);xp.event("run",1,1,b);check(xp.track(rule.skill()).xp>before,"characteristic XP continues when all abilities learned");
        Progress legacy=new Progress();legacy.counters.put("test_counter",500.0);var json=Balance.JSON.toJsonTree(legacy).getAsJsonObject();json.remove("practice");legacy=Progress.parse(json.toString(),b);check(legacy.requirementCount(root)==3&&legacy.requirementCount(replacement)==0,"legacy seeds eligible nodes only");
        Path dir=Files.createTempDirectory("counter-plan");try{
            Balance initial=Balance.load(dir);String health="faustcore:training_constitution_health",stamina="faustcore:training_constitution_stamina";
            check(initial.abilities.stream().filter(a->a.id().equals(health)).findFirst().orElseThrow().all().contains(stamina),"health now follows stamina");check(initial.abilities.stream().filter(a->a.id().equals(stamina)).findFirst().orElseThrow().all().isEmpty(),"stamina now root");
            var hp=initial.trees.stream().filter(t->t.skill()==Skill.constitution).findFirst().orElseThrow().nodes().stream().filter(n->n.ability().equals(health)).findFirst().orElseThrow();var sp=initial.trees.stream().filter(t->t.skill()==Skill.constitution).findFirst().orElseThrow().nodes().stream().filter(n->n.ability().equals(stamina)).findFirst().orElseThrow();check(hp.y()==SkillTrees.snap(4)&&sp.y()==0,"constitution default positions swapped");
            var oldAbilities=Balance.JSON.toJsonTree(initial.abilities).getAsJsonArray();for(var e:oldAbilities){var a=e.getAsJsonObject();if(a.get("id").getAsString().equals(health))a.addProperty("parent","");if(a.get("id").getAsString().equals(stamina))a.addProperty("parent",health);}Files.writeString(dir.resolve("abilities.json"),Balance.JSON.toJson(oldAbilities));
            var oldTrees=Balance.JSON.toJsonTree(initial.trees).getAsJsonArray();for(var tree:oldTrees)for(var e:tree.getAsJsonObject().getAsJsonArray("nodes")){var n=e.getAsJsonObject();if(n.get("ability").getAsString().equals(health))n.addProperty("y",0);if(n.get("ability").getAsString().equals(stamina))n.addProperty("y",4);}Files.writeString(dir.resolve("trees.json"),Balance.JSON.toJson(oldTrees));Files.delete(dir.resolve(".constitution-swap-format"));
            var loaded=Balance.load(dir);check(Files.exists(dir.resolve("migration-constitution-swap/abilities.json")),"existing configuration backed up");check(loaded.trees.stream().filter(t->t.skill()==Skill.constitution).findFirst().orElseThrow().nodes().stream().filter(n->n.ability().equals(stamina)).findFirst().orElseThrow().y()==0,"existing stamina position migrated to root");check(Balance.JSON.toJson(loaded.trees).equals(Balance.JSON.toJson(Balance.load(dir).trees)),"layout swap runs once");check(loaded.abilities.stream().filter(a->a.id().equals(health)).findFirst().orElseThrow().parent().equals(stamina)&&loaded.abilities.stream().filter(a->a.id().equals(stamina)).findFirst().orElseThrow().parent().isEmpty(),"constitution dependencies remain swapped on reload");
        }finally{try(var paths=Files.walk(dir)){for(Path file:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
        var price=new Progress();check(price.price(Skill.strength,b)==1,"free baseline excluded");price.buy(Skill.strength,5,b);check(price.price(Skill.building,b)==2,"purchased growth affects other branches");price.track(Skill.defense).limitOverride=0;check(price.price(Skill.building,b)==2,"low overrides never subtract paid growth");
        System.out.println("Demand-driven practice checks passed: "+checks);
    }
}
