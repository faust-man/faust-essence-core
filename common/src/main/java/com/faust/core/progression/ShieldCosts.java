package com.faust.core.progression;
/** One complete HUD segment is two resource units. */
public final class ShieldCosts {
    private ShieldCosts(){}
    public static double block(boolean projectile,double meleeCost,double reduction){return Math.max(0,(projectile?meleeCost/2:meleeCost)-reduction);}
}
