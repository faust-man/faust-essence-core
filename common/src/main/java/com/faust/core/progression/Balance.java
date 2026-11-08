package com.faust.core.progression;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Server-owned, reloadable configuration. No loader or client dependencies. */
public final class Balance {
    public static final Gson JSON = new GsonBuilder().registerTypeAdapter(Skill.class,new com.google.gson.TypeAdapter<Skill>() {
        public void write(com.google.gson.stream.JsonWriter out,Skill skill) throws IOException {out.value(skill.name());}
        public Skill read(com.google.gson.stream.JsonReader in) throws IOException {return Skill.saved(in.nextString());}
    }).setPrettyPrinting().create();
    public Map<String, Double> progression = numbers("levelBase",100,"levelSlope",20,"initialCap",5,"capStep",1,"maximumCap",20,"costDivisor",5,"maximumGrowth",2,"trainingMultiplier",2,"slowdown",0.5,"overCapGrowth",0.33);
    public Map<String, Double> stamina = numbers("enabled",1,"base",20,"constitutionPerLevel",1,"regeneration",4,"delaySeconds",1.5,"armorCost",0.25,"waterCost",1,"exhaustionLock",1,"exhaustionTrigger",1,"exhaustionRecovery",10,"airRecoveryMultiplier",0.5,"swimCost",0.4,"swimDelaySeconds",3,"throwCost",1,"bowCost",2,"crossbowCost",1,"attackCost",1,"maceCost",2,"shieldCost",2,"sprintJumpCost",0.5,"jumpArmorCost",0.05,"runCost",0.1,"secondWindMultiplier",1.5,"flowThreshold",0.05);
    public Map<String, Double> fatigue = numbers("enabled",1,"worldGraceSeconds",300,"accumulationMultiplier",0.5,"perSpentFraction",0.05,"maximum",16,"minimum",4,"sleepPerSecond",0.2,"campfirePerSecond",0.04,"campfireRadius",4,"soulCampfirePerSecond",0.04,"soulFirePerSecond",0.1,"resetAfterNight",1,"sleepReserve",5);
    public Map<String, Double> armor = numbers("baseCapacity",0,"capacityPerLevel",0,"fallbackWeight",2,"leather",1,"chainmail",2,"iron",2.5,"golden",2,"diamond",3,"netherite",3.5);
    public Map<String, Double> skills = numbers("damagePerLevel",0.002,"damageMaximum",0.10,"speedPerLevel",0.001,"speedMaximum",0.05,"breakingPerLevel",0.01,"breakingMaximum",1000000000,"constitutionSwimMultiplier",2,"swimMetresPerMinute",200,"damageMinimumHP",2,"hitCharge",0.9,"hitMaximum",10,"shieldMaximum",5,"shieldIntervalTicks",20,"shieldBudgetPerMinute",20,"runMetresPerMinute",200,"blockMaximumPerMinute",300,"readCooldownSeconds",30,"structureIntervalTicks",40,"healthPerLevel",1,"luckPerLevel",0.01,"luckMaximum",0.2,"reachEveryLevels",10,"reachPerStep",1,"buildingReachMaximum",3,"iconTier1",5,"iconTier2",10,"iconTier3",20);
    public Map<String, Double> client = numbers("hud",1,"hudX",0,"hudY",0,"hudWidth",81);
    public Map<String, Double> integrations = numbers("weaponDefenseEnabled",0);
    public Map<String, Rule> actions = new LinkedHashMap<>();
    public Map<String,Double> sweetFoods=numbers("minecraft:golden_apple",5,"minecraft:golden_carrot",3,"minecraft:glistering_melon_slice",2,"minecraft:cake",1,"minecraft:cookie",1,"minecraft:pumpkin_pie",3,"minecraft:honey_bottle",2,"minecraft:apple",1,"minecraft:melon_slice",0.5,"minecraft:sweet_berries",0.5,"minecraft:glow_berries",0.5);
    public List<Ability> abilities = new ArrayList<>();
    public List<SkillTrees.Tree> trees = new ArrayList<>();
    public List<StatBonus> statBonuses = new ArrayList<>();
    public static final class StatBonus {
        public String id,attribute,operation="add",requiresAbility="";
        public Skill skill;
        public double perLevel,maximum=1e9;
        public int everyLevels=1;
        public boolean sprintOnly;
        public double amount(int level){return Math.min(maximum,Math.floor((double)level/everyLevels)*perLevel);}
    }
    private void stat(String id,Skill skill,String attribute,String operation,double rate,int every,double max,boolean sprint) {
        StatBonus bonus=new StatBonus();bonus.id=id;bonus.skill=skill;bonus.attribute="minecraft:"+(attribute.equals("block_interaction_range")||attribute.equals("entity_interaction_range")?"player.":"generic.")+attribute;bonus.operation=operation;bonus.perLevel=rate;bonus.everyLevels=every;bonus.maximum=max;bonus.sprintOnly=sprint;statBonuses.add(bonus);
    }

