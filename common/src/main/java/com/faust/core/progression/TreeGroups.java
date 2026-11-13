package com.faust.core.progression;
import com.faust.essence.api.Skill;


import java.util.*;

/** Groups synchronized abilities by their owning namespace. */
public final class TreeGroups {
    private TreeGroups() {}
    public static String group(Balance b,String id){
        String namespace=id.split(":")[0];
        return b.integratedTreeNamespaces.contains(namespace)?"faustcore":namespace;
    }
    public static boolean contains(Balance b,String group,String id){return group.equals(group(b,id));}
    public static List<String> groups(Balance b){
        var result=new TreeSet<String>();
        for(var a:b.abilities)result.add(group(b,a.id()));
        result.remove("faustcore");var ordered=new ArrayList<String>();ordered.add("faustcore");ordered.addAll(result);return List.copyOf(ordered);
    }
    public static List<Skill> skills(Balance b,String group){
        return Arrays.stream(Skill.values()).filter(s->group.equals("faustcore")&&!s.name().contains(":")||b.abilities.stream().anyMatch(a->a.skill()==s&&contains(b,group,a.id()))).toList();
    }
}
