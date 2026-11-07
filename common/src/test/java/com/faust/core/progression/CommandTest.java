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
        var dispatcher=new CommandDispatcher<CommandSourceStack>();CoreCommands.register(dispatcher);
        var admin=new Source(2);var player=new Source(0);int checks=0;
        for(String command:new String[]{"core help","core status","core status @a","core cap strength","core cap buy defense","core cap set @a strength 20","core skill set @s strength 8","core skill add @a constitution -2","core skill xp @s building 1.5","core ability grant @s faustcore:shield_care","core ability revoke @s faustcore:shield_care","core stamina fill @s","core stamina set @a 10","core fatigue clear @a","core fatigue set @s 4","core reset @s","core unlock faustcore:controlled_strike","core path example:path","core reload"}) {
            var parsed=dispatcher.parse(command,admin);
            if(parsed.getReader().canRead()||!parsed.getExceptions().isEmpty())throw new AssertionError("Cannot parse: "+command+" "+parsed.getExceptions());checks++;
        }
        for(String command:new String[]{"core skill set @s strength -1","core cap set @s strength 21","core stamina set @s -1","core fatigue set @s -1"}) {
            if(!dispatcher.parse(command,admin).getReader().canRead())throw new AssertionError("Invalid command accepted: "+command);checks++;
        }
        var root=dispatcher.getRoot().getChild("core");
        for(String name:new String[]{"skill","ability","stamina","fatigue","reset","reload"}) {
            if(root.getChild(name).canUse(player)||!root.getChild(name).canUse(admin))throw new AssertionError("Permission failure: "+name);checks++;
        }
        if(root.getChild("cap").getChild("set").canUse(player))throw new AssertionError("Normal player may set limits");checks++;
        if(!root.getChild("cap").getChild("buy").canUse(player))throw new AssertionError("Normal player cannot buy limit");checks++;
        var completion=dispatcher.getCompletionSuggestions(dispatcher.parse("core cap buy str",player)).get();
        if(completion.getList().stream().noneMatch(s->s.getText().equals("strength")))throw new AssertionError("Missing skill completion");checks++;
        System.out.println("Core command checks passed: "+checks);
    }
}
