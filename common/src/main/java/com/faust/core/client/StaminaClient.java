package com.faust.core.client;
/** Client-only optional HUD extension; never discovered on a dedicated server. */
public interface StaminaClient {
    default void beginHud(){}
    default void airSprite(net.minecraft.resources.ResourceLocation sprite,int y){}
    default void reset(){}
    default boolean lastAirDrawn(){return false;}
    default void hud(net.minecraft.client.gui.GuiGraphics graphics){}
}
