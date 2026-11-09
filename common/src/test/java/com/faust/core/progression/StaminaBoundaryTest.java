package com.faust.core.progression;

import java.nio.file.*;

/** Runs in two JVMs so provider discovery is genuinely exercised in both installations. */
public final class StaminaBoundaryTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        boolean expected=Boolean.parseBoolean(args[0]);
        check(StaminaServices.present()==expected,"provider discovery");
        Balance b=new Balance();b.validate();
        check(b.abilities.stream().anyMatch(a->a.id().equals("faustcore:training_constitution_stamina"))==expected,"optional stamina node");
        check(b.abilities.stream().anyMatch(a->a.id().equals("faustcore:armor_familiarity"))==expected,"optional armor weight node");
        check(b.abilities.stream().anyMatch(a->a.id().equals("faustcore:training_constitution_health")),"health remains available");
        Progress p=new Progress();p.stamina=7;p.fatigue=3;p.exhausted=true;
        if(!expected){
            check(p.spend(5,b)==0&&p.stamina==7,"no stamina payment");
            p.recover(10,true,true,true,false,b);check(p.stamina==7&&p.fatigue==3,"inactive data retained");
            check(!p.actionsBlocked(b),"inactive exhaustion never blocks");
            p.reconcile(b);check(p.fatigue==3&&p.stamina==7,"reconcile retains inactive resource data");
            check(b.abilities.stream().filter(a->a.id().equals("faustcore:training_constitution_health")).allMatch(a->a.parent().isEmpty()),"health detached from optional prerequisite");
        }
        Path dir=args.length>1?Path.of(args[1]):Files.createTempDirectory("stamina-boundary");
        Balance loaded=Balance.load(dir);loaded.validate();
        if(expected){
            check(loaded.abilities.stream().anyMatch(a->a.id().equals("faustcore:training_constitution_stamina")),"install restores optional nodes after standalone core");
            check(loaded.stamina.get("enabled")==1,"new standalone config retains enabled default for future install");
            check(loaded.abilities.stream().filter(a->a.id().equals("faustcore:training_constitution_health")).allMatch(a->a.parent().equals("faustcore:training_constitution_stamina")),"installed module restores original health chain");
        }
        String before=Files.readString(dir.resolve("stamina.json"));
        Balance.load(dir);check(before.equals(Files.readString(dir.resolve("stamina.json"))),"reload does not rewrite persisted resource settings");
        System.out.println("Stamina boundary passed: provider="+expected);
    }
}
