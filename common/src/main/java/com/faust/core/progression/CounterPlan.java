package com.faust.core.progression;

import java.util.*;

/** Per-player demand index. Rebuilt only when config or learned prerequisites change. */
final class CounterPlan {
    static Set<String> learnedSet(Set<String> values){return values instanceof LearnedSet?values:new LearnedSet(values);}
    private record Need(Balance.Ability ability,Balance.Condition condition){}
    private final Balance balance;
    private final List<Balance.Ability> abilities;
    private final int count;
    private final long configEpoch,learnedEpoch;
    private final LearnedSet learned;
    private final Map<String,List<Need>> index=new HashMap<>();
    private final Set<String> finished=new HashSet<>();
    CounterPlan(Progress p,Balance b){
        balance=b;abilities=b.abilities;count=abilities.size();configEpoch=b.counterEpoch;
        p.unlocked=learnedSet(p.unlocked);
        learned=(LearnedSet)p.unlocked;learnedEpoch=learned.epoch;
        for(var a:abilities){
            p.practice.computeIfAbsent(a.id(),key->new HashMap<>());
            if(p.unlocked.contains(a.id())||!p.prerequisites(a)||p.excluded(a,b)||p.completedConditions(a)>=a.requiredConditions())continue;
            for(var c:a.actionConditions())if(c.amount()>p.conditionCount(a,c))index.computeIfAbsent(c.counter(),key->new ArrayList<>()).add(new Need(a,c));
        }
    }
    boolean valid(Progress p,Balance b){return balance==b&&abilities==b.abilities&&count==b.abilities.size()&&configEpoch==b.counterEpoch&&p.unlocked==learned&&learnedEpoch==learned.epoch;}
    boolean wants(Progress p,String counter){
        for(var n:index.getOrDefault(counter,List.of()))if(!finished.contains(n.ability.id())&&p.conditionCount(n.ability,n.condition)<n.condition.amount())return true;
        return false;
    }
    boolean record(Progress p,String key,double value,boolean peak){
        boolean changed=false;double ceiling=0;
        for(var n:index.getOrDefault(key,List.of())){
            if(finished.contains(n.ability.id()))continue;
            var values=p.practice.get(n.ability.id());double before=values.getOrDefault(key,0.0);
            double after=Math.min(n.condition.amount(),peak?Math.max(before,value):before+value);
            if(after>before){values.put(key,after);changed=true;ceiling=Math.max(ceiling,n.condition.amount());}
            if(p.completedConditions(n.ability)>=n.ability.requiredConditions())finished.add(n.ability.id());
        }
        if(changed){double old=p.counters.getOrDefault(key,0.0);p.counters.put(key,Math.max(old,Math.min(ceiling,peak?Math.max(old,value):old+value)));p.revision++;}
        return changed;
    }
    /** Tracks direct API mutations too, without hashing all learned nodes on every event. */
    private static final class LearnedSet extends AbstractSet<String> {
        private final Set<String> values;private long epoch;
        LearnedSet(Set<String> initial){values=new HashSet<>(initial);}
        @Override public int size(){return values.size();}
        @Override public boolean contains(Object key){return values.contains(key);}
        @Override public boolean add(String key){boolean changed=values.add(key);if(changed)epoch++;return changed;}
        @Override public boolean remove(Object key){boolean changed=values.remove(key);if(changed)epoch++;return changed;}
        @Override public void clear(){if(!values.isEmpty()){values.clear();epoch++;}}
        @Override public Iterator<String> iterator(){var delegate=values.iterator();return new Iterator<>(){public boolean hasNext(){return delegate.hasNext();}public String next(){return delegate.next();}public void remove(){delegate.remove();epoch++;}};}
    }
}
