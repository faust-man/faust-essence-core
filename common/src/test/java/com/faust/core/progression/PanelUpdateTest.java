package com.faust.core.progression;
import java.nio.file.*;
public final class PanelUpdateTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void run() throws Exception {
        var levels=new Progress();check(levels.totalLevel()==0,"new player level0");
        levels.track(Skill.strength).level=7;levels.track(Skill.dexterity).level=3;check(levels.totalLevel()==10,"sum characteristic levels");
        levels.track(Skill.strength).xp=9999;levels.unlocked.add("example:ability");check(levels.totalLevel()==10,"XP and unlocked nodes do not change level");
        check(Progress.parse(levels.json()).totalLevel()==10,"level survives save");
        Balance b=new Balance();Progress p=new Progress();p.max(b);p.stamina=3;
        for(boolean air:new boolean[]{false,true})for(boolean ready:new boolean[]{false,true}){
            p.recover(10,false,false,ready,false,false,air,true,b);
            check(p.stamina==3,"fast swimming blocks passive regeneration and second wind");
        }
        p.recover(1,false,false,true,false,false,false,false,b);check(p.stamina>3,"recovery resumes after swimming");
        for(var food:java.util.Map.of("minecraft:golden_apple",5.0,"minecraft:golden_carrot",3.0,"minecraft:glistering_melon_slice",2.0).entrySet()){
            p.fatigue=10;p.sweetFood(food.getKey(),b);check(p.fatigue==10-food.getValue(),"food relief");
        }
        p.fatigue=10;p.sweetFood("minecraft:enchanted_golden_apple",b);check(p.fatigue==0,"enchanted apple clears fatigue");
        p.fatigue=-3;p.sweetFood("minecraft:enchanted_golden_apple",b);check(p.fatigue==-3,"sleep reserve preserved");
        check(p.effect("fall_damage_reduction",b)==0.5,"fall damage halved");p.toggle("faustcore:cushioned_landing",b);check(p.effect("fall_damage_reduction",b)==0,"toggle restores damage");
        check(TreeGroups.groups(b).equals(java.util.List.of("faustcore")),"no redundant book");
        var a=b.abilities.getFirst();b.abilities.add(new Balance.Ability("panel_test:node",a.skill(),0,"",0,"","stamina",1));
        check(TreeGroups.groups(b).equals(java.util.List.of("faustcore","panel_test")),"addon book");check(TreeGroups.skills(b,"panel_test").equals(java.util.List.of(a.skill())),"addon skills");
        b.abilities.add(new Balance.Ability("faust_epic_compat:epicfight/guard",Skill.defense,0,"",0,"","stamina",1));
        b.abilities.add(new Balance.Ability("faust_epic_compat:other_addon/roll",Skill.dexterity,0,"",0,"","stamina",1));
        check(!TreeGroups.groups(b).contains("faust_epic_compat"),"compat and Epic addons merged by default");
        check(TreeGroups.contains(b,"faustcore","faust_epic_compat:epicfight/guard"),"merged nodes pass renderer filter");
        check(TreeGroups.contains(b,"faustcore","faust_epic_compat:other_addon/roll"),"other Epic namespace inside compat merges");
        check(TreeGroups.groups(b).contains("panel_test"),"unrelated addons remain separate");
        b=b.edited("integratedTreeNamespaces","[]");
        check(TreeGroups.groups(b).contains("faust_epic_compat"),"config restores separate tab");
        check(!TreeGroups.contains(b,"faustcore","faust_epic_compat:epicfight/guard"),"separate nodes excluded from core");
        check(TreeGroups.skills(b,"faust_epic_compat").containsAll(java.util.List.of(Skill.defense,Skill.dexterity)),"separate branches retained");
        try {b.edited("integratedTreeNamespaces","[\"bad:id\"]");throw new AssertionError("invalid namespace accepted");}catch(IllegalArgumentException expected){}
        b=b.edited("integratedTreeNamespaces","[\"faust_epic_compat\",\"panel_test\"]");
        check(TreeGroups.groups(b).equals(java.util.List.of("faustcore")),"additional namespace configurable");
        check(TreeGroups.contains(Balance.JSON.fromJson(Balance.JSON.toJson(b),Balance.class),"faustcore","panel_test:node"),"snapshot preserves merge policy");
        Path dir=Files.createTempDirectory("core-panel-update");Balance.load(dir);Files.delete(dir.resolve(".panel-update-format"));
        Path foods=Balance.sectionFile(dir,"sweetFoods");var json=com.google.gson.JsonParser.parseString(Files.readString(foods)).getAsJsonObject();json.addProperty("minecraft:golden_apple",7);json.remove("minecraft:golden_carrot");Files.writeString(foods,Balance.JSON.toJson(json));
        var loaded=Balance.load(dir);check(loaded.sweetFoods.get("minecraft:golden_apple")==7,"custom food preserved");check(loaded.sweetFoods.get("minecraft:golden_carrot")==3,"missing food migrated");
        check(Balance.load(dir).abilities.size()==loaded.abilities.size(),"migration idempotent");
        check(Files.exists(dir.resolve("integratedTreeNamespaces.json")),"new config created for existing installations");
        Balance.saveGroup(dir,"integratedTreeNamespaces","[]");
        check(Balance.load(dir).integratedTreeNamespaces.isEmpty(),"reload preserves separate setting");
        System.out.println("Panel update checks passed");
    }
}
