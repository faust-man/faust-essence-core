package com.faust.core.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
@Mixin(net.minecraft.world.item.PotionItem.class)
public abstract class DrinkMetabolismMixin {
 @WrapOperation(method="finishUsingItem",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/alchemy/PotionContents;forEachEffect(Ljava/util/function/Consumer;)V"))
 private void essence$potion(PotionContents contents,java.util.function.Consumer<MobEffectInstance> consumer,Operation<Void> original,@Local(argsOnly=true) net.minecraft.world.entity.LivingEntity recipient){
  original.call(contents,(java.util.function.Consumer<MobEffectInstance>)effect->consumer.accept(com.faust.core.runtime.Metabolism.potion(recipient,effect)));
 }
}
