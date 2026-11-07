package com.faust.core.progression;

/** Vanilla bubble visibility, including depleted air outside water. */
public final class Breathing {
    private Breathing() {}
    public static boolean visible(boolean eyesInWater,int air,int maximum) {
        return maximum>0&&(eyesInWater||air<maximum)&&Math.ceil(10.0*Math.min(air,maximum)/maximum)>0;
    }
}
