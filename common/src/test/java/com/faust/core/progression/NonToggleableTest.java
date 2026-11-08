package com.faust.core.progression;
import java.nio.file.*;
public final class NonToggleableTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static Balance configured(Balance b,String id,Boolean toggleable){
        var json=Balance.JSON.toJsonTree(b.abilities).getAsJsonArray();
        for(var entry:json)if(entry.getAsJsonObject().get("id").getAsString().equals(id)){
            if(toggleable==null)entry.getAsJsonObject().remove("toggleable");else entry.getAsJsonObject().addProperty("toggleable",toggleable);
        }
        return b.edited("abilities",Balance.JSON.toJson(json));
    }
    public static void run() throws Exception {
        var defaults=new Balance();defaults.validate();
        for(var a:defaults.abilities){
            check(a.toggleable(),"default abilities remain toggleable");
            var b=configured(defaults,a.id(),false);var node=b.abilities.stream().filter(n->n.id().equals(a.id())).findFirst().orElseThrow();check(!node.toggleable(),"false read from JSON");
            var p=new Progress();check(!p.toggle(a.id(),b)&&!p.enabled(a.id(),b),"non-toggleable does not auto unlock");
            p.max(b);long revision=p.revision;check(!p.toggle(a.id(),b)&&p.revision==revision,"server denies fixed skill without mutation");
            p.disabled.add(a.id());check(p.enabled(a.id(),b),"fixed skill ignores stale disabled flag");
            check(SkillTrees.nodeColor(p,node,b)==0xFF83C24A,"fixed learned skill stays green");
            double effect=p.effect(a.effect(),defaults);check(Math.abs(p.effect(a.effect(),b)-(effect+a.value()))<1e-8,"effect active before reconciliation");
            var restored=Progress.parse(p.json(),b);check(restored.enabled(a.id())&&!restored.disabled.contains(a.id()),"load clears disabled flag");
            p.reconcile(b);check(p.enabled(a.id())&&!p.disabled.contains(a.id()),"reload/tick reconciliation enables skill");
            var switchable=configured(b,a.id(),true);check(p.toggle(a.id(),switchable)&&!p.enabled(a.id()),"true allows disabling again");
            var legacy=configured(switchable,a.id(),null);check(legacy.abilities.stream().filter(n->n.id().equals(a.id())).findFirst().orElseThrow().toggleable(),"missing property defaults true");
        }
        var b=configured(defaults,"faustcore:training_constitution_stamina",false);var p=new Progress();p.max(b);p.disabled.add("faustcore:training_constitution_stamina");check(p.baseStamina(b)==40,"fixed stat gate works before reconciliation");
        p.disabled.add("faustcore:second_wind");p.reconcile(b);check(p.disabled.contains("faustcore:second_wind"),"other toggleable flags preserved");
        var old=defaults.abilities.getFirst();var compatible=new Balance.Ability(old.id(),old.skill(),old.level(),old.counter(),old.amount(),old.parent(),old.effect(),old.value(),old.requiresAll(),old.requiresAny(),old.excludes(),old.characteristics(),old.conditions(),old.conditionMode(),old.conditionsRequired(),old.requiresCount());check(compatible.toggleable(),"old full API constructor remains compatible");
        Path dir=Files.createTempDirectory("core-fixed-ability");
        try {
            Balance.load(dir);Balance.saveGroup(dir,"abilities",Balance.JSON.toJson(b.abilities));var loaded=Balance.load(dir);check(!loaded.abilities.stream().filter(a->a.id().equals("faustcore:training_constitution_stamina")).findFirst().orElseThrow().toggleable(),"false survives save/reload");
            var snapshot=Balance.JSON.fromJson(Balance.JSON.toJson(loaded),Balance.class);check(!snapshot.abilities.stream().filter(a->a.id().equals("faustcore:training_constitution_stamina")).findFirst().orElseThrow().toggleable(),"client snapshot keeps false");
        }finally{try(var files=Files.walk(dir)){for(var file:files.sorted(java.util.Comparator.reverseOrder()).toList())Files.delete(file);}}
        System.out.println("Non-toggleable ability checks passed: "+checks);
    }
}
