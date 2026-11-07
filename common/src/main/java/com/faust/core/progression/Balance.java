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
    public Map<String, Double> progression = numbers("levelBase",100,"levelSlope",20,"initialCap",10,"capStep",5,"purchaseBase",100,"purchaseGrowth",1.5,"slowdown",0.25);
    public Map<String, Double> stamina = numbers("enabled",1,"base",100,"constitutionPerLevel",1,"regeneration",20,"delaySeconds",1.5,"armorCost",0.5,"waterCost",5,"flowThreshold",0.05);
    public Map<String, Double> fatigue = numbers("enabled",1,"perSpentFraction",0.05,"maximum",0.8,"sleepPerSecond",0.01,"campfirePerSecond",0.002,"campfireRadius",4);
    public Map<String, Double> armor = numbers("baseCapacity",2,"capacityPerLevel",0.5,"fallbackWeight",2,"leather",1,"chainmail",2,"iron",2.5,"golden",2,"diamond",3,"netherite",3.5);
    public Map<String, Double> skills = numbers("damagePerLevel",0.002,"damageMaximum",0.10,"speedPerLevel",0.001,"speedMaximum",0.05,"breakingPerLevel",0.003,"breakingMaximum",0.15,"hitCharge",0.9,"hitMaximum",10,"shieldMaximum",5,"shieldIntervalTicks",20,"shieldBudgetPerMinute",20,"runMetresPerMinute",200,"blockMaximumPerMinute",300,"readCooldownSeconds",30,"structureIntervalTicks",40);
    public Map<String, Double> client = numbers("hud",1,"hudX",8,"hudY",8,"hudWidth",140,"openKey",75);
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
        ability("second_wind",Skill.constitution,5,"damage_taken",50,"","regeneration",2);
        ability("enduring_body",Skill.constitution,15,"damage_taken",200,"second_wind","stamina",10);
        ability("steady_pace",Skill.dexterity,5,"run_metres",1000,"","speed",0.01);
        ability("sure_footing",Skill.dexterity,10,"run_metres",3000,"steady_pace","speed",0.01);
        ability("armor_familiarity",Skill.defense,5,"armor_metres",500,"","capacity",2);
        ability("armor_mastery",Skill.defense,15,"armor_metres",2000,"armor_familiarity","capacity",4);
        ability("shield_care",Skill.defense,5,"blocks",30,"","shield_durability",0.10);
        ability("shield_recovery",Skill.defense,10,"blocks",100,"shield_care","shield_recovery",0.20);
        ability("applied_knowledge",Skill.intelligence,5,"books",10,"","regeneration",1);
        ability("deep_understanding",Skill.intelligence,15,"enchants",20,"applied_knowledge","regeneration",2);
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
        if(progression.get("levelBase")<=0 || progression.get("capStep")<=0 || progression.get("purchaseGrowth")<1 || stamina.get("base")<=0 || fatigue.get("maximum")>=1) throw new IllegalArgumentException("Invalid progression/stamina bounds");
        if(fatigue.get("campfireRadius")>16||client.get("hudWidth")>2000||client.get("openKey")>348)throw new IllegalArgumentException("Invalid radius/HUD/key bound");
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
    public static Balance load(Path directory) throws IOException {
        Files.createDirectories(directory); Balance result=new Balance();
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
}
