package com.faust.stamina.mixin;
import com.faust.core.client.CoreClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Gui.class)
public abstract class GuiMixin {
 @Inject(method="render",at=@At("HEAD")) private void core$begin(GuiGraphics g,DeltaTracker delta,CallbackInfo ci){CoreClient.beginHud();}
 @Inject(method="render",at=@At("TAIL")) private void core$hud(GuiGraphics g,DeltaTracker delta,CallbackInfo ci){CoreClient.hud(g);}
}
