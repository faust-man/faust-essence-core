package com.faust.core.progression;

import com.faust.core.client.TreeView;
import java.nio.file.*;
import java.util.*;

/** Graph semantics, persistence and inventory viewport geometry. */
public final class TreeTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void rejects(Runnable action,String message){try{action.run();}catch(IllegalArgumentException e){checks++;return;}throw new AssertionError(message);}
    private static Balance.Ability node(String id,List<String> all,List<String> any,List<String> excludes){return new Balance.Ability("test:"+id,Skill.strength,2,"hits",3,"","damage",0.01,all,any,excludes);}
    private static Balance balance(Balance.Ability... nodes){var b=new Balance();b.abilities=new ArrayList<>(List.of(nodes));b.statBonuses.clear();b.trees=SkillTrees.defaults(b.abilities);return b;}
    private static Progress player(Balance config){var p=new Progress();p.track(Skill.strength).level=20;p.recordCounter("hits",100,config);return p;}
    public static void run() throws Exception {
        var multi=Balance.JSON.fromJson("""
            {"id":"test:multi","skill":"defense","level":0,"effect":"damage","value":0.1,
             "characteristics":[{"skill":"strength","level":4},{"skill":"dexterity","level":6}],
             "conditions":[{"counter":"hits","amount":10},{"counter":"blocks","amount":5},{"counter":"books","amount":2}],
             "conditionMode":"at_least","conditionsRequired":2}
            """,Balance.Ability.class);
        var mb=balance(multi);mb.validate();var mp=player(mb);
        check(multi.skill()==Skill.strength,"first configured characteristic owns tree");
        check(SkillTrees.nodes(mb,Skill.strength).size()==1&&SkillTrees.nodes(mb,Skill.defense).isEmpty(),"multi attribute node appears in only first tree");
        mp.track(Skill.dexterity).level=5;mp.setCounter("blocks",5.0,mb);
        check(!mp.canUnlock(multi,mb),"all characteristic thresholds required");
        mp.track(Skill.dexterity).level=6;check(mp.canUnlock(multi,mb),"two of three action conditions unlock");
        mp.setCounter("blocks",0,mb);check(!mp.canUnlock(multi,mb),"one of three insufficient");
        mp.setCounter("books",2.0,mb);check(mp.canUnlock(multi,mb),"any subset of two accepted");
        var all=Balance.JSON.fromJson(Balance.JSON.toJson(multi).replace("at_least","all"),Balance.Ability.class);
        check(!mp.canUnlock(all,mb),"all mode requires remaining condition");mp.setCounter("blocks",5.0,mb);check(mp.canUnlock(all,mb),"all mode accepts all conditions");
        check(Balance.JSON.fromJson(Balance.JSON.toJson(multi),Balance.Ability.class).equals(multi),"new config round trip");
        rejects(()->balance(Balance.JSON.fromJson(Balance.JSON.toJson(multi).replace("\"conditionsRequired\": 2","\"conditionsRequired\": 4"),Balance.Ability.class)).validate(),"impossible condition count rejected");
        rejects(()->balance(Balance.JSON.fromJson(Balance.JSON.toJson(multi).replace("at_least","unknown"),Balance.Ability.class)).validate(),"unknown condition mode rejected");
        rejects(()->balance(Balance.JSON.fromJson(Balance.JSON.toJson(multi).replace("\"conditionsRequired\": 2","\"conditionsRequired\": 0"),Balance.Ability.class)).validate(),"zero threshold rejected");
        rejects(()->balance(Balance.JSON.fromJson(Balance.JSON.toJson(multi).replace("dexterity","strength"),Balance.Ability.class)).validate(),"duplicate characteristics rejected");
        rejects(()->balance(Balance.JSON.fromJson(Balance.JSON.toJson(multi).replace("blocks","hits"),Balance.Ability.class)).validate(),"duplicate action counters rejected");
        var a=node("a",List.of(),List.of(),List.of());var c=node("c",List.of(),List.of(),List.of());
        var both=node("both",List.of(a.id(),c.id()),List.of(),List.of());var either=node("either",List.of(),List.of(a.id(),c.id()),List.of());var combined=node("combined",List.of(a.id()),List.of(c.id(),either.id()),List.of());
        var counted=new Balance.Ability("test:counted",Skill.strength,2,"hits",3,"","damage",0.01,List.of(),List.of(a.id(),c.id()),List.of(),List.of(),List.of(),"all",0,2);
        var cb=balance(a,c,counted);cb.validate();var cp=player(cb);cp.unlock(a.id(),cb);check(!cp.canUnlock(counted,cb),"two parents needs second");cp.unlock(c.id(),cb);cp.recordCounter("hits",100,cb);check(cp.unlock(counted.id(),cb),"two parents accepted");check(cp.revoke(a.id(),cb).contains(counted.id()),"threshold revoke cascades");
        var b=balance(a,c,both,either,combined);b.validate();var p=player(b);check(!p.canUnlock(both,b),"AND needs both parents");check(!p.canUnlock(either,b),"OR needs at least one parent");p.unlock(a.id(),b);p.recordCounter("hits",100,b);check(!p.canUnlock(both,b),"AND does not accept only one");check(p.canUnlock(either,b),"OR accepts first alternative");check(!p.canUnlock(combined,b),"AND and OR requirements combine");p.unlock(c.id(),b);p.recordCounter("hits",100,b);check(p.unlock(both.id(),b),"AND unlocks with both");check(p.canUnlock(combined,b),"combined AND plus one OR works");
        p=player(b);p.unlock(c.id(),b);p.recordCounter("hits",100,b);check(p.canUnlock(either,b),"OR accepts second alternative");
        check(SkillTrees.lineColor(p,a.id())==0xFF000000,"line from unlearned parent is black");check(SkillTrees.lineColor(p,c.id())==0xFFFFFFFF,"line from learned parent is white");
        var exclusive=node("choice",List.of(),List.of(),List.of(c.id()));b=balance(exclusive,c);b.validate();p=player(b);p.unlock(exclusive.id(),b);check(!p.unlock(c.id(),b),"one-sided exclusion blocks reverse unlock");p=player(b);p.unlock(c.id(),b);p.recordCounter("hits",100,b);check(!p.unlock(exclusive.id(),b),"exclusion blocks declared direction");
        var legacy=new Balance.Ability("test:legacy",Skill.strength,2,"hits",3,a.id(),"damage",0.01);b=balance(a,legacy);b.validate();p=player(b);check(!p.canUnlock(legacy,b),"legacy parent remains mandatory");p.unlock(a.id(),b);p.recordCounter("hits",100,b);check(p.unlock(legacy.id(),b),"legacy parent unlock works");
        var json=Balance.JSON.toJson(combined);var restored=Balance.JSON.fromJson(json,Balance.Ability.class);check(restored.equals(combined),"dependency lists round trip");var old=Balance.JSON.fromJson("{\"id\":\"test:old\",\"skill\":\"strength\",\"level\":2,\"counter\":\"hits\",\"amount\":3,\"parent\":\"test:a\",\"effect\":\"damage\",\"value\":0.01}",Balance.Ability.class);check(old.requiresAll().isEmpty()&&old.all().equals(List.of(a.id())),"old JSON gets empty lists and keeps parent");
        var cycleA=node("a",List.of("test:c"),List.of(),List.of());var cycleC=node("c",List.of(),List.of("test:a"),List.of());rejects(()->balance(cycleA,cycleC).validate(),"mixed AND OR cycle rejected");rejects(()->balance(node("a",List.of("test:missing"),List.of(),List.of())).validate(),"unknown dependency rejected");rejects(()->balance(node("a",List.of(),List.of(),List.of("test:a"))).validate(),"self exclusion rejected");rejects(()->balance(node("a",List.of(c.id(),c.id()),List.of(),List.of()),c).validate(),"duplicate requirement rejected");
        var mutuallyExclusive=node("a",List.of(),List.of(),List.of(c.id()));rejects(()->balance(mutuallyExclusive,c,both).validate(),"impossible exclusive AND parents rejected");rejects(()->balance(a,node("child",List.of(a.id()),List.of(),List.of(a.id()))).validate(),"excluding mandatory parent rejected");
        var other=new Balance.Ability("test:other",Skill.defense,2,"hits",3,"","damage",0.01);balance(other,node("a",List.of(other.id()),List.of(),List.of())).validate();
        b=balance(a,c,both,either,combined);b.validate();p=player(b);for(var n:b.abilities)p.unlocked.add(n.id());var removed=p.revoke(a.id(),b);check(removed.contains(both.id())&&removed.contains(combined.id()),"revoke cascades through mandatory branches");check(p.unlocked.contains(either.id()),"OR child survives alternate learned parent");p.revoke(c.id(),b);check(!p.unlocked.contains(either.id()),"OR child removed after last alternative");
        p=player(b);p.unlocked.add(legacy.id());p.revoke(c.id(),b);check(p.unlocked.contains(legacy.id()),"unrelated admin-granted node preserved");
        b=new Balance();b.validate();check(b.trees.size()==6,"six distinct built-in tree layouts");
        for(var tree:b.trees){var nodes=SkillTrees.nodes(b,tree.skill());var view=new TreeView(nodes,160,96);for(var n:nodes){check(view.x(n)>=0&&view.y(n)>=0&&view.x(n)+22*view.zoom<=160.0001&&view.y(n)+22*view.zoom<=96.0001,"default node fits inventory viewport");check(view.hit(n,view.x(n)+11*view.zoom,view.y(n)+11*view.zoom),"rendered icon center is clickable");check(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.ResourceLocation.parse(n.icon())),"default vanilla icon exists");}
            for(int i=0;i<nodes.size();i++)for(int j=i+1;j<nodes.size();j++){var left=nodes.get(i);var right=nodes.get(j);check(Math.abs(left.x()-right.x())>=2||Math.abs(left.y()-right.y())>=2,"default nodes do not overlap");}
            view.zoomAt(1e6,80,48);check(view.zoom==3,"zoom upper bound");view.pan(1e6,-1e6);check(Double.isFinite(view.offsetX)&&Double.isFinite(view.offsetY),"pan clamped to finite bounds");view.zoomAt(1e-9,80,48);check(view.zoom==0.25,"zoom lower bound");}
        b=balance(a,c);b.trees=SkillTrees.defaults(List.of(a));check(SkillTrees.nodes(b,Skill.strength).stream().anyMatch(n->n.ability().equals(c.id())),"unplaced custom ability remains accessible");
        var customAbilities=new ArrayList<>(new Balance().abilities);customAbilities.addFirst(new Balance.Ability("faustcore:training_custom",Skill.strength,5,"hits",100,"","stat_unlock",0));var customTree=SkillTrees.defaults(customAbilities).stream().filter(t->t.skill()==Skill.strength).findFirst().orElseThrow();var customNode=customTree.nodes().stream().filter(n->n.ability().equals("faustcore:training_custom")).findFirst().orElseThrow();check(customTree.nodes().stream().filter(n->!n.ability().equals(customNode.ability())).noneMatch(n->Math.abs(n.x()-customNode.x())<2&&Math.abs(n.y()-customNode.y())<2),"custom root inserted first cannot overlap built-in root");
        var invalid=balance(a);invalid.trees.set(0,new SkillTrees.Tree(Skill.strength,List.of(new SkillTrees.Node("test:missing",0,0,"minecraft:book"))));rejects(invalid::validate,"unknown layout ability rejected");
        invalid=balance(a);invalid.trees.set(0,new SkillTrees.Tree(Skill.strength,List.of(new SkillTrees.Node(a.id(),Double.NaN,0,"minecraft:book"))));rejects(invalid::validate,"nonfinite layout position rejected");
        Path dir=Files.createTempDirectory("core-tree-config");try{
            var loaded=Balance.load(dir);check(Files.exists(dir.resolve("trees.json")),"six layouts written to config");var rules=loaded.abilities.stream().filter(n->n.id().equals("faustcore:training_strength_damage")).findFirst().orElseThrow();
            var layouts=new ArrayList<>(loaded.trees);int index=0;for(int i=0;i<layouts.size();i++)if(layouts.get(i).skill()==Skill.strength)index=i;var nodes=new ArrayList<>(layouts.get(index).nodes());var n=nodes.getFirst();nodes.set(0,new SkillTrees.Node(n.ability(),-123,87,"minecraft:diamond_sword"));layouts.set(index,new SkillTrees.Tree(Skill.strength,nodes));Balance.saveGroup(dir,"trees",Balance.JSON.toJson(layouts));var reread=Balance.load(dir);var saved=reread.trees.stream().filter(t->t.skill()==Skill.strength).findFirst().orElseThrow().nodes().getFirst();check(saved.x()==SkillTrees.snap(-123)&&saved.y()==SkillTrees.snap(87)&&saved.icon().equals("minecraft:diamond_sword"),"coordinates and item icon survive reload");
            Files.delete(dir.resolve("trees.json"));var modified=new ArrayList<>(reread.abilities);modified.add(node("custom",List.of(),List.of(),List.of()));Files.writeString(dir.resolve("abilities.json"),Balance.JSON.toJson(modified));loaded=Balance.load(dir);check(SkillTrees.nodes(loaded,Skill.strength).stream().anyMatch(n2->n2.ability().equals("test:custom")),"migration lays out existing custom abilities");
        }finally{try(var paths=Files.walk(dir)){for(Path file:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
        System.out.println("Core tree checks passed: "+checks);
    }
}
