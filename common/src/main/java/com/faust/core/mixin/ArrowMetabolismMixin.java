package com.faust.core.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(net.minecraft.world.entity.projectile.Arrow.class)
public abstract class ArrowMetabolismMixin {
 @WrapOperation(method="doPostHurtEffects",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
 private boolean essence$potion(net.minecraft.world.entity.LivingEntity target,net.minecraft.world.effect.MobEffectInstance effect,net.minecraft.world.entity.Entity source,Operation<Boolean> original,@com.llamalad7.mixinextras.sugar.Local(ordinal=0) net.minecraft.world.effect.MobEffectInstance sourceEffect){return original.call(target,sourceEffect.isInfiniteDuration()?effect:com.faust.core.runtime.Metabolism.potion(target,effect),source);}
}
