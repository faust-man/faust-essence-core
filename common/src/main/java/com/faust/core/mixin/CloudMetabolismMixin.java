package com.faust.core.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(net.minecraft.world.entity.AreaEffectCloud.class)
public abstract class CloudMetabolismMixin {
 @org.spongepowered.asm.mixin.Shadow private net.minecraft.world.item.alchemy.PotionContents potionContents;
@WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
 private boolean essence$potion(net.minecraft.world.entity.LivingEntity target,net.minecraft.world.effect.MobEffectInstance effect,net.minecraft.world.entity.Entity source,Operation<Boolean> original){return original.call(target,potionContents.hasEffects()?com.faust.core.runtime.Metabolism.potion(target,effect):effect,source);}
}
