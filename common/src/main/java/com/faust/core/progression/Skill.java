package com.faust.core.progression;
import java.util.*;
/** Stable IDs; six built-ins, namespaced extensions may register during initialization. */
public final class Skill {
 private static final Map<String,Skill> REGISTRY=new LinkedHashMap<>();
 public static final Skill strength=register("strength"),constitution=register("constitution"),dexterity=register("dexterity"),defense=register("defense"),intelligence=register("intelligence"),building=register("building");
 private final String id;
 private boolean declared;
 private Skill(String id){this.id=id;}
 public static synchronized Skill register(String id) {
  if(!id.matches("([a-z0-9_.-]+:)?[a-z0-9_./-]+")||(REGISTRY.containsKey(id)&&REGISTRY.get(id).declared))throw new IllegalArgumentException("Invalid/duplicate skill "+id);
  Skill skill=REGISTRY.computeIfAbsent(id,Skill::new);skill.declared=true;return skill;
 }
 public static synchronized Skill valueOf(String id){Skill skill=REGISTRY.get(id);if(skill==null)throw new IllegalArgumentException("Unknown skill "+id);return skill;}
 public static synchronized Skill saved(String id){if(REGISTRY.containsKey(id))return REGISTRY.get(id);if(!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("Unknown skill "+id);Skill skill=new Skill(id);REGISTRY.put(id,skill);return skill;}
 public static synchronized Skill[] values(){return REGISTRY.values().toArray(Skill[]::new);}
 public String name(){return id;}
 @Override public String toString(){return id;}
}
