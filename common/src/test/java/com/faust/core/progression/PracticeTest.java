package com.faust.core.progression;
import java.nio.file.*;
public final class PracticeTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void run() throws Exception {
        check(HudClearance.staminaY(190,Integer.MAX_VALUE,Integer.MAX_VALUE)==190,"no obstacles");
        check(HudClearance.staminaY(190,Integer.MAX_VALUE,191)==182,"single mount row");
        check(HudClearance.staminaY(190,Integer.MAX_VALUE,181)==172,"two mount rows over 20 HP");
        check(HudClearance.staminaY(190,171,181)==162,"air and mount together");
        check(HudClearance.staminaY(100,171,181)==100,"custom HUD above obstacles");
        check(HudClearance.vehicleHeart("minecraft","hud/heart/vehicle_container"),"empty mount hearts count");
        check(!HudClearance.vehicleHeart("minecraft","hud/heart/full"),"player hearts excluded");
        Balance b=new Balance();b.validate();Progress p=new Progress();
        check(b.actions.get("craft").skill()==Skill.intelligence,"craft trains intelligence");
        check(!b.actions.containsKey("structure"),"structures removed");
        check(b.actions.get("brew").skill()==Skill.intelligence,"brewing intelligence");
        check(b.stamina.get("throwCost")==1,"throw cost");
        p.track(Skill.intelligence).level=20;
        check(PracticeBonuses.shelves(p,b)==0&&PracticeBonuses.duration(p,b)==1,"bonuses locked");
        p.unlocked.add("faustcore:enchanting_power");p.unlocked.add("faustcore:potion_duration");
        for(int level=0;level<=20;level++){
            p.track(Skill.intelligence).level=level;p.track(Skill.dexterity).level=level;
            check(PracticeBonuses.shelves(p,b)==(level/5)*15/8,"shelf boundary "+level);
            check(Math.abs(PracticeBonuses.duration(p,b)-(1+level*0.01))<1e-8,"duration "+level);
            check(SkillEffects.attribute(p,b,"minecraft:generic.safe_fall_distance",false)==0,"fall locked");
            p.unlocked.add("faustcore:soft_landing");
            check(SkillEffects.attribute(p,b,"minecraft:generic.safe_fall_distance",false)==level/5,"fall boundary "+level);
            p.unlocked.remove("faustcore:soft_landing");
        }
        var potion=net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION,net.minecraft.world.item.alchemy.Potions.SWIFTNESS);
        int base=potion.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS).potion().get().value().getEffects().getFirst().getDuration();
        com.faust.core.runtime.BrewingRewards.extend(potion,1.2);
        check(potion.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS).customEffects().getFirst().getDuration()==(int)(base*1.2),"actual potion duration extended");
        var clean=com.faust.core.runtime.BrewingRewards.unboosted(potion);
        check(clean.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS).customEffects().isEmpty(),"rebrewing removes generated effects");
        check(potion.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS).customEffects().size()==1,"normalization preserves original stack");
        com.faust.core.runtime.BrewingRewards.extend(clean,1.2);
        check(clean.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS).customEffects().getFirst().getDuration()==(int)(base*1.2),"no cumulative duration");
        var instant=net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION,net.minecraft.world.item.alchemy.Potions.HEALING);
        com.faust.core.runtime.BrewingRewards.extend(instant,1.2);
        check(instant.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS).customEffects().isEmpty(),"instant effect unchanged");
        Path dir=Files.createTempDirectory("core-practice-test");
        try {
            Balance.load(dir);Files.delete(dir.resolve(".practice-format"));
            var actions=new java.util.LinkedHashMap<>(b.actions);actions.put("structure",new Balance.Rule(Skill.intelligence,20,"structures"));actions.put("craft",new Balance.Rule(Skill.building,3,"crafted"));actions.remove("brew");
            Files.writeString(dir.resolve("actions.json"),Balance.JSON.toJson(actions));
            var abilities=b.abilities.stream().filter(a->!java.util.Set.of("faustcore:soft_landing","faustcore:enchanting_power","faustcore:potion_duration").contains(a.id())).toList();
            Files.writeString(dir.resolve("abilities.json"),Balance.JSON.toJson(abilities));
            var stats=b.statBonuses.stream().filter(r->!r.id.equals("dexterity_safe_fall")).toList();Files.writeString(dir.resolve("statBonuses.json"),Balance.JSON.toJson(stats));
            var stamina=new java.util.LinkedHashMap<>(b.stamina);stamina.remove("throwCost");Files.writeString(dir.resolve("stamina.json"),Balance.JSON.toJson(stamina));
            Balance migrated=Balance.load(dir);check(!migrated.actions.containsKey("structure"),"migration removes structures");check(migrated.actions.get("craft").skill()==Skill.intelligence&&migrated.actions.get("craft").xp()==3,"migration retains craft XP");check(migrated.abilities.size()==b.abilities.size(),"new abilities migrate");check(migrated.statBonuses.size()==b.statBonuses.size(),"fall stat migrates");
            check(Balance.load(dir).abilities.size()==b.abilities.size(),"idempotent migration");check(Files.exists(dir.resolve("migration-practice/actions.json")),"backup");
        } finally {try(var files=Files.walk(dir)){for(var file:files.sorted(java.util.Comparator.reverseOrder()).toList())Files.delete(file);}}
        System.out.println("Practice checks: "+checks);
    }
}
