package com.faust.core.progression;

/** Optional stamina model. The default implementation leaves saved resource data untouched. */
public interface StaminaMechanics {
    default void tick(net.minecraft.server.level.ServerPlayer player,Progress p,com.faust.core.runtime.CoreRuntime.Session s,Balance b) {}
    default void prepare(Balance b) {}
    default void defaults(Balance b,int stage) {}
    default double baseStamina(Progress p, Balance b) {return 20;}
    default double maxFatigue(Progress p, Balance b) {return 0;}
    default double maxStamina(Progress p, Balance b) {return 20;}
    default double capacity(Progress p, Balance b) {return 0;}
    default double spend(Progress p, double requested,Balance b) {return 0;}
    default double spend(Progress p, double requested,Balance b,long worldAgeTicks) {return 0;}
    default void rest(Progress p, double dt,boolean sleeping,boolean campfire,boolean regenerate,Balance b) {}
    default void rest(Progress p, double dt,boolean sleeping,boolean campfire,boolean regenerate,double recoveryScale,Balance b) {}
    default void environmentalFatigue(Progress p, double dt,boolean soulCampfire,boolean soulFire,Balance b) {}
    default void environmentalFatigue(Progress p, double dt,boolean soulCampfire,boolean soulFire,Balance b,long worldAgeTicks) {}
    default void recover(Progress p, double dt,boolean sleep,boolean campfire,boolean delayReady,boolean shielding,Balance b) {}
    default void recover(Progress p, double dt,boolean sleep,boolean campfire,boolean delayReady,boolean shielding,boolean flying,boolean airVisible,Balance b) {}
    default void recover(Progress p, double dt,boolean sleep,boolean campfire,boolean delayReady,boolean shielding,boolean flying,boolean airVisible,boolean fastSwimming,Balance b) {}
    default double exhaustionThreshold(Progress p, Balance b) {return 0;}
    default boolean empty(Progress p, Balance b) {return false;}
    default boolean actionsBlocked(Progress p, Balance b) {return false;}
    default double actionCost(Progress p, String action,Balance b) {return 0;}
    default void sleptNight(Progress p, Balance b) {}
    default void secondWind(Progress p, double dt,Balance b) {}
    default void sweetFood(Progress p, String item,Balance b) {}
    default void sweetFood(Progress p, String item,double count,Balance b) {}
    default void updateExhaustion(Progress p, Balance b) {p.exhausted=false;}
}
