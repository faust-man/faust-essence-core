package com.faust.core.progression;

/** Shared initial grace period, measured in persistent overworld game ticks. */
public final class FatigueRules {
    private FatigueRules() {}
    public static boolean accumulates(long worldAgeTicks,Balance balance){
        return Math.max(0,worldAgeTicks)>=balance.fatigue.get("worldGraceSeconds")*20;
    }
}
