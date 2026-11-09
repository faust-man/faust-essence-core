package com.faust.stamina.mixin;
import com.faust.core.runtime.CoreRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin({SnowballItem.class,EggItem.class,EnderpearlItem.class,WindChargeItem.class,ExperienceBottleItem.class,ThrowablePotionItem.class})
public abstract class ThrownItemMixin {
    @Inject(method="use",at=@At("HEAD"),cancellable=true)
    private void core$throw(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci){
        if(player instanceof ServerPlayer server&&!CoreRuntime.pay(server,CoreRuntime.balance.stamina.get("throwCost")))ci.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
    }
}
