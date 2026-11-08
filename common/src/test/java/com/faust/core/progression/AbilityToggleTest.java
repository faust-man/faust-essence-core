package com.faust.core.progression;
public final class AbilityToggleTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void near(double actual,double expected,String message){check(Math.abs(actual-expected)<1e-8,message);}
    public static void run(){
        var b=new Balance();var p=new Progress();long revision=p.revision;
        check(!p.toggle("faustcore:second_wind",b),"unlearned skill cannot toggle");check(p.revision==revision,"rejected request has no mutation");
        p.unlocked.add("test:unknown");check(!p.toggle("test:unknown",b),"unknown configuration ID rejected");
        for(var skill:Skill.values()){p.track(skill).level=3;p.track(skill).limitOverride=4;}
        p.track(Skill.strength).level=27;p.track(Skill.strength).xp=15;p.track(Skill.strength).threshold=p.needed(Skill.strength,b);
        p.max(b);check(p.unlocked.containsAll(b.abilities.stream().map(Balance.Ability::id).toList()),"max opens every ability");
        for(var skill:Skill.values()){check(p.cap(skill,b)==20,"max cap "+skill);check(p.track(skill).level==(skill==Skill.strength?27:20),"max level never lowers "+skill);}
        near(p.track(Skill.strength).xp,15,"max preserves XP at unchanged level");
        for(var a:b.abilities){
            double before=p.effect(a.effect(),b);check(p.enabled(a.id()),"learned active initially");
            check(p.toggle(a.id(),b),"disable learned");check(!p.enabled(a.id())&&p.unlocked.contains(a.id()),"disabled remains learned");
            near(p.effect(a.effect(),b),before-a.value(),"disabled contribution removed");
            check(SkillTrees.nodeColor(p,a,b)==0xFFFF4040,"disabled frame red");
            check(p.toggle(a.id(),b),"reenable learned");near(p.effect(a.effect(),b),before,"effect restored");check(SkillTrees.nodeColor(p,a,b)==0xFF83C24A,"active frame green");
        }
        p.toggle("faustcore:training_constitution_stamina",b);near(p.baseStamina(b),20,"stat gate disabled");
        p.fatigue=30;p.stamina=40;p.reconcile(b);check(p.fatigue<=p.maxFatigue(b)&&p.stamina<=p.maxStamina(b),"reduced capacity clamps reserves");
        p.toggle("faustcore:training_constitution_stamina",b);near(p.baseStamina(b),40,"stat gate restored");
        p.toggle("faustcore:soft_landing",b);near(SkillEffects.attribute(p,b,"minecraft:generic.safe_fall_distance",false),0,"fall bonus disabled");
        p.toggle("faustcore:potion_duration",b);near(PracticeBonuses.duration(p,b),1,"potion bonus disabled");
        p.toggle("faustcore:enchanting_power",b);check(PracticeBonuses.shelves(p,b)==0,"table power disabled");
        var child=b.abilities.stream().filter(a->!a.all().isEmpty()).findFirst().orElseThrow();String parent=child.all().getFirst();
        if(p.enabled(parent))p.toggle(parent,b);check(p.prerequisites(child),"disabled parent still satisfies learned requirement");
        var saved=Progress.parse(p.json(),b);check(saved.disabled.equals(p.disabled)&&saved.unlocked.equals(p.unlocked),"disabled status survives save and sync");
        var old=Balance.JSON.toJsonTree(p).getAsJsonObject();old.remove("disabled");var migrated=Progress.parse(old.toString(),b);check(migrated.disabled.isEmpty()&&migrated.enabled(parent),"old saves default to enabled");
        p.revoke(parent,b);check(!p.disabled.contains(parent),"revoke clears disabled state");
        p.max(b);check(p.disabled.isEmpty()&&p.enabled("faustcore:soft_landing"),"max enables all skills");
        var reset=new Progress();check(reset.disabled.isEmpty()&&reset.unlocked.isEmpty(),"reset clears learned and disabled state");
        old.add("disabled",com.google.gson.JsonNull.INSTANCE);try{Progress.parse(old.toString(),b);throw new AssertionError("null set accepted");}catch(IllegalArgumentException expected){checks++;}
        System.out.println("Ability toggle/max checks passed: "+checks);
    }
}
