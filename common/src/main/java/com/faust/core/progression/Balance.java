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
    public Map<String, Double> progression = numbers("levelBase",100,"levelSlope",20,"initialCap",10,"capStep",1,"maximumCap",20,"costDivisor",5,"maximumGrowth",2,"slowdown",0.25);
    public Map<String, Double> stamina = numbers("enabled",1,"base",20,"constitutionPerLevel",0.2,"regeneration",4,"delaySeconds",1.5,"armorCost",0.5,"waterCost",1,"exhaustionLock",1,"flowThreshold",0.05);
    public Map<String, Double> fatigue = numbers("enabled",1,"perSpentFraction",0.05,"maximum",16,"minimum",4,"sleepPerSecond",0.2,"campfirePerSecond",0.04,"campfireRadius",4);
    public Map<String, Double> armor = numbers("baseCapacity",2,"capacityPerLevel",0.5,"fallbackWeight",2,"leather",1,"chainmail",2,"iron",2.5,"golden",2,"diamond",3,"netherite",3.5);
    public Map<String, Double> skills = numbers("damagePerLevel",0.002,"damageMaximum",0.10,"speedPerLevel",0.001,"speedMaximum",0.05,"breakingPerLevel",0.01,"breakingMaximum",1000000000,"hitCharge",0.9,"hitMaximum",10,"shieldMaximum",5,"shieldIntervalTicks",20,"shieldBudgetPerMinute",20,"runMetresPerMinute",200,"blockMaximumPerMinute",300,"readCooldownSeconds",30,"structureIntervalTicks",40,"healthPerLevel",1,"luckPerLevel",0.01,"luckMaximum",0.2,"reachEveryLevels",10,"reachPerStep",1,"iconTier1",5,"iconTier2",10,"iconTier3",20);
    public Map<String, Double> client = numbers("hud",1,"hudX",8,"hudY",8,"hudWidth",140);
    public Map<String, Double> integrations = numbers("weaponDefenseEnabled",0);
    public Map<String, Rule> actions = new LinkedHashMap<>();
    public List<Ability> abilities = new ArrayList<>();

    public record Rule(Skill skill, double xp, String counter) {}
    public record Ability(String id, Skill skill, int level, String counter, double amount,
                          String parent, String effect, double value) {}

    public Balance() {
        action("melee",Skill.strength,1,"hits");
        action("damage",Skill.constitution,1,"damage_taken");
        action("run",Skill.dexterity,0.05,"run_metres");
        action("armor_run",Skill.defense,0.05,"armor_metres");
        action("shield",Skill.defense,1,"blocks");
        action("read",Skill.intelligence,5,"books");
        action("enchant",Skill.intelligence,10,"enchants");
        action("structure",Skill.intelligence,20,"structures");
        action("trade",Skill.intelligence,3,"trades");
        action("place",Skill.building,1,"placed");
        action("craft",Skill.building,1,"crafted");
        ability("controlled_strike",Skill.strength,5,"hits",30,"","damage",0.01);
        ability("strong_grip",Skill.strength,10,"hits",100,"controlled_strike","damage",0.02);
        ability("second_wind",Skill.constitution,5,"damage_taken",50,"","regeneration",0.4);
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
                    if(!map.keySet().containsAll(required.keySet())) throw new IllegalArgumentException("Missing settings in "+field.getName());
                    for(Object n:map.values()) if(!(n instanceof Double d)||!Double.isFinite(d)||d<0||d>1e9) throw new IllegalArgumentException("Invalid number in "+field.getName());
                }
            }
        } catch(IllegalAccessException e) { throw new IllegalStateException(e); }
        if(progression.get("levelBase")<=0 || progression.get("capStep")<=0 || progression.get("costDivisor")<=0 || progression.get("maximumCap")<progression.get("initialCap") || progression.get("maximumCap")>20 || progression.get("maximumGrowth")<=0 || progression.get("maximumGrowth")>2 || stamina.get("base")<=0 || fatigue.get("minimum")<=0 || fatigue.get("minimum")>stamina.get("base") || fatigue.get("maximum")>stamina.get("base")-fatigue.get("minimum")) throw new IllegalArgumentException("Invalid progression/stamina bounds");
        if(skills.get("reachEveryLevels")<=0||skills.get("luckMaximum")>0.2||skills.get("iconTier1")>skills.get("iconTier2")||skills.get("iconTier2")>skills.get("iconTier3"))throw new IllegalArgumentException("Invalid skill stat/ icon bounds");
        if(fatigue.get("campfireRadius")>16||client.get("hudWidth")>2000)throw new IllegalArgumentException("Invalid radius/HUD/key bound");
        for(var r:actions.values()) if(r==null||r.skill()==null||r.counter()==null||!Double.isFinite(r.xp())||r.xp()<0) throw new IllegalArgumentException("Invalid action");
        Set<String> ids=new HashSet<>();
        for(var a:abilities) {
            if(a==null||a.id()==null||!a.id().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||!ids.add(a.id())||a.skill()==null||a.level()<0||a.level()>20||a.counter()==null||a.parent()==null||!(BonusEffects.known(a.effect())||(a.effect()!=null&&a.effect().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")))||!Double.isFinite(a.amount())||a.amount()<0||!Double.isFinite(a.value())||a.value()<0) throw new IllegalArgumentException("Invalid ability");
            if((a.effect().startsWith("shield_"))&&a.value()>0.95) throw new IllegalArgumentException("Shield discount exceeds 95%");
        }
        for(var a:abilities) {
            Set<String> visited=new HashSet<>(); var cursor=a;
            while(!cursor.parent().isEmpty()) {
                if(!visited.add(cursor.id())) throw new IllegalArgumentException("Ability cycle");
                String parent=cursor.parent();
                cursor=abilities.stream().filter(x->x.id().equals(parent)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown parent "+parent));
                if(cursor.skill()!=a.skill()) throw new IllegalArgumentException("Parent belongs to another skill");
            }
        }
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
        if(data.has("armorCost")&&Math.abs(data.get("armorCost").getAsDouble()-0.1)<1e-9)data.addProperty("armorCost",0.5);
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
        var data=JSON.fromJson(Files.readString(file),com.google.gson.JsonObject.class);if(data==null||data.has("healthPerLevel"))return;
        Path backup=directory.resolve("migration-v1.1-resource");Files.createDirectories(backup);
        if(!Files.exists(backup.resolve("skills.json")))Files.copy(file,backup.resolve("skills.json"));
        Balance defaults=new Balance();
        for(String key:List.of("healthPerLevel","luckPerLevel","luckMaximum","reachEveryLevels","reachPerStep","iconTier1","iconTier2","iconTier3"))data.addProperty(key,defaults.skills.get(key));
        if(data.has("breakingPerLevel")&&Math.abs(data.get("breakingPerLevel").getAsDouble()-0.003)<1e-9)data.addProperty("breakingPerLevel",0.01);
        if(data.has("breakingMaximum")&&Math.abs(data.get("breakingMaximum").getAsDouble()-0.15)<1e-9)data.addProperty("breakingMaximum",1000000000);
        Files.writeString(file,JSON.toJson(data)+"\n");
    }
    public static Balance load(Path directory) throws IOException {
        Files.createDirectories(directory); migrate(directory); migrateExhaustion(directory); migrateGrowth(directory); migrateStats(directory); Balance result=new Balance();
        try {
            for(var f:Balance.class.getFields()) {
                if(java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                Path file=directory.resolve(f.getName()+".json");
                if(Files.exists(file)) f.set(result,JSON.fromJson(Files.readString(file),f.getGenericType()));
                else Files.writeString(file,JSON.toJson(f.get(result)) + "\n",StandardOpenOption.CREATE_NEW);
            }
        } catch(ReflectiveOperationException|RuntimeException e) { throw new IOException("Invalid core configuration",e); }
        try {result.validate();} catch(RuntimeException e) {throw new IOException("Invalid core balance",e);}
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
