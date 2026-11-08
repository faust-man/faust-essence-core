package com.faust.core.runtime;

import com.faust.core.api.CoreApi;
import com.faust.core.progression.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.function.Consumer;
import static net.minecraft.commands.Commands.*;

public final class CoreCommands {
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack,net.minecraft.resources.ResourceLocation> skill() {
        return argument("skill",ResourceLocationArgument.id()).suggests((ctx,b)->SharedSuggestionProvider.suggest(Arrays.stream(Skill.values()).map(Skill::name),b));
    }
    private static Skill selected(CommandContext<CommandSourceStack> ctx) {
        var id=ResourceLocationArgument.getId(ctx,"skill");return Skill.valueOf(id.getNamespace().equals("minecraft")?id.getPath():id.toString());
    }
    private static String abilityId(CommandContext<CommandSourceStack> ctx) {
        var id=ResourceLocationArgument.getId(ctx,"ability");return id.getNamespace().equals("minecraft")?"faustcore:"+id.getPath():id.toString();
    }
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack,net.minecraft.resources.ResourceLocation> ability() {
        return argument("ability",ResourceLocationArgument.id()).suggests((ctx,b)->SharedSuggestionProvider.suggest(CoreRuntime.balance.abilities.stream().map(Balance.Ability::id),b));
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root=literal("core").executes(CoreCommands::help);
        root.then(literal("help").executes(CoreCommands::help));
        root.then(literal("max").requires(source->source.hasPermission(2)).then(argument("player",EntityArgument.player()).executes(ctx->{
            var player=EntityArgument.getPlayer(ctx,"player");CoreRuntime.data(player).max(CoreRuntime.balance);CoreRuntime.refresh(player);
            ctx.getSource().sendSuccess(()->Component.translatable("core.command.updated",1),true);return 1;
        })));
        root.then(literal("status").executes(ctx->{ctx.getSource().sendSuccess(()->CommandSupport.status(ctx.getSource().getPlayer()),false);return 1;})
            .then(argument("targets",EntityArgument.players()).requires(s->s.hasPermission(2)).executes(ctx->{var targets=EntityArgument.getPlayers(ctx,"targets");for(var p:targets)ctx.getSource().sendSuccess(()->CommandSupport.status(p),false);return targets.size();})));
        var cap=literal("cap");
        cap.then(skill().executes(CoreCommands::buy));
        cap.then(literal("buy").then(skill().executes(CoreCommands::buy)));
        cap.then(literal("set").requires(s->s.hasPermission(2)).then(argument("targets",EntityArgument.players()).then(skill().then(argument("value",IntegerArgumentType.integer(0,20)).executes(ctx->modify(ctx,p->{var t=CoreRuntime.data(p).track(selected(ctx));t.limitOverride=IntegerArgumentType.getInteger(ctx,"value");CoreRuntime.data(p).revision++;}))))));
        root.then(cap);
        root.then(literal("unlock").then(ability().executes(ctx->{var p=ctx.getSource().getPlayerOrException();boolean result=CoreRuntime.active(p)&&CoreRuntime.data(p).unlock(abilityId(ctx),CoreRuntime.balance);CoreRuntime.sync(p);return reply(ctx,result?"core.action.ok":"core.action.denied",result);})));
        root.then(literal("toggle").then(ability().executes(ctx->{
            var player=ctx.getSource().getPlayerOrException();boolean result=CoreRuntime.data(player).toggle(abilityId(ctx),CoreRuntime.balance);
            if(result)CoreRuntime.refresh(player);
            return reply(ctx,result?"core.action.ok":"core.action.denied",result);
        })));
        var skills=literal("skill").requires(s->s.hasPermission(2));
        for(String mode:List.of("set","add"))skills.then(literal(mode).then(argument("targets",EntityArgument.players()).then(skill().then(argument("value",mode.equals("set")?IntegerArgumentType.integer(0):IntegerArgumentType.integer()).executes(ctx->modify(ctx,p->CommandSupport.level(p,selected(ctx),IntegerArgumentType.getInteger(ctx,"value"),mode.equals("add"))))))));
        skills.then(literal("xp").then(argument("targets",EntityArgument.players()).then(skill().then(argument("amount",DoubleArgumentType.doubleArg(0,1000000)).executes(ctx->modify(ctx,p->CoreRuntime.data(p).grant(selected(ctx),DoubleArgumentType.getDouble(ctx,"amount"),CoreRuntime.balance)))))));
        root.then(skills);
        var abilities=literal("ability").requires(s->s.hasPermission(2));
        for(String mode:List.of("grant","revoke"))abilities.then(literal(mode).then(argument("targets",EntityArgument.players()).then(ability().executes(ctx->{String id=abilityId(ctx);if(!CommandSupport.knownAbility(id))return reply(ctx,"core.unknown_ability",false);if(mode.equals("grant")){var node=CoreRuntime.balance.abilities.stream().filter(a->a.id().equals(id)).findFirst().orElseThrow();for(var target:EntityArgument.getPlayers(ctx,"targets"))if(CoreRuntime.data(target).excluded(node,CoreRuntime.balance))return reply(ctx,"core.tree.excluded",false);}return modify(ctx,p->CommandSupport.ability(p,id,mode.equals("grant")));}))));
        root.then(abilities);
        root.then(literal("stamina").requires(s->s.hasPermission(2))
            .then(literal("fill").then(argument("targets",EntityArgument.players()).executes(ctx->modify(ctx,p->CoreRuntime.data(p).stamina=CoreRuntime.data(p).maxStamina(CoreRuntime.balance)))))
            .then(literal("set").then(argument("targets",EntityArgument.players()).then(argument("amount",DoubleArgumentType.doubleArg(0,1000000)).executes(ctx->modify(ctx,p->CoreRuntime.data(p).stamina=Math.min(DoubleArgumentType.getDouble(ctx,"amount"),CoreRuntime.data(p).maxStamina(CoreRuntime.balance))))))));
        root.then(literal("fatigue").requires(s->s.hasPermission(2))
            .then(literal("clear").then(argument("targets",EntityArgument.players()).executes(ctx->modify(ctx,p->CoreRuntime.data(p).fatigue=0))))
            .then(literal("set").then(argument("targets",EntityArgument.players()).then(argument("amount",DoubleArgumentType.doubleArg(-1000000,1000000)).executes(ctx->modify(ctx,p->{var data=CoreRuntime.data(p);data.fatigue=Math.max(-CoreRuntime.balance.fatigue.get("sleepReserve"),Math.min(DoubleArgumentType.getDouble(ctx,"amount"),data.maxFatigue(CoreRuntime.balance)));data.stamina=Math.min(data.stamina,data.maxStamina(CoreRuntime.balance));}))))));
        root.then(literal("reset").requires(s->s.hasPermission(2)).then(argument("targets",EntityArgument.players()).executes(ctx->modify(ctx,p->((PlayerData)p).coreData(new Progress())))));
        root.then(literal("path").then(argument("id",ResourceLocationArgument.id()).suggests((ctx,b)->SharedSuggestionProvider.suggest(CoreApi.paths(),b)).executes(ctx->reply(ctx,"core.action.ok",CoreApi.choosePath(ctx.getSource().getPlayerOrException(),ResourceLocationArgument.getId(ctx,"id").toString())))));
        root.then(literal("reload").requires(s->s.hasPermission(2)).executes(ctx->{try{CoreRuntime.reload(ctx.getSource().getServer());return reply(ctx,"core.reload.ok",true);}catch(RuntimeException e){ctx.getSource().sendFailure(Component.literal(e.getMessage()));return 0;}}));
        dispatcher.register(root);
    }
    private static int help(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(()->Component.translatable("core.command.help"),false);
        if(ctx.getSource().hasPermission(2))ctx.getSource().sendSuccess(()->Component.translatable("core.command.admin_help"),false);
        return 1;
    }
    private static int buy(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var p=ctx.getSource().getPlayerOrException();Skill skill;
        try{skill=selected(ctx);}catch(IllegalArgumentException e){return reply(ctx,"core.unknown_skill",false);}
        var data=CoreRuntime.data(p);
        if(data.cap(skill,CoreRuntime.balance)>=CoreRuntime.balance.progression.get("maximumCap"))return reply(ctx,"core.cap.maximum",false);
        if(p.experienceLevel<data.price(skill,CoreRuntime.balance))return reply(ctx,"core.insufficient_levels",false);
        return reply(ctx,"core.action.ok",CoreRuntime.buy(p,skill));
    }
    private static int modify(CommandContext<CommandSourceStack> ctx,Consumer<ServerPlayer> mutation) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var targets=EntityArgument.getPlayers(ctx,"targets");
        try{for(var p:targets){mutation.accept(p);CoreRuntime.refresh(p);}}catch(IllegalArgumentException e){return reply(ctx,"core.unknown_skill",false);}
        ctx.getSource().sendSuccess(()->Component.translatable("core.command.updated",targets.size()),true);return targets.size();
    }
    private static int reply(CommandContext<CommandSourceStack> ctx,String key,boolean success){if(success)ctx.getSource().sendSuccess(()->Component.translatable(key),false);else ctx.getSource().sendFailure(Component.translatable(key.equals("core.action.ok")?"core.action.denied":key));return success?1:0;}
}
