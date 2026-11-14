package com.faust.core.mixin;
import com.faust.core.runtime.CoreRuntime;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(FishingHook.class)
public class FishingPracticeMixin {
 @Inject(method="retrieve",at=@At(value="INVOKE",target="Lnet/minecraft/advancements/critereon/FishingRodHookedTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/projectile/FishingHook;Ljava/util/Collection;)V",ordinal=1))
 private void core$catch(ItemStack rod,CallbackInfoReturnable<Integer> ci){if(((FishingHook)(Object)this).getPlayerOwner() instanceof ServerPlayer p&&CoreRuntime.active(p))CoreRuntime.data(p).recordCounter("fish_caught",1,CoreRuntime.balance);}
}
