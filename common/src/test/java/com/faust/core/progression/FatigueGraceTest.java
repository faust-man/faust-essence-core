package com.faust.core.progression;

import java.nio.file.*;
import java.util.*;

/** Boundary and reload checks for the shared world-start fatigue grace period. */
public final class FatigueGraceTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void near(double actual,double expected,String message){check(Math.abs(actual-expected)<1e-8,message+": "+actual+" != "+expected);}
    public static void run() throws Exception {
        var b=new Balance();b.validate();near(b.fatigue.get("worldGraceSeconds"),300,"default grace five minutes");
        check(!FatigueRules.accumulates(0,b),"no accumulation at world start");check(!FatigueRules.accumulates(5999,b),"last protected tick");check(FatigueRules.accumulates(6000,b),"accumulation starts at five minutes");check(FatigueRules.accumulates(Long.MAX_VALUE,b),"old world has no new grace");
        var p=new Progress();p.spend(8,b,5999);near(p.stamina,12,"stamina cost still applies during grace");near(p.fatigue,0,"spending causes no fatigue during grace");p.spend(8,b,6000);near(p.fatigue,0.2,"spending accumulates after grace");
        p=new Progress();p.environmentalFatigue(10,true,true,b,5999);near(p.fatigue,0,"soul campfire and fire protected");p.environmentalFatigue(10,true,true,b,6000);near(p.fatigue,0.7,"soul sources resume after grace");
        p.fatigue=2;p.spend(1,b,0);near(p.fatigue,2,"existing fatigue not reset by grace");p.sleptNight(b);p.stamina=20;p.spend(1,b,0);near(p.fatigue,-5,"sleep reserve not consumed during grace");
        var restored=Progress.parse(p.json());restored.spend(1,b,6000);near(restored.fatigue,-4.975,"reload uses current world age, not fresh player grace");
        b.fatigue.put("worldGraceSeconds",0.0);check(FatigueRules.accumulates(0,b),"zero disables grace");p=new Progress();p.spend(1,b,0);near(p.fatigue,0.025,"disabled grace spends fatigue immediately");
        b.fatigue.put("worldGraceSeconds",10.0);check(!FatigueRules.accumulates(199,b)&&FatigueRules.accumulates(200,b),"custom duration boundary");
        Path dir=Files.createTempDirectory("core-fatigue-grace");try{
            var original=Balance.load(dir);var data=Balance.JSON.toJsonTree(original.fatigue).getAsJsonObject();data.remove("worldGraceSeconds");data.addProperty("perSpentFraction",0.123);Files.writeString(Balance.sectionFile(dir,"fatigue"),Balance.JSON.toJson(data));
            var migrated=Balance.load(dir);near(migrated.fatigue.get("worldGraceSeconds"),300,"existing configs gain duration");near(migrated.fatigue.get("perSpentFraction"),0.123,"custom rate preserved");check(Files.exists(dir.resolve("migration-v1.5.1/fatigue.json")),"old fatigue config backed up");
            var edited=Balance.JSON.toJsonTree(migrated.fatigue).getAsJsonObject();edited.addProperty("worldGraceSeconds",0);Balance.saveGroup(dir,"fatigue",edited.toString());near(Balance.load(dir).fatigue.get("worldGraceSeconds"),0,"explicit disabling survives reload");
        }finally{try(var paths=Files.walk(dir)){for(Path file:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
        System.out.println("Core fatigue grace checks passed: "+checks);
    }
}
