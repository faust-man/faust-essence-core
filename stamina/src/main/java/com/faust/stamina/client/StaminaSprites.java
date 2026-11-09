package com.faust.stamina.client;

/** Neutral 16px lightning masks, tinted independently for each half-unit. */
public final class StaminaSprites {
    public static final int SIZE = 16;
    public static final int GUI_SIZE = 8;
    public static final int STAMINA_COLOR = 0xFFFFD000;
    public static final int FATIGUE_COLOR = 0xFF000000;
    public static final int EMPTY_COLOR = 0xFF666666;
    public static final int EXHAUSTED_COLOR = 0xFFFF4040;

    private StaminaSprites() {}

    /** 0 = filled, 1 = spent, 2 = unavailable due to fatigue. */
    public static int state(int unit, int available, int filled) {
        return unit >= available ? 2 : unit < filled ? 0 : 1;
    }

    public static int color(int state, boolean exhausted) {
        if (state == 2) return FATIGUE_COLOR;
        if (exhausted) return EXHAUSTED_COLOR;
        return state == 0 ? STAMINA_COLOR : EMPTY_COLOR;
    }

    public static String texture(int state) {
        return "textures/gui/stamina_lightning_" + (state == 1 ? "empty" : "full") + ".png";
    }

    public static int textureX(int state, int half) {
        return half * SIZE / 2;
    }
}
