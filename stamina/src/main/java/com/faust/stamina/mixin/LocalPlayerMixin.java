package com.faust.stamina.mixin;

import com.faust.core.client.CoreClient;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method="canStartSprinting",at=@At("RETURN"),cancellable=true)
    private void core$sprint(CallbackInfoReturnable<Boolean> ci){if(!CoreClient.canSprint(this))ci.setReturnValue(false);}
}
