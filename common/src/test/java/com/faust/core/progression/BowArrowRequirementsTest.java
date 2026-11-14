package com.faust.core.progression;
import java.nio.file.*;
import java.util.*;
public final class BowArrowRequirementsTest {
 private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 private static Balance.Ability node(Balance b){return b.abilities.stream().filter(a->a.id().equals("faustcore:bow_mastery")).findFirst().orElseThrow();}
 public static void run() throws Exception {
  var b=new Balance();var a=node(b);check(a.actionConditions().size()==3&&a.actionConditions().contains(new Balance.Condition("bow_shots",150)),"retain 150 shots plus both arrow conditions");
  var p=new Progress();p.track(CoreSkills.dexterity).level=10;p.recordMaximum("spectral_arrows_inventory",16,b);p.recordMaximum("tipped_arrows_inventory",15,b);p.unlocked.addAll(a.all());p.setCounter("bow_shots",150,b);check(!p.canUnlock(a,b),"15 tipped arrows insufficient");p.recordMaximum("tipped_arrows_inventory",16,b);check(p.canUnlock(a,b),"inventory milestones before parent count");p.recordMaximum("tipped_arrows_inventory",0,b);check(p.canUnlock(a,b),"removing arrows preserves milestone");p.setCounter("bow_shots",149,b);check(!p.canUnlock(a,b),"149 shots insufficient");
  var dir=Files.createTempDirectory("bow-arrows");try{Balance.load(dir);Files.delete(dir.resolve(".bow-mastery-arrows"));var data=Balance.JSON.toJsonTree(b.abilities).getAsJsonArray();for(var e:data){var j=e.getAsJsonObject();if(j.get("id").getAsString().equals(a.id())){j.add("conditions",new com.google.gson.JsonArray());j.addProperty("amount",250);}}Files.writeString(dir.resolve("abilities.json"),Balance.JSON.toJson(data));var migrated=Balance.load(dir);check(node(migrated).actionConditions().contains(new Balance.Condition("bow_shots",250)),"migration preserves custom shot requirement");check(node(migrated).actionConditions().size()==3,"migration appends arrows");check(Balance.load(dir).abilities.equals(migrated.abilities),"migration idempotent");check(Files.exists(dir.resolve("abilities-before-bow-mastery-arrows.json")),"migration backup");}finally{try(var files=Files.walk(dir)){for(var f:files.sorted(Comparator.reverseOrder()).toList())Files.delete(f);}}
 }
}
