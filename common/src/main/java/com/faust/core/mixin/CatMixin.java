package com.faust.core.mixin;
import com.faust.core.runtime.CoreRuntime;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(net.minecraft.world.entity.TamableAnimal.class)
public abstract class CatMixin {
 @Inject(method="tame",at=@At("RETURN"))
 private void core$tamed(net.minecraft.world.entity.player.Player owner,CallbackInfo ci){if((Object)this instanceof Cat&&owner instanceof ServerPlayer p&&CoreRuntime.active(p))CoreRuntime.data(p).recordCounter("cats_tamed",1,CoreRuntime.balance);}
}
