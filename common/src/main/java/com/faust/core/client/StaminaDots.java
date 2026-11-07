package com.faust.core.client;

/** Explicit pixel colors for the exhausted HUD; independent of global shader tint. */
public final class StaminaDots {
    private static final int[] OUTER={0x3c,0x7e,0xff,0xff,0xff,0xff,0x7e,0x3c};
    private static final int[] INNER={0,0x3c,0x7e,0x7e,0x7e,0x7e,0x3c,0};
    private StaminaDots() {}
    public static int redPixel(int state,int x,int y){
        int bit=1<<x;
        if((OUTER[y]&bit)==0||(state==1&&(INNER[y]&bit)!=0))return 0;
        return 0xFFFF4040;
    }
}