    public record Rule(Skill skill, double xp, String counter) {}
    public record SkillRequirement(Skill skill,int level) {}
    public record Condition(String counter,double amount) {}
    public record Ability(String id, Skill skill, int level, String counter, double amount,
                          String parent, String effect, double value,List<String> requiresAll,List<String> requiresAny,List<String> excludes, List<SkillRequirement> characteristics, List<Condition> conditions, String conditionMode, int conditionsRequired, int requiresCount, Boolean toggleable) {
        public Ability {
            toggleable=toggleable==null?Boolean.TRUE:toggleable;
            if(counter==null&&conditions!=null&&!conditions.isEmpty())counter="";
            if(parent==null)parent="";
            characteristics=characteristics==null?List.of():List.copyOf(characteristics);
            if(!characteristics.isEmpty())skill=characteristics.getFirst().skill();
            conditions=conditions==null?List.of():List.copyOf(conditions);
            conditionMode=conditionMode==null?"all":conditionMode;
            requiresAll=requiresAll==null?List.of():List.copyOf(requiresAll);requiresAny=requiresAny==null?List.of():List.copyOf(requiresAny);excludes=excludes==null?List.of():List.copyOf(excludes);}
        // Retain the previous full constructor for integrations compiled against the API.
        public Ability(String id,Skill skill,int level,String counter,double amount,String parent,String effect,double value,List<String> all,List<String> any,List<String> excludes,List<SkillRequirement> characteristics,List<Condition> conditions,String conditionMode,int conditionsRequired,int requiresCount){this(id,skill,level,counter,amount,parent,effect,value,all,any,excludes,characteristics,conditions,conditionMode,conditionsRequired,requiresCount,true);}
        public Ability(String id,Skill skill,int level,String counter,double amount,String parent,String effect,double value,List<String> all,List<String> any,List<String> excludes){this(id,skill,level,counter,amount,parent,effect,value,all,any,excludes,List.of(),List.of(),"all",0,0);}
        public Skill skill(){return characteristics.isEmpty()?skill:characteristics.getFirst().skill();}
        public List<SkillRequirement> skillRequirements(){return characteristics.isEmpty()?List.of(new SkillRequirement(skill,level)):characteristics;}
        public List<Condition> actionConditions(){return conditions.isEmpty()?List.of(new Condition(counter,amount)):conditions;}
        public int requiredConditions(){return conditionMode.equals("at_least")?conditionsRequired:actionConditions().size();}
        public int requiredParents(){return requiresAny.isEmpty()?0:requiresCount==0?1:requiresCount;}
        public Ability(String id,Skill skill,int level,String counter,double amount,String parent,String effect,double value){this(id,skill,level,counter,amount,parent,effect,value,List.of(),List.of(),List.of());}
        public List<String> all(){var ids=new LinkedHashSet<>(requiresAll);if(parent!=null&&!parent.isEmpty())ids.add(parent);return List.copyOf(ids);}
        public List<String> dependencies(){var ids=new LinkedHashSet<>(all());ids.addAll(requiresAny);return List.copyOf(ids);}
    }

