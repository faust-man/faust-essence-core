package com.faust.core.runtime;

import com.faust.core.progression.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Items;
import java.nio.file.*;
import java.util.*;

/** Actual vanilla stat dispatch, per-slice amounts and editable cake migration. */
public final class CakeFoodTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void near(double actual,double expected,String message){check(Math.abs(actual-expected)<1e-8,message+": "+actual+" != "+expected);}
    public static void run() throws Exception {
        var b=new Balance();var p=new Progress();p.fatigue=5;var cake=Stats.CUSTOM.get(Stats.EAT_CAKE_SLICE);
        check(CoreRuntime.foodStat(p,cake,1,b),"vanilla slice stat handled");near(p.fatigue,4,"one fatigue relieved per slice");near(p.stamina,20,"cake does not refill stamina");
        CoreRuntime.foodStat(p,cake,2,b);near(p.fatigue,2,"multiple slice count scales relief");check(!CoreRuntime.foodStat(p,cake,0,b),"zero count ignored");check(!CoreRuntime.foodStat(p,cake,-1,b),"negative count ignored");
        check(!CoreRuntime.foodStat(p,Stats.CUSTOM.get(Stats.SPRINT_ONE_CM),1,b),"other stats do not eat cake");check(!CoreRuntime.foodStat(p,Stats.ITEM_USED.get(Items.CAKE),1,b),"placing cake gives no relief");near(p.fatigue,2,"ignored events preserve fatigue");
        p.fatigue=0.5;CoreRuntime.foodStat(p,cake,1,b);near(p.fatigue,0,"cake relief clamps at zero");p.fatigue=-5;CoreRuntime.foodStat(p,cake,1,b);near(p.fatigue,-5,"cake preserves negative sleep reserve");
        p.fatigue=5;b.sweetFoods.put("minecraft:cake",2.5);CoreRuntime.foodStat(p,cake,1,b);near(p.fatigue,2.5,"configured per-slice relief applied");
        b.fatigue.put("enabled",0.0);CoreRuntime.foodStat(p,cake,1,b);near(p.fatigue,2.5,"fatigue disabled preserves history");b.fatigue.put("enabled",1.0);b.stamina.put("enabled",0.0);CoreRuntime.foodStat(p,cake,1,b);near(p.fatigue,2.5,"stamina disabled preserves history");
        b.stamina.put("enabled",1.0);p.sweetFood("minecraft:cake",Double.NaN,b);near(p.fatigue,2.5,"nonfinite food count ignored");
        Path dir=Files.createTempDirectory("core-cake-food");try{
            var loaded=Balance.load(dir);Files.delete(dir.resolve(".cake-format-1.5.2"));loaded.sweetFoods.remove("minecraft:cake");loaded.sweetFoods.put("minecraft:cookie",9.0);Files.writeString(Balance.sectionFile(dir,"sweetFoods"),Balance.JSON.toJson(loaded.sweetFoods));
            var migrated=Balance.load(dir);near(migrated.sweetFoods.get("minecraft:cake"),1,"old config gains cake entry");near(migrated.sweetFoods.get("minecraft:cookie"),9,"other custom food value retained");check(Files.exists(dir.resolve("migration-v1.5.2/sweetFoods.json")),"food config backup exists");
            migrated.sweetFoods.put("minecraft:cake",0.0);Balance.saveGroup(dir,"sweetFoods",Balance.JSON.toJson(migrated.sweetFoods));near(Balance.load(dir).sweetFoods.get("minecraft:cake"),0,"explicit zero survives reload");
            migrated.sweetFoods.remove("minecraft:cake");Balance.saveGroup(dir,"sweetFoods",Balance.JSON.toJson(migrated.sweetFoods));check(!Balance.load(dir).sweetFoods.containsKey("minecraft:cake"),"explicit removal survives reload");
        }finally{try(var paths=Files.walk(dir)){for(Path file:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
        System.out.println("Core cake food checks passed: "+checks);
    }
}
