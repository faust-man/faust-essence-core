package com.faust.core.progression;
import java.nio.file.*;
public final class RequestedRequirementsTest {
 private static Balance.Ability node(Balance b,String name){return b.abilities.stream().filter(a->a.id().equals("faustcore:"+name)).findFirst().orElseThrow();}
 private static void check(boolean value){if(!value)throw new AssertionError("Requested requirements");}
 public static void run() throws Exception {
  var b=new Balance();check(node(b,"enchanting_power").level()==7&&node(b,"enchanting_power").amount()==30);
  check(node(b,"training_constitution_health").level()==5&&node(b,"training_constitution_stamina").level()==3);
  var health=node(b,"training_constitution_health");var early=new Progress();early.recordMaximum("regeneration_2_received",1,b);check(early.conditionCount(health,health.actionConditions().getFirst())==0);early.unlocked.addAll(health.all());check(early.conditionCount(health,health.actionConditions().getFirst())==1);
  var armor=node(b,"armor_familiarity");check(armor.actionConditions().size()==2&&armor.actionConditions().stream().anyMatch(c->c.counter().equals("armor_metres")));
  var luck=node(b,"training_intelligence_luck");var p=new Progress();p.track(CoreSkills.intelligence).level=3;
  p.setCounter("world_chests",15,b);p.setCounter("fish_caught",14,b);check(!p.canUnlock(luck,b));p.setCounter("fish_caught",15,b);check(p.canUnlock(luck,b));
  Path dir=Files.createTempDirectory("requested-requirements");Balance.load(dir);Files.delete(dir.resolve(".requirements-2026-11"));
  var j=Balance.JSON.toJsonTree(b.abilities).getAsJsonArray();for(var e:j)if(e.getAsJsonObject().get("id").getAsString().equals("faustcore:enchanting_power"))e.getAsJsonObject().addProperty("level",5);
  for(var e:j)if(e.getAsJsonObject().get("id").getAsString().equals("faustcore:armor_familiarity")){var a=e.getAsJsonObject();a.addProperty("amount",777);a.add("conditions",new com.google.gson.JsonArray());}
  Files.writeString(dir.resolve("abilities.json"),Balance.JSON.toJson(j));var migrated=Balance.load(dir);check(node(migrated,"enchanting_power").level()==7);check(node(migrated,"armor_familiarity").actionConditions().stream().anyMatch(c->c.counter().equals("armor_metres")&&c.amount()==777));check(Balance.load(dir).abilities.equals(migrated.abilities));
  check(Files.exists(dir.resolve("migration-requirements-2026-11/abilities.json")));
 }
}
