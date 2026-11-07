package com.faust.core.runtime;

import com.faust.core.api.CoreApi;
import com.faust.core.progression.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import static net.minecraft.commands.Commands.*;

public final class CoreCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root=literal("core").executes(ctx->{CoreRuntime.sync(ctx.getSource().getPlayerOrException());ctx.getSource().sendSuccess(()->Component.translatable("core.command.help"),false);return 1;});
        root.then(literal("cap").then(argument("skill",StringArgumentType.greedyString()).suggests((ctx,b)->SharedSuggestionProvider.suggest(java.util.Arrays.stream(Skill.values()).map(Skill::name),b)).executes(ctx->{
            ServerPlayer p=ctx.getSource().getPlayerOrException();
            try {return reply(p,CoreRuntime.buy(p,Skill.valueOf(StringArgumentType.getString(ctx,"skill"))));}catch(IllegalArgumentException e){return reply(p,false);}
        })));
        root.then(literal("unlock").then(argument("ability",StringArgumentType.greedyString()).suggests((ctx,b)->SharedSuggestionProvider.suggest(CoreRuntime.balance.abilities.stream().map(Balance.Ability::id),b)).executes(ctx->{
            ServerPlayer p=ctx.getSource().getPlayerOrException();boolean result=CoreRuntime.data(p).unlock(StringArgumentType.getString(ctx,"ability"),CoreRuntime.balance);CoreRuntime.sync(p);return reply(p,result);
        })));
        root.then(literal("path").then(argument("id",StringArgumentType.greedyString()).suggests((ctx,b)->SharedSuggestionProvider.suggest(CoreApi.paths(),b)).executes(ctx->reply(ctx.getSource().getPlayerOrException(),CoreApi.choosePath(ctx.getSource().getPlayerOrException(),StringArgumentType.getString(ctx,"id"))))));
        root.then(literal("reload").requires(s->s.hasPermission(2)).executes(ctx->{
            try {CoreRuntime.reload(ctx.getSource().getServer());ctx.getSource().sendSuccess(()->Component.translatable("core.reload.ok"),true);return 1;}
            catch(RuntimeException e){ctx.getSource().sendFailure(Component.literal(e.getMessage()));return 0;}
        }));
        dispatcher.register(root);
    }
    private static int reply(ServerPlayer player,boolean success) {player.sendSystemMessage(Component.translatable(success?"core.action.ok":"core.action.denied"));return success?1:0;}
}
