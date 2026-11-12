package com.faust.core.progression;
import java.nio.file.*;
public final class ForestSkillsTest {
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void run() throws Exception {
  Balance b=new Balance();b.validate();
  var reach=b.abilities.stream().filter(a->a.id().endsWith(":training_building_reach")).findFirst().orElseThrow();
  check(reach.actionConditions().size()==2,"reach needs both counters");
  var p=new Progress();p.max(b);p.unlocked.remove(reach.id());p.setCounter("mined",1024,b);p.setCounter("placed",1023,b);check(!p.canUnlock(reach,b),"placing below threshold");p.setCounter("placed",1024,b);check(p.canUnlock(reach,b),"both thresholds reached");
  var soft=b.abilities.stream().filter(a->a.id().endsWith(":soft_foot")).findFirst().orElseThrow();check(soft.level()==10&&soft.counter().equals("slow_falling_received"),"soft foot requirements");
  var cat=b.abilities.stream().filter(a->a.id().endsWith(":cat_scent")).findFirst().orElseThrow();check(cat.level()==5&&cat.counter().equals("cats_tamed"),"cat requirements");
  check(soft.parent().equals("faustcore:soft_landing"),"soft foot needs soft landing");
  var fire=b.abilities.stream().filter(a->a.id().endsWith(":fire_starter")).findFirst().orElseThrow();
  var locked=new Progress();locked.track(Skill.building).level=20;check(locked.requirementProgress(fire)==0&&!locked.canUnlock(fire,b),"no green progress without forester");
  check(fire.level()==5&&fire.counter().equals("charcoal_smelted"),"fire starter requirements");
  var bad=new SkillTrees.Node("test:bad",22,0,"minecraft:stick");var good=new SkillTrees.Node("test:good",0,0,"minecraft:stick");
  check(!SkillTrees.validPosition(bad,java.util.List.of(good,bad)),"gap below half width invalid");
  check(new SkillTrees.Node("test:snap",40,-52,"minecraft:stick").x()==44,"grid rounding");
  for(var tree:b.trees)for(var node:tree.nodes())check(SkillTrees.validPosition(node,tree.nodes()),"default spacing "+node.ability());
  Path dir=Files.createTempDirectory("forest-skills");try{
   Balance.load(dir);Files.delete(dir.resolve(".forest-skills"));
   var old=Balance.JSON.toJsonTree(b.abilities).getAsJsonArray();for(int i=old.size()-1;i>=0;i--)if(old.get(i).getAsJsonObject().get("id").getAsString().endsWith(":forester"))old.remove(i);
   Files.writeString(dir.resolve("abilities.json"),Balance.JSON.toJson(old));Balance migrated=Balance.load(dir);check(migrated.abilities.stream().anyMatch(a->a.id().endsWith(":forester")),"migration adds skill");
   String saved=Files.readString(dir.resolve("abilities.json"));Balance.load(dir);check(saved.equals(Files.readString(dir.resolve("abilities.json"))),"migration idempotent");
  }finally{try(var files=Files.walk(dir)){for(var f:files.sorted(java.util.Comparator.reverseOrder()).toList())Files.delete(f);}}
 }
}
