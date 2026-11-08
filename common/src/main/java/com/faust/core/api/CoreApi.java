package com.faust.core.api;

import com.faust.core.progression.*;
import com.faust.core.runtime.CoreRuntime;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.function.*;

/** Server-thread API v1. Register extensions during mod initialization. */
public final class CoreApi {
    private CoreApi() {}
    public enum DefenseKind { SHIELD_BLOCK, WEAPON_BLOCK, PARRY, DODGE }
    public record Grant(ServerPlayer player, String action, double units, double count) {}
    public record Change(UUID player, String action, String before, String after) {}
    private static final List<ToDoubleFunction<Grant>> BEFORE=new ArrayList<>();
    private static final List<Consumer<Change>> AFTER=new ArrayList<>();
    private static final Map<String,Predicate<ServerPlayer>> PATHS=new LinkedHashMap<>();
    private static final Map<String,java.util.function.BiConsumer<ServerPlayer,Double>> BONUSES=new LinkedHashMap<>();
    public static Skill registerSkill(String id) {if(!id.contains(":"))throw new IllegalArgumentException("Extension skill requires namespace");return Skill.register(id);}
    public static void registerBonus(String id,java.util.function.BiConsumer<ServerPlayer,Double> handler) {
        Objects.requireNonNull(handler);BonusEffects.register(id);BONUSES.put(id,handler);
    }
    public static void applyBonuses(ServerPlayer player) {
        for(var bonus:BONUSES.entrySet())bonus.getValue().accept(player,CoreRuntime.data(player).effect(bonus.getKey(),CoreRuntime.balance));
    }
    public static void beforeProgress(ToDoubleFunction<Grant> handler) {BEFORE.add(Objects.requireNonNull(handler));}
    public static void afterProgress(Consumer<Change> handler) {AFTER.add(Objects.requireNonNull(handler));}
    public static boolean hasAfterListeners(){return !AFTER.isEmpty();}
    public static boolean needsCounter(ServerPlayer player,String key){requireServerThread(player);return CoreRuntime.data(player).wantsCounter(key,CoreRuntime.balance);}
    public static boolean recordCounter(ServerPlayer player,String key,double amount){requireServerThread(player);return CoreRuntime.active(player)&&CoreRuntime.data(player).recordCounter(key,amount,CoreRuntime.balance);}
    public static boolean recordMaximum(ServerPlayer player,String key,double value){requireServerThread(player);return CoreRuntime.active(player)&&CoreRuntime.data(player).recordMaximum(key,value,CoreRuntime.balance);}
    public static void registerPath(String id,Predicate<ServerPlayer> requirement) {
        if(!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||PATHS.putIfAbsent(id,Objects.requireNonNull(requirement))!=null) throw new IllegalArgumentException("Invalid/duplicate path "+id);
    }
    public static Set<String> paths() {return Set.copyOf(PATHS.keySet());}
    public static boolean choosePath(ServerPlayer player,String id) {
        requireServerThread(player);
        var data=CoreRuntime.data(player); var requirement=PATHS.get(id);
        if(!data.path.isEmpty()||requirement==null||!requirement.test(player)) return false;
        data.path=id;data.revision++;CoreRuntime.sync(player);return true;
    }
    public static Progress snapshot(ServerPlayer player) {requireServerThread(player);return Progress.parse(CoreRuntime.data(player).json());}
    public static boolean meets(ServerPlayer player,Skill skill,int level) {requireServerThread(player);return CoreRuntime.data(player).track(skill).level>=level;}
    public static boolean grantAction(ServerPlayer player,String action,double units,double count) {return CoreRuntime.grant(player,action,units,count);}
    public static double before(Grant grant) {
        double scale=1;
        for(var handler:List.copyOf(BEFORE)) {double value=handler.applyAsDouble(grant);if(!Double.isFinite(value)||value<0||value>100) throw new IllegalArgumentException("Invalid progress multiplier");scale*=value;}
        return Math.min(100,scale);
    }
    public static void after(Change change) {for(var handler:List.copyOf(AFTER)) handler.accept(change);}
    public static void requireServerThread(ServerPlayer player) {
        if(player.getServer()==null||!player.getServer().isSameThread()) throw new IllegalStateException("Core API requires server thread");
    }
}
