package com.faust.core.progression;

/** Dynamic fatigue capacity must follow learned stamina bonuses across all gain paths. */
public final class FatigueCapacityTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void near(double actual,double expected,String message){check(Math.abs(actual-expected)<1e-8,message+": "+actual+" != "+expected);}
    public static void run(){
        var b=new Balance();b.validate();var p=new Progress();near(p.maxFatigue(b),16,"base fatigue limit remains sixteen");
        p.track(Skill.constitution).level=10;near(p.maxFatigue(b),16,"unlearned stamina bonus does not increase fatigue limit");p.unlocked.add("faustcore:training_constitution_stamina");near(p.baseStamina(b),30,"learned stamina capacity thirty");near(p.maxFatigue(b),26,"ten bonus stamina adds ten fatigue capacity");
        p.fatigue=12;p.track(Skill.constitution).level=20;p.reconcile(b);near(p.maxFatigue(b),36,"twenty bonus stamina adds twenty fatigue capacity");near(p.fatigue,12,"more capacity does not create fatigue");
        p.fatigue=16;p.stamina=20;p.spend(1,b);near(p.fatigue,16.025,"spending can increase fatigue above sixteen");
        p.fatigue=35.99;p.stamina=4.01;p.spend(1,b);near(p.fatigue,36,"spending clamps at dynamic fatigue capacity");near(p.maxStamina(b),4,"full fatigue retains minimum stamina");
        p.fatigue=35.98;p.environmentalFatigue(1,false,true,b);near(p.fatigue,36,"soul fire clamps at dynamic capacity");p.fatigue=16;p.environmentalFatigue(1,true,false,b);near(p.fatigue,16.02,"soul campfire can increase fatigue above sixteen");
        p.fatigue=30;near(p.maxStamina(b),10,"stamina uses actual fatigue above old limit");var restored=Progress.parse(p.json());restored.reconcile(b);near(restored.fatigue,30,"saved fatigue above sixteen survives load");near(restored.maxFatigue(b),36,"dynamic capacity after load");
        p.unlocked.remove("faustcore:training_constitution_stamina");p.reconcile(b);near(p.maxFatigue(b),16,"revoking stamina lowers fatigue capacity");near(p.fatigue,16,"revoke clamps excess fatigue");near(p.maxStamina(b),4,"revoke preserves minimum stamina");
        p=new Progress();p.track(Skill.constitution).level=10;p.unlocked.add("faustcore:training_constitution_stamina");b.abilities.add(new Balance.Ability("test:flat_stamina",Skill.constitution,1,"damage_taken",1,"","stamina",5));p.unlocked.add("test:flat_stamina");near(p.maxFatigue(b),31,"flat ability stamina also raises fatigue capacity");
        b.fatigue.put("maximum",12.0);near(p.maxFatigue(b),27,"custom base fatigue limit retained plus bonuses");
        b=new Balance();p=new Progress();var extra=new Balance.StatBonus();extra.id="dex_stamina";extra.skill=Skill.dexterity;extra.attribute="faustcore:stamina";extra.perLevel=0.5;b.statBonuses.add(extra);b.validate();p.track(Skill.dexterity).level=10;p.unlocked.add(extra.requiresAbility);near(p.maxFatigue(b),21,"generic stamina rule from another skill raises fatigue cap");
        p.fatigue=-5;p.reconcile(b);near(p.fatigue,-5,"negative reserve unchanged");near(p.maxStamina(b),25,"negative reserve cannot inflate capacity");
        p.fatigue=20;p.stamina=4;p.spend(1,b,0);near(p.fatigue,20,"world-start grace remains effective with higher cap");
        p.fatigue=p.maxFatigue(b);p.stamina=0;p.updateExhaustion(b);p.recover(10,false,false,true,false,b);check(!p.exhausted,"dynamic fatigue capacity cannot softlock exhaustion");
        b.fatigue.put("enabled",0.0);near(p.maxStamina(b),25,"disabled fatigue ignores stored higher fatigue");
        System.out.println("Core fatigue capacity checks passed: "+checks);
    }
}
