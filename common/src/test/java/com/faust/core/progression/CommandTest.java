package com.faust.core.progression;

import com.faust.core.runtime.CoreCommands;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.util.TaskChainer;
import net.minecraft.world.phys.*;

/** Parses real Brigadier commands and verifies permissions without a running server. */
public final class CommandTest {
    private static final class Source extends CommandSourceStack {
        Source(int permission) {
            super(CommandSource.NULL,Vec3.ZERO,Vec2.ZERO,null,permission,"test",Component.literal("test"),null,null,
                false,CommandResultCallback.EMPTY,EntityAnchorArgument.Anchor.FEET,CommandSigningContext.ANONYMOUS,TaskChainer.immediate(Runnable::run));
        }
    }
    public static void run() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
        for(var rule:new Balance().statBonuses)if(!rule.attribute.startsWith("faustcore:")&&!net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.containsKey(net.minecraft.resources.ResourceLocation.parse(rule.attribute)))throw new AssertionError("Unknown 1.21.1 attribute "+rule.attribute);
        var dispatcher=new CommandDispatcher<CommandSourceStack>();CoreCommands.register(dispatcher);
        var max=dispatcher.getRoot().getChild("essence").getChild("max");if(max.getChild("defense")==null||max.getChild("defence")!=null)throw new AssertionError("max lists defense once");
        var admin=new Source(2);var player=new Source(0);int checks=0;
        for(String command:new String[]{"essence max","essence max all @s 4","essence max defense @a 5","essence max strength Steve 10","essence max Steve 3","essence help","essence toggle faustcore:second_wind","essence max Steve","essence max @s","essence max @a","essence panel toggle faustcore:second_wind","essence panel cap strength","essence status","essence status @a","essence cap strength","essence cap buy defense","essence cap set @a strength 20","essence skill set @s strength 8","essence skill add @a constitution -2","essence skill xp @s building 1.5","essence ability grant @s faustcore:shield_care","essence ability revoke @s faustcore:shield_care","essence stamina fill @s","essence stamina set @a 10","essence fatigue clear @a","essence fatigue set @s 4","essence fatigue set @s -5","essence reset @s","essence unlock faustcore:controlled_strike","essence path example:path","essence reload"}) {
            var parsed=dispatcher.parse(command,admin);
            if(parsed.getReader().canRead()||!parsed.getExceptions().isEmpty())throw new AssertionError("Cannot parse: "+command+" "+parsed.getExceptions());checks++;
        }
        for(String command:new String[]{"essence skill set @s strength -1","essence cap set @s strength 21","essence stamina set @s -1","essence fatigue set @s -1000001"}) {
            if(!dispatcher.parse(command,admin).getReader().canRead())throw new AssertionError("Invalid command accepted: "+command);checks++;
        }
        var balance=new Balance();
        for(int level:new int[]{0,3,4,5,10,20}){
            var progress=new Progress();progress.max(balance,null,level);
            for(var ability:balance.abilities)if(progress.unlocked.contains(ability.id())&&ability.skillRequirements().stream().anyMatch(r->r.level()>level))throw new AssertionError("max granted above requested level");
        }
        var progress=new Progress();progress.max(balance,Skill.defense,5);
        if(progress.track(Skill.strength).level!=0||progress.track(Skill.defense).level!=5)throw new AssertionError("max changed unrelated characteristic");
        var root=dispatcher.getRoot().getChild("essence");
        for(String name:new String[]{"skill","ability","stamina","fatigue","reset","reload","max"}) {
            if(root.getChild(name).canUse(player)||!root.getChild(name).canUse(admin))throw new AssertionError("Permission failure: "+name);checks++;
        }
        if(root.getChild("cap").getChild("set").canUse(player))throw new AssertionError("Normal player may set limits");checks++;
        if(!root.getChild("cap").getChild("buy").canUse(player))throw new AssertionError("Normal player cannot buy limit");checks++;
        if(!root.getChild("toggle").canUse(player))throw new AssertionError("Normal player cannot toggle own skill");checks++;
        if(dispatcher.parse("essence max @a",admin).getReader().canRead())throw new AssertionError("Max supports all players");checks++;
        var completion=dispatcher.getCompletionSuggestions(dispatcher.parse("essence cap buy str",player)).get();
        if(completion.getList().stream().noneMatch(s->s.getText().equals("strength")))throw new AssertionError("Missing skill completion");checks++;
        System.out.println("Core command checks passed: "+checks);
    }
}
