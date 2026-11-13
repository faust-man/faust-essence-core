package com.faust.core.runtime;
import com.faust.essence.api.*;
import com.faust.core.progression.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
/** Adapter from the public API to private Core storage and game rules. */
public final class EssenceBackendImpl implements EssenceBackend {
 public PlayerSnapshot snapshot(ServerPlayer player){var p=CoreRuntime.data(player);var b=CoreRuntime.balance;return view(p,b);}
 public static PlayerSnapshot view(Progress p,Balance b){var tracks=new LinkedHashMap<Skill,PlayerSnapshot.Track>();for(var skill:Skill.values()){var t=p.track(skill);tracks.put(skill,new PlayerSnapshot.Track(t.level,t.xp,p.needed(skill,b),p.cap(skill,b),p.price(skill,b),p.multiplier(skill,b)));}return new PlayerSnapshot(tracks,p.unlocked,p.disabled,p.path);}
 public boolean active(ServerPlayer p){return CoreRuntime.active(p);}
 public boolean grantAction(ServerPlayer p,String action,double units,double count){return CoreRuntime.grant(p,action,units,count);}
 public void grantExperience(ServerPlayer p,Skill skill,double amount){if(active(p)){CoreRuntime.data(p).grant(skill,amount,CoreRuntime.balance);CoreRuntime.sync(p);}}
 public boolean needsCounter(ServerPlayer p,String key){return CoreRuntime.data(p).wantsCounter(key,CoreRuntime.balance);}
 public boolean recordCounter(ServerPlayer p,String key,double amount,boolean maximum){return maximum?CoreRuntime.data(p).recordMaximum(key,amount,CoreRuntime.balance):CoreRuntime.data(p).recordCounter(key,amount,CoreRuntime.balance);}
 public double effect(ServerPlayer p,String key){return CoreRuntime.data(p).effect(key,CoreRuntime.balance);}
 public boolean learned(ServerPlayer p,String id){return CoreRuntime.data(p).unlocked.contains(id);}
 public boolean enabled(ServerPlayer p,String id){return CoreRuntime.data(p).enabled(id,CoreRuntime.balance);}
 public boolean unlock(ServerPlayer p,String id){boolean ok=active(p)&&CoreRuntime.data(p).unlock(id,CoreRuntime.balance);if(ok)CoreRuntime.sync(p);return ok;}
 public boolean toggle(ServerPlayer p,String id){boolean ok=CoreRuntime.data(p).toggle(id,CoreRuntime.balance);if(ok)CoreRuntime.refresh(p);return ok;}
 public boolean buyCap(ServerPlayer p,Skill skill){return CoreRuntime.buy(p,skill);}

 public boolean choosePath(ServerPlayer p,String id){var data=CoreRuntime.data(p);if(!data.path.isEmpty())return false;data.path=id;data.revision++;CoreRuntime.sync(p);return true;}
 public void definitionsChanged(){if(CoreRuntime.balance!=null)apply(CoreRuntime.balance);}
 public static synchronized void apply(Balance b){
  b.trees=new ArrayList<>(b.trees);
  for(var rule:Extensions.characteristics())if(b.characteristicRules.stream().noneMatch(old->old.skill()==rule.skill()))b.characteristicRules.add(rule);
  for(var tab:Extensions.tabs()){b.limitCosts.putIfAbsent(tab.namespace()+":base",tab.basePrice());b.limitCosts.putIfAbsent(tab.namespace()+":step",tab.priceStep());if(tab.integrated()&&!b.integratedTreeNamespaces.contains(tab.namespace()))b.integratedTreeNamespaces.add(tab.namespace());}
  for(var action:Extensions.actions())b.actions.putIfAbsent(action.id(),new Balance.Rule(action.skill(),action.experience(),action.counter()));
  for(var a:Extensions.abilities()){
   if(b.abilities.stream().noneMatch(old->old.id().equals(a.id()))){var first=a.requirements().getFirst();b.abilities.add(new Balance.Ability(a.id(),first.skill(),first.level(),"",0,"",a.effect(),a.value(),a.parents(),a.anyParents(),a.excludes(),a.requirements().stream().map(r->new Balance.SkillRequirement(r.skill(),r.level())).toList(),a.conditions().stream().map(c->new Balance.Condition(c.counter(),c.amount())).toList(),a.requiredConditions()==0?"all":"at_least",a.requiredConditions(),a.requiredParents(),a.toggleable()));}
   var skill=a.requirements().getFirst().skill();var tree=b.trees.stream().filter(t->t.skill()==skill).findFirst().orElse(null);if(tree==null){tree=new SkillTrees.Tree(skill,new ArrayList<>());b.trees.add(tree);}if(tree.nodes().stream().noneMatch(n->n.ability().equals(a.id()))){var nodes=new ArrayList<>(tree.nodes());nodes.add(new SkillTrees.Node(a.id(),a.x(),a.y(),a.icon()));b.trees.set(b.trees.indexOf(tree),new SkillTrees.Tree(skill,SkillTrees.spaced(nodes)));}
  }
 }
}
