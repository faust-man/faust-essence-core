package com.faust.core.mixin;
import com.faust.core.runtime.CoreRuntime;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowMixin {
 @Inject(method="canEntityWalkOnPowderSnow",at=@At("HEAD"),cancellable=true)
 private static void core$walk(Entity entity,CallbackInfoReturnable<Boolean> ci){if(entity.level().isClientSide?com.faust.core.runtime.ClientActions.softFoot.test(entity):entity instanceof ServerPlayer p&&CoreRuntime.active(p)&&CoreRuntime.data(p).enabled("faustcore:soft_foot",CoreRuntime.balance))ci.setReturnValue(true);}
}
