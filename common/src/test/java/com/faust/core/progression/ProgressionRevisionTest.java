package com.faust.core.progression;
import com.faust.essence.api.Skill;

public final class ProgressionRevisionTest {
 private static void check(boolean ok){if(!ok)throw new AssertionError("progression revision");}
 public static void run()throws Exception{
  var b=new Balance();var p=new Progress();check(p.cap(CoreSkills.strength,b)==3);
  double[] expected={1.5,1.25,1,.5,.25,.13,.07,.04,.02,.01};
  for(int i=0;i<expected.length;i++){p.track(CoreSkills.strength).level=i;check(Math.abs(p.multiplier(CoreSkills.strength,b)-expected[i])<1e-9);}
  p.track(CoreSkills.strength).level=Integer.MAX_VALUE;check(p.multiplier(CoreSkills.strength,b)==.01);
  p.track(CoreSkills.strength).level=0;p.track(CoreSkills.strength).limitOverride=9;check(p.multiplier(CoreSkills.strength,b)==2);
  check(p.price(CoreSkills.defense,b)==1);p.buy(CoreSkills.strength,1,b);check(p.price(CoreSkills.defense,b)==2);
  var addon=Skill.saved("revision:skill");check(p.price(addon,b)==1);p.buy(addon,1,b);check(p.price(addon,b)==2&&p.price(CoreSkills.defense,b)==2);
  b.limitCosts.put("revision:base",4.0);b.limitCosts.put("revision:step",2.0);check(p.price(addon,b)==6);
  var old=Progress.parse("{\"schema\":2,\"tracks\":{\"strength\":{\"purchases\":2,\"level\":7}}}",b);
  check(old.cap(CoreSkills.strength,b)==7&&old.cap(CoreSkills.defense,b)==5&&old.price(CoreSkills.defense,b)==3);
  check(Progress.parse(old.json(),b).cap(CoreSkills.strength,b)==7);
  var dir=java.nio.file.Files.createTempDirectory("cap-revision");
  try{
   Balance.load(dir);var custom=new Balance();custom.progression.put("initialCap",7.0);
   java.nio.file.Files.writeString(dir.resolve("progression.json"),Balance.JSON.toJson(custom.progression));java.nio.file.Files.delete(dir.resolve(".cap-format-3"));
   var migrated=Balance.load(dir);check(new Progress().cap(CoreSkills.strength,migrated)==3);
   check(Progress.parse("{\"schema\":2}",migrated).cap(CoreSkills.strength,migrated)==7);
   check(java.nio.file.Files.exists(dir.resolve("migration-cap-3/progression.json")));
   check(Balance.load(dir).progression.get("legacyInitialCap")==7);
  }finally{try(var paths=java.nio.file.Files.walk(dir)){for(var file:paths.sorted(java.util.Comparator.reverseOrder()).toList())java.nio.file.Files.delete(file);}}
 }
}
