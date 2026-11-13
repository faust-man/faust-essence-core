package com.faust.core.progression;
import com.faust.essence.api.*;
import java.util.*;
public final class ExtensionAdapterTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void run()throws Exception{
  check(EssenceApi.available(),"Core backend discovered through SPI");
  var skill=EssenceApi.registerSkill("extension_test:craft");EssenceApi.registerBonus("extension_test:power",(player,value)->{});
  Extensions.tab(new Extensions.Tab("extension_test",4,2,false));
  Extensions.characteristic(new Extensions.Characteristic(skill,4,12,2,40,10,2));
  Extensions.action(new Extensions.Action("extension_test:craft",skill,10,"extension_test:crafts"));
  Extensions.ability(new Extensions.Ability("extension_test:ability",List.of(new Extensions.Requirement(skill,1)),List.of(new Extensions.Condition("extension_test:crafts",2)),List.of(),"extension_test:power",1,true,0,0,"minecraft:stone"));
  var b=new Balance();b.validate();var p=new Progress();check(p.cap(skill,b)==4&&p.price(skill,b)==4&&p.needed(skill,b)==40,"registered rules reach model");check(p.buy(skill,4,b)&&p.cap(skill,b)==6&&p.price(skill,b)==6,"custom cap step and price");
  var existing=Progress.parse("{\"schema\":2}",b);check(existing.cap(skill,b)==4&&existing.cap(CoreSkills.strength,b)==5,"extension defaults do not overwrite legacy Core potential");
  check(p.event("extension_test:craft",1,1,b),"registered practice action");check(p.track(skill).level>0||p.track(skill).xp>0,"registered action grants XP");
  check(b.abilities.stream().anyMatch(a->a.id().equals("extension_test:ability")),"registered ability reaches engine");check(TreeGroups.groups(b).contains("extension_test"),"registered tab visible");
  var dir=java.nio.file.Files.createTempDirectory("extension-adapter");try{var loaded=Balance.load(dir);check(loaded.actions.containsKey("extension_test:craft"),"definitions survive config load");check(Balance.load(dir).abilities.stream().filter(a->a.id().equals("extension_test:ability")).count()==1,"reload has no duplicates");}finally{try(var paths=java.nio.file.Files.walk(dir)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())java.nio.file.Files.delete(path);}}
  System.out.println("Core public API adapter checks passed");
 }
}
