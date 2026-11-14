package com.faust.core.progression;
import com.faust.essence.api.Skill;


import java.util.*;

/** Configurable layouts and dependency graph validation; no platform or client types. */
public final class SkillTrees {
    public static final int PIXELS_PER_UNIT=11;
    public static final int NODE_SIZE=2;
    public static final int GRID=1;
    public static double snap(double value){return Double.isFinite(value)?Math.round(value/GRID)*(double)GRID:value;}
    public record Node(String ability,double x,double y,String icon) {public Node{x=snap(x);y=snap(y);}}
    public static boolean validPosition(Node node,List<Node> nodes){return nodes.stream().filter(n->!n.ability().equals(node.ability())).allMatch(n->Math.hypot(Math.max(0,Math.abs(n.x()-node.x())-NODE_SIZE),Math.max(0,Math.abs(n.y()-node.y())-NODE_SIZE))>=GRID);}
    public static List<Node> spaced(List<Node> nodes){
        var placed=new ArrayList<Node>();
        for(var node:nodes){if(!Double.isFinite(node.x())||!Double.isFinite(node.y()))throw new IllegalArgumentException("Nonfinite tree position");var n=node;while(!validPosition(n,placed)){if(n.y()>4096)throw new IllegalArgumentException("Tree exceeds layout bounds");n=new Node(n.ability(),n.x(),n.y()+GRID,n.icon());}placed.add(n);}return placed;
    }
    public record Tree(Skill skill,List<Node> nodes) {}
    private SkillTrees() {}
    public static List<Tree> defaults(List<Balance.Ability> abilities){
        var trees=new ArrayList<Tree>();
        for(var skill:Skill.values()){
            var nodes=new ArrayList<Node>();
            for(var a:abilities)if(a.skill()==skill){
                double[] pos=switch(a.id().replace("faustcore:","")){
                    case "metabolism" -> new double[]{10,0};
                    case "cat_scent" -> new double[]{-5,4};
                    case "experience_steal" -> new double[]{-9,4};
                    case "silent_step" -> new double[]{-9,8};
                    case "soft_foot" -> new double[]{-9,4};
                    case "forester" -> new double[]{4,0};
                    case "fire_starter" -> new double[]{4,4};
                    case "cushioned_landing" -> new double[]{-5,4};
                    case "soft_landing","potion_duration" -> new double[]{-5,0};
                    case "enchanting_power" -> new double[]{-9,0};
                    case "crossbow_efficiency" -> new double[]{4,0};
                    case "bow_efficiency","second_wind" -> new double[]{5,0};
                    case "mace_efficiency","training_dexterity_attack","training_constitution_health","training_building_reach" -> new double[]{0,4};
                    case "bow_mastery","resilient_recovery","shield_care" -> new double[]{5,4};
                    case "armor_mastery" -> new double[]{-2,4};
                    case "training_defense_armor" -> new double[]{2,4};
                    case "shield_efficiency" -> new double[]{7,0};
                    case "shield_recovery" -> new double[]{9,4};
                    default -> new double[]{0,nodes.isEmpty()?0:7+nodes.size()*3};
                };
                if(Set.of("faustcore:training_strength_damage","faustcore:training_dexterity_sprint","faustcore:training_constitution_stamina","faustcore:training_intelligence_luck","faustcore:training_building_breaking","faustcore:armor_familiarity").contains(a.id()))pos=new double[]{0,0};
                if(a.id().equals("faustcore:training_intelligence_luck"))pos[0]=-1;
                boolean collision;do{collision=false;for(var n:nodes)if(Math.abs(n.x()-pos[0])<NODE_SIZE&&Math.abs(n.y()-pos[1])<NODE_SIZE){pos=new double[]{0,nodes.stream().mapToDouble(Node::y).max().orElse(0)+4};collision=true;break;}}while(collision);
                nodes.add(new Node(a.id(),pos[0],pos[1],icon(a)));
            }
            trees.add(new Tree(skill,spaced(nodes)));
        }
        return trees;
    }
    public static String icon(Balance.Ability a){return "minecraft:"+switch(a.id().replace("faustcore:","")){
        case "metabolism" -> "cooked_porkchop";
        case "cat_scent" -> "cod";
        case "experience_steal" -> "experience_bottle";
        case "silent_step" -> "chainmail_boots";
        case "soft_foot" -> "leather_boots";
        case "forester" -> "oak_sapling";
        case "fire_starter" -> "campfire";
        case "training_strength_damage" -> "iron_sword";
        case "mace_efficiency" -> "mace";
        case "crossbow_efficiency" -> "crossbow";
        case "training_dexterity_sprint" -> "leather_boots";
        case "training_dexterity_attack" -> "feather";
        case "soft_landing","cushioned_landing" -> "feather";
        case "enchanting_power" -> "enchanting_table";
        case "potion_duration" -> "brewing_stand";
        case "bow_efficiency" -> "bow";
        case "bow_mastery" -> "spectral_arrow";
        case "training_constitution_health" -> "golden_apple";
        case "training_constitution_stamina" -> "cooked_beef";
        case "second_wind" -> "sugar";
        case "resilient_recovery" -> "honey_bottle";
        case "armor_familiarity" -> "iron_chestplate";
        case "armor_mastery" -> "netherite_chestplate";
        case "training_defense_armor" -> "iron_helmet";
        case "shield_care" -> "shield";
        case "shield_efficiency" -> "iron_ingot";
        case "shield_recovery" -> "clock";
        case "training_intelligence_luck" -> "enchanted_book";
        case "training_building_breaking" -> "iron_pickaxe";
        case "training_building_reach" -> "spyglass";
        default -> "book";
    };}
    /** Missing layout entries remain accessible, below the explicitly placed nodes. */
    public static List<Node> nodes(Balance b,Skill skill){
        var nodes=new ArrayList<Node>();b.trees.stream().filter(t->t.skill()==skill).findFirst().ifPresent(t->nodes.addAll(t.nodes().stream().filter(n->b.abilities.stream().anyMatch(a->a.id().equals(n.ability())&&a.skill()==skill)).toList()));
        double y=nodes.stream().mapToDouble(Node::y).max().orElse(-4)+4;
        for(var a:b.abilities)if(a.skill()==skill&&nodes.stream().noneMatch(n->n.ability().equals(a.id()))){nodes.add(new Node(a.id(),0,y,icon(a)));y+=4;}
        return nodes;
    }
    public static int nodeColor(Progress p,Balance.Ability a,Balance b){
        if(p.unlocked.contains(a.id()))return p.enabled(a.id(),b)?0xFF83C24A:0xFFFF4040;
        return p.excluded(a,b)?0xFFA64747:p.canUnlock(a,b)?0xFFFFD36A:0xFF858585;
    }
    public static int lineColor(Progress progress,String previous){return progress.unlocked.contains(previous)?0xFFFFFFFF:0xFF000000;}
    public static boolean exclusive(Balance.Ability a,Balance.Ability b){return a.excludes().contains(b.id())||b.excludes().contains(a.id());}
    public static void validate(Balance b){
        var byId=new LinkedHashMap<String,Balance.Ability>();for(var a:b.abilities)byId.put(a.id(),a);
        for(var a:b.abilities){
            for(var ids:List.of(a.requiresAll(),a.requiresAny(),a.excludes()))if(new HashSet<>(ids).size()!=ids.size())throw new IllegalArgumentException("Duplicate dependency in "+a.id());
            var refs=new ArrayList<>(a.dependencies());refs.addAll(a.excludes());
            for(String id:refs){var other=byId.get(id);if(other==null||id.equals(a.id()))throw new IllegalArgumentException("Invalid reference "+id+" in "+a.id());}
        }
        for(var a:b.abilities)visit(a,byId,new HashSet<>(),new HashSet<>());
        for(var a:b.abilities){
            var mandatory=new LinkedHashSet<String>();mandatory(a,byId,mandatory);mandatory.add(a.id());
            for(String left:mandatory)for(String right:mandatory)if(exclusive(byId.get(left),byId.get(right)))throw new IllegalArgumentException("Exclusive mandatory prerequisites in "+a.id());
            if(a.requiresAny().stream().filter(id->mandatory.stream().noneMatch(m->exclusive(byId.get(id),byId.get(m)))).count()<a.requiredParents())throw new IllegalArgumentException("All alternatives excluded in "+a.id());
        }
        var skills=new HashSet<Skill>();
        for(var tree:b.trees){
            if(tree==null||tree.skill()==null||tree.nodes()==null||!skills.add(tree.skill()))throw new IllegalArgumentException("Invalid/duplicate tree");
            var ids=new HashSet<String>();
            for(var node:tree.nodes())if(node==null||node.ability()==null||!ids.add(node.ability())||!byId.containsKey(node.ability())||!Double.isFinite(node.x())||!Double.isFinite(node.y())||Math.abs(node.x())>4096||Math.abs(node.y())>4096||node.icon()==null||!node.icon().matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("Invalid tree node");
        }
        for(String name:List.of("strength","constitution","dexterity","defense","intelligence","building"))if(!skills.contains(Skill.valueOf(name)))throw new IllegalArgumentException("Missing tree "+name);
    }
    private static void visit(Balance.Ability a,Map<String,Balance.Ability> nodes,Set<String> active,Set<String> done){
        if(done.contains(a.id()))return;if(!active.add(a.id()))throw new IllegalArgumentException("Ability cycle");
        for(String id:a.dependencies())visit(nodes.get(id),nodes,active,done);active.remove(a.id());done.add(a.id());
    }
    private static void mandatory(Balance.Ability a,Map<String,Balance.Ability> nodes,Set<String> result){for(String id:a.all())if(result.add(id))mandatory(nodes.get(id),nodes,result);}
}
