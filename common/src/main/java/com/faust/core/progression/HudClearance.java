package com.faust.core.progression;
/** Uses actual sprite positions, including every mount-health row. */
public final class HudClearance {
    private HudClearance() {}
    public static boolean vehicleHeart(String namespace,String path){return namespace.equals("minecraft")&&java.util.Set.of("hud/heart/vehicle_container","hud/heart/vehicle_full","hud/heart/vehicle_half").contains(path);}
    public static int staminaY(int preferred,int airTop,int vehicleTop){
        int top=Math.min(airTop,vehicleTop);
        return top==Integer.MAX_VALUE?preferred:Math.min(preferred,top-9);
    }
}
