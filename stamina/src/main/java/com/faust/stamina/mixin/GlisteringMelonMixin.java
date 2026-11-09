package com.faust.stamina.mixin;

import com.faust.core.runtime.CoreRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds a use action to the otherwise inedible brewing ingredient. */
@Mixin(Item.class)
public abstract class GlisteringMelonMixin {
    @Inject(method="use",at=@At("HEAD"),cancellable=true)
    private void core$use(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci){
        var stack=player.getItemInHand(hand);
        if(com.faust.core.progression.StaminaServices.present()&&stack.is(Items.GLISTERING_MELON_SLICE)){player.startUsingItem(hand);ci.setReturnValue(InteractionResultHolder.consume(stack));}
    }
    @Inject(method="getUseDuration",at=@At("HEAD"),cancellable=true)
    private void core$duration(ItemStack stack,LivingEntity entity,CallbackInfoReturnable<Integer> ci){if(com.faust.core.progression.StaminaServices.present()&&stack.is(Items.GLISTERING_MELON_SLICE))ci.setReturnValue(32);}
    @Inject(method="getUseAnimation",at=@At("HEAD"),cancellable=true)
    private void core$animation(ItemStack stack,CallbackInfoReturnable<UseAnim> ci){if(com.faust.core.progression.StaminaServices.present()&&stack.is(Items.GLISTERING_MELON_SLICE))ci.setReturnValue(UseAnim.EAT);}
    @Inject(method="finishUsingItem",at=@At("HEAD"),cancellable=true)
    private void core$finish(ItemStack stack,Level level,LivingEntity entity,CallbackInfoReturnable<ItemStack> ci){
        if(com.faust.core.progression.StaminaServices.present()&&stack.is(Items.GLISTERING_MELON_SLICE)){
            if(entity instanceof ServerPlayer player){CoreRuntime.food(player,stack);if(!player.isCreative())stack.shrink(1);}
            ci.setReturnValue(stack);
        }
    }
}
