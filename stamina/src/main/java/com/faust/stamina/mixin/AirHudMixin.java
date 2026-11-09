package com.faust.stamina.mixin;

import com.faust.core.client.CoreClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe actual air sprite draws, regardless of which mod requests them. */
@Mixin(GuiGraphics.class)
public abstract class AirHudMixin {
    @Inject(method="blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V",at=@At("HEAD"))
    private void core$air(ResourceLocation sprite,int x,int y,int width,int height,CallbackInfo ci){CoreClient.airSprite(sprite,y);}
    @Inject(method="blitSprite(Lnet/minecraft/resources/ResourceLocation;IIIII)V",at=@At("HEAD"))
    private void core$airWithDepth(ResourceLocation sprite,int x,int y,int depth,int width,int height,CallbackInfo ci){CoreClient.airSprite(sprite,y);}
}
