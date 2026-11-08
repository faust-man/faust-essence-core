package com.faust.core.progression;

import java.util.*;

/** Groups synchronized abilities by their owning namespace. */
public final class TreeGroups {
    private TreeGroups() {}
    public static List<String> groups(Balance b){
        var result=new TreeSet<String>();
        for(var a:b.abilities)result.add(a.id().split(":")[0]);
        result.remove("faustcore");var ordered=new ArrayList<String>();ordered.add("faustcore");ordered.addAll(result);return List.copyOf(ordered);
    }
    public static List<Skill> skills(Balance b,String group){
        return Arrays.stream(Skill.values()).filter(s->group.equals("faustcore")&&!s.name().contains(":")||b.abilities.stream().anyMatch(a->a.skill()==s&&a.id().startsWith(group+":"))).toList();
    }
}
