package com.faust.core.progression;
import java.nio.file.*;
import java.util.*;
public final class ExperienceSkillsTest {
 private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 private static Balance.Ability ability(Balance b,String id){return b.abilities.stream().filter(a->a.id().equals("faustcore:"+id)).findFirst().orElseThrow();}
 private static SkillTrees.Node node(Balance b,com.faust.essence.api.Skill skill,String id){return SkillTrees.nodes(b,skill).stream().filter(n->n.ability().equals("faustcore:"+id)).findFirst().orElseThrow();}
 public static void run() throws Exception {
  check(ExperienceMath.points(0,0)==0&&ExperienceMath.points(16,0)==352&&ExperienceMath.points(17,0)==394&&ExperienceMath.points(30,0)==1395&&ExperienceMath.points(31,0)==1507&&ExperienceMath.points(32,0)==1628,"experience formula boundaries");
  check(ExperienceMath.points(Integer.MAX_VALUE,0)==Integer.MAX_VALUE,"large levels do not overflow");
  check(ExperienceMath.points(0,1F/7)==1,"floating experience fractions round to actual points");
  check(ExperienceMath.stolen(100,29,0)==29&&ExperienceMath.stolen(1000,7,0)==70&&ExperienceMath.stolen(1000,200,0)==1000&&ExperienceMath.stolen(1,7,0)==0&&ExperienceMath.stolen(1000,7,Integer.MAX_VALUE-3)==3,"theft limits, rounding, recipient overflow");
  Balance b=new Balance();b.validate();var theft=ability(b,"experience_steal");check(theft.level()==7&&theft.amount()==25,"theft requirements");
  var p=new Progress();p.track(CoreSkills.intelligence).level=7;p.recordMaximum("experience_levels_held",24,b);check(!p.canUnlock(theft,b),"24 levels insufficient");p.recordMaximum("experience_levels_held",25,b);check(p.canUnlock(theft,b),"25 levels unlock theft");
  var silent=ability(b,"silent_step");p.track(CoreSkills.dexterity).level=10;p.recordMaximum("warden_noise",1,b);p.recordMaximum("invisibility_received",1,b);p.unlocked.add("faustcore:soft_foot");p.setCounter("breeze_kills",4,b);check(!p.canUnlock(silent,b),"four breezes insufficient");p.setCounter("breeze_kills",5,b);check(p.canUnlock(silent,b),"all three conditions unlock silent step");
  var enchant=node(b,CoreSkills.intelligence,"enchanting_power");var xp=node(b,CoreSkills.intelligence,"experience_steal");var brew=node(b,CoreSkills.intelligence,"potion_duration");var cat=node(b,CoreSkills.intelligence,"cat_scent");check(xp.x()==enchant.x()&&xp.y()==enchant.y()+4&&xp.icon().equals("minecraft:experience_bottle"),"theft under enchanting");check(cat.x()==brew.x()&&cat.y()==brew.y()+4,"cat under brewing");
  var soft=node(b,CoreSkills.dexterity,"soft_foot");var quiet=node(b,CoreSkills.dexterity,"silent_step");check(quiet.x()==soft.x()&&quiet.y()==soft.y()+4&&quiet.icon().equals("minecraft:chainmail_boots"),"silent under soft foot");
  Path dir=Files.createTempDirectory("experience-skills");try{Balance.load(dir);Files.delete(dir.resolve(".experience-steal-silence"));Files.writeString(dir.resolve("abilities.json"),Balance.JSON.toJson(b.abilities.stream().filter(a->!Set.of(theft.id(),silent.id()).contains(a.id())).toList()));var migrated=Balance.load(dir);check(ability(migrated,"experience_steal").level()==7&&ability(migrated,"soft_foot").level()==7,"migration adds skills");check(Balance.load(dir).abilities.equals(migrated.abilities),"migration idempotent");check(Files.exists(dir.resolve("migration-experience-steal-silence/abilities.json")),"migration backup");}finally{try(var files=Files.walk(dir)){for(var f:files.sorted(Comparator.reverseOrder()).toList())Files.delete(f);}}
 }
}
