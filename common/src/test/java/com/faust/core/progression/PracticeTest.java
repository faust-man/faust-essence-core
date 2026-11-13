package com.faust.core.progression;
import com.faust.essence.api.Skill;

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
        check(b.actions.get("craft").skill()==CoreSkills.intelligence,"craft trains intelligence");
        check(!b.actions.containsKey("structure"),"structures removed");
        check(b.actions.get("brew").skill()==CoreSkills.intelligence,"brewing intelligence");
        check(b.stamina.get("throwCost")==1,"throw cost");
        p.track(CoreSkills.intelligence).level=20;
        check(PracticeBonuses.shelves(p,b)==0&&PracticeBonuses.duration(p,b)==1,"bonuses locked");
        p.unlocked.add("faustcore:enchanting_power");p.unlocked.add("faustcore:potion_duration");
        for(int level=0;level<=20;level++){
            p.track(CoreSkills.intelligence).level=level;p.track(CoreSkills.dexterity).level=level;
            check(PracticeBonuses.shelves(p,b)==(level/5)*15/8,"shelf boundary "+level);
            check(Math.abs(PracticeBonuses.duration(p,b)-(1+level*0.02))<1e-8,"duration "+level);
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
        check(com.faust.core.runtime.BrewingRewards.displayed(potion).customEffects().size()==1,"boosted tooltip has one duration");
        check(com.faust.core.runtime.BrewingRewards.displayed(potion).customEffects().getFirst().getDuration()==(int)(base*1.2),"tooltip shows boosted duration");
        var recipes=net.minecraft.world.item.alchemy.PotionBrewing.bootstrap(net.minecraft.world.flag.FeatureFlags.VANILLA_SET);
        var longPotion=recipes.mix(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE),com.faust.core.runtime.BrewingRewards.unboosted(potion));
        check(com.faust.core.runtime.BrewingRewards.recipeChanged(potion,longPotion),"actual redstone recipe detected");
        check(!com.faust.core.runtime.BrewingRewards.recipeChanged(potion,com.faust.core.runtime.BrewingRewards.unboosted(potion)),"normalizing is not a new brew");
        check(!com.faust.core.runtime.BrewingRewards.recipeChanged(potion,net.minecraft.world.item.ItemStack.EMPTY),"empty slot not rewarded");
        var view=new com.faust.core.client.TreeView(java.util.List.of(new SkillTrees.Node("test:node",0,0,"minecraft:book")),160,96);
        double x=view.offsetX,y=view.offsetY;view.pan(12,-8);check(view.offsetX==x+12&&view.offsetY==y-8,"small tree drags instead of snapping back");
        view.pan(-12,8);check(view.offsetX==x&&view.offsetY==y,"reverse drag restores position");
        for(var tree:b.trees)if(tree.skill()==CoreSkills.intelligence||tree.skill()==CoreSkills.dexterity){
            var nodes=tree.nodes();String root=tree.skill()==CoreSkills.intelligence?"faustcore:training_intelligence_luck":"faustcore:training_dexterity_sprint";
            var anchor=nodes.stream().filter(n->n.ability().equals(root)).findFirst().orElseThrow();
            for(var n:nodes)if(java.util.Set.of("faustcore:soft_landing","faustcore:enchanting_power","faustcore:potion_duration").contains(n.ability()))check(n.x()<anchor.x()&&n.y()==anchor.y(),"new skills left of root on same row");
        }
        Path dir=Files.createTempDirectory("core-practice-test");
        try {
            Balance.load(dir);Balance.load(dir);
            var layouts=java.util.Arrays.asList(Balance.JSON.fromJson(Files.readString(dir.resolve("trees.json")),SkillTrees.Tree[].class));
            var oldLayouts=new java.util.ArrayList<SkillTrees.Tree>();
            for(var tree:layouts)oldLayouts.add(new SkillTrees.Tree(tree.skill(),tree.nodes().stream().filter(n->!java.util.Set.of("faustcore:enchanting_power","faustcore:potion_duration","faustcore:soft_landing").contains(n.ability())).toList()));
            Files.writeString(dir.resolve("trees.json"),Balance.JSON.toJson(oldLayouts));Files.delete(dir.resolve(".practice-layout-2"));
            var relocated=Balance.load(dir);
            for(var skill:java.util.List.of(CoreSkills.intelligence,CoreSkills.dexterity)){
                var nodes=SkillTrees.nodes(relocated,skill);var root=nodes.stream().filter(n->n.ability().equals(skill==CoreSkills.intelligence?"faustcore:training_intelligence_luck":"faustcore:training_dexterity_sprint")).findFirst().orElseThrow();
                for(var node:nodes)if(java.util.Set.of("faustcore:enchanting_power","faustcore:potion_duration","faustcore:soft_landing").contains(node.ability()))check(node.x()<root.x()&&node.y()==root.y(),"old config gets horizontal placement");
            }
            check(Files.exists(dir.resolve("migration-practice-layout/trees.json")),"layout migration backup");
            check(Balance.JSON.toJson(Balance.load(dir).trees).equals(Balance.JSON.toJson(relocated.trees)),"layout migration idempotent");
            Files.delete(dir.resolve(".practice-format"));
            var actions=new java.util.LinkedHashMap<>(b.actions);actions.put("structure",new Balance.Rule(CoreSkills.intelligence,20,"structures"));actions.put("craft",new Balance.Rule(CoreSkills.building,3,"crafted"));actions.remove("brew");
            Files.writeString(dir.resolve("actions.json"),Balance.JSON.toJson(actions));
            var abilities=b.abilities.stream().filter(a->!java.util.Set.of("faustcore:soft_landing","faustcore:enchanting_power","faustcore:potion_duration").contains(a.id())).toList();
            Files.writeString(dir.resolve("abilities.json"),Balance.JSON.toJson(abilities));
            var stats=b.statBonuses.stream().filter(r->!r.id.equals("dexterity_safe_fall")).toList();Files.writeString(dir.resolve("statBonuses.json"),Balance.JSON.toJson(stats));
            var stamina=new java.util.LinkedHashMap<>(b.stamina);stamina.remove("throwCost");Files.writeString(Balance.sectionFile(dir,"stamina"),Balance.JSON.toJson(stamina));
            Balance migrated=Balance.load(dir);check(!migrated.actions.containsKey("structure"),"migration removes structures");check(migrated.actions.get("craft").skill()==CoreSkills.intelligence&&migrated.actions.get("craft").xp()==3,"migration retains craft XP");check(migrated.abilities.size()==b.abilities.size(),"new abilities migrate");check(migrated.statBonuses.size()==b.statBonuses.size(),"fall stat migrates");
            check(Balance.load(dir).abilities.size()==b.abilities.size(),"idempotent migration");check(Files.exists(dir.resolve("migration-practice/actions.json")),"backup");
        } finally {try(var files=Files.walk(dir)){for(var file:files.sorted(java.util.Comparator.reverseOrder()).toList())Files.delete(file);}}
        System.out.println("Practice checks: "+checks);
    }
}