    public Balance() {
        skills.keySet().removeAll(List.of("damagePerLevel","damageMaximum","speedPerLevel","speedMaximum","healthPerLevel","luckPerLevel","luckMaximum","reachEveryLevels","reachPerStep"));
        stat("strength_damage",Skill.strength,"attack_damage","multiply_base",0.01,1,1e9,false);
        stat("dexterity_sprint",Skill.dexterity,"movement_speed","multiply_base",0.01,1,1e9,true);
        stat("dexterity_attack",Skill.dexterity,"attack_speed","multiply_base",0.01,1,1e9,false);
        stat("constitution_health",Skill.constitution,"max_health","add",1,1,1e9,false);
        stat("defense_armor",Skill.defense,"armor","add",1,2,1e9,false);
        stat("intelligence_luck",Skill.intelligence,"luck","add",0.01,1,0.2,false);
        stat("building_block_reach",Skill.building,"block_interaction_range","add",1,10,3,false);
        action("melee",Skill.strength,1,"hits");
        action("damage",Skill.constitution,1,"damage_taken");
        action("run",Skill.dexterity,0.05,"run_metres");
        action("constitution_run",Skill.constitution,0.01,"constitution_run_metres");
        action("constitution_swim",Skill.constitution,0.01,"swim_metres");
        action("bow",Skill.dexterity,1,"bow_shots");
        action("crossbow",Skill.strength,1,"crossbow_shots");
        action("mace",Skill.strength,0,"mace_hits");
        action("armor_run",Skill.defense,0.05,"armor_metres");
        action("shield",Skill.defense,1,"blocks");
        action("read",Skill.intelligence,5,"books");
        action("enchant",Skill.intelligence,10,"enchants");
        action("brew",Skill.intelligence,5,"brewed");
        action("trade",Skill.intelligence,3,"trades");
        action("place",Skill.building,1,"placed");
        action("craft",Skill.intelligence,1,"crafted");
        ability("controlled_strike",Skill.strength,5,"hits",30,"","damage",0.01);
        ability("strong_grip",Skill.strength,10,"hits",100,"controlled_strike","damage",0.02);
        ability("second_wind",Skill.constitution,5,"damage_taken",50,"","second_wind",1);
        ability("enduring_body",Skill.constitution,15,"damage_taken",200,"second_wind","stamina",2);
        ability("steady_pace",Skill.dexterity,5,"run_metres",1000,"","speed",0.01);
        ability("sure_footing",Skill.dexterity,10,"run_metres",3000,"steady_pace","speed",0.01);
        ability("armor_familiarity",Skill.defense,5,"armor_metres",500,"","capacity",2);
        ability("armor_mastery",Skill.defense,15,"armor_metres",2000,"armor_familiarity","capacity",4);
        ability("shield_care",Skill.defense,5,"blocks",30,"","shield_durability",0.10);
        ability("shield_recovery",Skill.defense,10,"blocks",100,"shield_care","shield_recovery",0.20);
        ability("applied_knowledge",Skill.intelligence,5,"books",10,"","regeneration",0.2);
        ability("deep_understanding",Skill.intelligence,15,"enchants",20,"applied_knowledge","regeneration",0.4);
        ability("tool_handling",Skill.building,5,"placed",100,"","breaking",0.02);
        ability("master_builder",Skill.building,20,"crafted",300,"tool_handling","breaking",0.03);
        for(int i=0;i<abilities.size();i++){var a=abilities.get(i);abilities.set(i,new Ability(a.id(),a.skill(),a.level(),a.counter(),a.amount(),a.parent(),a.effect(),a.effect().equals("second_wind")?1:a.effect().startsWith("shield_")?Math.min(0.95,a.value()*10):a.value()*10));}
        abilities.removeIf(a->a.skill()!=Skill.defense&&!a.id().equals("faustcore:second_wind"));
        for(int i=0;i<abilities.size();i++){var a=abilities.get(i);if(a.id().equals("faustcore:armor_familiarity")||a.id().equals("faustcore:armor_mastery"))abilities.set(i,new Ability(a.id(),a.skill(),a.level(),a.counter(),a.amount(),a.parent(),"capacity",a.id().endsWith("familiarity")?10:4));}
        stat("constitution_stamina",Skill.constitution,"max_health","add",1,1,1e9,false);statBonuses.getLast().attribute="faustcore:stamina";
        stat("building_breaking",Skill.building,"max_health","add",0.01,1,1e9,false);statBonuses.getLast().attribute="faustcore:breaking";
        for(var rule:statBonuses){String id="training_"+rule.id;rule.requiresAbility="faustcore:"+id;String counter=switch(rule.skill.name()){case "strength"->"hits";case "constitution"->"damage_taken";case "dexterity"->"run_metres";case "defense"->"blocks";case "building"->"placed";default->"books";};String perk=rule.id.startsWith("building_")&&rule.id.endsWith("reach")?"training_building_reach":id;rule.requiresAbility="faustcore:"+perk;if(abilities.stream().noneMatch(a->a.id().equals(rule.requiresAbility)))ability(perk,rule.skill,1,counter,1,"","stat_unlock",0);}
        ability("shield_efficiency",Skill.defense,8,"blocks",60,"shield_care","shield_cost_reduction",1);
        ability("resilient_recovery",Skill.constitution,10,"constitution_run_metres",5000,"second_wind","exhaustion_threshold_reduction",0.5);
        ability("bow_efficiency",Skill.dexterity,5,"bow_shots",30,"","bow_cost_reduction",1);
        ability("bow_mastery",Skill.dexterity,10,"bow_shots",150,"bow_efficiency","bow_cost_reduction",1);
        ability("crossbow_efficiency",Skill.strength,5,"crossbow_shots",30,"","crossbow_cost_reduction",1);
        ability("mace_efficiency",Skill.strength,10,"mace_hits",50,"training_strength_damage","mace_cost_reduction",1);
        for(int i=0;i<abilities.size();i++)abilities.set(i,tuneAbility(abilities.get(i)));
        stat("dexterity_safe_fall",Skill.dexterity,"safe_fall_distance","add",1,5,1e9,false);
        statBonuses.getLast().requiresAbility="faustcore:soft_landing";
        ability("cushioned_landing",Skill.dexterity,5,"run_metres",1500,"soft_landing","fall_damage_reduction",0.5);
        ability("soft_landing",Skill.dexterity,5,"run_metres",1500,"","stat_unlock",0);
        ability("enchanting_power",Skill.intelligence,5,"enchants",1,"","enchanting_power",1);
        ability("potion_duration",Skill.intelligence,5,"brewed",1,"","potion_duration",0.01);
        trees=SkillTrees.defaults(abilities);


    }
    private static Ability tuneAbility(Ability a) {
        int level=a.level();String counter=a.counter(),parent=a.parent();double amount=a.amount(),value=a.value();
        switch(a.id()) {
            case "faustcore:training_strength_damage" -> {level=5;amount=100;}
            case "faustcore:training_dexterity_sprint" -> {level=5;amount=1500;}
            case "faustcore:training_dexterity_attack" -> {level=8;counter="hits";amount=150;parent="faustcore:training_dexterity_sprint";}
            case "faustcore:training_constitution_health" -> {level=5;amount=100;}
            case "faustcore:training_constitution_stamina" -> {level=8;counter="constitution_run_metres";amount=2500;parent="faustcore:training_constitution_health";}
            case "faustcore:training_defense_armor" -> {level=8;counter="armor_metres";amount=1500;parent="faustcore:armor_familiarity";}
            case "faustcore:training_intelligence_luck" -> {level=5;amount=15;}
            case "faustcore:training_building_breaking" -> {level=5;amount=500;}
            case "faustcore:training_building_reach" -> {level=10;amount=1500;parent="faustcore:training_building_breaking";}
            case "faustcore:shield_care" -> value=0.10;
            case "faustcore:shield_recovery" -> value=0.15;
        }
        return new Ability(a.id(),a.skill(),level,counter,amount,parent,a.effect(),value,a.requiresAll(),a.requiresAny(),a.excludes(),a.characteristics(),a.conditions(),a.conditionMode(),a.conditionsRequired(),a.requiresCount(),a.toggleable());
    }
    private void action(String id, Skill skill, double xp, String counter) { actions.put(id,new Rule(skill,xp,counter)); }
    private void ability(String id, Skill skill,int level,String counter,double amount,String parent,String effect,double value) {
        abilities.add(new Ability("faustcore:"+id,skill,level,counter,amount,parent.isEmpty()?"":"faustcore:"+parent,effect,value));
    }
    private static Map<String,Double> numbers(Object... pairs) {
        Map<String,Double> map=new LinkedHashMap<>();
        for(int i=0;i<pairs.length;i+=2) map.put((String)pairs[i],((Number)pairs[i+1]).doubleValue());
        return map;
    }
    public void validate() {
        Balance defaults=new Balance();
        try {
            for(var field:Balance.class.getFields()) {
                if(java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                Object value=field.get(this);
                if(value==null) throw new IllegalArgumentException("Missing "+field.getName());
                if(value instanceof Map<?,?> map && !field.getName().equals("actions")) {
                    Map<?,?> required=(Map<?,?>)field.get(defaults);
                    if(!field.getName().equals("sweetFoods")&&!map.keySet().containsAll(required.keySet())) throw new IllegalArgumentException("Missing settings in "+field.getName());
                    for(Object n:map.values()) if(!(n instanceof Double d)||!Double.isFinite(d)||d<0||d>1e9) throw new IllegalArgumentException("Invalid number in "+field.getName());
                }
            }
        } catch(IllegalAccessException e) { throw new IllegalStateException(e); }
        if(progression.get("levelBase")<=0 || progression.get("capStep")<=0 || progression.get("costDivisor")<=0 || progression.get("maximumCap")<progression.get("initialCap") || progression.get("maximumCap")>20 || progression.get("maximumGrowth")<=0 || progression.get("maximumGrowth")>2 || stamina.get("base")<=0 || fatigue.get("minimum")<=0 || fatigue.get("minimum")>stamina.get("base") || fatigue.get("maximum")>stamina.get("base")-fatigue.get("minimum")) throw new IllegalArgumentException("Invalid progression/stamina bounds");
        if(skills.get("iconTier1")>skills.get("iconTier2")||skills.get("iconTier2")>skills.get("iconTier3"))throw new IllegalArgumentException("Invalid skill stat/ icon bounds");
        if(progression.get("overCapGrowth")>1)throw new IllegalArgumentException("Over-cap growth exceeds normal growth");
        if(stamina.get("exhaustionTrigger")>stamina.get("base"))throw new IllegalArgumentException("Exhaustion trigger exceeds base stamina");
        if(stamina.get("airRecoveryMultiplier")>1||stamina.get("exhaustionRecovery")<=0)throw new IllegalArgumentException("Invalid recovery settings");
        if(fatigue.get("campfireRadius")>16||client.get("hudWidth")>2000)throw new IllegalArgumentException("Invalid radius/HUD/key bound");
        for(var r:actions.values()) if(r==null||r.skill()==null||r.counter()==null||!Double.isFinite(r.xp())||r.xp()<0) throw new IllegalArgumentException("Invalid action");
        for(var rule:statBonuses){if(rule==null||rule.id==null||rule.skill==null)continue;if(rule.requiresAbility==null||rule.requiresAbility.isEmpty())rule.requiresAbility="faustcore:training_"+rule.id;String gate=rule.requiresAbility;if(abilities.stream().noneMatch(a->a.id().equals(gate))){String counter=actions.values().stream().filter(a->a.skill()==rule.skill).map(Rule::counter).findFirst().orElse("progress");abilities.add(new Ability(gate,rule.skill,5,counter,100,"","stat_unlock",0));}}
        Set<String> statIds=new HashSet<>();
        for(var rule:statBonuses)if(rule==null||rule.id==null||!rule.id.matches("[a-z0-9_./-]+")||!statIds.add(rule.id)||rule.skill==null||rule.requiresAbility==null||rule.attribute==null||!rule.attribute.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||!Set.of("add","multiply_base","multiply_total").contains(rule.operation)||rule.everyLevels<1||!Double.isFinite(rule.perLevel)||rule.perLevel<0||!Double.isFinite(rule.maximum)||rule.maximum<0)throw new IllegalArgumentException("Invalid stat bonus rule");
        Set<String> ids=new HashSet<>();
        for(var a:abilities) {
            if(a==null||a.id()==null||!a.id().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||!ids.add(a.id())||a.skill()==null||a.level()<0||a.level()>20||a.counter()==null||a.parent()==null||!(BonusEffects.known(a.effect())||(a.effect()!=null&&a.effect().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")))||!Double.isFinite(a.amount())||a.amount()<0||!Double.isFinite(a.value())||a.value()<0) throw new IllegalArgumentException("Invalid ability");
            if((Set.of("shield_durability","shield_recovery").contains(a.effect()))&&a.value()>0.95) throw new IllegalArgumentException("Shield discount exceeds 95%");
        }
        for(var a:abilities){
            var seen=new HashSet<Skill>();
            for(var r:a.skillRequirements())if(r.skill()==null||r.level()<0||r.level()>20||!seen.add(r.skill()))throw new IllegalArgumentException("Invalid characteristics: "+a.id());
            if(!Set.of("all","at_least").contains(a.conditionMode())||a.conditionsRequired()<0||a.requiredConditions()<1||a.requiredConditions()>a.actionConditions().size()||a.requiresCount()<0||a.requiresCount()>a.requiresAny().size())throw new IllegalArgumentException("Invalid condition count: "+a.id());
            var counters=new HashSet<String>();
            for(var c:a.actionConditions())if(c.counter()==null||!Double.isFinite(c.amount())||c.amount()<0||!counters.add(c.counter()))throw new IllegalArgumentException("Invalid condition: "+a.id());
        }
        SkillTrees.validate(this);
    }
    private static void migrate(Path directory) throws IOException {
        Path old=directory.resolve("progression.json");
        if(!Files.exists(old)) return;
        var tree=JSON.fromJson(Files.readString(old),com.google.gson.JsonObject.class);
        if(tree==null||!tree.has("purchaseGrowth"))return;
        Path backup=directory.resolve("migration-v1");Files.createDirectories(backup);
        for(String group:List.of("progression","stamina","fatigue","abilities","client")) {
            Path file=directory.resolve(group+".json");if(!Files.exists(file))continue;
            if(!Files.exists(backup.resolve(file.getFileName())))Files.copy(file,backup.resolve(file.getFileName()));
        }
        tree.remove("purchaseBase");tree.remove("purchaseGrowth");tree.addProperty("capStep",1);tree.addProperty("maximumCap",20);tree.addProperty("costDivisor",5);
        Files.writeString(old,JSON.toJson(tree)+"\n");
        double migratedBase=20;Path staminaFile=directory.resolve("stamina.json");if(Files.exists(staminaFile)){var data=JSON.fromJson(Files.readString(staminaFile),com.google.gson.JsonObject.class);if(data.has("base"))migratedBase=data.get("base").getAsDouble()*0.2;}
        for(String group:List.of("stamina","fatigue")) {
            Path file=directory.resolve(group+".json");if(!Files.exists(file))continue;
            var data=JSON.fromJson(Files.readString(file),com.google.gson.JsonObject.class);
            for(String key:(group.equals("stamina")?List.of("base","constitutionPerLevel","regeneration","armorCost","waterCost"):List.of("sleepPerSecond","campfirePerSecond")))
                if(data.has(key))data.addProperty(key,data.get(key).getAsDouble()*(group.equals("stamina")?0.2:migratedBase));
            if(group.equals("fatigue")){if(data.has("maximum"))data.addProperty("maximum",data.get("maximum").getAsDouble()*migratedBase);data.addProperty("minimum",migratedBase*0.2);}
            Files.writeString(file,JSON.toJson(data)+"\n");
        }
        Path clientFile=directory.resolve("client.json");if(Files.exists(clientFile)){var client=JSON.fromJson(Files.readString(clientFile),com.google.gson.JsonObject.class);client.remove("openKey");Files.writeString(clientFile,JSON.toJson(client)+"\n");}
        Path abilities=directory.resolve("abilities.json");if(Files.exists(abilities)) {
            var list=JSON.fromJson(Files.readString(abilities),com.google.gson.JsonArray.class);
            for(var item:list) {var a=item.getAsJsonObject();String effect=a.get("effect").getAsString();if(effect.equals("stamina")||effect.equals("regeneration"))a.addProperty("value",a.get("value").getAsDouble()*0.2);}
            Files.writeString(abilities,JSON.toJson(list)+"\n");
        }
    }
    private static void migrateExhaustion(Path directory) throws IOException {
        Path file=directory.resolve("stamina.json");if(!Files.exists(file))return;
        var data=JSON.fromJson(Files.readString(file),com.google.gson.JsonObject.class);
        if(data==null||data.has("exhaustionLock"))return;
        Path backup=directory.resolve("migration-v1.1-resource");Files.createDirectories(backup);
        if(!Files.exists(backup.resolve("stamina.json")))Files.copy(file,backup.resolve("stamina.json"));
        if(data.has("armorCost")&&Math.abs(data.get("armorCost").getAsDouble()-0.1)<1e-9)data.addProperty("armorCost",0.25);
        data.addProperty("exhaustionLock",1);Files.writeString(file,JSON.toJson(data)+"\n");
    }
    private static void migrateGrowth(Path directory) throws IOException {
        Path file=directory.resolve("progression.json");if(!Files.exists(file))return;
        var data=JSON.fromJson(Files.readString(file),com.google.gson.JsonObject.class);if(data==null||data.has("maximumGrowth"))return;
        Path backup=directory.resolve("migration-v1.1-resource");Files.createDirectories(backup);
        if(!Files.exists(backup.resolve("progression.json")))Files.copy(file,backup.resolve("progression.json"));
        data.addProperty("maximumGrowth",2);Files.writeString(file,JSON.toJson(data)+"\n");
    }
    private static void migrateStats(Path directory) throws IOException {
        Path file=directory.resolve("skills.json");if(!Files.exists(file))return;
        var data=JSON.fromJson(Files.readString(file),com.google.gson.JsonObject.class);if(data==null||data.has("healthPerLevel")||Files.exists(directory.resolve("statBonuses.json")))return;
        Path backup=directory.resolve("migration-v1.1-resource");Files.createDirectories(backup);
        if(!Files.exists(backup.resolve("skills.json")))Files.copy(file,backup.resolve("skills.json"));
        Balance defaults=new Balance();
        for(String key:List.of("healthPerLevel","luckPerLevel","luckMaximum","reachEveryLevels","reachPerStep","iconTier1","iconTier2","iconTier3"))if(defaults.skills.containsKey(key))data.addProperty(key,defaults.skills.get(key));
        if(data.has("breakingPerLevel")&&Math.abs(data.get("breakingPerLevel").getAsDouble()-0.003)<1e-9)data.addProperty("breakingPerLevel",0.01);
        if(data.has("breakingMaximum")&&Math.abs(data.get("breakingMaximum").getAsDouble()-0.15)<1e-9)data.addProperty("breakingMaximum",1000000000);
        Files.writeString(file,JSON.toJson(data)+"\n");
    }
    private static void migrateV12(Path directory) throws IOException {
        Path attackFile=directory.resolve("stamina.json");if(Files.exists(attackFile)){var attack=JSON.fromJson(Files.readString(attackFile),com.google.gson.JsonObject.class);if(!attack.has("attackCost")){attack.addProperty("attackCost",1);Files.writeString(attackFile,JSON.toJson(attack)+"\n");}}
        Path marker=directory.resolve("statBonuses.json");if(Files.exists(directory.resolve(".balance-format"))||Files.exists(marker)||!Files.exists(directory.resolve("abilities.json")))return;
        Path backup=directory.resolve("migration-v1.2");Files.createDirectories(backup);
        for(String name:List.of("skills","stamina","abilities","progression")){Path file=directory.resolve(name+".json");if(Files.exists(file)&&!Files.exists(backup.resolve(name+".json")))Files.copy(file,backup.resolve(name+".json"));}
        Path progression=directory.resolve("progression.json");if(Files.exists(progression)){var data=JSON.fromJson(Files.readString(progression),com.google.gson.JsonObject.class);data.addProperty("trainingMultiplier",2);Files.writeString(progression,JSON.toJson(data)+"\n");}
        Path stamina=directory.resolve("stamina.json");if(Files.exists(stamina)){var data=JSON.fromJson(Files.readString(stamina),com.google.gson.JsonObject.class);if(data.has("constitutionPerLevel")&&Math.abs(data.get("constitutionPerLevel").getAsDouble()-0.2)<1e-9)data.addProperty("constitutionPerLevel",1);Files.writeString(stamina,JSON.toJson(data)+"\n");}
        Path abilities=directory.resolve("abilities.json");if(Files.exists(abilities)){var data=JSON.fromJson(Files.readString(abilities),com.google.gson.JsonArray.class);for(var item:data){var a=item.getAsJsonObject();double value=a.get("value").getAsDouble()*10;if(a.get("effect").getAsString().startsWith("shield_"))value=Math.min(0.95,value);a.addProperty("value",value);}Files.writeString(abilities,JSON.toJson(data)+"\n");}
        Files.writeString(marker,JSON.toJson(new Balance().statBonuses)+"\n");
    }
    private static void migrateEnvironment(Path directory) throws IOException {
        Path stamina=directory.resolve("stamina.json");if(!Files.exists(stamina))return;
        var st=JSON.fromJson(Files.readString(stamina),com.google.gson.JsonObject.class);if(st.has("runCost"))return;
        Path backup=directory.resolve("migration-v1.2-environment");Files.createDirectories(backup);
        for(String name:List.of("stamina","fatigue","progression","statBonuses")){Path file=directory.resolve(name+".json");if(Files.exists(file)&&!Files.exists(backup.resolve(name+".json")))Files.copy(file,backup.resolve(name+".json"));}
        Path hud=directory.resolve("client.json");if(Files.exists(hud)){var data=JSON.fromJson(Files.readString(hud),com.google.gson.JsonObject.class);if(data.has("hudX")&&data.get("hudX").getAsDouble()==8)data.addProperty("hudX",0);if(data.has("hudY")&&data.get("hudY").getAsDouble()==8)data.addProperty("hudY",0);if(data.has("hudWidth")&&data.get("hudWidth").getAsDouble()==140)data.addProperty("hudWidth",81);Files.writeString(hud,JSON.toJson(data)+"\n");}
        Path perks=directory.resolve("abilities.json");if(Files.exists(perks)){var list=JSON.fromJson(Files.readString(perks),com.google.gson.JsonArray.class);for(var item:list){var a=item.getAsJsonObject();if(a.get("id").getAsString().equals("faustcore:second_wind")){a.addProperty("effect","second_wind");a.addProperty("value",1);}}Files.writeString(perks,JSON.toJson(list)+"\n");}
        st.addProperty("runCost",0.1);Files.writeString(stamina,JSON.toJson(st)+"\n");
        Path file=directory.resolve("fatigue.json");if(Files.exists(file)){var data=JSON.fromJson(Files.readString(file),com.google.gson.JsonObject.class);data.addProperty("soulCampfirePerSecond",0.04);data.addProperty("soulFirePerSecond",0.1);Files.writeString(file,JSON.toJson(data)+"\n");}
        file=directory.resolve("progression.json");if(Files.exists(file)){var data=JSON.fromJson(Files.readString(file),com.google.gson.JsonObject.class);if(data.has("slowdown"))data.addProperty("slowdown",data.get("slowdown").getAsDouble()*2);Files.writeString(file,JSON.toJson(data)+"\n");}
        file=directory.resolve("statBonuses.json");if(Files.exists(file)){var list=new java.util.ArrayList<StatBonus>(java.util.Arrays.asList(JSON.fromJson(Files.readString(file),StatBonus[].class)));for(var r:list)if(r.skill==Skill.building&&(r.attribute.equals("minecraft:player.block_interaction_range")||r.attribute.equals("minecraft:player.entity_interaction_range")))r.maximum=Math.min(3,r.maximum);if(list.stream().noneMatch(r->r.id.equals("defense_armor")))list.add(new Balance().statBonuses.stream().filter(r->r.id.equals("defense_armor")).findFirst().orElseThrow());Files.writeString(file,JSON.toJson(list)+"\n");}
    }
    private static void migrateV13(Path directory) throws IOException {
        if(Files.exists(directory.resolve(".format-1.3"))||!Files.exists(directory.resolve("abilities.json")))return;
        Path backup=directory.resolve("migration-v1.3");Files.createDirectories(backup);Balance defaults=new Balance();
        for(String name:List.of("stamina","skills","armor","abilities","statBonuses")){Path file=directory.resolve(name+".json");if(Files.exists(file)&&!Files.exists(backup.resolve(name+".json")))Files.copy(file,backup.resolve(name+".json"));}
        for(String group:List.of("stamina","skills","armor")){Path file=directory.resolve(group+".json");if(!Files.exists(file))continue;var data=JSON.fromJson(Files.readString(file),com.google.gson.JsonObject.class);var base=JSON.fromJson(defaults.group(group),com.google.gson.JsonObject.class);for(var entry:base.entrySet())if(!data.has(entry.getKey()))data.add(entry.getKey(),entry.getValue());if(group.equals("armor")){data.addProperty("baseCapacity",0);data.addProperty("capacityPerLevel",0);}Files.writeString(file,JSON.toJson(data)+"\n");}
        // Preserve external/custom abilities; retire only original built-in nodes requested for removal.
        Path file=directory.resolve("abilities.json");var old=JSON.fromJson(Files.readString(file),Ability[].class);var merged=new ArrayList<>(defaults.abilities);Set<String> retired=Set.of("controlled_strike","strong_grip","enduring_body","steady_pace","sure_footing","applied_knowledge","deep_understanding","tool_handling","master_builder");for(var a:old)if(!retired.contains(a.id().replace("faustcore:",""))&&merged.stream().noneMatch(n->n.id().equals(a.id())))merged.add(a);Files.writeString(file,JSON.toJson(merged)+"\n");
        file=directory.resolve("statBonuses.json");var rules=new ArrayList<>(Arrays.asList(JSON.fromJson(Files.readString(file),StatBonus[].class)));for(var rule:rules){var match=defaults.statBonuses.stream().filter(r->r.id.equals(rule.id)).findFirst();rule.requiresAbility=match.isPresent()?match.get().requiresAbility:"";}for(var rule:defaults.statBonuses)if(rules.stream().noneMatch(r->r.id.equals(rule.id)))rules.add(rule);Files.writeString(file,JSON.toJson(rules)+"\n");
    }
    private static void migrateV14(Path directory) throws IOException {
        if(Files.exists(directory.resolve(".format-1.4"))||!Files.exists(directory.resolve("progression.json")))return;
        Balance defaults=new Balance();Path backup=directory.resolve("migration-v1.4");Files.createDirectories(backup);
        for(String name:List.of("progression","stamina","fatigue","skills","actions","abilities")) {
            Path file=directory.resolve(name+".json");if(!Files.exists(file))continue;
            if(!Files.exists(backup.resolve(file.getFileName())))Files.copy(file,backup.resolve(file.getFileName()));
            if(name.equals("abilities")) {
                var list=new ArrayList<>(Arrays.asList(JSON.fromJson(Files.readString(file),Ability[].class)));
                for(int i=0;i<list.size();i++)list.set(i,tuneAbility(list.get(i)));
                for(var a:defaults.abilities)if(list.stream().noneMatch(old->old.id().equals(a.id())))list.add(a);
                // New shield economy node now follows shield care.
                for(int i=0;i<list.size();i++)if(list.get(i).id().equals("faustcore:shield_efficiency"))list.set(i,defaults.abilities.stream().filter(a->a.id().equals("faustcore:shield_efficiency")).findFirst().orElseThrow());
                Files.writeString(file,JSON.toJson(list)+"\n");continue;
            }
            var data=JSON.fromJson(Files.readString(file),com.google.gson.JsonObject.class);var base=JSON.fromJson(defaults.group(name),com.google.gson.JsonObject.class);
            for(var e:base.entrySet())if(!data.has(e.getKey()))data.add(e.getKey(),e.getValue());
            if(name.equals("progression")&&data.get("initialCap").getAsDouble()==10)data.addProperty("initialCap",5);
            if(name.equals("stamina")){
                data.addProperty("exhaustionLock",1);
                for(var change:Map.of("runCost",0.2,"armorCost",0.5,"sprintJumpCost",1.0,"jumpArmorCost",0.1).entrySet())if(data.has(change.getKey())&&Math.abs(data.get(change.getKey()).getAsDouble()-change.getValue())<1e-9)data.addProperty(change.getKey(),change.getValue()/2);
            }
            Files.writeString(file,JSON.toJson(data)+"\n");
        }
    }
    private static void migrateV141(Path directory) throws IOException {
        if(Files.exists(directory.resolve(".format-1.4.1")))return;
        Path backup=directory.resolve("migration-v1.4.1");
        for(String group:List.of("stamina","fatigue","statBonuses","progression")){
            Path file=directory.resolve(group+".json");if(!Files.exists(file))continue;
            Files.createDirectories(backup);if(!Files.exists(backup.resolve(file.getFileName())))Files.copy(file,backup.resolve(file.getFileName()));
            if(group.equals("statBonuses")){
                var rules=new ArrayList<>(Arrays.asList(JSON.fromJson(Files.readString(file),StatBonus[].class)));
                rules.removeIf(r->r.skill==Skill.building&&r.attribute.equals("minecraft:player.entity_interaction_range"));
                Files.writeString(file,JSON.toJson(rules)+"\n");continue;
            }
            var data=JSON.fromJson(Files.readString(file),com.google.gson.JsonObject.class);
            if(group.equals("progression")){if(!data.has("overCapGrowth"))data.addProperty("overCapGrowth",0.33);}
            else if(group.equals("stamina")){if(!data.has("exhaustionTrigger"))data.addProperty("exhaustionTrigger",1);data.addProperty("exhaustionLock",1);}
            else if(!data.has("accumulationMultiplier"))data.addProperty("accumulationMultiplier",0.5);
            Files.writeString(file,JSON.toJson(data)+"\n");
        }
    }
    private static void migrateFatigueGrace(Path directory) throws IOException {
        Path file=directory.resolve("fatigue.json");if(!Files.exists(file))return;
        var data=JSON.fromJson(Files.readString(file),com.google.gson.JsonObject.class);if(data.has("worldGraceSeconds"))return;
        Path backup=directory.resolve("migration-v1.5.1");Files.createDirectories(backup);if(!Files.exists(backup.resolve("fatigue.json")))Files.copy(file,backup.resolve("fatigue.json"));
        data.addProperty("worldGraceSeconds",300);Files.writeString(file,JSON.toJson(data)+"\n");
    }
    private static void migrateCakeFood(Path directory) throws IOException {
        Path file=directory.resolve("sweetFoods.json");if(Files.exists(directory.resolve(".cake-format-1.5.2"))||!Files.exists(file))return;
        var data=JSON.fromJson(Files.readString(file),com.google.gson.JsonObject.class);if(data.has("minecraft:cake"))return;
        Path backup=directory.resolve("migration-v1.5.2");Files.createDirectories(backup);if(!Files.exists(backup.resolve("sweetFoods.json")))Files.copy(file,backup.resolve("sweetFoods.json"));
        data.addProperty("minecraft:cake",1);Files.writeString(file,JSON.toJson(data)+"\n");
    }
    private static void migratePractice(Path directory) throws IOException {
        if(Files.exists(directory.resolve(".practice-format")))return;
        Balance defaults=new Balance();Path backup=directory.resolve("migration-practice");
        for(String group:List.of("actions","stamina","abilities","statBonuses")){
            Path file=directory.resolve(group+".json");if(!Files.exists(file))continue;
            Files.createDirectories(backup);if(!Files.exists(backup.resolve(group+".json")))Files.copy(file,backup.resolve(group+".json"));
            var json=com.google.gson.JsonParser.parseString(Files.readString(file));
            if(group.equals("actions")){var obj=json.getAsJsonObject();obj.remove("structure");if(obj.has("craft"))obj.getAsJsonObject("craft").addProperty("skill","intelligence");if(!obj.has("brew"))obj.add("brew",JSON.toJsonTree(defaults.actions.get("brew")));}
            if(group.equals("stamina")&&!json.getAsJsonObject().has("throwCost"))json.getAsJsonObject().addProperty("throwCost",1);
            if(group.equals("abilities"))for(var a:defaults.abilities)if(Set.of("faustcore:soft_landing","faustcore:enchanting_power","faustcore:potion_duration").contains(a.id())&&java.util.stream.StreamSupport.stream(json.getAsJsonArray().spliterator(),false).noneMatch(e->e.getAsJsonObject().get("id").getAsString().equals(a.id())))json.getAsJsonArray().add(JSON.toJsonTree(a));
            if(group.equals("statBonuses")&&java.util.stream.StreamSupport.stream(json.getAsJsonArray().spliterator(),false).noneMatch(e->e.getAsJsonObject().get("id").getAsString().equals("dexterity_safe_fall")))json.getAsJsonArray().add(JSON.toJsonTree(defaults.statBonuses.getLast()));
            Files.writeString(file,JSON.toJson(json)+"\n");
        }
        Files.writeString(directory.resolve(".practice-format"),"1\n");
    }
    private static void migratePracticeLayout(Path directory) throws IOException {
        Path marker=directory.resolve(".practice-layout-2"),file=directory.resolve("trees.json");
        if(Files.exists(marker)||!Files.exists(file))return;
        var trees=new ArrayList<SkillTrees.Tree>(Arrays.asList(JSON.fromJson(Files.readString(file),SkillTrees.Tree[].class)));
        for(int i=0;i<trees.size();i++){
            var tree=trees.get(i);var nodes=new ArrayList<>(tree.nodes());
            String root=tree.skill()==Skill.intelligence?"faustcore:training_intelligence_luck":tree.skill()==Skill.dexterity?"faustcore:training_dexterity_sprint":"";
            if(root.isEmpty())continue;
            var anchor=nodes.stream().filter(n->n.ability().equals(root)).findFirst().orElse(new SkillTrees.Node(root,0,0,"minecraft:book"));
            var ids=tree.skill()==Skill.intelligence?List.of("faustcore:potion_duration","faustcore:enchanting_power"):List.of("faustcore:soft_landing");
            for(int j=0;j<ids.size();j++){
                String id=ids.get(j);String icon=nodes.stream().filter(n->n.ability().equals(id)).map(SkillTrees.Node::icon).findFirst().orElse(id.endsWith("soft_landing")?"minecraft:feather":id.endsWith("potion_duration")?"minecraft:brewing_stand":"minecraft:enchanting_table");
                nodes.removeIf(n->n.ability().equals(id));nodes.add(new SkillTrees.Node(id,anchor.x()-52*(j+1),anchor.y(),icon));
            }
            trees.set(i,new SkillTrees.Tree(tree.skill(),nodes));
        }
        Path backup=directory.resolve("migration-practice-layout");Files.createDirectories(backup);if(!Files.exists(backup.resolve("trees.json")))Files.copy(file,backup.resolve("trees.json"));
        Files.writeString(file,JSON.toJson(trees)+"\n");Files.writeString(marker,"1\n");
    }
    private static void migratePanelUpdate(Path directory) throws IOException {
        Path marker=directory.resolve(".panel-update-format");if(Files.exists(marker))return;
        Balance defaults=new Balance();
        for(String group:List.of("abilities","sweetFoods","trees")){
            Path file=directory.resolve(group+".json");if(!Files.exists(file))continue;
            Path backup=directory.resolve("migration-panel-update");Files.createDirectories(backup);
            if(!Files.exists(backup.resolve(file.getFileName())))Files.copy(file,backup.resolve(file.getFileName()));
            var json=com.google.gson.JsonParser.parseString(Files.readString(file));
            if(group.equals("abilities")){
                var list=json.getAsJsonArray();if(java.util.stream.StreamSupport.stream(list.spliterator(),false).noneMatch(e->e.getAsJsonObject().get("id").getAsString().equals("faustcore:cushioned_landing")))list.add(JSON.toJsonTree(defaults.abilities.stream().filter(a->a.id().equals("faustcore:cushioned_landing")).findFirst().orElseThrow()));
            }else if(group.equals("sweetFoods")){
                for(String id:List.of("minecraft:golden_apple","minecraft:golden_carrot","minecraft:glistering_melon_slice"))if(!json.getAsJsonObject().has(id))json.getAsJsonObject().addProperty(id,defaults.sweetFoods.get(id));
            }else{
                for(var element:json.getAsJsonArray()){
                    var tree=element.getAsJsonObject();if(!tree.get("skill").getAsString().equals("dexterity"))continue;
                    var nodes=tree.getAsJsonArray("nodes");boolean exists=false;double x=-52,y=0;
                    for(var n:nodes){var node=n.getAsJsonObject();String id=node.get("ability").getAsString();if(id.equals("faustcore:cushioned_landing"))exists=true;if(id.equals("faustcore:soft_landing")){x=node.get("x").getAsDouble();y=node.get("y").getAsDouble();}}
                    if(!exists)nodes.add(JSON.toJsonTree(new SkillTrees.Node("faustcore:cushioned_landing",x,y+40,"minecraft:feather")));
                }
            }
            Files.writeString(file,JSON.toJson(json)+"\n");
        }
        Files.writeString(marker,"1\n");
    }
    public static Balance load(Path directory) throws IOException {
        Files.createDirectories(directory); migrate(directory); migrateExhaustion(directory); migrateGrowth(directory); migrateStats(directory); migrateV12(directory); migrateEnvironment(directory); migrateV13(directory); migrateV14(directory); migrateV141(directory); migrateFatigueGrace(directory); migrateCakeFood(directory); migratePractice(directory); migratePracticeLayout(directory); migratePanelUpdate(directory);
        Path windFile=directory.resolve("stamina.json");if(Files.exists(windFile)){var data=JSON.fromJson(Files.readString(windFile),com.google.gson.JsonObject.class);if(!data.has("secondWindMultiplier")){data.addProperty("secondWindMultiplier",1.5);Files.writeString(windFile,JSON.toJson(data)+"\n");}}
        Path skillsFile=directory.resolve("skills.json");if(Files.exists(skillsFile)){var data=JSON.fromJson(Files.readString(skillsFile),com.google.gson.JsonObject.class);if(!data.has("buildingReachMaximum")){data.addProperty("buildingReachMaximum",3);Files.writeString(skillsFile,JSON.toJson(data)+"\n");}}
        Balance result=new Balance();
        try {
            for(var f:Balance.class.getFields()) {
                if(java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                Path file=directory.resolve(f.getName()+".json");
                if(Files.exists(file)) f.set(result,JSON.fromJson(Files.readString(file),f.getGenericType()));
                else if(!f.getName().equals("trees"))Files.writeString(file,JSON.toJson(f.get(result)) + "\n",StandardOpenOption.CREATE_NEW);
            }
        } catch(ReflectiveOperationException|RuntimeException e) { throw new IOException("Invalid core configuration",e); }
        if(!Files.exists(directory.resolve("trees.json"))){result.trees=SkillTrees.defaults(result.abilities);Files.writeString(directory.resolve("trees.json"),JSON.toJson(result.trees)+"\n");}
        try {result.validate();} catch(RuntimeException e) {throw new IOException("Invalid core balance",e);}
        Files.writeString(directory.resolve(".balance-format"),"1.2\n");Files.writeString(directory.resolve(".format-1.3"),"1\n");Files.writeString(directory.resolve(".format-1.4"),"1\n");Files.writeString(directory.resolve(".format-1.4.1"),"1\n");Files.writeString(directory.resolve(".cake-format-1.5.2"),"1\n");
        return result;
    }
    public static List<String> groups() {
        return Arrays.stream(Balance.class.getFields()).filter(f->!java.lang.reflect.Modifier.isStatic(f.getModifiers())).map(java.lang.reflect.Field::getName).toList();
    }
    public String group(String name) {
        if(!groups().contains(name))throw new IllegalArgumentException("Unknown config section");
        try{return JSON.toJson(Balance.class.getField(name).get(this));}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    public Balance edited(String name,String text) {
        if(!groups().contains(name))throw new IllegalArgumentException("Unknown config section");
        var root=JSON.toJsonTree(this).getAsJsonObject();root.add(name,com.google.gson.JsonParser.parseString(text));
        Balance result=JSON.fromJson(root,Balance.class);result.validate();return result;
    }
    public static void saveGroup(Path directory,String name,String text) throws IOException {
        Balance candidate=load(directory).edited(name,text);
        Path temp=Files.createTempFile(directory,".core-config-",".tmp");
        try {
            Files.writeString(temp,candidate.group(name)+"\n");
            try{Files.move(temp,directory.resolve(name+".json"),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,directory.resolve(name+".json"),StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temp);}
    }
}
