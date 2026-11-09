package com.faust.stamina.mixin;

import com.faust.core.runtime.CoreRuntime;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class SleepMixin {
    @Inject(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V",shift=At.Shift.AFTER))
    private void core$night(java.util.function.BooleanSupplier timeLeft,CallbackInfo ci){CoreRuntime.sleptNight((ServerLevel)(Object)this);}
}
