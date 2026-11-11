package com.faust.core.progression;
import java.nio.file.*;
public final class BalanceRevisionTest {
    private static void check(boolean v){if(!v)throw new AssertionError("balance revision");}
    public static void run() throws Exception {
        Balance b=new Balance();b.validate();
        check(b.actions.get("craft").xp()==0.5);check(Math.abs(b.actions.get("melee").xp()-2.0/3)<1e-9);
        check(ShieldCosts.block(false,2,0)==2);check(ShieldCosts.block(true,2,0)==1);
        check(ShieldCosts.block(false,2,2)==0);check(ShieldCosts.block(true,2,2)==0);
        check(ShieldCosts.block(true,2,1.5)==0);check(ShieldCosts.block(true,2,0.5)==0.5);
        Progress p=new Progress();p.stamina=7;p.spend(ShieldCosts.block(true,2,1.5),b);check(p.stamina==7);
        var care=b.abilities.stream().filter(a->a.id().equals("faustcore:shield_care")).findFirst().orElseThrow();
        var economy=b.abilities.stream().filter(a->a.id().equals("faustcore:shield_efficiency")).findFirst().orElseThrow();
        check(care.parent().equals(economy.id()));check(care.value()==0.25);check(economy.value()==2);
        Path dir=Files.createTempDirectory("balance-revision");Balance.load(dir);
        check(Files.exists(Balance.sectionFile(dir,"stamina")));check(!Files.exists(dir.resolve("stamina.json")));
        Balance.saveGroup(dir,"stamina",b.group("stamina"));check(Balance.load(dir).stamina.get("shieldCost")==2);
        System.out.println("Balance revision checks passed");
    }
}
