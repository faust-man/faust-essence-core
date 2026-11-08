package com.faust.core.runtime;
import com.faust.core.progression.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
/** Shared administrative mutations; all invoked by permission-gated server commands. */
public final class CommandSupport {
    public static Component status(ServerPlayer player) {
        if(player==null)return Component.literal("Console: /core status <players> (for example @a)");
        Progress p=CoreRuntime.data(player);StringJoiner text=new StringJoiner(" | ");
        for(Skill skill:Skill.values())text.add(skill+" "+p.track(skill).level+"/"+(int)p.cap(skill,CoreRuntime.balance));
        return Component.literal(player.getScoreboardName()+": "+text+" | XP levels: "+player.experienceLevel);
    }
    public static void level(ServerPlayer player,Skill skill,int value,boolean add) {
        Progress p=CoreRuntime.data(player);var t=p.track(skill);
        t.level=(int)Math.max(0,Math.min(Integer.MAX_VALUE,add?(long)t.level+value:value));t.xp=0;t.threshold=p.needed(skill,CoreRuntime.balance);p.revision++;
    }
    public static boolean knownAbility(String id){return CoreRuntime.balance.abilities.stream().anyMatch(a->a.id().equals(id));}
    public static void ability(ServerPlayer player,String id,boolean grant) {
        Progress p=CoreRuntime.data(player);
        if(grant){var a=CoreRuntime.balance.abilities.stream().filter(node->node.id().equals(id)).findFirst().orElseThrow();if(p.excluded(a,CoreRuntime.balance))throw new IllegalArgumentException("Exclusive skill already learned");p.unlocked.add(id);}
        else p.revoke(id,CoreRuntime.balance);
        p.revision++;
    }
}
